package io.github.deveshb1.peptideessentials.ui.journal

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.JournalEntry
import io.github.deveshb1.peptideessentials.data.VialStatus
import io.github.deveshb1.peptideessentials.progress.MetricMath
import io.github.deveshb1.peptideessentials.timeline.Timeline
import io.github.deveshb1.peptideessentials.timeline.TimelineItem
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.EmptyState
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.Route
import java.time.LocalDate
import java.time.ZoneId

private enum class JournalView { ENTRIES, TIMELINE }

private const val TIMELINE_PAGE = 30

@Composable
fun JournalScreen(data: AppData, today: LocalDate, contentPadding: PaddingValues) {
    var view by rememberSaveable { mutableStateOf(JournalView.ENTRIES) }
    var timelineDays by rememberSaveable { mutableIntStateOf(TIMELINE_PAGE) }
    val nav = LocalNavigator.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding.withGutters(extraBottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "view") {
            ChipChoice(JournalView.entries, view, {
                stringResource(if (it == JournalView.ENTRIES) R.string.journal_entries else R.string.timeline)
            }) { view = it }
        }
        when (view) {
            JournalView.ENTRIES -> entries(data, today) { nav.push(Route.journal(it)) }
            JournalView.TIMELINE -> timeline(data, today, timelineDays) { timelineDays += TIMELINE_PAGE }
        }
    }
}

private fun LazyListScope.entries(data: AppData, today: LocalDate, open: (LocalDate) -> Unit) {
    if (data.journalFor(today) == null) {
        item(key = "prompt") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.journal_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Button(onClick = { open(today) }) { Text(stringResource(R.string.write_today)) }
                }
            }
        }
    }
    val sorted = data.journal.sortedByDescending { it.date }
    if (sorted.isEmpty()) {
        item(key = "empty") {
            EmptyState(
                icon = rememberVectorPainter(Icons.Filled.Create),
                title = stringResource(R.string.empty_journal_title),
                body = stringResource(R.string.empty_journal_body),
            )
        }
    }
    items(sorted, key = { it.date.toString() }) { JournalCard(it, today) { open(it.date) } }
}

@Composable
private fun JournalCard(entry: JournalEntry, today: LocalDate, onClick: () -> Unit) {
    val res = LocalResources.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Format.longDate(entry.date, today), style = MaterialTheme.typography.titleMedium)
            val ratings = Rating.entries.mapNotNull { r -> entry.rating(r)?.let { "${stringResource(r.label)} $it/5" } }
            if (ratings.isNotEmpty()) {
                Text(ratings.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (entry.sideEffects.isNotEmpty()) {
                Text(
                    entry.sideEffects.sorted().joinToString(", ") { Format.sideEffect(res, it) } +
                        (entry.reactionSite?.let { " (${Format.site(res, it)})" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun LazyListScope.timeline(data: AppData, today: LocalDate, days: Int, more: () -> Unit) {
    val all = Timeline.build(data, ZoneId.systemDefault())
    if (all.isEmpty()) {
        item(key = "empty-timeline") {
            EmptyState(
                icon = rememberVectorPainter(Icons.Filled.Star),
                title = stringResource(R.string.empty_timeline_title),
                body = stringResource(R.string.empty_timeline_body),
            )
        }
        return
    }
    all.take(days).forEach { day ->
        item(key = "t-${day.date}") {
            SectionHeader(
                when (day.date) {
                    today -> stringResource(R.string.day_today)
                    today.minusDays(1) -> stringResource(R.string.day_yesterday)
                    else -> Format.longDate(day.date, today)
                },
            )
        }
        items(day.items.size, key = { "t-${day.date}-$it" }) { index -> TimelineRow(day.items[index], data) }
    }
    if (all.size > days) {
        item(key = "more") {
            OutlinedButton(onClick = more, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.show_more)) }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem, data: AppData) {
    val res = LocalResources.current
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val (icon, title, detail) = when (item) {
        is TimelineItem.Dose -> {
            val e = item.entry
            if (e.status == DoseStatus.TAKEN) {
                Triple(
                    rememberVectorPainter(Icons.Filled.CheckCircle),
                    e.medicationName,
                    listOfNotNull(
                        stringResource(R.string.taken_at_time, Format.time(context, e.time.atZone(zone).toLocalTime())),
                        Format.dose(res, e.amount, e.unit),
                        e.site?.let { Format.site(res, it) },
                    ).joinToString(" · "),
                )
            } else {
                Triple(rememberVectorPainter(Icons.Filled.Close), e.medicationName, stringResource(R.string.status_skipped))
            }
        }
        is TimelineItem.VialMixed -> Triple(
            painterResource(R.drawable.ic_vial),
            stringResource(R.string.timeline_vial_mixed, data.medication(item.vial.medicationId)?.name.orEmpty()),
            Format.vialAmount(res, item.vial.amount, item.vial.unit) + " · " + Format.concentration(res, item.vial),
        )
        is TimelineItem.VialClosed -> Triple(
            painterResource(R.drawable.ic_vial),
            stringResource(
                if (item.vial.status == VialStatus.DISCARDED) R.string.timeline_vial_discarded else R.string.timeline_vial_finished,
                data.medication(item.vial.medicationId)?.name.orEmpty(),
            ),
            item.vial.label,
        )
        is TimelineItem.Journal -> Triple(
            rememberVectorPainter(Icons.Filled.Create),
            stringResource(R.string.timeline_journal),
            listOfNotNull(
                Rating.entries.mapNotNull { r -> item.entry.rating(r)?.let { "${stringResource(r.label)} $it/5" } }
                    .joinToString(" · ").ifBlank { null },
                item.entry.sideEffects.joinToString(", ") { Format.sideEffect(res, it) }.ifBlank { null },
                item.entry.note.ifBlank { null },
            ).joinToString("\n"),
        )
        is TimelineItem.Measured -> {
            val metric = data.metric(item.measurement.metricId)
            Triple(
                painterResource(R.drawable.ic_chart),
                metric?.let { Format.metricName(res, it) } ?: "",
                metric?.let {
                    Format.metricValue(res, MetricMath.toDisplay(item.measurement.value, it.kind, data.settings), it, data.settings)
                }.orEmpty(),
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
