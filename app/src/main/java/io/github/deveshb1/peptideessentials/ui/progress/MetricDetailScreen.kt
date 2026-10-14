package io.github.deveshb1.peptideessentials.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.MetricKind
import io.github.deveshb1.peptideessentials.progress.ChartRange
import io.github.deveshb1.peptideessentials.progress.MetricMath
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import java.time.LocalDate

@Composable
fun MetricDetailScreen(data: AppData, metricId: String, today: LocalDate) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val actions = rememberActions()
    val metric = data.metric(metricId)
    if (metric == null) {
        LaunchedEffect(metricId) { nav.pop() }
        return
    }
    var range by rememberSaveable { mutableStateOf(ChartRange.ALL) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val series = MetricSeries(data, metric, range, today)
    val all = data.measurements.filter { it.metricId == metric.id }.sortedByDescending { it.date }

    SubScreen(
        title = Format.metricName(res, metric),
        actions = {
            if (metric.kind == MetricKind.CUSTOM) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_metric)) }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = "" },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_measurement)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding.withGutters(extraBottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "range") { ChipChoice(ChartRange.entries, range, { rangeLabel(it) }) { range = it } }
            item(key = "chart") {
                SectionCard {
                    changeText(series, data)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    if (series.points.isEmpty()) {
                        Text(stringResource(R.string.no_measurements), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LineChart(
                            points = series.points,
                            from = series.from,
                            to = series.to,
                            doseDays = series.doseDays,
                            color = MaterialTheme.colorScheme.primary,
                            description = chartDescription(series),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                        )
                        val values = series.points.map { it.second }
                        Text(
                            stringResource(
                                R.string.metric_min_max,
                                Format.metricValue(res, values.min(), metric, data.settings),
                                Format.metricValue(res, values.max(), metric, data.settings),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(stringResource(R.string.dose_marker_legend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            if (all.isNotEmpty()) item(key = "entries") { SectionHeader(stringResource(R.string.entries)) }
            items(all, key = { it.id }) { m ->
                ListItem(
                    headlineContent = {
                        Text(Format.metricValue(res, MetricMath.toDisplay(m.value, metric.kind, data.settings), metric, data.settings))
                    },
                    supportingContent = if (m.note.isNotBlank()) {
                        { Text(m.note) }
                    } else {
                        null
                    },
                    trailingContent = { Text(Format.relativeDay(res, m.date, today), style = MaterialTheme.typography.labelLarge) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable(onClickLabel = stringResource(R.string.edit_entry)) { editing = m.id },
                )
            }
        }
    }

    editing?.let { id ->
        val existing = data.measurements.firstOrNull { it.id == id }
        if (existing != null) {
            MeasurementDialog(
                data,
                onDismiss = { editing = null },
                existing = existing,
                onDelete = {
                    editing = null
                    actions.run { deleteMeasurement(existing.id) }
                    actions.undoable(res.getString(R.string.entry_deleted), res.getString(R.string.undo)) { saveMeasurement(existing) }
                },
            )
        } else {
            MeasurementDialog(data, onDismiss = { editing = null }, metricId = metric.id)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_metric_title, metric.name)) },
            text = { Text(stringResource(R.string.delete_metric_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.run { deleteMetric(metric.id) }
                    nav.pop()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
