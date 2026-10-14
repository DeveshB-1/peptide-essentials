package io.github.deveshb1.peptideessentials.ui.more

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.LengthUnit
import io.github.deveshb1.peptideessentials.data.WeightUnit
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.components.SwitchRow
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import io.github.deveshb1.peptideessentials.ui.sites.RestStepper
import io.github.deveshb1.peptideessentials.data.Settings as AppSettings

private const val MAX_USE_BY_DAYS = 120

@Composable
fun SettingsScreen(data: AppData) {
    val context = LocalContext.current
    val actions = rememberActions()
    val settings = data.settings
    fun update(transform: (AppSettings) -> AppSettings) = actions.run { updateSettings(transform) }

    SubScreen(title = stringResource(R.string.settings)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = stringResource(R.string.settings_units)) {
                Text(stringResource(R.string.metric_weight), style = MaterialTheme.typography.bodyMedium)
                ChipChoice(WeightUnit.entries, settings.weightUnit, {
                    stringResource(if (it == WeightUnit.KG) R.string.unit_kg else R.string.unit_lb)
                }) { unit -> update { it.copy(weightUnit = unit) } }
                Text(stringResource(R.string.metric_waist), style = MaterialTheme.typography.bodyMedium)
                ChipChoice(LengthUnit.entries, settings.lengthUnit, {
                    stringResource(if (it == LengthUnit.CM) R.string.unit_cm else R.string.unit_in)
                }) { unit -> update { it.copy(lengthUnit = unit) } }
            }

            SectionCard(title = stringResource(R.string.settings_vials)) {
                Text(stringResource(R.string.settings_use_by), style = MaterialTheme.typography.bodyMedium)
                RestStepper(settings.useByDays, onChange = { days ->
                    update { it.copy(useByDays = days.coerceIn(1, MAX_USE_BY_DAYS)) }
                })
                Text(stringResource(R.string.settings_use_by_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard(title = stringResource(R.string.settings_sites)) {
                Text(stringResource(R.string.settings_rest_all), style = MaterialTheme.typography.bodyMedium)
                val common = InjectionSite.entries.map(settings::restDays).distinct().singleOrNull()
                RestStepper(common ?: AppSettings.DEFAULT_REST_DAYS, onChange = { days ->
                    update { it.copy(siteRestDays = InjectionSite.entries.associateWith { days }) }
                })
                Text(
                    stringResource(if (common == null) R.string.settings_rest_mixed else R.string.settings_rest_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (data.customSideEffects.isNotEmpty()) {
                SectionCard(title = stringResource(R.string.settings_custom_side_effects)) {
                    data.customSideEffects.forEach { label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f))
                            IconButton(onClick = { actions.run { removeCustomSideEffect(label) } }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.remove_item, label))
                            }
                        }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_appearance)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        title = stringResource(R.string.settings_dynamic_color),
                        body = stringResource(R.string.settings_dynamic_color_body),
                        checked = settings.dynamicColor,
                        onCheckedChange = { on -> update { it.copy(dynamicColor = on) } },
                    )
                } else {
                    Text(stringResource(R.string.settings_theme_follows_system), style = MaterialTheme.typography.bodyMedium)
                }
            }

            SectionCard(title = stringResource(R.string.reminders)) {
                Text(stringResource(R.string.settings_notifications_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        actions.message(context.getString(R.string.notifications_denied))
                    }
                }) { Text(stringResource(R.string.settings_open_notifications)) }
            }
        }
    }
}
