package io.github.deveshb1.peptideessentials.ui.more

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.ui.components.withGutters
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.Route

private data class MoreItem(@param:StringRes val title: Int, @param:StringRes val body: Int, @param:DrawableRes val icon: Int, val route: String)

private val items = listOf(
    MoreItem(R.string.tab_medications, R.string.more_medications, R.drawable.ic_pill, Route.MEDICATIONS),
    MoreItem(R.string.calculator, R.string.more_calculator, R.drawable.ic_syringe, Route.CALCULATOR),
    MoreItem(R.string.sites, R.string.more_sites, R.drawable.ic_body, Route.SITES),
    MoreItem(R.string.tab_history, R.string.more_history, R.drawable.ic_history, Route.HISTORY),
    MoreItem(R.string.export_and_backup, R.string.more_export, R.drawable.ic_export, Route.EXPORT),
    MoreItem(R.string.settings, R.string.more_settings, R.drawable.ic_settings, Route.SETTINGS),
    MoreItem(R.string.about, R.string.more_about, R.drawable.ic_info, Route.ABOUT),
)

@Composable
fun MoreScreen(contentPadding: PaddingValues) {
    val nav = LocalNavigator.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding.withGutters(horizontal = 0.dp)) {
        items(items, key = { it.route }) { item ->
            ListItem(
                headlineContent = { Text(stringResource(item.title)) },
                supportingContent = { Text(stringResource(item.body)) },
                leadingContent = { Icon(painterResource(item.icon), null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable { nav.push(item.route) },
            )
        }
    }
}

