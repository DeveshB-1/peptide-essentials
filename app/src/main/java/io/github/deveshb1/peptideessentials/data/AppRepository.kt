package io.github.deveshb1.peptideessentials.data

import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Single source of truth for everything the app stores, persisted as JSON in app-private storage. */
class AppRepository(file: File) {
    private val store = AtomicFile(file)
    private val mutex = Mutex()
    private val _data = MutableStateFlow(AppData())
    private val _loaded = MutableStateFlow(false)

    val data: StateFlow<AppData> = _data.asStateFlow()
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun ensureLoaded(): AppData = mutex.withLock {
        if (!_loaded.value) {
            _data.value = withContext(Dispatchers.IO) { read() }
            _loaded.value = true
        }
        _data.value
    }

    // Medications

    suspend fun saveMedication(medication: Medication) = update { it.copy(medications = it.medications.upsert(medication) { m -> m.id }) }

    /** Removes the medication but keeps its history and vials. */
    suspend fun deleteMedication(id: String) = update { data ->
        data.copy(medications = data.medications.filterNot { it.id == id })
    }

    suspend fun setPaused(id: String, paused: Boolean) = update { data ->
        data.copy(
            medications = data.medications.map { med ->
                when {
                    med.id != id || med.paused == paused -> med
                    paused -> med.copy(paused = true)
                    // Doses that fell inside the pause shouldn't show up as missed.
                    else -> med.copy(paused = false, trackedFrom = LocalDate.now())
                }
            },
        )
    }

    // Doses

    /** Logs a dose against the medication's active vial; returns null if that scheduled dose was already logged. */
    suspend fun logDose(
        medication: Medication,
        scheduledDate: LocalDate?,
        status: DoseStatus,
        time: Instant,
        amount: Double = medication.doseAmount,
        site: InjectionSite? = null,
        note: String = "",
    ): DoseEntry? {
        var created: DoseEntry? = null
        update { data ->
            val alreadyLogged = scheduledDate != null && data.entries.any {
                it.medicationId == medication.id && it.scheduledDate == scheduledDate
            }
            if (alreadyLogged) return@update data
            val entry = DoseEntry(
                id = newId(),
                medicationId = medication.id,
                medicationName = medication.name,
                scheduledDate = scheduledDate,
                status = status,
                time = time,
                amount = amount,
                unit = medication.unit,
                site = site,
                note = note,
                vialId = if (status == DoseStatus.TAKEN) data.activeVial(medication.id)?.id else null,
            )
            created = entry
            data.copy(entries = data.entries + entry)
        }
        return created
    }

    /** Updates an existing entry, or re-adds one (used to undo a delete). */
    suspend fun saveEntry(entry: DoseEntry) = update { it.copy(entries = it.entries.upsert(entry) { e -> e.id }) }

    suspend fun deleteEntry(id: String) = update { data -> data.copy(entries = data.entries.filterNot { it.id == id }) }

    // Vials

    /** Saves a vial. A newly opened vial closes any other open vial of the same medication as finished. */
    suspend fun saveVial(vial: Vial) = update { data ->
        val isNew = data.vials.none { it.id == vial.id }
        val others = if (isNew && vial.status == VialStatus.ACTIVE) {
            data.vials.map {
                if (it.medicationId == vial.medicationId && it.status == VialStatus.ACTIVE) {
                    it.copy(status = VialStatus.FINISHED, closedOn = LocalDate.now())
                } else {
                    it
                }
            }
        } else {
            data.vials
        }
        data.copy(vials = others.upsert(vial) { it.id })
    }

    suspend fun closeVial(id: String, status: VialStatus) = update { data ->
        data.copy(
            vials = data.vials.map {
                if (it.id == id) it.copy(status = status, closedOn = if (status == VialStatus.ACTIVE) null else LocalDate.now()) else it
            },
        )
    }

    /** Deletes a vial; doses drawn from it stay in history without a vial. */
    suspend fun deleteVial(id: String) = update { data ->
        data.copy(
            vials = data.vials.filterNot { it.id == id },
            entries = data.entries.map { if (it.vialId == id) it.copy(vialId = null) else it },
        )
    }

    // Progress

    suspend fun saveMetric(metric: Metric) = update { it.copy(customMetrics = it.customMetrics.upsert(metric) { m -> m.id }) }

    suspend fun deleteMetric(id: String) = update { data ->
        data.copy(
            customMetrics = data.customMetrics.filterNot { it.id == id },
            measurements = data.measurements.filterNot { it.metricId == id },
        )
    }

    suspend fun saveMeasurement(m: Measurement) = update { it.copy(measurements = it.measurements.upsert(m) { x -> x.id }) }

    suspend fun deleteMeasurement(id: String) = update { data ->
        data.copy(measurements = data.measurements.filterNot { it.id == id })
    }

    // Journal

    suspend fun saveJournal(entry: JournalEntry) = update { it.copy(journal = it.journal.upsert(entry) { j -> j.date }) }

    suspend fun deleteJournal(date: LocalDate) = update { data -> data.copy(journal = data.journal.filterNot { it.date == date }) }

    suspend fun addCustomSideEffect(label: String) = update { data ->
        if (label in data.customSideEffects) data else data.copy(customSideEffects = data.customSideEffects + label)
    }

    suspend fun removeCustomSideEffect(label: String) = update { data ->
        data.copy(customSideEffects = data.customSideEffects - label)
    }

    // Settings and restore

    suspend fun updateSettings(transform: (Settings) -> Settings) = update { it.copy(settings = transform(it.settings)) }

    suspend fun replaceAll(newData: AppData) = update { newData }

    private suspend fun update(transform: (AppData) -> AppData): AppData {
        ensureLoaded()
        return mutex.withLock {
            val updated = transform(_data.value)
            if (updated != _data.value) {
                withContext(Dispatchers.IO) { write(updated) }
                _data.value = updated
            }
            updated
        }
    }

    private fun read(): AppData {
        if (!store.baseFile.exists()) return AppData()
        return try {
            JsonCodec.decode(store.readFully().decodeToString())
        } catch (e: Exception) {
            // Keep the unreadable file aside rather than silently overwriting it.
            Log.e(TAG, "Could not read data file", e)
            store.baseFile.copyTo(File(store.baseFile.path + ".corrupt"), overwrite = true)
            AppData()
        }
    }

    private fun write(data: AppData) {
        val out = store.startWrite()
        try {
            out.write(JsonCodec.encode(data).encodeToByteArray())
            store.finishWrite(out)
        } catch (e: Exception) {
            store.failWrite(out)
            throw e
        }
    }

    companion object {
        private const val TAG = "AppRepository"
        fun newId(): String = UUID.randomUUID().toString()

        private fun <T, K> List<T>.upsert(item: T, key: (T) -> K): List<T> {
            val k = key(item)
            return if (any { key(it) == k }) map { if (key(it) == k) item else it } else this + item
        }
    }
}
