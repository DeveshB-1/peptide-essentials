package io.github.deveshb1.peptideessentials.ui.calculator

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.calc.MassUnit
import io.github.deveshb1.peptideessentials.calc.Reconstitution
import io.github.deveshb1.peptideessentials.calc.Syringe
import io.github.deveshb1.peptideessentials.calc.UNITS_PER_ML
import io.github.deveshb1.peptideessentials.calc.Warning
import io.github.deveshb1.peptideessentials.calc.warningsFor
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.VialStatus
import io.github.deveshb1.peptideessentials.data.VialUnit
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.NumberField
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SubScreen

private enum class Mode { UNITS_TO_DRAW, DOSE_FROM_UNITS }

/** Calculator inputs, kept between visits. */
private class Inputs(private val context: Context) {
    private val prefs = context.getSharedPreferences("calculator", Context.MODE_PRIVATE)
    var mode by mutableStateOf(Mode.entries[prefs.getInt("mode", 0).coerceIn(0, 1)])
    var vial by mutableStateOf(prefs.getString("vial", "").orEmpty())
    var vialUnit by mutableStateOf(MassUnit.entries[prefs.getInt("vialUnit", 0).coerceIn(0, 1)])
    var water by mutableStateOf(prefs.getString("water", "").orEmpty())
    var dose by mutableStateOf(prefs.getString("dose", "").orEmpty())
    var doseUnit by mutableStateOf(MassUnit.entries[prefs.getInt("doseUnit", 0).coerceIn(0, 1)])
    var units by mutableStateOf(prefs.getString("units", "").orEmpty())
    var syringe by mutableStateOf(Syringe.entries[prefs.getInt("syringe", Syringe.ML_0_5.ordinal).coerceIn(0, 2)])

    fun save() = prefs.edit {
        putInt("mode", mode.ordinal)
        putString("vial", vial)
        putInt("vialUnit", vialUnit.ordinal)
        putString("water", water)
        putString("dose", dose)
        putInt("doseUnit", doseUnit.ordinal)
        putString("units", units)
        putInt("syringe", syringe.ordinal)
    }
}

/** Standalone reconstitution and U-100 syringe calculator. */
@Composable
fun CalculatorScreen(data: AppData) {
    val context = LocalContext.current
    val res = LocalResources.current
    val inputs = remember { Inputs(context) }
    DisposableEffect(Unit) { onDispose { inputs.save() } }
    val massVials = data.vials.filter { it.status == VialStatus.ACTIVE && it.unit != VialUnit.IU && it.diluentMl > 0 }

    SubScreen(title = stringResource(R.string.calculator)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChipChoice(Mode.entries, inputs.mode, {
                stringResource(if (it == Mode.UNITS_TO_DRAW) R.string.calc_units_to_draw else R.string.calc_dose_from_units)
            }) { inputs.mode = it }

            if (massVials.isNotEmpty()) {
                Text(stringResource(R.string.calc_fill_from_vial), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    massVials.take(3).forEach { vial ->
                        val name = data.medication(vial.medicationId)?.name.orEmpty()
                        AssistChip(
                            onClick = {
                                inputs.vial = Format.amount(vial.amount)
                                inputs.vialUnit = if (vial.unit == VialUnit.MCG) MassUnit.MCG else MassUnit.MG
                                inputs.water = Format.amount(vial.diluentMl)
                            },
                            label = { Text(name, maxLines = 1) },
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.calc_vial)) {
                NumberField(inputs.vial, { inputs.vial = it }, stringResource(R.string.vial_amount), Modifier.fillMaxWidth(), inputs.vialUnit.label)
                ChipChoice(MassUnit.entries, inputs.vialUnit, { it.label }) { inputs.vialUnit = it }
                NumberField(inputs.water, { inputs.water = it }, stringResource(R.string.diluent_added), Modifier.fillMaxWidth(), stringResource(R.string.unit_ml))
            }

            if (inputs.mode == Mode.UNITS_TO_DRAW) {
                SectionCard(title = stringResource(R.string.dose_amount)) {
                    NumberField(inputs.dose, { inputs.dose = it }, stringResource(R.string.dose_amount), Modifier.fillMaxWidth(), inputs.doseUnit.label)
                    ChipChoice(MassUnit.entries, inputs.doseUnit, { it.label }) { inputs.doseUnit = it }
                }
            } else {
                SectionCard(title = stringResource(R.string.calc_units_drawn)) {
                    NumberField(inputs.units, { inputs.units = it }, stringResource(R.string.calc_units_on_syringe), Modifier.fillMaxWidth(), stringResource(R.string.unit_units))
                }
            }

            SectionCard(title = stringResource(R.string.calc_syringe)) {
                ChipChoice(Syringe.entries, inputs.syringe, { stringResource(R.string.calc_syringe_option, it.label, it.units) }) { inputs.syringe = it }
            }

            Result(inputs, res)

            Text(
                stringResource(R.string.calc_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Result(inputs: Inputs, res: android.content.res.Resources) {
    val vialAmount = Format.parseAmount(inputs.vial)
    val water = Format.parseAmount(inputs.water)
    if (vialAmount == null || water == null) return Hint(stringResource(R.string.calc_hint_vial))
    val recon = Reconstitution(inputs.vialUnit.toMg(vialAmount), water)

    val doseMg: Double
    val units: Double
    if (inputs.mode == Mode.UNITS_TO_DRAW) {
        val dose = Format.parseAmount(inputs.dose) ?: return Hint(stringResource(R.string.calc_hint_dose))
        doseMg = inputs.doseUnit.toMg(dose)
        units = recon.unitsFor(doseMg)
    } else {
        units = Format.parseAmount(inputs.units) ?: return Hint(stringResource(R.string.calc_hint_units))
        doseMg = recon.doseMgFor(units)
    }
    val ml = units / UNITS_PER_ML
    val warnings = warningsFor(recon, doseMg, inputs.syringe)
    val fits = warnings.none { it is Warning.OverCapacity }
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        if (inputs.mode == Mode.UNITS_TO_DRAW) {
            Text(stringResource(R.string.calc_draw_units, Format.amount(units, 1)), style = MaterialTheme.typography.displaySmall, color = onContainer)
            Text(stringResource(R.string.calc_draw_sub, Format.amount(ml, 3), inputs.syringe.label), style = MaterialTheme.typography.bodyLarge, color = onContainer)
        } else {
            Text(stringResource(R.string.calc_thats_mg, Format.amount(doseMg, 3)), style = MaterialTheme.typography.displaySmall, color = onContainer)
            Text(
                stringResource(R.string.calc_thats_sub, Format.amount(doseMg * 1000, 1), Format.amount(ml, 3)),
                style = MaterialTheme.typography.bodyLarge,
                color = onContainer,
            )
        }
        Spacer(Modifier.height(8.dp))
        SyringeGraphic(inputs.syringe, units, fits, onContainer)
        Spacer(Modifier.height(8.dp))
        ResultRow(stringResource(R.string.calc_concentration), stringResource(R.string.calc_mg_per_ml, Format.amount(recon.mgPerMl, 3)), onContainer)
        ResultRow(stringResource(R.string.calc_each_unit), stringResource(R.string.calc_mcg, Format.amount(recon.mcgPerUnit, 2)), onContainer)
        val doses = recon.dosesPerVial(doseMg)
        ResultRow(stringResource(R.string.calc_doses_per_vial), pluralStringResource(R.plurals.doses_count, doses, doses), onContainer)
    }

    warnings.forEach { warning ->
        val text = when (warning) {
            is Warning.OverCapacity -> stringResource(R.string.calc_warn_capacity, warning.syringe.label, warning.injections)
            Warning.HardToMeasure -> stringResource(R.string.calc_warn_small)
            Warning.MoreThanVial -> stringResource(R.string.calc_warn_more_than_vial)
        }
        SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
}

@Composable
private fun ResultRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
