package io.github.deveshb1.doselog.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Reads and writes [AppData] as JSON. Used for the private data file and for backups. */
object JsonCodec {
    private const val FORMAT_VERSION = 1

    fun encode(data: AppData): String = JSONObject()
        .put("app", "dose-log")
        .put("version", FORMAT_VERSION)
        .put("medications", JSONArray(data.medications.map(::encodeMedication)))
        .put("entries", JSONArray(data.entries.map(::encodeEntry)))
        .toString(2)

    /** @throws org.json.JSONException or [IllegalArgumentException] if the text isn't a Dose Log file. */
    fun decode(text: String): AppData {
        val root = JSONObject(text)
        require(root.optString("app") == "dose-log") { "Not a Dose Log file" }
        require(root.optInt("version", 0) in 1..FORMAT_VERSION) { "Unsupported version" }
        return AppData(
            medications = root.getJSONArray("medications").objects().map(::decodeMedication),
            entries = root.getJSONArray("entries").objects().map(::decodeEntry),
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
    )

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
