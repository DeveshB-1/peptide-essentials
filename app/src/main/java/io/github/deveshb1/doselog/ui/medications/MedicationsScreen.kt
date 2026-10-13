package io.github.deveshb1.doselog.ui.medications

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.Medication
import io.github.deveshb1.doselog.schedule.ScheduleMath
import io.github.deveshb1.doselog.ui.Format
import io.github.deveshb1.doselog.ui.components.ColorDot
import io.github.deveshb1.doselog.ui.components.EmptyState
import io.github.deveshb1.doselog.ui.components.withGutters
import io.github.deveshb1.doselog.ui.theme.TagColors
import java.time.ZonedDateTime

@Composable
fun MedicationsScreen(
    data: AppData,
    now: ZonedDateTime,
    contentPadding: PaddingValues,
    onEdit: (Medication) -> Unit,
    onSetPaused: (Medication, Boolean) -> Unit,
    onAdd: () -> Unit,
) {
    val medications = remember(data) {
        data.medications.sortedWith(compareBy<Medication> { it.paused }.thenBy { it.name.lowercase() })
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Leave room for the floating action button.
        contentPadding = contentPadding.withGutters(extraBottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (medications.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = painterResource(R.drawable.ic_pill),
                    title = stringResource(R.string.empty_medications_title),
                    body = stringResource(R.string.empty_medications_body),
                    action = { Button(onClick = onAdd) { Text(stringResource(R.string.add_medication)) } },
                )
            }
        }
        items(medications, key = { it.id }) { med ->
            MedicationCard(med, data, now, onEdit = { onEdit(med) }, onSetPaused = { onSetPaused(med, it) })
        }
    }
}

@Composable
private fun MedicationCard(
    med: Medication,
    data: AppData,
    now: ZonedDateTime,
    onEdit: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
) {
    val res = LocalResources.current
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val time = Format.time(context, med.timeOfDay)
    val status = if (med.paused) {
        stringResource(R.string.paused)
    } else {
        val logged = data.entries.filter { it.medicationId == med.id }.mapNotNull { it.scheduledDate }.toSet()
        val next = ScheduleMath.nextReminder(med, logged, now)
        if (next == null) {
            stringResource(R.string.no_upcoming_doses)
        } else {
            stringResource(R.string.next_dose, Format.relativeDay(res, next.toLocalDate(), now.toLocalDate()), time)
        }
    }
    val tags = listOfNotNull(
        stringResource(R.string.tag_sites).takeIf { med.usesInjectionSites },
        stringResource(R.string.tag_reminders_off).takeIf { !med.remindersEnabled },
    )

    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ColorDot(
                TagColors.of(med.colorIndex).color,
                size = 14.dp,
                modifier = Modifier
                    .padding(top = 5.dp)
                    .alpha(if (med.paused) 0.4f else 1f),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(med.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        R.string.dose_schedule_line,
                        Format.dose(res, med.doseAmount, med.unit),
                        Format.schedule(res, med.schedule),
                        time,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (med.paused) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options_for, med.name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (med.paused) R.string.resume else R.string.pause)) },
                        onClick = {
                            menuOpen = false
                            onSetPaused(!med.paused)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                }
            }
        }
    }
}
