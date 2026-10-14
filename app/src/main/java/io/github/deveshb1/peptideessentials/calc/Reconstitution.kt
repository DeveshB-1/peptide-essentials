package io.github.deveshb1.peptideessentials.calc

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Insulin-style syringes are U-100: 100 units per mL. */
const val UNITS_PER_ML = 100.0

enum class MassUnit(val label: String, val perMg: Double) {
    MG("mg", 1.0),
    MCG("mcg", 1000.0);

    fun toMg(value: Double) = value / perMg
}

enum class Syringe(val capacityMl: Double, val label: String) {
    ML_0_3(0.3, "0.3 mL"),
    ML_0_5(0.5, "0.5 mL"),
    ML_1_0(1.0, "1 mL");

    val units: Int get() = (capacityMl * UNITS_PER_ML).roundToInt()
}

/** A vial of [vialMg] dissolved in [waterMl] of diluent. */
data class Reconstitution(val vialMg: Double, val waterMl: Double) {
    init {
        require(vialMg > 0 && waterMl > 0) { "vial and water must be positive" }
    }

    val mgPerMl: Double get() = vialMg / waterMl
    val mcgPerUnit: Double get() = mgPerMl * 1000 / UNITS_PER_ML

    fun mlFor(doseMg: Double): Double = doseMg / mgPerMl
    fun unitsFor(doseMg: Double): Double = mlFor(doseMg) * UNITS_PER_ML
    fun doseMgFor(units: Double): Double = units / UNITS_PER_ML * mgPerMl

    /** Whole doses in the vial; the epsilon stops 2.5/0.25 landing on 9.999… */
    fun dosesPerVial(doseMg: Double): Int = floor(vialMg / doseMg + 1e-9).toInt()
}

sealed interface Warning {
    /** The draw doesn't fit in one syringe; it would take [injections] fills. */
    data class OverCapacity(val syringe: Syringe, val injections: Int) : Warning

    /** Under 2 units is hard to measure accurately on a U-100 scale. */
    data object HardToMeasure : Warning

    /** The dose is more than the whole vial holds. */
    data object MoreThanVial : Warning
}

fun warningsFor(recon: Reconstitution, doseMg: Double, syringe: Syringe): List<Warning> = buildList {
    val units = recon.unitsFor(doseMg)
    if (doseMg > recon.vialMg + 1e-9) add(Warning.MoreThanVial)
    if (units > syringe.units + 1e-9) add(Warning.OverCapacity(syringe, ceil(units / syringe.units - 1e-9).toInt()))
    if (units < 2.0) add(Warning.HardToMeasure)
}
