package io.github.deveshb1.peptideessentials.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.sites.SiteSuggestion
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.sites.SiteUsage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Result of the dose sheet; [time] is when the dose was taken (or skipped). */
data class DoseResult(
    val status: DoseStatus,
    val time: Instant,
    val amount: Double,
    val site: InjectionSite?,
    val note: String,
)

/**
 * Bottom sheet for logging a due dose or editing a history entry.
 * [onDelete] is only offered when editing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoseSheet(
    title: String,
    subtitle: String,
    unit: DoseUnit,
    initialStatus: DoseStatus,
    initialTime: Instant,
    initialAmount: Double,
    initialSite: InjectionSite?,
    initialNote: String,
    showSites: Boolean,
    suggestion: SiteSuggestion?,
    usage: SiteUsage,
    onSave: (DoseResult) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    drawText: String? = null,
    vialText: String? = null,
) {
    val res = LocalResources.current
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val now = LocalDateTime.now(zone)
    val start = initialTime.atZone(zone).toLocalDateTime()

    var status by rememberSaveable { mutableStateOf(initialStatus) }
    var date by rememberSaveable { mutableStateOf(start.toLocalDate()) }
    var time by rememberSaveable { mutableStateOf(start.toLocalTime().withSecond(0).withNano(0)) }
    var amountText by rememberSaveable { mutableStateOf(Format.amount(initialAmount)) }
    var site by rememberSaveable { mutableStateOf(initialSite) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }

    val taken = status == DoseStatus.TAKEN
    val amount = Format.parseAmount(amountText)
    val inFuture = LocalDateTime.of(date, time).isAfter(now.plusMinutes(1))
    val canSave = !inFuture && (!taken || amount != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (drawText != null) {
                SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        drawText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (vialText != null) {
                        Text(vialText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (onDelete != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = taken,
                        onClick = { status = DoseStatus.TAKEN },
                        label = { Text(stringResource(R.string.status_taken)) },
                    )
                    FilterChip(
                        selected = !taken,
                        onClick = { status = DoseStatus.SKIPPED },
                        label = { Text(stringResource(R.string.status_skipped)) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (taken) R.string.taken_at else R.string.logged_at),
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { picker = "date" },
                        label = { Text(Format.shortDate(date)) },
                        leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)) },
                    )
                    AssistChip(
                        onClick = { picker = "time" },
                        label = { Text(Format.time(context, time)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_clock), null, Modifier.size(18.dp)) },
                    )
                }
                if (inFuture) {
                    Text(
                        stringResource(R.string.error_future_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (taken) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.dose_amount)) },
                    suffix = { Text(Format.unitLabel(res, unit)) },
                    isError = amount == null,
                    supportingText = if (amount == null) {
                        { Text(stringResource(R.string.error_amount)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showSites) {
                    SitePicker(
                        selected = site,
                        suggestion = suggestion,
                        usage = usage,
                        onSelect = { site = it },
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_optional)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.size(8.dp))
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(
                            DoseResult(
                                status = status,
                                time = LocalDateTime.of(date, time).atZone(zone).toInstant(),
                                amount = amount ?: initialAmount,
                                site = if (taken && showSites) site else null,
                                note = note.trim(),
                            ),
                        )
                    },
                ) { Text(stringResource(if (onDelete == null) R.string.log_dose else R.string.save)) }
            }
        }
    }

    when (picker) {
        "date" -> DateDialog(
            initial = date,
            latest = now.toLocalDate(),
            onConfirm = { date = it; picker = null },
            onDismiss = { picker = null },
        )
        "time" -> TimeDialog(
            initial = time,
            onConfirm = { time = it; picker = null },
            onDismiss = { picker = null },
        )
    }
}

