package io.github.deveshb1.doselog.schedule

import io.github.deveshb1.doselog.data.Medication
import io.github.deveshb1.doselog.data.Schedule
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Calendar maths for dose schedules. Everything works on local dates, so daylight-saving
 * shifts never move a dose to a different day; the clock time is applied only at the end.
 */
object ScheduleMath {

    fun occursOn(medication: Medication, date: LocalDate): Boolean {
        if (date < medication.startDate) return false
        return when (val s = medication.schedule) {
            Schedule.Daily -> true
            is Schedule.Weekdays -> date.dayOfWeek in s.days
            is Schedule.EveryNDays -> ChronoUnit.DAYS.between(medication.startDate, date) % s.interval == 0L
        }
    }

    /** First scheduled date on or after [date], or null if the schedule never fires. */
    fun nextOccurrence(medication: Medication, date: LocalDate): LocalDate? {
        val from = maxOf(date, medication.startDate)
        return when (val s = medication.schedule) {
            Schedule.Daily -> from
            is Schedule.Weekdays ->
                if (s.days.isEmpty()) null
                else generateSequence(from) { it.plusDays(1) }.take(7).first { it.dayOfWeek in s.days }
            is Schedule.EveryNDays -> {
                val elapsed = ChronoUnit.DAYS.between(medication.startDate, from)
                val steps = (elapsed + s.interval - 1) / s.interval
                medication.startDate.plusDays(steps * s.interval)
            }
        }
    }

    /** Scheduled dates in the inclusive range [from]..[to]. */
    fun occurrences(medication: Medication, from: LocalDate, to: LocalDate): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var next = nextOccurrence(medication, from)
        while (next != null && next <= to) {
            result += next
            next = nextOccurrence(medication, next.plusDays(1))
        }
        return result
    }

    /** Wall-clock moment of the dose on [date]; times skipped by a DST jump move forward. */
    fun doseTime(medication: Medication, date: LocalDate, zone: ZoneId): ZonedDateTime =
        ZonedDateTime.of(date, medication.timeOfDay, zone)

    /**
     * The next dose strictly after [now] whose date isn't in [loggedDates].
     * Returns null when the schedule has no further doses within a year.
     */
    fun nextReminder(medication: Medication, loggedDates: Set<LocalDate>, now: ZonedDateTime): ZonedDateTime? {
        val limit = now.toLocalDate().plusYears(1)
        var date = nextOccurrence(medication, now.toLocalDate())
        while (date != null && date <= limit) {
            val at = doseTime(medication, date, now.zone)
            if (at.isAfter(now) && date !in loggedDates) return at
            date = nextOccurrence(medication, date.plusDays(1))
        }
        return null
    }
}
