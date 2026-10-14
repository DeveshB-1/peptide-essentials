package io.github.deveshb1.peptideessentials.report

import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.Medication
import io.github.deveshb1.peptideessentials.schedule.ScheduleMath
import java.time.LocalDate
import kotlin.math.roundToInt

data class Adherence(val scheduled: Int, val taken: Int, val skipped: Int) {
    val missed: Int get() = scheduled - taken - skipped

    /** Taken as a share of scheduled doses, or null if nothing was scheduled. */
    val percent: Int? get() = if (scheduled == 0) null else (taken * 100.0 / scheduled).roundToInt()
}

object AdherenceCalc {
    /**
     * Scheduled doses from [from] to [to] (capped at [today]) since the medication was tracked.
     * Today's dose only counts once it has been logged, so a dose that's still to come isn't "missed".
     */
    fun compute(med: Medication, entries: List<DoseEntry>, from: LocalDate, to: LocalDate, today: LocalDate): Adherence {
        val logged = entries.filter { it.medicationId == med.id && it.scheduledDate != null }
            .associateBy { it.scheduledDate!! }
        val start = maxOf(from, med.trackedFrom)
        val end = minOf(to, today)
        val dates = ScheduleMath.occurrences(med, start, end).filter { it < today || it in logged }
        return Adherence(
            scheduled = dates.size,
            taken = dates.count { logged[it]?.status == DoseStatus.TAKEN },
            skipped = dates.count { logged[it]?.status == DoseStatus.SKIPPED },
        )
    }
}
