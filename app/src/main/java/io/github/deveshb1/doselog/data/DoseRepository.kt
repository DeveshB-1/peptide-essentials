package io.github.deveshb1.doselog.data

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

/** Single source of truth for medications and dose history, persisted as JSON in app-private storage. */
class DoseRepository(file: File) {
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

    suspend fun saveMedication(medication: Medication) = update { data ->
        val exists = data.medications.any { it.id == medication.id }
        data.copy(
            medications = if (exists) {
                data.medications.map { if (it.id == medication.id) medication else it }
            } else {
                data.medications + medication
            },
        )
    }

    /** Removes the medication but keeps its history entries. */
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

    /** Logs a dose; returns null if that scheduled dose was already logged. */
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
            )
            created = entry
            data.copy(entries = data.entries + entry)
        }
        return created
    }

    /** Updates an existing entry, or re-adds one (used to undo a delete). */
    suspend fun saveEntry(entry: DoseEntry) = update { data ->
        val exists = data.entries.any { it.id == entry.id }
        data.copy(
            entries = if (exists) data.entries.map { if (it.id == entry.id) entry else it } else data.entries + entry,
        )
    }

    suspend fun deleteEntry(id: String) = update { data ->
        data.copy(entries = data.entries.filterNot { it.id == id })
    }

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
        private const val TAG = "DoseRepository"
        fun newId(): String = UUID.randomUUID().toString()
    }
}
