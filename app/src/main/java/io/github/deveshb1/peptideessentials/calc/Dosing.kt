package io.github.deveshb1.peptideessentials.calc

import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialUnit

/** How much to draw on a U-100 syringe. */
data class Draw(val units: Double, val ml: Double)

/**
 * Links a medication's dose to a mixed vial. Mass doses work with mass vials (mg and mcg convert),
 * IU only with IU vials, "units" are syringe units and mL is volume. Anything else returns null:
 * no answer is better than a guess.
 */
object Dosing {
    private fun massMg(unit: DoseUnit): Double? = when (unit) {
        DoseUnit.MG -> 1.0
        DoseUnit.MCG -> 0.001
        else -> null
    }

    private fun massMg(unit: VialUnit): Double? = when (unit) {
        VialUnit.MG -> 1.0
        VialUnit.MCG -> 0.001
        VialUnit.IU -> null
    }

    /** The dose expressed in the vial's own unit, or null if the two can't be related. */
    fun inVialUnit(amount: Double, unit: DoseUnit, vial: Vial): Double? {
        if (amount <= 0 || !vial.isMixed) return null
        return when (unit) {
            DoseUnit.MG, DoseUnit.MCG -> {
                val vialFactor = massMg(vial.unit) ?: return null
                amount * massMg(unit)!! / vialFactor
            }
            DoseUnit.IU -> if (vial.unit == VialUnit.IU) amount else null
            DoseUnit.ML -> amount * vial.concentration
            DoseUnit.UNITS -> amount / UNITS_PER_ML * vial.concentration
        }
    }

    fun draw(amount: Double, unit: DoseUnit, vial: Vial): Draw? {
        if (amount <= 0 || !vial.isMixed) return null
        val ml = when (unit) {
            DoseUnit.ML -> amount
            DoseUnit.UNITS -> amount / UNITS_PER_ML
            else -> (inVialUnit(amount, unit, vial) ?: return null) / vial.concentration
        }
        return Draw(units = ml * UNITS_PER_ML, ml = ml)
    }

    private val Vial.isMixed: Boolean get() = amount > 0 && diluentMl > 0
}
