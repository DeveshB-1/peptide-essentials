package io.github.deveshb1.doselog.schedule

import io.github.deveshb1.doselog.data.DoseEntry
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.data.InjectionSite
import java.time.Instant

object SiteRotation {

    /** Most recent time each site was used for [medicationId]. */
    fun lastUsed(medicationId: String, entries: List<DoseEntry>): Map<InjectionSite, Instant> =
        entries.asSequence()
            .filter { it.medicationId == medicationId && it.status == DoseStatus.TAKEN && it.site != null }
            .groupBy { it.site!! }
            .mapValues { (_, uses) -> uses.maxOf { it.time } }

    /** Least recently used site; never-used sites come first, in body order. */
    fun suggest(medicationId: String, entries: List<DoseEntry>): InjectionSite {
        val used = lastUsed(medicationId, entries)
        return InjectionSite.entries.firstOrNull { it !in used }
            ?: used.minBy { it.value }.key
    }
}
