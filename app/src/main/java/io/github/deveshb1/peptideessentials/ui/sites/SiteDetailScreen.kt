package io.github.deveshb1.peptideessentials.ui.sites

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.SiteStatusLine
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SiteDetailScreen(data: AppData, site: InjectionSite, today: LocalDate) {
    val res = LocalResources.current
    val context = LocalContext.current
    val actions = rememberActions()
    val zone = ZoneId.systemDefault()
    val usage = remember(data, today) { SiteUsage(data.entries, data, today) }
    val uses = remember(data, site) {
        data.entries.filter { it.site == site && it.status == DoseStatus.TAKEN }.sortedByDescending { it.time }
    }
    val reactions = remember(data, site) { data.journal.filter { it.reactionSite == site }.sortedByDescending { it.date } }

    SubScreen(title = Format.site(res, site)) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item(key = "summary") {
                SectionCard {
                    BodyModel(
                        siteColor = usage::color,
                        selected = site,
                        suggested = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                    SiteStatusLine(site, usage)
                    Text(pluralStringResource(R.plurals.injections_count, uses.size, uses.size), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.rest_period), style = MaterialTheme.typography.titleSmall)
                    RestStepper(usage.restDays(site), onChange = { days ->
                        actions.run { updateSettings { it.copy(siteRestDays = it.siteRestDays + (site to days)) } }
                    })
                }
            }
            if (reactions.isNotEmpty()) {
                item(key = "reactions") { SectionHeader(stringResource(R.string.site_reactions), color = MaterialTheme.colorScheme.error) }
                items(reactions, key = { "r-${it.date}" }) { j ->
                    ListItem(
                        headlineContent = { Text(Format.longDate(j.date, today)) },
                        supportingContent = { Text(j.note.ifBlank { stringResource(R.string.no_note) }) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
            item(key = "uses") { SectionHeader(stringResource(R.string.site_history)) }
            if (uses.isEmpty()) {
                item(key = "none") { Text(stringResource(R.string.site_never_used), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(uses, key = { it.id }) { e ->
                val local = e.time.atZone(zone)
                ListItem(
                    headlineContent = { Text(e.medicationName) },
                    supportingContent = { Text(Format.dose(res, e.amount, e.unit)) },
                    trailingContent = {
                        Text(
                            stringResource(R.string.day_and_time, Format.relativeDay(res, local.toLocalDate(), today), Format.time(context, local.toLocalTime())),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}
