package io.github.deveshb1.peptideessentials.sites

import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Suggested next site; [allResting] means every site is still inside its rest period. */
data class SiteSuggestion(val site: InjectionSite, val allResting: Boolean)

/**
 * Rotation works across all medications: tissue needs rest regardless of what was injected.
 * A site is resting while fewer than its rest period's days have passed since it was last used.
 */
object SiteRotation {

    /** Most recent use of each site by a taken dose. */
    fun lastUsed(entries: List<DoseEntry>): Map<InjectionSite, Instant> = entries.asSequence()
        .filter { it.status == DoseStatus.TAKEN && it.site != null }
        .groupBy { it.site!! }
        .mapValues { (_, uses) -> uses.maxOf { it.time } }

    fun useCount(entries: List<DoseEntry>): Map<InjectionSite, Int> = entries
        .filter { it.status == DoseStatus.TAKEN && it.site != null }
        .groupingBy { it.site!! }
        .eachCount()

    fun daysSince(last: Instant?, today: LocalDate, zone: ZoneId): Long? =
        last?.let { ChronoUnit.DAYS.between(it.atZone(zone).toLocalDate(), today) }

    fun isResting(site: InjectionSite, daysSince: Long?, settings: Settings): Boolean =
        daysSince != null && daysSince < settings.restDays(site)

    /**
     * 1 for a site used today, falling to 0 when its rest period is over; 0 for never-used sites.
     * Drives the warm-to-cool heatmap.
     */
    fun heat(daysSince: Long?, restDays: Int): Float {
        if (daysSince == null || restDays <= 0) return 0f
        return (1f - daysSince.toFloat() / restDays).coerceIn(0f, 1f)
    }

    /** Never-used sites first in body order, then the least recently used rested site. */
    fun suggest(entries: List<DoseEntry>, settings: Settings, today: LocalDate, zone: ZoneId): SiteSuggestion {
        val last = lastUsed(entries)
        val rested = InjectionSite.entries.filterNot { isResting(it, daysSince(last[it], today, zone), settings) }
        rested.firstOrNull { it !in last }?.let { return SiteSuggestion(it, allResting = false) }
        rested.minByOrNull { last.getValue(it) }?.let { return SiteSuggestion(it, allResting = false) }
        return SiteSuggestion(InjectionSite.entries.minBy { last.getValue(it) }, allResting = true)
    }
}
