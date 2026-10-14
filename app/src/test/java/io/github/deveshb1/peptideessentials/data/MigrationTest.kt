package io.github.deveshb1.peptideessentials.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MigrationTest {
    private val v1Backup = """
        {
          "app": "dose-log",
          "version": 1,
          "medications": [{
            "id": "m1", "name": "Weekly pen", "doseAmount": 0.5, "unit": "mg",
            "schedule": {"type": "interval", "days": 7}, "timeOfDay": "08:00", "startDate": "2026-09-01",
            "notes": "", "color": 2, "usesInjectionSites": true, "reminders": true, "paused": false,
            "trackedFrom": "2026-09-01"
          }],
          "entries": [
            {"id": "e1", "medicationId": "m1", "medicationName": "Weekly pen", "scheduledDate": "2026-09-01",
             "status": "TAKEN", "time": "2026-09-01T08:05:00Z", "amount": 0.5, "unit": "mg",
             "site": "ABDOMEN_LEFT", "note": ""},
            {"id": "e2", "medicationId": "m1", "medicationName": "Weekly pen", "scheduledDate": "2026-09-08",
             "status": "TAKEN", "time": "2026-09-08T08:05:00Z", "amount": 0.5, "unit": "mg",
             "site": "BUTTOCK_RIGHT", "note": ""},
            {"id": "e3", "medicationId": "m1", "medicationName": "Weekly pen", "scheduledDate": "2026-09-15",
             "status": "TAKEN", "time": "2026-09-15T08:05:00Z", "amount": 0.5, "unit": "mg",
             "site": "THIGH_LEFT", "note": ""},
            {"id": "e4", "medicationId": "m1", "medicationName": "Weekly pen", "scheduledDate": "2026-09-22",
             "status": "SKIPPED", "time": "2026-09-22T08:05:00Z", "amount": 0.5, "unit": "mg",
             "site": null, "note": "travel"}
          ]
        }
    """.trimIndent()

    @Test
    fun version1BackupMigratesSitesAndDefaults() {
        val data = JsonCodec.decode(v1Backup)
        val med = data.medications.single()
        assertEquals(Schedule.EveryNDays(7), med.schedule)
        assertEquals(LocalTime.of(8, 0), med.timeOfDay)
        assertEquals(
            listOf(InjectionSite.ABDOMEN_LOWER_LEFT, InjectionSite.GLUTE_RIGHT, InjectionSite.THIGH_LEFT, null),
            data.entries.map { it.site },
        )
        assertTrue(data.entries.all { it.vialId == null })
        assertEquals("travel", data.entries.last().note)
        assertTrue(data.vials.isEmpty() && data.measurements.isEmpty() && data.journal.isEmpty())
        assertEquals(Settings(), data.settings)
    }

    @Test
    fun migratedDataSavesAsCurrentSchema() {
        val reencoded = JsonCodec.encode(JsonCodec.decode(v1Backup))
        assertTrue(reencoded.contains("\"version\": ${JsonCodec.SCHEMA_VERSION}"))
        assertTrue(reencoded.contains("ABDOMEN_LOWER_LEFT"))
        assertEquals(JsonCodec.decode(v1Backup), JsonCodec.decode(reencoded))
    }

    @Test
    fun version2RoundTripKeepsEverything() {
        val day = LocalDate.of(2026, 10, 14)
        val original = JsonCodec.decode(v1Backup).copy(
            vials = listOf(Vial("v1", "m1", 5.0, VialUnit.MG, 2.0, day, day.plusDays(28), label = "Lot 7")),
            customMetrics = listOf(Metric("c1", "Resting HR", "bpm")),
            measurements = listOf(Measurement("x1", Metric.WEIGHT_ID, day, 82.4), Measurement("x2", "c1", day, 61.0)),
            journal = listOf(
                JournalEntry(
                    date = day,
                    energy = 4,
                    sleep = null,
                    appetite = 2,
                    mood = 5,
                    sideEffects = setOf(SideEffect.NAUSEA.name, SideEffect.customKey("Dry mouth")),
                    reactionSite = InjectionSite.THIGH_RIGHT,
                    note = "ok",
                ),
            ),
            customSideEffects = listOf("Dry mouth"),
            settings = Settings(WeightUnit.LB, LengthUnit.IN, 21, mapOf(InjectionSite.GLUTE_LEFT to 10), dynamicColor = true),
        )
        assertEquals(original, JsonCodec.decode(JsonCodec.encode(original)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherFiles() {
        JsonCodec.decode("""{"app": "something-else", "version": 1, "medications": [], "entries": []}""")
    }

    @Test
    fun siteNameMapping() {
        assertNull(InjectionSite.fromName("ELBOW"))
        assertEquals(InjectionSite.GLUTE_LEFT, InjectionSite.fromName("BUTTOCK_LEFT"))
        assertEquals(InjectionSite.THIGH_RIGHT, InjectionSite.fromName("THIGH_RIGHT"))
    }
}
