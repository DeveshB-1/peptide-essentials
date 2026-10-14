package io.github.deveshb1.peptideessentials.vials

import io.github.deveshb1.peptideessentials.calc.Dosing
import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.Medication
import io.github.deveshb1.peptideessentials.data.Vial
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/** Derived state of a vial on a given day. [dosesLeft] is null when the dose can't be expressed in the vial's unit. */
data class VialState(
    val remaining: Double,
    val fraction: Float,
    val dosesLeft: Int?,
    val daysToUseBy: Long,
) {
    val empty: Boolean get() = remaining <= EPSILON || dosesLeft == 0
    val lowStock: Boolean get() = !empty && dosesLeft != null && dosesLeft <= VialMath.LOW_STOCK_DOSES
    val expired: Boolean get() = daysToUseBy < 0
    val expiring: Boolean get() = !expired && daysToUseBy <= VialMath.EXPIRING_DAYS

    /** True when the vial should be finished or replaced. */
    val needsReplacing: Boolean get() = empty || expired

    private companion object {
        const val EPSILON = 1e-9
    }
}

object VialMath {
    const val LOW_STOCK_DOSES = 2
    const val EXPIRING_DAYS = 3L

    /** Total drawn from [vial] by taken doses, in the vial's unit. Doses that can't be converted count as zero. */
    fun consumed(vial: Vial, entries: List<DoseEntry>): Double = entries
        .filter { it.vialId == vial.id && it.status == DoseStatus.TAKEN }
        .sumOf { Dosing.inVialUnit(it.amount, it.unit, vial) ?: 0.0 }

    fun remaining(vial: Vial, entries: List<DoseEntry>): Double = (vial.amount - consumed(vial, entries)).coerceAtLeast(0.0)

    fun dosesLeft(remaining: Double, medication: Medication?, vial: Vial): Int? {
        val dose = medication?.let { Dosing.inVialUnit(it.doseAmount, it.unit, vial) } ?: return null
        // The epsilon stops 2.5 / 0.25 landing on 9.999…
        return floor(remaining / dose + 1e-9).toInt()
    }

    fun state(vial: Vial, medication: Medication?, entries: List<DoseEntry>, today: LocalDate): VialState {
        val remaining = remaining(vial, entries)
        return VialState(
            remaining = remaining,
            fraction = (remaining / vial.amount).toFloat().coerceIn(0f, 1f),
            dosesLeft = dosesLeft(remaining, medication, vial),
            daysToUseBy = ChronoUnit.DAYS.between(today, vial.useBy),
        )
    }

    fun defaultUseBy(mixedOn: LocalDate, days: Int): LocalDate = mixedOn.plusDays(days.toLong())
}
