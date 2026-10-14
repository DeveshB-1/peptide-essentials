package io.github.deveshb1.peptideessentials.progress

import io.github.deveshb1.peptideessentials.data.LengthUnit
import io.github.deveshb1.peptideessentials.data.Measurement
import io.github.deveshb1.peptideessentials.data.MetricKind
import io.github.deveshb1.peptideessentials.data.Settings
import io.github.deveshb1.peptideessentials.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MetricMathTest {
    private val today = LocalDate.of(2026, 10, 14)
    private fun m(daysAgo: Long, value: Double) = Measurement("$daysAgo", "weight", today.minusDays(daysAgo), value)
    private val history = listOf(m(200, 100.0), m(60, 95.0), m(20, 92.0), m(1, 90.0))

    @Test
    fun windowFiltersAndSorts() {
        assertEquals(listOf(20L, 1L).map { today.minusDays(it) }, MetricMath.window(history, ChartRange.M1, today).map { it.date })
        assertEquals(4, MetricMath.window(history.reversed(), ChartRange.ALL, today).size)
        assertEquals(today.minusDays(200), MetricMath.window(history.reversed(), ChartRange.ALL, today).first().date)
    }

    @Test
    fun changeSinceStartOfWindow() {
        val all = MetricMath.change(MetricMath.window(history, ChartRange.ALL, today))!!
        assertEquals(-10.0, all.delta, 1e-9)
        assertEquals(-10.0, all.percent!!, 1e-9)
        val threeMonths = MetricMath.change(MetricMath.window(history, ChartRange.M3, today))!!
        assertEquals(-5.0, threeMonths.delta, 1e-9)
    }

    @Test
    fun changeNeedsTwoPoints() {
        assertNull(MetricMath.change(listOf(m(1, 90.0))))
        assertNull(MetricMath.change(emptyList()))
    }

    @Test
    fun unitConversionsRoundTrip() {
        val imperial = Settings(weightUnit = WeightUnit.LB, lengthUnit = LengthUnit.IN)
        assertEquals(220.462, MetricMath.toDisplay(100.0, MetricKind.WEIGHT, imperial), 1e-3)
        assertEquals(100.0, MetricMath.fromDisplay(220.46226218, MetricKind.WEIGHT, imperial), 1e-6)
        assertEquals(10.0, MetricMath.toDisplay(25.4, MetricKind.WAIST, imperial), 1e-9)
        assertEquals(25.0, MetricMath.toDisplay(25.0, MetricKind.BODY_FAT, imperial), 0.0)
        assertEquals(80.0, MetricMath.toDisplay(80.0, MetricKind.WEIGHT, Settings()), 0.0)
    }
}
