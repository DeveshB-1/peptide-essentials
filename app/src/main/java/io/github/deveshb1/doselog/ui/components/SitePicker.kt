package io.github.deveshb1.doselog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.BodyArea
import io.github.deveshb1.doselog.data.InjectionSite
import io.github.deveshb1.doselog.data.Side
import io.github.deveshb1.doselog.ui.Format
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun SitePicker(
    selected: InjectionSite?,
    suggested: InjectionSite?,
    lastUsed: Map<InjectionSite, Instant>,
    today: LocalDate,
    onSelect: (InjectionSite) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.injection_site), style = MaterialTheme.typography.titleSmall)
        BodySchematic(
            selected = selected,
            suggested = suggested,
            onSelect = onSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
        for (area in BodyArea.entries) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    Format.area(res, area),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                for (side in Side.entries) {
                    val site = InjectionSite.entries.first { it.area == area && it.side == side }
                    val isSelected = site == selected
                    val isSuggested = site == suggested
                    val description = Format.site(res, site) +
                        if (isSuggested) ", " + stringResource(R.string.suggested) else ""
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(site) },
                        label = { Text(Format.side(res, side)) },
                        leadingIcon = when {
                            isSelected -> {
                                { Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            }
                            isSuggested -> {
                                { Icon(Icons.Filled.Star, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            }
                            else -> null
                        },
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                }
            }
        }
        if (suggested != null && suggested != selected) {
            Text(
                stringResource(R.string.suggested_site, Format.site(res, suggested)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (selected != null) {
            val last = lastUsed[selected]?.atZone(ZoneId.systemDefault())?.toLocalDate()
            val text = when (val days = last?.let { ChronoUnit.DAYS.between(it, today) }) {
                null -> stringResource(R.string.site_never_used)
                0L -> stringResource(R.string.site_used_today)
                else -> pluralStringResource(R.plurals.site_used_days_ago, days.toInt(), days.toInt())
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
