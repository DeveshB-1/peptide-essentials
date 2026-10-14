package io.github.deveshb1.peptideessentials.schedule

import io.github.deveshb1.peptideessentials.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ScheduleMathTest {
    private val newYork = ZoneId.of("America/New_York")
    private val london = ZoneId.of("Europe/London")

    @Test
    fun daily_nextOccurrence_isSameDayOrStart() {
        val m = med(Schedule.Daily, start = LocalDate.of(2026, 3, 10))
        assertEquals(LocalDate.of(2026, 3, 10), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 3, 1)))
        assertEquals(LocalDate.of(2026, 3, 15), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun daily_crossesMonthAndYearBoundaries() {
        val m = med(Schedule.Daily, start = LocalDate.of(2025, 12, 1))
        assertEquals(
            listOf(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)),
            ScheduleMath.occurrences(m, LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 2)),
        )
    }

    @Test
    fun weekdays_findsNextMatchingDay() {
        val m = med(Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)))
        // 2026-10-13 is a Tuesday.
        assertEquals(LocalDate.of(2026, 10, 15), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 13)))
        assertEquals(LocalDate.of(2026, 10, 19), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 16)))
        assertEquals(LocalDate.of(2026, 10, 15), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun weekdays_acrossMonthBoundary() {
        val m = med(Schedule.Weekdays(setOf(DayOfWeek.SUNDAY)))
        // 2026-10-31 is a Saturday; next Sunday is 1 November.
        assertEquals(LocalDate.of(2026, 11, 1), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun weekdays_emptySetNeverOccurs() {
        val m = med(Schedule.Weekdays(emptySet()))
        assertNull(ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 13)))
        assertTrue(ScheduleMath.occurrences(m, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).isEmpty())
    }

    @Test
    fun everyNDays_isAnchoredToStartDate() {
        val m = med(Schedule.EveryNDays(7), start = LocalDate.of(2026, 10, 1))
        assertEquals(LocalDate.of(2026, 10, 1), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 9, 1)))
        assertEquals(LocalDate.of(2026, 10, 8), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 2)))
        assertEquals(LocalDate.of(2026, 10, 8), ScheduleMath.nextOccurrence(m, LocalDate.of(2026, 10, 8)))
        assertTrue(ScheduleMath.occursOn(m, LocalDate.of(2026, 10, 15)))
        assertFalse(ScheduleMath.occursOn(m, LocalDate.of(2026, 10, 16)))
    }

    @Test
    fun everyNDays_acrossMonthEndsAndLeapDay() {
        val m = med(Schedule.EveryNDays(3), start = LocalDate.of(2028, 1, 30))
        assertEquals(LocalDate.of(2028, 2, 2), ScheduleMath.nextOccurrence(m, LocalDate.of(2028, 1, 31)))
        assertEquals(
            listOf(LocalDate.of(2028, 2, 26), LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 3)),
            ScheduleMath.occurrences(m, LocalDate.of(2028, 2, 25), LocalDate.of(2028, 3, 4)),
        )
    }

    @Test
    fun everyNDays_isNotShiftedByDstTransitions() {
        // Weekly dose spanning the US spring-forward (8 March 2026) and fall-back (1 November 2026).
        val m = med(Schedule.EveryNDays(7), start = LocalDate.of(2026, 3, 2))
        val spring = ScheduleMath.occurrences(m, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20))
        assertEquals(listOf(2, 9, 16).map { LocalDate.of(2026, 3, it) }, spring)
        spring.forEach { assertEquals(LocalTime.of(8, 0), ScheduleMath.doseTime(m, it, newYork).toLocalTime()) }

        val fall = ScheduleMath.occurrences(m, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 9))
        assertEquals(listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9)), fall)
    }

    @Test
    fun daily_reminderAcrossSpringForwardKeepsLocalTime() {
        val m = med(Schedule.Daily)
        val now = ZonedDateTime.of(2026, 3, 7, 9, 0, 0, 0, newYork)
        val next = ScheduleMath.nextReminder(m, emptySet(), now)!!
        assertEquals(ZonedDateTime.of(2026, 3, 8, 8, 0, 0, 0, newYork), next)
        // Only 22 hours elapse because the clocks jump forward overnight.
        assertEquals(Duration.ofHours(22), Duration.between(now, next))
    }

    @Test
    fun doseTimeInsideDstGapMovesForward() {
        val m = med(Schedule.Daily, time = LocalTime.of(1, 30))
        // London clocks jump from 01:00 to 02:00 on 29 March 2026.
        val at = ScheduleMath.doseTime(m, LocalDate.of(2026, 3, 29), london)
        assertEquals(LocalTime.of(2, 30), at.toLocalTime())
        assertEquals(LocalDate.of(2026, 3, 29), at.toLocalDate())
    }

    @Test
    fun doseTimeInsideDstOverlapUsesEarlierOffset() {
        val m = med(Schedule.Daily, time = LocalTime.of(1, 30))
        val at = ScheduleMath.doseTime(m, LocalDate.of(2026, 11, 1), newYork)
        assertEquals(LocalTime.of(1, 30), at.toLocalTime())
        assertEquals(ZoneOffset.ofHours(-4), at.offset)
    }

    @Test
    fun nextReminder_skipsPastTimesAndLoggedDates() {
        val m = med(Schedule.Daily)
        val now = ZonedDateTime.of(2026, 10, 13, 9, 0, 0, 0, london)
        assertEquals(ZonedDateTime.of(2026, 10, 14, 8, 0, 0, 0, london), ScheduleMath.nextReminder(m, emptySet(), now))

        val early = now.withHour(7)
        assertEquals(now.withHour(8), ScheduleMath.nextReminder(m, emptySet(), early))
        assertEquals(
            ZonedDateTime.of(2026, 10, 14, 8, 0, 0, 0, london),
            ScheduleMath.nextReminder(m, setOf(LocalDate.of(2026, 10, 13)), early),
        )
    }

    @Test
    fun nextReminder_weeklyAcrossMonthBoundary() {
        val m = med(Schedule.Weekdays(setOf(DayOfWeek.MONDAY)), time = LocalTime.of(20, 0))
        val now = ZonedDateTime.of(2026, 10, 27, 12, 0, 0, 0, london)
        assertEquals(ZonedDateTime.of(2026, 11, 2, 20, 0, 0, 0, london), ScheduleMath.nextReminder(m, emptySet(), now))
    }
}
