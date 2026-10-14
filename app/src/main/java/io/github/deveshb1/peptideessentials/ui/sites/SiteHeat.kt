package io.github.deveshb1.peptideessentials.ui.sites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.sites.SiteRotation
import io.github.deveshb1.peptideessentials.ui.theme.HeatColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Recency of each site for the heatmap and picker. */
class SiteUsage(entries: List<DoseEntry>, private val data: AppData, private val today: LocalDate) {
    private val zone = ZoneId.systemDefault()
    val lastUsed: Map<InjectionSite, Instant> = SiteRotation.lastUsed(entries)

    fun daysSince(site: InjectionSite): Long? = SiteRotation.daysSince(lastUsed[site], today, zone)

    fun restDays(site: InjectionSite): Int = data.settings.restDays(site)

    fun isResting(site: InjectionSite): Boolean = SiteRotation.isResting(site, daysSince(site), data.settings)

    /** Warm while resting (hotter when more recent), cool once rested, faint if never used. */
    fun color(site: InjectionSite): Color {
        val days = daysSince(site) ?: return HeatColors.rested.copy(alpha = 0.22f)
        val heat = SiteRotation.heat(days, restDays(site))
        return if (heat <= 0f) HeatColors.rested.copy(alpha = 0.6f) else lerp(HeatColors.warm, HeatColors.hot, heat).copy(alpha = 0.9f)
    }
}
