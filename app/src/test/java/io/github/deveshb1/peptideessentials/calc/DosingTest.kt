package io.github.deveshb1.peptideessentials.calc

import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DosingTest {
    private val eps = 1e-9
    private fun vial(amount: Double, unit: VialUnit, ml: Double) =
        Vial("v", "m", amount, unit, ml, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29))

    @Test
    fun massDoseInMassVial() {
        val v = vial(5.0, VialUnit.MG, 2.0)
        val draw = Dosing.draw(0.25, DoseUnit.MG, v)!!
        assertEquals(10.0, draw.units, eps)
        assertEquals(0.1, draw.ml, eps)
        assertEquals(0.25, Dosing.inVialUnit(250.0, DoseUnit.MCG, v)!!, eps)
        assertEquals(10.0, Dosing.draw(250.0, DoseUnit.MCG, v)!!.units, eps)
    }

    @Test
    fun mgDoseInMcgVial() {
        val v = vial(5000.0, VialUnit.MCG, 2.0)
        assertEquals(250.0, Dosing.inVialUnit(0.25, DoseUnit.MG, v)!!, eps)
        assertEquals(10.0, Dosing.draw(0.25, DoseUnit.MG, v)!!.units, eps)
    }

    @Test
    fun iuOnlyWithIuVials() {
        val iu = vial(5000.0, VialUnit.IU, 5.0)
        assertEquals(25.0, Dosing.draw(250.0, DoseUnit.IU, iu)!!.units, eps)
        assertNull(Dosing.draw(250.0, DoseUnit.IU, vial(5.0, VialUnit.MG, 2.0)))
        assertNull(Dosing.draw(0.25, DoseUnit.MG, iu))
    }

    @Test
    fun syringeUnitsAndMillilitres() {
        val v = vial(10.0, VialUnit.MG, 2.0)
        assertEquals(20.0, Dosing.draw(20.0, DoseUnit.UNITS, v)!!.units, eps)
        assertEquals(0.2, Dosing.draw(20.0, DoseUnit.UNITS, v)!!.ml, eps)
        assertEquals(1.0, Dosing.inVialUnit(20.0, DoseUnit.UNITS, v)!!, eps)
        assertEquals(30.0, Dosing.draw(0.3, DoseUnit.ML, v)!!.units, eps)
        assertEquals(1.5, Dosing.inVialUnit(0.3, DoseUnit.ML, v)!!, eps)
    }

    @Test
    fun unmixedVialOrZeroDoseGivesNothing() {
        assertNull(Dosing.draw(0.25, DoseUnit.MG, vial(5.0, VialUnit.MG, 0.0)))
        assertNull(Dosing.draw(0.0, DoseUnit.MG, vial(5.0, VialUnit.MG, 2.0)))
    }
}
