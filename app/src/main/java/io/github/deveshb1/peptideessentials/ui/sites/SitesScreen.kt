package io.github.deveshb1.peptideessentials.ui.sites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.sites.SiteRotation
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ColorDot
import io.github.deveshb1.peptideessentials.ui.components.HeatLegend
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.Route
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SitesScreen(data: AppData, today: LocalDate) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val usage = remember(data, today) { SiteUsage(data.entries, data, today) }
    val counts = remember(data) { SiteRotation.useCount(data.entries) }
    val suggestion = remember(data, today) { SiteRotation.suggest(data.entries, data.settings, today, ZoneId.systemDefault()) }

    SubScreen(title = stringResource(R.string.sites)) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item(key = "model") {
                SectionCard {
                    BodyModel(
                        siteColor = usage::color,
                        selected = null,
                        suggested = suggestion.site,
                        onSelect = { nav.push(Route.site(it.name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )
                    HeatLegend()
                    Text(
                        stringResource(
                            if (suggestion.allResting) R.string.suggested_site_all_resting else R.string.suggested_site,
                            Format.site(res, suggestion.site),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(stringResource(R.string.sites_tap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "header") { SectionHeader(stringResource(R.string.all_sites)) }
            items(InjectionSite.entries, key = { it.name }) { site ->
                val days = usage.daysSince(site)
                val last = when (days) {
                    null -> stringResource(R.string.never_used)
                    0L -> stringResource(R.string.used_today)
                    else -> pluralStringResource(R.plurals.used_days_ago, days.toInt(), days.toInt())
                }
                val count = counts[site] ?: 0
                ListItem(
                    headlineContent = { Text(Format.site(res, site)) },
                    supportingContent = {
                        Text(
                            listOf(last, pluralStringResource(R.plurals.injections_count, count, count)).joinToString(" · ") +
                                if (usage.isResting(site)) " · " + stringResource(R.string.resting) else "",
                        )
                    },
                    leadingContent = { ColorDot(usage.color(site).copy(alpha = 1f), size = 16.dp) },
                    trailingContent = {
                        Text(
                            pluralStringResource(R.plurals.rest_short, usage.restDays(site), usage.restDays(site)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { nav.push(Route.site(site.name)) },
                )
            }
        }
    }
}
