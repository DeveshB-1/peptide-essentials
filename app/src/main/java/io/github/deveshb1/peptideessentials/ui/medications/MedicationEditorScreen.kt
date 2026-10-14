package io.github.deveshb1.peptideessentials.ui.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppRepository
import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.Medication
import io.github.deveshb1.peptideessentials.data.Schedule
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.DateDialog
import io.github.deveshb1.peptideessentials.ui.components.PickerRow
import io.github.deveshb1.peptideessentials.ui.components.SwitchRow
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.TimeDialog
import io.github.deveshb1.peptideessentials.ui.theme.TagColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val SCHEDULE_DAILY = 0
private const val SCHEDULE_WEEKDAYS = 1
private const val SCHEDULE_INTERVAL = 2
private const val MIN_INTERVAL = 2
private const val MAX_INTERVAL = 365

private fun Set<DayOfWeek>.toMask() = fold(0) { mask, day -> mask or (1 shl day.value) }
private fun Int.toDays() = DayOfWeek.entries.filter { this and (1 shl it.value) != 0 }.toSet()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEditorScreen(
    existing: Medication?,
    snackbarHostState: SnackbarHostState,
    onSave: (Medication) -> Unit,
    onDelete: (Medication) -> Unit,
    onClose: () -> Unit,
) {
    val res = LocalResources.current
    val context = LocalContext.current
    val schedule = existing?.schedule

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var amountText by rememberSaveable { mutableStateOf(existing?.let { Format.amount(it.doseAmount) }.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: DoseUnit.MG) }
    var scheduleType by rememberSaveable {
        mutableIntStateOf(
            when (schedule) {
                is Schedule.Weekdays -> SCHEDULE_WEEKDAYS
                is Schedule.EveryNDays -> SCHEDULE_INTERVAL
                else -> SCHEDULE_DAILY
            },
        )
    }
    var weekdayMask by rememberSaveable {
        mutableIntStateOf((schedule as? Schedule.Weekdays)?.days?.toMask() ?: setOf(LocalDate.now().dayOfWeek).toMask())
    }
    var interval by rememberSaveable { mutableIntStateOf((schedule as? Schedule.EveryNDays)?.interval ?: 7) }
    var minuteOfDay by rememberSaveable {
        mutableIntStateOf(existing?.timeOfDay?.let { it.hour * 60 + it.minute } ?: (9 * 60))
    }
    var startEpochDay by rememberSaveable { mutableLongStateOf((existing?.startDate ?: LocalDate.now()).toEpochDay()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var colorIndex by rememberSaveable { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var usesSites by rememberSaveable { mutableStateOf(existing?.usesInjectionSites ?: false) }
    var reminders by rememberSaveable { mutableStateOf(existing?.remindersEnabled ?: true) }
    var paused by rememberSaveable { mutableStateOf(existing?.paused ?: false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    val timeOfDay = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val startDate = LocalDate.ofEpochDay(startEpochDay)
    val amount = Format.parseAmount(amountText)
    val nameError = attempted && name.isBlank()
    val amountError = attempted && amount == null
    val weekdaysError = attempted && scheduleType == SCHEDULE_WEEKDAYS && weekdayMask == 0

    fun save() {
        attempted = true
        if (name.isBlank() || amount == null || (scheduleType == SCHEDULE_WEEKDAYS && weekdayMask == 0)) return
        val newSchedule = when (scheduleType) {
            SCHEDULE_WEEKDAYS -> Schedule.Weekdays(weekdayMask.toDays())
            SCHEDULE_INTERVAL -> Schedule.EveryNDays(interval)
            else -> Schedule.Daily
        }
        onSave(
            Medication(
                id = existing?.id ?: AppRepository.newId(),
                name = name.trim(),
                doseAmount = amount,
                unit = unit,
                schedule = newSchedule,
                timeOfDay = timeOfDay,
                startDate = startDate,
                notes = notes.trim(),
                colorIndex = colorIndex,
                usesInjectionSites = usesSites,
                remindersEnabled = reminders,
                paused = paused,
                trackedFrom = when {
                    existing == null || (existing.paused && !paused) -> LocalDate.now()
                    else -> existing.trackedFrom
                },
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.add_medication else R.string.edit_medication)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { dialog = "delete" }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete_medication))
                        }
                    }
                    TextButton(onClick = ::save) { Text(stringResource(R.string.save)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.medication_name)) },
                placeholder = { Text(stringResource(R.string.medication_name_hint)) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.error_name)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.dose_amount)) },
                suffix = { Text(Format.unitLabel(res, unit)) },
                isError = amountError,
                supportingText = {
                    Text(stringResource(if (amountError) R.string.error_amount else R.string.dose_amount_hint))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DoseUnit.entries.forEach { option ->
                    FilterChip(
                        selected = unit == option,
                        onClick = { unit = option },
                        label = { Text(Format.unitLabel(res, option)) },
                    )
                }
            }

            SectionHeader(stringResource(R.string.schedule))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    SCHEDULE_DAILY to R.string.schedule_daily,
                    SCHEDULE_WEEKDAYS to R.string.schedule_weekdays,
                    SCHEDULE_INTERVAL to R.string.schedule_interval,
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = scheduleType == type,
                        onClick = { scheduleType = type },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            when (scheduleType) {
                SCHEDULE_WEEKDAYS -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Format.orderedWeek().forEach { day ->
                            val bit = 1 shl day.value
                            val fullName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            FilterChip(
                                selected = weekdayMask and bit != 0,
                                onClick = { weekdayMask = weekdayMask xor bit },
                                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                                modifier = Modifier.semantics { contentDescription = fullName },
                            )
                        }
                    }
                    if (weekdaysError) {
                        Text(
                            stringResource(R.string.error_weekdays),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                SCHEDULE_INTERVAL -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { interval = (interval - 1).coerceAtLeast(MIN_INTERVAL) },
                            enabled = interval > MIN_INTERVAL,
                        ) { Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.fewer_days)) }
                        Text(
                            pluralStringResource(R.plurals.schedule_every_n_days, interval, interval),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        FilledTonalIconButton(
                            onClick = { interval = (interval + 1).coerceAtMost(MAX_INTERVAL) },
                            enabled = interval < MAX_INTERVAL,
                        ) { Icon(Icons.Filled.Add, stringResource(R.string.more_days)) }
                    }
                    Text(
                        stringResource(R.string.interval_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PickerRow(
                icon = painterResource(R.drawable.ic_clock),
                label = stringResource(R.string.time_of_day),
                value = Format.time(context, timeOfDay),
                onClick = { dialog = "time" },
            )
            PickerRow(
                icon = rememberVectorPainter(Icons.Filled.DateRange),
                label = stringResource(R.string.start_date),
                value = Format.longDate(startDate, LocalDate.now()),
                onClick = { dialog = "date" },
            )

            SectionHeader(stringResource(R.string.colour_tag))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TagColors.all.forEachIndexed { index, tag ->
                    val selected = index == colorIndex
                    val label = stringResource(tag.name)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton) { colorIndex = index }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(tag.color)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Filled.Check, null, tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }

            SwitchRow(
                title = stringResource(R.string.uses_injection_sites),
                body = stringResource(R.string.uses_injection_sites_body),
                checked = usesSites,
                onCheckedChange = { usesSites = it },
            )
            SwitchRow(
                title = stringResource(R.string.reminders),
                body = stringResource(R.string.reminders_body),
                checked = reminders,
                onCheckedChange = { reminders = it },
            )
            if (existing != null) {
                SwitchRow(
                    title = stringResource(R.string.paused),
                    body = stringResource(R.string.paused_body),
                    checked = paused,
                    onCheckedChange = { paused = it },
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.editor_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    when (dialog) {
        "time" -> TimeDialog(
            initial = timeOfDay,
            onConfirm = { minuteOfDay = it.hour * 60 + it.minute; dialog = null },
            onDismiss = { dialog = null },
        )
        "date" -> DateDialog(
            initial = startDate,
            onConfirm = { startEpochDay = it.toEpochDay(); dialog = null },
            onDismiss = { dialog = null },
        )
        "delete" -> if (existing != null) {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.delete_medication_title, existing.name)) },
                text = { Text(stringResource(R.string.delete_medication_body)) },
                confirmButton = {
                    TextButton(onClick = { dialog = null; onDelete(existing) }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}
