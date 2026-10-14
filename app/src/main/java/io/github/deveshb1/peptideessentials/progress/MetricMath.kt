package io.github.deveshb1.peptideessentials.progress

import io.github.deveshb1.peptideessentials.data.LengthUnit
import io.github.deveshb1.peptideessentials.data.Measurement
import io.github.deveshb1.peptideessentials.data.MetricKind
import io.github.deveshb1.peptideessentials.data.Settings
import io.github.deveshb1.peptideessentials.data.WeightUnit
import java.time.LocalDate

enum class ChartRange(val days: Long?) { M1(30), M3(91), M6(182), ALL(null) }

/** Difference between the first and last measurement in a window. */
data class MetricChange(val first: Measurement, val last: Measurement) {
    val delta: Double get() = last.value - first.value
    val percent: Double? get() = if (first.value == 0.0) null else delta / first.value * 100
}

object MetricMath {
    private const val LB_PER_KG = 2.20462262185
    private const val CM_PER_IN = 2.54

    fun window(measurements: List<Measurement>, range: ChartRange, today: LocalDate): List<Measurement> {
        val from = range.days?.let { today.minusDays(it) }
        return measurements.filter { from == null || it.date >= from }.sortedWith(compareBy({ it.date }, { it.id }))
    }

    /** Change across the window, or null with fewer than two measurements. */
    fun change(sorted: List<Measurement>): MetricChange? =
        if (sorted.size < 2) null else MetricChange(sorted.first(), sorted.last())

    /** Converts a stored (canonical) value into the unit the user chose to see. */
    fun toDisplay(value: Double, kind: MetricKind, settings: Settings): Double = when (kind) {
        MetricKind.WEIGHT -> if (settings.weightUnit == WeightUnit.LB) value * LB_PER_KG else value
        MetricKind.WAIST -> if (settings.lengthUnit == LengthUnit.IN) value / CM_PER_IN else value
        else -> value
    }

    fun fromDisplay(value: Double, kind: MetricKind, settings: Settings): Double = when (kind) {
        MetricKind.WEIGHT -> if (settings.weightUnit == WeightUnit.LB) value / LB_PER_KG else value
        MetricKind.WAIST -> if (settings.lengthUnit == LengthUnit.IN) value * CM_PER_IN else value
        else -> value
    }
}
