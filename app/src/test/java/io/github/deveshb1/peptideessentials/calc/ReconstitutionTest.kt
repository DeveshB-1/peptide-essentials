package io.github.deveshb1.peptideessentials.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconstitutionTest {
    private val eps = 1e-9

    @Test
    fun `5 mg in 2 mL gives 2_5 mg per mL`() {
        val r = Reconstitution(5.0, 2.0)
        assertEquals(2.5, r.mgPerMl, eps)
        assertEquals(25.0, r.mcgPerUnit, eps)
    }

    @Test
    fun `dose converts to volume and units`() {
        val r = Reconstitution(5.0, 2.0)
        assertEquals(0.1, r.mlFor(0.25), eps)
        assertEquals(10.0, r.unitsFor(0.25), eps)
    }

    @Test
    fun `mcg dose converts through mg`() {
        val r = Reconstitution(5.0, 2.0)
        assertEquals(10.0, r.unitsFor(MassUnit.MCG.toMg(250.0)), eps)
    }

    @Test
    fun `units convert back to dose`() {
        val r = Reconstitution(10.0, 1.0)
        assertEquals(2.5, r.doseMgFor(25.0), eps)
    }

    @Test
    fun `doses per vial survives floating point`() {
        assertEquals(10, Reconstitution(2.5, 1.0).dosesPerVial(0.25))
        assertEquals(20, Reconstitution(5.0, 2.0).dosesPerVial(0.25))
        assertEquals(3, Reconstitution(10.0, 2.0).dosesPerVial(3.0))
    }

    @Test
    fun `no warnings for a normal draw`() {
        assertTrue(warningsFor(Reconstitution(5.0, 2.0), 0.25, Syringe.ML_0_3).isEmpty())
    }

    @Test
    fun `over capacity counts injections`() {
        // 10 mg in 1 mL, 5 mg dose = 50 units: fits a 0.5 mL syringe exactly, needs 2 fills of a 0.3 mL.
        val r = Reconstitution(10.0, 1.0)
        assertTrue(warningsFor(r, 5.0, Syringe.ML_0_5).isEmpty())
        assertEquals(listOf(Warning.OverCapacity(Syringe.ML_0_3, 2)), warningsFor(r, 5.0, Syringe.ML_0_3))
    }

    @Test
    fun `tiny draws and oversized doses are flagged`() {
        val r = Reconstitution(10.0, 1.0)
        assertTrue(Warning.HardToMeasure in warningsFor(r, 0.1, Syringe.ML_1_0))
        assertTrue(Warning.MoreThanVial in warningsFor(r, 12.0, Syringe.ML_1_0))
    }
}
