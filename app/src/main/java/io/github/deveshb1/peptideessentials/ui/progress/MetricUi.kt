package io.github.deveshb1.peptideessentials.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.Metric
import io.github.deveshb1.peptideessentials.progress.ChartRange
import io.github.deveshb1.peptideessentials.progress.MetricChange
import io.github.deveshb1.peptideessentials.progress.MetricMath
import io.github.deveshb1.peptideessentials.ui.Format
import java.time.LocalDate
import java.time.ZoneId

/** Everything a chart needs for one metric over a range, in display units. */
class MetricSeries(data: AppData, val metric: Metric, range: ChartRange, today: LocalDate) {
    val sorted = MetricMath.window(data.measurements.filter { it.metricId == metric.id }, range, today)
    val points = sorted.map { it.date to MetricMath.toDisplay(it.value, metric.kind, data.settings) }
    val from: LocalDate = range.days?.let { today.minusDays(it) } ?: (sorted.firstOrNull()?.date ?: today.minusDays(30))
    val to: LocalDate = today
    val change: MetricChange? = MetricMath.change(sorted)
    val doseDays: Set<LocalDate> = data.entries
        .filter { it.status == DoseStatus.TAKEN }
        .map { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSet()
}

@Composable
fun rangeLabel(range: ChartRange): String = stringResource(
    when (range) {
        ChartRange.M1 -> R.string.range_1m
        ChartRange.M3 -> R.string.range_3m
        ChartRange.M6 -> R.string.range_6m
        ChartRange.ALL -> R.string.range_all
    },
)

/** "−3.2 kg (−3.5%) since 12 Aug", converted to display units. */
@Composable
fun changeText(series: MetricSeries, data: AppData): String? {
    val change = series.change ?: return null
    val res = LocalResources.current
    val delta = MetricMath.toDisplay(change.last.value, series.metric.kind, data.settings) -
        MetricMath.toDisplay(change.first.value, series.metric.kind, data.settings)
    val unit = Format.metricUnit(res, series.metric, data.settings)
    val amount = if (unit.isBlank()) Format.signed(delta) else "${Format.signed(delta)} $unit"
    val percent = change.percent?.let { " (${Format.signed(it)}%)" }.orEmpty()
    return stringResource(R.string.change_since, amount + percent, Format.shortDate(change.first.date))
}

@Composable
fun chartDescription(series: MetricSeries): String {
    val name = Format.metricName(LocalResources.current, series.metric)
    return pluralStringResource(R.plurals.chart_description, series.points.size, name, series.points.size)
}
