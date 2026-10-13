package io.github.deveshb1.doselog.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DoseStatus { TAKEN, SKIPPED }

/**
 * One logged dose. Name and unit are copied from the medication so history stays readable
 * after a medication is renamed or deleted.
 */
data class DoseEntry(
    val id: String,
    val medicationId: String,
    val medicationName: String,
    val scheduledDate: LocalDate?,
    val status: DoseStatus,
    val time: Instant,
    val amount: Double,
    val unit: DoseUnit,
    val site: InjectionSite? = null,
    val note: String = "",
) {
    /** Day the entry belongs to in history: when it was taken, or the day that was skipped. */
    fun displayDate(zone: ZoneId): LocalDate =
        if (status == DoseStatus.SKIPPED && scheduledDate != null) scheduledDate
        else time.atZone(zone).toLocalDate()
}
