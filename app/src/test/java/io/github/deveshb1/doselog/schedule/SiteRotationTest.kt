package io.github.deveshb1.doselog.schedule

import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.data.InjectionSite
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SiteRotationTest {
    private fun day(n: Int) = Instant.parse("2026-10-01T08:00:00Z").plusSeconds(n * 86_400L)

    @Test
    fun firstSuggestionIsFirstSite() {
        assertEquals(InjectionSite.ABDOMEN_LEFT, SiteRotation.suggest("m1", emptyList()))
    }

    @Test
    fun unusedSitesComeBeforeUsedOnes() {
        val entries = listOf(entry(site = InjectionSite.ABDOMEN_LEFT, time = day(0)))
        assertEquals(InjectionSite.ABDOMEN_RIGHT, SiteRotation.suggest("m1", entries))
    }

    @Test
    fun suggestsLeastRecentlyUsedOnceAllSitesUsed() {
        val order = listOf(
            InjectionSite.THIGH_RIGHT, InjectionSite.ABDOMEN_LEFT, InjectionSite.ABDOMEN_RIGHT,
            InjectionSite.THIGH_LEFT, InjectionSite.UPPER_ARM_LEFT, InjectionSite.UPPER_ARM_RIGHT,
            InjectionSite.BUTTOCK_LEFT, InjectionSite.BUTTOCK_RIGHT,
        )
        val entries = order.mapIndexed { i, site -> entry(site = site, time = day(i)) } +
            entry(site = InjectionSite.THIGH_RIGHT, time = day(20)) // reused later
        assertEquals(InjectionSite.ABDOMEN_LEFT, SiteRotation.suggest("m1", entries))
    }

    @Test
    fun ignoresOtherMedicationsAndSkippedDoses() {
        val entries = listOf(
            entry(medicationId = "other", site = InjectionSite.ABDOMEN_LEFT, time = day(1)),
            entry(site = InjectionSite.ABDOMEN_LEFT, time = day(2), status = DoseStatus.SKIPPED),
        )
        assertEquals(InjectionSite.ABDOMEN_LEFT, SiteRotation.suggest("m1", entries))
    }

    @Test
    fun lastUsedKeepsMostRecentUse() {
        val entries = listOf(
            entry(site = InjectionSite.THIGH_LEFT, time = day(1)),
            entry(site = InjectionSite.THIGH_LEFT, time = day(5)),
        )
        assertEquals(mapOf(InjectionSite.THIGH_LEFT to day(5)), SiteRotation.lastUsed("m1", entries))
    }
}
