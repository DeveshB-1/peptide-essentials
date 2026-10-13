package io.github.deveshb1.doselog.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.DoseEntry
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.schedule.DosePlanner
import io.github.deveshb1.doselog.schedule.PlannedDose
import io.github.deveshb1.doselog.ui.Format
import io.github.deveshb1.doselog.ui.components.ColorDot
import io.github.deveshb1.doselog.ui.components.EmptyState
import io.github.deveshb1.doselog.ui.components.SectionHeader
import io.github.deveshb1.doselog.ui.components.withGutters
import io.github.deveshb1.doselog.ui.theme.TagColors
import java.time.ZonedDateTime

@Composable
fun TodayScreen(
    data: AppData,
    now: ZonedDateTime,
    contentPadding: PaddingValues,
    showNotificationBanner: Boolean,
    onEnableNotifications: () -> Unit,
    onTake: (PlannedDose) -> Unit,
    onSkip: (PlannedDose) -> Unit,
    onOpenEntry: (DoseEntry) -> Unit,
    onAddMedication: () -> Unit,
) {
    val plan = remember(data, now) { DosePlanner.plan(data, now) }
    val today = now.toLocalDate()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding.withGutters(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            val due = plan.overdue.size + plan.dueToday.size
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(Format.longDate(today, today), style = MaterialTheme.typography.headlineSmall)
                if (data.medications.isNotEmpty()) {
                    Text(
                        if (due == 0) stringResource(R.string.all_caught_up)
                        else pluralStringResource(R.plurals.doses_to_log, due, due),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (showNotificationBanner) {
            item(key = "notifications") { NotificationBanner(onEnableNotifications) }
        }

        if (data.medications.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = painterResource(R.drawable.ic_pill),
                    title = stringResource(R.string.empty_today_title),
                    body = stringResource(R.string.empty_today_body),
                    action = { Button(onClick = onAddMedication) { Text(stringResource(R.string.add_medication)) } },
                )
            }
            return@LazyColumn
        }

        if (plan.overdue.isNotEmpty()) {
            item(key = "overdue-header") {
                SectionHeader(stringResource(R.string.section_overdue), color = MaterialTheme.colorScheme.error)
            }
            items(plan.overdue, key = { "o-${it.medication.id}-${it.date}" }) { dose ->
                DueDoseCard(dose, now, overdue = true, onTake = { onTake(dose) }, onSkip = { onSkip(dose) })
            }
        }

        if (plan.dueToday.isNotEmpty()) {
            item(key = "due-header") { SectionHeader(stringResource(R.string.section_due_today)) }
            items(plan.dueToday, key = { "d-${it.medication.id}-${it.date}" }) { dose ->
                DueDoseCard(dose, now, overdue = false, onTake = { onTake(dose) }, onSkip = { onSkip(dose) })
            }
        } else if (plan.overdue.isEmpty()) {
            item(key = "done") { NothingDueCard() }
        }

        loggedSection(plan.loggedToday, data, onOpenEntry)
        upcomingSection(plan.upcoming, now)
    }
}

@Composable
private fun DueDoseCard(
    dose: PlannedDose,
    now: ZonedDateTime,
    overdue: Boolean,
    onTake: () -> Unit,
    onSkip: () -> Unit,
) {
    val res = LocalResources.current
    val context = LocalContext.current
    val med = dose.medication
    val late = !overdue && dose.dueAt.isBefore(now)
    val time = Format.time(context, med.timeOfDay)
    val whenText = when {
        overdue -> stringResource(R.string.missed_on, Format.relativeDay(res, dose.date, now.toLocalDate()), time)
        late -> stringResource(R.string.was_due_at, time)
        else -> stringResource(R.string.due_at, time)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(TagColors.of(med.colorIndex).color, size = 14.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        med.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        Format.dose(res, med.doseAmount, med.unit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        whenText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue || late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                val skipLabel = stringResource(R.string.skip_dose_description, med.name)
                val takeLabel = stringResource(R.string.take_dose_description, med.name)
                OutlinedButton(onClick = onSkip, modifier = Modifier.semantics { contentDescription = skipLabel }) {
                    Text(stringResource(R.string.skip))
                }
                Button(onClick = onTake, modifier = Modifier.semantics { contentDescription = takeLabel }) {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.taken))
                }
            }
        }
    }
}

@Composable
private fun NothingDueCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.nothing_due_today),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun NotificationBanner(onEnable: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.notifications_off_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEnable) { Text(stringResource(R.string.turn_on)) }
        }
    }
}

private fun LazyListScope.loggedSection(entries: List<DoseEntry>, data: AppData, onOpen: (DoseEntry) -> Unit) {
    if (entries.isEmpty()) return
    item(key = "logged-header") { SectionHeader(stringResource(R.string.section_logged_today)) }
    items(entries, key = { "l-${it.id}" }) { entry ->
        val res = LocalResources.current
        val context = LocalContext.current
        val taken = entry.status == DoseStatus.TAKEN
        val time = Format.time(context, entry.time.atZone(java.time.ZoneId.systemDefault()).toLocalTime())
        val detail = if (taken) {
            listOfNotNull(
                stringResource(R.string.taken_at_time, time),
                Format.dose(res, entry.amount, entry.unit),
                entry.site?.let { Format.site(res, it) },
            ).joinToString(" · ")
        } else {
            stringResource(R.string.status_skipped)
        }
        ListItem(
            headlineContent = { Text(entry.medicationName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(detail) },
            leadingContent = {
                Icon(
                    if (taken) Icons.Filled.CheckCircle else Icons.Filled.Close,
                    contentDescription = stringResource(if (taken) R.string.status_taken else R.string.status_skipped),
                    tint = if (taken) {
                        data.medication(entry.medicationId)?.let { TagColors.of(it.colorIndex).color }
                            ?: MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.clickable(onClickLabel = stringResource(R.string.edit_entry)) { onOpen(entry) },
        )
    }
}

private fun LazyListScope.upcomingSection(upcoming: List<PlannedDose>, now: ZonedDateTime) {
    if (upcoming.isEmpty()) return
    item(key = "upcoming-header") { SectionHeader(stringResource(R.string.section_upcoming)) }
    items(upcoming, key = { "u-${it.medication.id}-${it.date}" }) { dose ->
        val res = LocalResources.current
        val context = LocalContext.current
        val med = dose.medication
        ListItem(
            headlineContent = { Text(med.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    stringResource(
                        R.string.day_and_time,
                        Format.relativeDay(res, dose.date, now.toLocalDate()),
                        Format.time(context, med.timeOfDay),
                    ),
                )
            },
            leadingContent = { ColorDot(TagColors.of(med.colorIndex).color) },
            trailingContent = { Text(Format.dose(res, med.doseAmount, med.unit), style = MaterialTheme.typography.bodyMedium) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}
