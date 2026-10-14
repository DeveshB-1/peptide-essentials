package io.github.deveshb1.peptideessentials.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Reads and writes [AppData] as JSON, for the private data file and for backups.
 *
 * Schema history:
 * - 1 (Dose Log): medications and dose entries; four injection sites per side pair.
 * - 2: vials, measurements, journal, settings; abdomen split into upper/lower. Version 1 files are
 *   migrated on read (see [InjectionSite.fromName] for the site mapping).
 */
object JsonCodec {
    const val SCHEMA_VERSION = 2
    private const val APP_ID = "peptide-essentials"
    private const val LEGACY_APP_ID = "dose-log"

    fun encode(data: AppData): String = JSONObject()
        .put("app", APP_ID)
        .put("version", SCHEMA_VERSION)
        .put("medications", JSONArray(data.medications.map(::encodeMedication)))
        .put("entries", JSONArray(data.entries.map(::encodeEntry)))
        .put("vials", JSONArray(data.vials.map(::encodeVial)))
        .put("customMetrics", JSONArray(data.customMetrics.map(::encodeMetric)))
        .put("measurements", JSONArray(data.measurements.map(::encodeMeasurement)))
        .put("journal", JSONArray(data.journal.map(::encodeJournal)))
        .put("customSideEffects", JSONArray(data.customSideEffects))
        .put("settings", encodeSettings(data.settings))
        .toString(2)

    /** @throws org.json.JSONException or [IllegalArgumentException] if the text isn't a backup from this app. */
    fun decode(text: String): AppData {
        val root = JSONObject(text)
        require(root.optString("app") in setOf(APP_ID, LEGACY_APP_ID)) { "Not a Peptide Essentials file" }
        val version = root.optInt("version", 0)
        require(version in 1..SCHEMA_VERSION) { "Unsupported version $version" }
        val base = AppData(
            medications = root.getJSONArray("medications").objects().map(::decodeMedication),
            entries = root.getJSONArray("entries").objects().map(::decodeEntry),
        )
        if (version == 1) return base
        return base.copy(
            vials = root.optArray("vials").objects().map(::decodeVial),
            customMetrics = root.optArray("customMetrics").objects().map(::decodeMetric),
            measurements = root.optArray("measurements").objects().map(::decodeMeasurement),
            journal = root.optArray("journal").objects().map(::decodeJournal),
            customSideEffects = root.optArray("customSideEffects").let { a -> (0 until a.length()).map(a::getString) },
            settings = root.optJSONObject("settings")?.let(::decodeSettings) ?: Settings(),
        )
    }

    private fun encodeMedication(m: Medication) = JSONObject()
        .put("id", m.id)
        .put("name", m.name)
        .put("doseAmount", m.doseAmount)
        .put("unit", m.unit.key)
        .put("schedule", encodeSchedule(m.schedule))
        .put("timeOfDay", m.timeOfDay.toString())
        .put("startDate", m.startDate.toString())
        .put("notes", m.notes)
        .put("color", m.colorIndex)
        .put("usesInjectionSites", m.usesInjectionSites)
        .put("reminders", m.remindersEnabled)
        .put("paused", m.paused)
        .put("trackedFrom", m.trackedFrom.toString())

    private fun decodeMedication(o: JSONObject): Medication {
        val start = LocalDate.parse(o.getString("startDate"))
        return Medication(
            id = o.getString("id"),
            name = o.getString("name"),
            doseAmount = o.getDouble("doseAmount"),
            unit = DoseUnit.fromKey(o.getString("unit")),
            schedule = decodeSchedule(o.getJSONObject("schedule")),
            timeOfDay = LocalTime.parse(o.getString("timeOfDay")),
            startDate = start,
            notes = o.optString("notes"),
            colorIndex = o.optInt("color"),
            usesInjectionSites = o.optBoolean("usesInjectionSites"),
            remindersEnabled = o.optBoolean("reminders", true),
            paused = o.optBoolean("paused"),
            trackedFrom = o.optStringOrNull("trackedFrom")?.let(LocalDate::parse) ?: start,
        )
    }

    private fun encodeSchedule(s: Schedule): JSONObject = when (s) {
        Schedule.Daily -> JSONObject().put("type", "daily")
        is Schedule.Weekdays -> JSONObject()
            .put("type", "weekdays")
            .put("days", JSONArray(s.days.sorted().map { it.value }))
        is Schedule.EveryNDays -> JSONObject().put("type", "interval").put("days", s.interval)
    }

    private fun decodeSchedule(o: JSONObject): Schedule = when (val type = o.getString("type")) {
        "daily" -> Schedule.Daily
        "weekdays" -> {
            val days = o.getJSONArray("days")
            Schedule.Weekdays((0 until days.length()).map { DayOfWeek.of(days.getInt(it)) }.toSet())
        }
        "interval" -> Schedule.EveryNDays(o.getInt("days"))
        else -> throw IllegalArgumentException("Unknown schedule type $type")
    }

    private fun encodeEntry(e: DoseEntry) = JSONObject()
        .put("id", e.id)
        .put("medicationId", e.medicationId)
        .put("medicationName", e.medicationName)
        .put("scheduledDate", e.scheduledDate?.toString() ?: JSONObject.NULL)
        .put("status", e.status.name)
        .put("time", e.time.toString())
        .put("amount", e.amount)
        .put("unit", e.unit.key)
        .put("site", e.site?.name ?: JSONObject.NULL)
        .put("note", e.note)
        .put("vialId", e.vialId ?: JSONObject.NULL)

    private fun decodeEntry(o: JSONObject) = DoseEntry(
        id = o.getString("id"),
        medicationId = o.getString("medicationId"),
        medicationName = o.getString("medicationName"),
        scheduledDate = o.optStringOrNull("scheduledDate")?.let(LocalDate::parse),
        status = DoseStatus.valueOf(o.getString("status")),
        time = Instant.parse(o.getString("time")),
        amount = o.getDouble("amount"),
        unit = DoseUnit.fromKey(o.getString("unit")),
        site = InjectionSite.fromName(o.optStringOrNull("site")),
        note = o.optString("note"),
        vialId = o.optStringOrNull("vialId"),
    )

    private fun encodeVial(v: Vial) = JSONObject()
        .put("id", v.id)
        .put("medicationId", v.medicationId)
        .put("amount", v.amount)
        .put("unit", v.unit.key)
        .put("diluentMl", v.diluentMl)
        .put("mixedOn", v.mixedOn.toString())
        .put("useBy", v.useBy.toString())
        .put("label", v.label)
        .put("notes", v.notes)
        .put("status", v.status.name)
        .put("closedOn", v.closedOn?.toString() ?: JSONObject.NULL)

    private fun decodeVial(o: JSONObject) = Vial(
        id = o.getString("id"),
        medicationId = o.getString("medicationId"),
        amount = o.getDouble("amount"),
        unit = VialUnit.fromKey(o.getString("unit")),
        diluentMl = o.getDouble("diluentMl"),
        mixedOn = LocalDate.parse(o.getString("mixedOn")),
        useBy = LocalDate.parse(o.getString("useBy")),
        label = o.optString("label"),
        notes = o.optString("notes"),
        status = VialStatus.valueOf(o.getString("status")),
        closedOn = o.optStringOrNull("closedOn")?.let(LocalDate::parse),
    )

    private fun encodeMetric(m: Metric) = JSONObject().put("id", m.id).put("name", m.name).put("unit", m.unitLabel)

    private fun decodeMetric(o: JSONObject) = Metric(o.getString("id"), o.getString("name"), o.optString("unit"))

    private fun encodeMeasurement(m: Measurement) = JSONObject()
        .put("id", m.id)
        .put("metricId", m.metricId)
        .put("date", m.date.toString())
        .put("value", m.value)
        .put("note", m.note)

    private fun decodeMeasurement(o: JSONObject) = Measurement(
        id = o.getString("id"),
        metricId = o.getString("metricId"),
        date = LocalDate.parse(o.getString("date")),
        value = o.getDouble("value"),
        note = o.optString("note"),
    )

    private fun encodeJournal(j: JournalEntry) = JSONObject()
        .put("date", j.date.toString())
        .put("energy", j.energy ?: JSONObject.NULL)
        .put("sleep", j.sleep ?: JSONObject.NULL)
        .put("appetite", j.appetite ?: JSONObject.NULL)
        .put("mood", j.mood ?: JSONObject.NULL)
        .put("sideEffects", JSONArray(j.sideEffects.sorted()))
        .put("reactionSite", j.reactionSite?.name ?: JSONObject.NULL)
        .put("note", j.note)

    private fun decodeJournal(o: JSONObject) = JournalEntry(
        date = LocalDate.parse(o.getString("date")),
        energy = o.optRating("energy"),
        sleep = o.optRating("sleep"),
        appetite = o.optRating("appetite"),
        mood = o.optRating("mood"),
        sideEffects = o.optArray("sideEffects").let { a -> (0 until a.length()).map(a::getString).toSet() },
        reactionSite = InjectionSite.fromName(o.optStringOrNull("reactionSite")),
        note = o.optString("note"),
    )

    private fun encodeSettings(s: Settings) = JSONObject()
        .put("weightUnit", s.weightUnit.name)
        .put("lengthUnit", s.lengthUnit.name)
        .put("useByDays", s.useByDays)
        .put("dynamicColor", s.dynamicColor)
        .put("siteRestDays", JSONObject().apply { s.siteRestDays.forEach { (site, days) -> put(site.name, days) } })

    private fun decodeSettings(o: JSONObject): Settings {
        val rest = o.optJSONObject("siteRestDays")
        return Settings(
            weightUnit = WeightUnit.entries.firstOrNull { it.name == o.optString("weightUnit") } ?: WeightUnit.KG,
            lengthUnit = LengthUnit.entries.firstOrNull { it.name == o.optString("lengthUnit") } ?: LengthUnit.CM,
            useByDays = o.optInt("useByDays", Settings.DEFAULT_USE_BY_DAYS),
            dynamicColor = o.optBoolean("dynamicColor"),
            siteRestDays = rest?.keys()?.asSequence()
                ?.mapNotNull { key -> InjectionSite.fromName(key)?.let { it to rest.getInt(key) } }
                ?.toMap()
                .orEmpty(),
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

    private fun JSONObject.optArray(key: String): JSONArray = optJSONArray(key) ?: JSONArray()

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optRating(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key).takeIf { it in 1..5 } else null
}
