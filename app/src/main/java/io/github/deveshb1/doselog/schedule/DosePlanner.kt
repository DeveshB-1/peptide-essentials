package io.github.deveshb1.doselog.schedule

import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.DoseEntry
import io.github.deveshb1.doselog.data.Medication
import java.time.LocalDate
import java.time.ZonedDateTime

data class PlannedDose(val medication: Medication, val date: LocalDate, val dueAt: ZonedDateTime)

data class TodayPlan(
    val overdue: List<PlannedDose>,
    val dueToday: List<PlannedDose>,
    val upcoming: List<PlannedDose>,
    val loggedToday: List<DoseEntry>,
)

/** Builds the home screen: missed, due and upcoming doses for active medications. */
object DosePlanner {
    const val LOOKBACK_DAYS = 7L
    private const val UPCOMING_PER_MEDICATION = 3
    private const val UPCOMING_HORIZON_DAYS = 62L

    fun plan(data: AppData, now: ZonedDateTime, upcomingLimit: Int = 5): TodayPlan {
        val today = now.toLocalDate()
        val active = data.medications.filterNot { it.paused }
        val loggedByMed = data.entries
            .filter { it.scheduledDate != null }
            .groupBy({ it.medicationId }, { it.scheduledDate!! })
            .mapValues { it.value.toSet() }

        val overdue = mutableListOf<PlannedDose>()
        val dueToday = mutableListOf<PlannedDose>()
        val upcoming = mutableListOf<PlannedDose>()

        for (med in active) {
            val logged = loggedByMed[med.id].orEmpty()
            // Gaps before the most recent logged dose are treated as settled.
            val lastLogged = logged.filter { it < today }.maxOrNull()
            val from = listOfNotNull(
                med.startDate,
                med.trackedFrom,
                today.minusDays(LOOKBACK_DAYS),
                lastLogged?.plusDays(1),
            ).max()
            ScheduleMath.occurrences(med, from, today.minusDays(1))
                .filterNot { it in logged }
                .mapTo(overdue) { PlannedDose(med, it, ScheduleMath.doseTime(med, it, now.zone)) }

            if (ScheduleMath.occursOn(med, today) && today !in logged) {
                dueToday += PlannedDose(med, today, ScheduleMath.doseTime(med, today, now.zone))
            }

            ScheduleMath.occurrences(med, today.plusDays(1), today.plusDays(UPCOMING_HORIZON_DAYS))
                .filterNot { it in logged }
                .take(UPCOMING_PER_MEDICATION)
                .mapTo(upcoming) { PlannedDose(med, it, ScheduleMath.doseTime(med, it, now.zone)) }
        }

        val loggedToday = data.entries
            .filter { it.scheduledDate == today || (it.scheduledDate == null && it.displayDate(now.zone) == today) }
            .sortedBy { it.time }

        return TodayPlan(
            overdue = overdue.sortedBy { it.dueAt },
            dueToday = dueToday.sortedBy { it.dueAt },
            upcoming = upcoming.sortedBy { it.dueAt }.take(upcomingLimit),
            loggedToday = loggedToday,
        )
    }
}
