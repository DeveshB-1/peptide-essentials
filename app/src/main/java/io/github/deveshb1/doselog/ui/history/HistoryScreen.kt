package io.github.deveshb1.doselog.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.DoseEntry
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.ui.Format
import io.github.deveshb1.doselog.ui.components.ColorDot
import io.github.deveshb1.doselog.ui.components.EmptyState
import io.github.deveshb1.doselog.ui.components.SectionHeader
import io.github.deveshb1.doselog.ui.components.withGutters
import io.github.deveshb1.doselog.ui.theme.TagColors
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HistoryScreen(
    data: AppData,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpenEntry: (DoseEntry) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    // Medications that appear in history, named as most recently logged (or as currently named).
    val filters = remember(data) {
        data.entries
            .sortedBy { it.time }
            .associate { it.medicationId to (data.medication(it.medicationId)?.name ?: it.medicationName) }
            .toList()
            .sortedBy { it.second.lowercase() }
    }
    // A filter only applies while its chip is shown.
    val activeFilter = filter?.takeIf { id -> filters.size > 1 && filters.any { it.first == id } }
    val groups = remember(data, activeFilter) {
        data.entries
            .filter { activeFilter == null || it.medicationId == activeFilter }
            .sortedByDescending { it.time }
            .groupBy { it.displayDate(zone) }
            .toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding.withGutters(),
    ) {
        if (filters.size > 1) {
            item(key = "filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = activeFilter == null,
                            onClick = { filter = null },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    items(filters, key = { it.first }) { (id, name) ->
                        FilterChip(
                            selected = activeFilter == id,
                            onClick = { filter = if (activeFilter == id) null else id },
                            label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                    title = stringResource(R.string.empty_history_title),
                    body = stringResource(R.string.empty_history_body),
                )
            }
        }

        groups.forEach { (date, entries) ->
            item(key = "h-$date") {
                SectionHeader(
                    when (date) {
                        today -> stringResource(R.string.day_today)
                        today.minusDays(1) -> stringResource(R.string.day_yesterday)
                        else -> Format.longDate(date, today)
                    },
                )
            }
            items(entries, key = { it.id }) { entry -> HistoryRow(entry, data, onOpenEntry) }
        }
    }
}

@Composable
private fun HistoryRow(entry: DoseEntry, data: AppData, onOpen: (DoseEntry) -> Unit) {
    val res = LocalResources.current
    val context = LocalContext.current
    val taken = entry.status == DoseStatus.TAKEN
    val color = data.medication(entry.medicationId)?.let { TagColors.of(it.colorIndex).color }
        ?: MaterialTheme.colorScheme.outline
    val detail = if (taken) {
        listOfNotNull(Format.dose(res, entry.amount, entry.unit), entry.site?.let { Format.site(res, it) })
            .joinToString(" · ")
    } else {
        stringResource(R.string.status_skipped)
    }
    ListItem(
        headlineContent = { Text(entry.medicationName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(if (entry.note.isBlank()) detail else "$detail\n${entry.note}", maxLines = 3, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = { ColorDot(color) },
        trailingContent = {
            if (taken) {
                Text(
                    Format.time(context, entry.time.atZone(ZoneId.systemDefault()).toLocalTime()),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.edit_entry)) { onOpen(entry) },
    )
}
