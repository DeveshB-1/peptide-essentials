package io.github.deveshb1.peptideessentials.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.BodyArea
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.Side
import io.github.deveshb1.peptideessentials.sites.SiteSuggestion
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.sites.BodyModel
import io.github.deveshb1.peptideessentials.ui.sites.SiteUsage
import io.github.deveshb1.peptideessentials.ui.theme.HeatColors

/** Body model plus chip fallback for choosing where a dose went. */
@Composable
fun SitePicker(
    selected: InjectionSite?,
    suggestion: SiteSuggestion?,
    usage: SiteUsage,
    onSelect: (InjectionSite) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val suggested = suggestion?.site
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.injection_site), style = MaterialTheme.typography.titleSmall)
        BodyModel(
            siteColor = usage::color,
            selected = selected,
            suggested = suggested,
            onSelect = onSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
        HeatLegend()
        for (area in BodyArea.entries) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Format.area(res, area), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                for (side in Side.entries) {
                    val site = InjectionSite.entries.first { it.area == area && it.side == side }
                    val isSelected = site == selected
                    val isSuggested = site == suggested
                    val description = buildString {
                        append(Format.site(res, site))
                        if (isSuggested) append(", ").append(res.getString(R.string.suggested))
                        if (usage.isResting(site)) append(", ").append(res.getString(R.string.resting))
                    }
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
        if (suggestion != null && suggested != selected) {
            Text(
                stringResource(
                    if (suggestion.allResting) R.string.suggested_site_all_resting else R.string.suggested_site,
                    Format.site(res, suggestion.site),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (selected != null) SiteStatusLine(selected, usage)
    }
}

/** "Last used 3 days ago" with a warning while the site is inside its rest period. */
@Composable
fun SiteStatusLine(site: InjectionSite, usage: SiteUsage) {
    val days = usage.daysSince(site)
    val text = when (days) {
        null -> stringResource(R.string.site_never_used)
        0L -> stringResource(R.string.site_used_today)
        else -> pluralStringResource(R.plurals.site_used_days_ago, days.toInt(), days.toInt())
    }
    if (usage.isResting(site)) {
        val rest = usage.restDays(site)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Text(
                text + " · " + pluralStringResource(R.plurals.rest_period_warning, rest, rest),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HeatLegend() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(HeatColors.hot, stringResource(R.string.heat_recent))
        LegendItem(HeatColors.warm, stringResource(R.string.heat_resting))
        LegendItem(HeatColors.rested, stringResource(R.string.heat_rested))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ColorDot(color, size = 10.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
