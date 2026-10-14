package io.github.deveshb1.peptideessentials.ui.vials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.calc.Dosing
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialStatus
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ColorDot
import io.github.deveshb1.peptideessentials.ui.components.EmptyState
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.Route
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import io.github.deveshb1.peptideessentials.ui.theme.TagColors
import io.github.deveshb1.peptideessentials.vials.VialMath
import java.time.LocalDate

@Composable
fun VialsScreen(data: AppData, today: LocalDate, contentPadding: PaddingValues) {
    val nav = LocalNavigator.current
    val active = remember(data) { data.vials.filter { it.status == VialStatus.ACTIVE }.sortedBy { it.useBy } }
    val past = remember(data) { data.vials.filter { it.status != VialStatus.ACTIVE }.sortedByDescending { it.closedOn ?: it.mixedOn } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding.withGutters(extraBottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (data.vials.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = painterResource(R.drawable.ic_vial),
                    title = stringResource(R.string.empty_vials_title),
                    body = stringResource(R.string.empty_vials_body),
                    action = {
                        if (data.medications.isEmpty()) {
                            Button(onClick = { nav.push(Route.editMedication(null)) }) { Text(stringResource(R.string.add_medication)) }
                        } else {
                            Button(onClick = { nav.push(Route.editVial(null)) }) { Text(stringResource(R.string.add_vial)) }
                        }
                    },
                )
            }
        }
        if (active.isNotEmpty()) {
            item(key = "active-header") { SectionHeader(stringResource(R.string.vials_in_use)) }
            items(active, key = { it.id }) { VialCard(it, data, today) }
        }
        if (past.isNotEmpty()) {
            item(key = "past-header") { SectionHeader(stringResource(R.string.vials_past)) }
            items(past, key = { it.id }) { PastVialRow(it, data) }
        }
    }
}

@Composable
private fun VialCard(vial: Vial, data: AppData, today: LocalDate) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val actions = rememberActions()
    val med = data.medication(vial.medicationId)
    val state = VialMath.state(vial, med, data.entries, today)
    val color = med?.let { TagColors.of(it.colorIndex).color } ?: MaterialTheme.colorScheme.primary
    var menu by remember { mutableStateOf(false) }

    Card(
        onClick = { nav.push(Route.editVial(vial.id)) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.Top) {
            ProgressRing(state.fraction, color)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    med?.name ?: stringResource(R.string.unknown_medication),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (vial.label.isNotBlank()) {
                    Text(vial.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    stringResource(
                        R.string.vial_remaining,
                        Format.vialAmount(res, state.remaining, vial.unit),
                        Format.vialAmount(res, vial.amount, vial.unit),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    Format.concentration(res, vial),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                med?.let { Dosing.draw(it.doseAmount, it.unit, vial) }?.let {
                    Text(Format.draw(res, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                state.dosesLeft?.let {
                    Text(
                        pluralStringResource(R.plurals.doses_left, it, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.lowStock || state.empty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    useByText(vial, state.daysToUseBy, today),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.expired || state.expiring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options_for, med?.name.orEmpty()))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.finish_vial)) },
                        onClick = {
                            menu = false
                            actions.run { closeVial(vial.id, VialStatus.FINISHED) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.discard_vial)) },
                        onClick = {
                            menu = false
                            actions.run { closeVial(vial.id, VialStatus.DISCARDED) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_vial)) },
                        onClick = {
                            menu = false
                            nav.push(Route.editVial(null, vial.medicationId))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun useByText(vial: Vial, days: Long, today: LocalDate): String {
    val date = Format.relativeDay(LocalResources.current, vial.useBy, today)
    return when {
        days < 0 -> stringResource(R.string.vial_expired_on, date)
        days == 0L -> stringResource(R.string.vial_use_by_today)
        else -> stringResource(R.string.vial_use_by, date) + " · " + pluralStringResource(R.plurals.in_days, days.toInt(), days.toInt())
    }
}

@Composable
private fun PastVialRow(vial: Vial, data: AppData) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val med = data.medication(vial.medicationId)
    val status = stringResource(if (vial.status == VialStatus.FINISHED) R.string.vial_finished else R.string.vial_discarded)
    ListItem(
        headlineContent = { Text(med?.name ?: stringResource(R.string.unknown_medication), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(
                    Format.vialAmount(res, vial.amount, vial.unit),
                    vial.label.ifBlank { null },
                    status + (vial.closedOn?.let { " " + Format.shortDate(it) } ?: ""),
                ).joinToString(" · "),
            )
        },
        leadingContent = { ColorDot(med?.let { TagColors.of(it.colorIndex).color } ?: MaterialTheme.colorScheme.outline) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable { nav.push(Route.editVial(vial.id)) },
    )
}
