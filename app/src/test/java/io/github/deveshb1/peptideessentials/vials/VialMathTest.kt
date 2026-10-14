package io.github.deveshb1.peptideessentials.vials

import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.Schedule
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialStatus
import io.github.deveshb1.peptideessentials.data.VialUnit
import io.github.deveshb1.peptideessentials.schedule.entry
import io.github.deveshb1.peptideessentials.schedule.med
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VialMathTest {
    private val today = LocalDate.of(2026, 10, 14)
    private val weekly = med(Schedule.EveryNDays(7), dose = 0.5, unit = DoseUnit.MG)
    private val vial = Vial("v1", "m1", 5.0, VialUnit.MG, 2.0, today.minusDays(10), today.plusDays(18))

    @Test
    fun remainingDecrementsWithTakenDosesOnly() {
        val entries = listOf(
            entry(vialId = "v1", amount = 0.5),
            entry(vialId = "v1", amount = 500.0, unit = DoseUnit.MCG),
            entry(vialId = "v1", amount = 0.5, status = DoseStatus.SKIPPED),
            entry(vialId = "other", amount = 0.5),
        )
        assertEquals(4.0, VialMath.remaining(vial, entries), 1e-9)
        val state = VialMath.state(vial, weekly, entries, today)
        assertEquals(8, state.dosesLeft)
        assertEquals(0.8f, state.fraction, 1e-6f)
        assertFalse(state.lowStock)
    }

    @Test
    fun remainingNeverGoesNegative() {
        val entries = List(12) { entry(vialId = "v1", amount = 0.5) }
        val state = VialMath.state(vial, weekly, entries, today)
        assertEquals(0.0, state.remaining, 0.0)
        assertTrue(state.empty)
        assertTrue(state.needsReplacing)
    }

    @Test
    fun lowStockAtTwoDosesLeft() {
        val entries = List(6) { entry(vialId = "v1", amount = 0.75) } // 4.5 used, 0.5 left = 1 dose
        val state = VialMath.state(vial, weekly, entries, today)
        assertEquals(1, state.dosesLeft)
        assertTrue(state.lowStock)
        val two = VialMath.state(vial, weekly, List(3) { entry(vialId = "v1", amount = 1.0) }, today)
        assertEquals(4, two.dosesLeft)
        assertFalse(two.lowStock)
    }

    @Test
    fun dosesLeftUnknownWhenUnitsDontMatch() {
        val iuMed = med(Schedule.Daily, dose = 100.0, unit = DoseUnit.IU)
        assertNull(VialMath.state(vial, iuMed, emptyList(), today).dosesLeft)
    }

    @Test
    fun expiryWindow() {
        assertFalse(VialMath.state(vial, weekly, emptyList(), today).expiring)
        val soon = vial.copy(useBy = today.plusDays(3))
        assertTrue(VialMath.state(soon, weekly, emptyList(), today).expiring)
        val past = vial.copy(useBy = today.minusDays(1))
        val state = VialMath.state(past, weekly, emptyList(), today)
        assertTrue(state.expired)
        assertFalse(state.expiring)
        assertTrue(state.needsReplacing)
    }

    @Test
    fun defaultUseByCountsFromMixing() {
        assertEquals(LocalDate.of(2026, 11, 11), VialMath.defaultUseBy(today, 28))
    }

    @Test
    fun alertsPickTheMostPressingStateForActiveVials() {
        val data = AppData(
            medications = listOf(weekly),
            vials = listOf(
                vial.copy(useBy = today.plusDays(2)),
                vial.copy(id = "v2", status = VialStatus.FINISHED, useBy = today.minusDays(5)),
            ),
        )
        assertEquals(listOf(VialAlert.Expiring(data.vials[0], 2)), VialAlert.forData(data, today))
    }
}
