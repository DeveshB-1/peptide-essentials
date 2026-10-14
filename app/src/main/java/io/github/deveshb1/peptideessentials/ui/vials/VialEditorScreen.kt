package io.github.deveshb1.peptideessentials.ui.vials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.calc.Dosing
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.AppRepository
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialStatus
import io.github.deveshb1.peptideessentials.data.VialUnit
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.DateDialog
import io.github.deveshb1.peptideessentials.ui.components.NumberField
import io.github.deveshb1.peptideessentials.ui.components.PickerRow
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import io.github.deveshb1.peptideessentials.vials.VialMath
import java.time.LocalDate
import kotlin.math.floor

@Composable
fun VialEditorScreen(data: AppData, vialId: String?, medicationId: String?) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val actions = rememberActions()
    val existing = data.vial(vialId)
    val today = LocalDate.now()

    var medId by rememberSaveable {
        mutableStateOf(existing?.medicationId ?: medicationId ?: data.medications.singleOrNull()?.id)
    }
    var amountText by rememberSaveable { mutableStateOf(existing?.let { Format.amount(it.amount) }.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: VialUnit.MG) }
    var diluentText by rememberSaveable { mutableStateOf(existing?.let { Format.amount(it.diluentMl) }.orEmpty()) }
    var mixedDay by rememberSaveable { mutableLongStateOf((existing?.mixedOn ?: today).toEpochDay()) }
    var useByDay by rememberSaveable {
        mutableLongStateOf((existing?.useBy ?: VialMath.defaultUseBy(today, data.settings.useByDays)).toEpochDay())
    }
    var useByEdited by rememberSaveable { mutableStateOf(existing != null) }
    var label by rememberSaveable { mutableStateOf(existing?.label.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var status by rememberSaveable { mutableStateOf(existing?.status ?: VialStatus.ACTIVE) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    val amount = Format.parseAmount(amountText)
    val diluent = Format.parseAmount(diluentText)
    val mixedOn = LocalDate.ofEpochDay(mixedDay)
    val useBy = LocalDate.ofEpochDay(useByDay)
    val med = medId?.let(data::medication)
    val replacing = existing == null && medId != null && data.activeVial(medId!!) != null

    fun save() {
        attempted = true
        val id = medId ?: return
        if (amount == null || diluent == null || useBy < mixedOn) return
        val vial = Vial(
            id = existing?.id ?: AppRepository.newId(),
            medicationId = id,
            amount = amount,
            unit = unit,
            diluentMl = diluent,
            mixedOn = mixedOn,
            useBy = useBy,
            label = label.trim(),
            notes = notes.trim(),
            status = status,
            closedOn = when {
                status == VialStatus.ACTIVE -> null
                existing?.status == status -> existing.closedOn
                else -> today
            },
        )
        actions.run { saveVial(vial) }
        nav.pop()
    }

    SubScreen(
        title = stringResource(if (existing == null) R.string.add_vial else R.string.edit_vial),
        actions = {
            if (existing != null) {
                IconButton(onClick = { dialog = "delete" }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_vial)) }
            }
            TextButton(onClick = ::save) { Text(stringResource(R.string.save)) }
        },
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
            SectionHeader(stringResource(R.string.medication))
            if (data.medications.isEmpty()) {
                Text(stringResource(R.string.vial_needs_medication), color = MaterialTheme.colorScheme.error)
            } else {
                ChipChoice(data.medications.sortedBy { it.name.lowercase() }, med, { it.name }) { medId = it.id }
                if (attempted && medId == null) {
                    Text(stringResource(R.string.error_pick_medication), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionHeader(stringResource(R.string.vial_contents))
            NumberField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.vial_amount),
                suffix = Format.vialUnit(res, unit),
                isError = attempted && amount == null,
                modifier = Modifier.fillMaxWidth(),
            )
            ChipChoice(VialUnit.entries, unit, { Format.vialUnit(res, it) }) { unit = it }
            NumberField(
                value = diluentText,
                onValueChange = { diluentText = it },
                label = stringResource(R.string.diluent_added),
                suffix = stringResource(R.string.unit_ml),
                isError = attempted && diluent == null,
                supporting = stringResource(R.string.diluent_hint),
                modifier = Modifier.fillMaxWidth(),
            )

            if (amount != null && diluent != null) {
                val preview = Vial("preview", medId.orEmpty(), amount, unit, diluent, mixedOn, useBy)
                SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
                    Text(Format.concentration(res, preview), style = MaterialTheme.typography.titleMedium, color = onContainer)
                    val draw = med?.let { Dosing.draw(it.doseAmount, it.unit, preview) }
                    val perDose = med?.let { Dosing.inVialUnit(it.doseAmount, it.unit, preview) }
                    if (draw != null && med != null) {
                        Text(
                            stringResource(R.string.vial_preview_draw, Format.dose(res, med.doseAmount, med.unit), Format.draw(res, draw)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onContainer,
                        )
                    }
                    if (perDose != null) {
                        val doses = floor(amount / perDose + 1e-9).toInt()
                        Text(pluralStringResource(R.plurals.doses_per_vial, doses, doses), style = MaterialTheme.typography.bodyMedium, color = onContainer)
                    } else if (med != null) {
                        Text(stringResource(R.string.vial_units_dont_match), style = MaterialTheme.typography.bodySmall, color = onContainer)
                    }
                }
            }

            SectionHeader(stringResource(R.string.vial_dates))
            PickerRow(
                icon = rememberVectorPainter(Icons.Filled.DateRange),
                label = stringResource(R.string.mixed_on),
                value = Format.longDate(mixedOn, today),
                onClick = { dialog = "mixed" },
            )
            PickerRow(
                icon = rememberVectorPainter(Icons.Filled.DateRange),
                label = stringResource(R.string.use_by),
                value = Format.longDate(useBy, today),
                onClick = { dialog = "useBy" },
            )
            if (useBy < mixedOn) {
                Text(stringResource(R.string.error_use_by), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else if (!useByEdited) {
                Text(
                    pluralStringResource(R.plurals.use_by_default_hint, data.settings.useByDays, data.settings.useByDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.vial_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (existing != null) {
                SectionHeader(stringResource(R.string.vial_status))
                ChipChoice(VialStatus.entries, status, { statusLabel(it) }) { status = it }
            }
            if (replacing) {
                Text(
                    stringResource(R.string.vial_replaces_current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row { Text(stringResource(R.string.vial_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    when (dialog) {
        "mixed" -> DateDialog(
            initial = mixedOn,
            latest = today,
            onConfirm = {
                mixedDay = it.toEpochDay()
                if (!useByEdited) useByDay = VialMath.defaultUseBy(it, data.settings.useByDays).toEpochDay()
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        "useBy" -> DateDialog(
            initial = useBy,
            onConfirm = {
                useByDay = it.toEpochDay()
                useByEdited = true
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        "delete" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.delete_vial_title)) },
            text = { Text(stringResource(R.string.delete_vial_body)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    existing?.let { v -> actions.run { deleteVial(v.id) } }
                    nav.pop()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun statusLabel(status: VialStatus): String = stringResource(
    when (status) {
        VialStatus.ACTIVE -> R.string.vial_active
        VialStatus.FINISHED -> R.string.vial_finished
        VialStatus.DISCARDED -> R.string.vial_discarded
    },
)
