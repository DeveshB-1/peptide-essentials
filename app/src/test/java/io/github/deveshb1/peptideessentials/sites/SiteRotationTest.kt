package io.github.deveshb1.peptideessentials.sites

import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.Settings
import io.github.deveshb1.peptideessentials.schedule.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SiteRotationTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 14)
    private val settings = Settings()
    private fun daysAgo(n: Long) = today.minusDays(n).atTime(8, 0).toInstant(zone)

    @Test
    fun firstSuggestionIsFirstSite() {
        val s = SiteRotation.suggest(emptyList(), settings, today, zone)
        assertEquals(SiteSuggestion(InjectionSite.ABDOMEN_UPPER_LEFT, allResting = false), s)
    }

    @Test
    fun neverUsedSitesComeFirst() {
        val entries = listOf(entry(site = InjectionSite.ABDOMEN_UPPER_LEFT, time = daysAgo(30)))
        assertEquals(InjectionSite.ABDOMEN_UPPER_RIGHT, SiteRotation.suggest(entries, settings, today, zone).site)
    }

    @Test
    fun restingSitesAreSkipped() {
        // Every site used; only the two used 8+ days ago have finished resting.
        val entries = InjectionSite.entries.mapIndexed { i, site ->
            val ago = when (site) {
                InjectionSite.THIGH_LEFT -> 9L
                InjectionSite.GLUTE_RIGHT -> 8L
                else -> i.toLong() % 5
            }
            entry(site = site, time = daysAgo(ago))
        }
        assertEquals(InjectionSite.THIGH_LEFT, SiteRotation.suggest(entries, settings, today, zone).site)
    }

    @Test
    fun perSiteRestPeriodIsRespected() {
        val entries = InjectionSite.entries.map { entry(site = it, time = daysAgo(if (it == InjectionSite.THIGH_LEFT) 9 else 5)) }
        val longRest = settings.copy(siteRestDays = mapOf(InjectionSite.THIGH_LEFT to 14))
        val s = SiteRotation.suggest(entries, longRest, today, zone)
        assertTrue(s.allResting)
        assertEquals(InjectionSite.THIGH_LEFT, s.site)
        assertFalse(SiteRotation.suggest(entries, settings, today, zone).allResting)
    }

    @Test
    fun usageIsSharedAcrossMedicationsButIgnoresSkips() {
        val entries = listOf(
            entry(medicationId = "other", site = InjectionSite.ABDOMEN_UPPER_LEFT, time = daysAgo(1)),
            entry(site = InjectionSite.ABDOMEN_UPPER_RIGHT, time = daysAgo(1), status = DoseStatus.SKIPPED),
        )
        assertEquals(InjectionSite.ABDOMEN_UPPER_RIGHT, SiteRotation.suggest(entries, settings, today, zone).site)
        assertEquals(mapOf(InjectionSite.ABDOMEN_UPPER_LEFT to 1), SiteRotation.useCount(entries))
    }

    @Test
    fun heatFadesOverTheRestPeriod() {
        assertEquals(1f, SiteRotation.heat(0, 7), 1e-6f)
        assertEquals(0.5f, SiteRotation.heat(3, 6), 1e-6f)
        assertEquals(0f, SiteRotation.heat(7, 7), 1e-6f)
        assertEquals(0f, SiteRotation.heat(30, 7), 1e-6f)
        assertEquals(0f, SiteRotation.heat(null, 7), 1e-6f)
    }

    @Test
    fun restingDependsOnDaysSinceLastUse() {
        assertTrue(SiteRotation.isResting(InjectionSite.THIGH_LEFT, 6, settings))
        assertFalse(SiteRotation.isResting(InjectionSite.THIGH_LEFT, 7, settings))
        assertFalse(SiteRotation.isResting(InjectionSite.THIGH_LEFT, null, settings))
        assertEquals(2L, SiteRotation.daysSince(daysAgo(2), today, zone))
    }
}
