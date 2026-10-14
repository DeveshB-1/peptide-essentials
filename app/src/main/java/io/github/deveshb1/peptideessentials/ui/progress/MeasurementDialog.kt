package io.github.deveshb1.peptideessentials.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.AppRepository
import io.github.deveshb1.peptideessentials.data.Measurement
import io.github.deveshb1.peptideessentials.data.Metric
import io.github.deveshb1.peptideessentials.progress.MetricMath
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.DateDialog
import io.github.deveshb1.peptideessentials.ui.components.NumberField
import io.github.deveshb1.peptideessentials.ui.components.PickerRow
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import java.time.LocalDate

/** Adds or edits one measurement. Values are entered in display units and stored canonically. */
@Composable
fun MeasurementDialog(
    data: AppData,
    onDismiss: () -> Unit,
    metricId: String? = null,
    existing: Measurement? = null,
    onDelete: (() -> Unit)? = null,
) {
    val res = LocalResources.current
    val actions = rememberActions()
    val today = LocalDate.now()
    var selectedId by rememberSaveable { mutableStateOf(existing?.metricId ?: metricId ?: Metric.WEIGHT_ID) }
    val metric = data.metric(selectedId) ?: data.metrics.first()
    var valueText by rememberSaveable {
        mutableStateOf(existing?.let { Format.amount(MetricMath.toDisplay(it.value, metric.kind, data.settings), 2) }.orEmpty())
    }
    var day by rememberSaveable { mutableStateOf(existing?.date ?: today) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    // Body fat can be zero-or-more percent; other metrics use the general positive parser.
    val value = valueText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_measurement else R.string.edit_measurement)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existing == null && metricId == null) {
                    ChipChoice(data.metrics, metric, { Format.metricName(res, it) }) { selectedId = it.id }
                }
                NumberField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = Format.metricName(res, metric),
                    suffix = Format.metricUnit(res, metric, data.settings).ifBlank { null },
                    isError = valueText.isNotEmpty() && value == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerRow(
                    icon = rememberVectorPainter(Icons.Filled.DateRange),
                    label = stringResource(R.string.date),
                    value = Format.longDate(day, today),
                    onClick = { pickDate = true },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = value != null,
                onClick = {
                    val stored = MetricMath.fromDisplay(value ?: return@TextButton, metric.kind, data.settings)
                    val m = Measurement(existing?.id ?: AppRepository.newId(), metric.id, day, stored, note.trim())
                    actions.run { saveMeasurement(m) }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )

    if (pickDate) {
        DateDialog(initial = day, latest = today, onConfirm = { day = it; pickDate = false }, onDismiss = { pickDate = false })
    }
}

/** Creates a user-defined metric such as resting heart rate. */
@Composable
fun CustomMetricDialog(onDismiss: () -> Unit) {
    val actions = rememberActions()
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_metric)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.metric_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it.take(12) },
                    label = { Text(stringResource(R.string.metric_unit_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                actions.run { saveMetric(Metric(AppRepository.newId(), name.trim(), unit.trim())) }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
