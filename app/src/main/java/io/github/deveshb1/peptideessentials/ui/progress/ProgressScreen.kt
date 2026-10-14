package io.github.deveshb1.peptideessentials.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.Metric
import io.github.deveshb1.peptideessentials.progress.ChartRange
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.Route
import java.time.LocalDate

@Composable
fun ProgressScreen(data: AppData, today: LocalDate, contentPadding: PaddingValues) {
    var range by rememberSaveable { mutableStateOf(ChartRange.M3) }
    var addMetric by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding.withGutters(extraBottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "range") { ChipChoice(ChartRange.entries, range, { rangeLabel(it) }) { range = it } }
        items(data.metrics, key = { it.id }) { metric -> MetricCard(data, metric, range, today) }
        item(key = "add-metric") {
            OutlinedButton(onClick = { addMetric = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.add_custom_metric), modifier = Modifier.padding(start = 8.dp))
            }
        }
        item(key = "note") {
            Text(
                stringResource(R.string.progress_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (addMetric) CustomMetricDialog(onDismiss = { addMetric = false })
}

@Composable
private fun MetricCard(data: AppData, metric: Metric, range: ChartRange, today: LocalDate) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val series = MetricSeries(data, metric, range, today)
    val latest = series.points.lastOrNull()
    Card(
        onClick = { nav.push(Route.metric(metric.id)) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Format.metricName(res, metric), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (latest != null) {
                    Text(Format.metricValue(res, latest.second, metric, data.settings), style = MaterialTheme.typography.headlineSmall)
                }
            }
            if (latest == null) {
                Text(stringResource(R.string.no_measurements), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                changeText(series, data)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
                LineChart(
                    points = series.points,
                    from = series.from,
                    to = series.to,
                    doseDays = series.doseDays,
                    color = MaterialTheme.colorScheme.primary,
                    description = chartDescription(series),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
            }
        }
    }
}
