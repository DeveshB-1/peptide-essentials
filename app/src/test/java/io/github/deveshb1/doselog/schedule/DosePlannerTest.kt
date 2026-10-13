package io.github.deveshb1.doselog.schedule

import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DosePlannerTest {
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 10, 13, 12, 0, 0, 0, zone)

    @Test
    fun dailyMedicationShowsMissedDaysSinceLastLog() {
        val m = med(Schedule.Daily, start = LocalDate.of(2026, 9, 1))
        val data = AppData(listOf(m), listOf(entry(date = LocalDate.of(2026, 10, 10))))
        val plan = DosePlanner.plan(data, now)
        assertEquals(listOf(11, 12).map { LocalDate.of(2026, 10, it) }, plan.overdue.map { it.date })
        assertEquals(listOf(LocalDate.of(2026, 10, 13)), plan.dueToday.map { it.date })
    }

    @Test
    fun overdueIsLimitedToLookbackWindow() {
        val m = med(Schedule.Daily, start = LocalDate.of(2026, 1, 1))
        val plan = DosePlanner.plan(AppData(listOf(m)), now)
        assertEquals(DosePlanner.LOOKBACK_DAYS.toInt(), plan.overdue.size)
    }

    @Test
    fun noOverdueBeforeMedicationWasAdded() {
        val m = med(Schedule.EveryNDays(7), start = LocalDate.of(2026, 9, 1)).copy(trackedFrom = LocalDate.of(2026, 10, 13))
        val plan = DosePlanner.plan(AppData(listOf(m)), now)
        assertTrue(plan.overdue.isEmpty())
    }

    @Test
    fun skippedTodayIsNotDueAndAppearsAsLogged() {
        val m = med(Schedule.Daily, start = LocalDate.of(2026, 10, 13))
        val skip = entry(date = LocalDate.of(2026, 10, 13), status = DoseStatus.SKIPPED)
        val plan = DosePlanner.plan(AppData(listOf(m), listOf(skip)), now)
        assertTrue(plan.dueToday.isEmpty())
        assertEquals(listOf(skip), plan.loggedToday)
    }

    @Test
    fun pausedMedicationsAreHidden() {
        val m = med(Schedule.Daily, paused = true)
        val plan = DosePlanner.plan(AppData(listOf(m)), now)
        assertTrue(plan.overdue.isEmpty() && plan.dueToday.isEmpty() && plan.upcoming.isEmpty())
    }

    @Test
    fun upcomingIsSortedAndLimited() {
        val weekly = med(Schedule.EveryNDays(7), start = LocalDate.of(2026, 10, 15), id = "a")
        val daily = med(Schedule.Daily, start = LocalDate.of(2026, 10, 1), id = "b")
        val plan = DosePlanner.plan(AppData(listOf(weekly, daily)), now, upcomingLimit = 4)
        assertEquals(
            listOf("b" to 14, "a" to 15, "b" to 15, "b" to 16),
            plan.upcoming.map { it.medication.id to it.date.dayOfMonth },
        )
    }
}
