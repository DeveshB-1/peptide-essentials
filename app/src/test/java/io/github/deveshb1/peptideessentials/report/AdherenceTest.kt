package io.github.deveshb1.peptideessentials.report

import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.Schedule
import io.github.deveshb1.peptideessentials.schedule.entry
import io.github.deveshb1.peptideessentials.schedule.med
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AdherenceTest {
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun countsTakenSkippedAndMissed() {
        val m = med(Schedule.Daily, start = LocalDate.of(2026, 10, 1))
        val entries = listOf(
            entry(date = LocalDate.of(2026, 10, 10)),
            entry(date = LocalDate.of(2026, 10, 11)),
            entry(date = LocalDate.of(2026, 10, 12), status = DoseStatus.SKIPPED),
        )
        val a = AdherenceCalc.compute(m, entries, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 13), today)
        assertEquals(Adherence(scheduled = 4, taken = 2, skipped = 1), a)
        assertEquals(1, a.missed)
        assertEquals(50, a.percent)
    }

    @Test
    fun todayCountsOnlyOnceLogged() {
        val m = med(Schedule.Daily, start = LocalDate.of(2026, 10, 13))
        assertEquals(1, AdherenceCalc.compute(m, emptyList(), today.minusDays(5), today, today).scheduled)
        val logged = listOf(entry(date = today))
        assertEquals(Adherence(2, 1, 0), AdherenceCalc.compute(m, logged, today.minusDays(5), today, today))
    }

    @Test
    fun weeklyScheduleFromTrackedDate() {
        val m = med(Schedule.EveryNDays(7), start = LocalDate.of(2026, 9, 1)).copy(trackedFrom = LocalDate.of(2026, 9, 20))
        // Doses on 22 and 29 Sep, 6 and 13 Oct fall after tracking began.
        val entries = listOf(entry(date = LocalDate.of(2026, 9, 22)), entry(date = LocalDate.of(2026, 10, 6)))
        val a = AdherenceCalc.compute(m, entries, LocalDate.of(2026, 9, 1), today, today)
        assertEquals(Adherence(4, 2, 0), a)
        assertEquals(50, a.percent)
    }

    @Test
    fun nothingScheduledHasNoPercent() {
        val m = med(Schedule.Daily, start = today.plusDays(3))
        assertNull(AdherenceCalc.compute(m, emptyList(), today.minusDays(30), today, today).percent)
    }
}
