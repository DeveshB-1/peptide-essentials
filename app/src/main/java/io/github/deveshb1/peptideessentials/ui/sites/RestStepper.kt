package io.github.deveshb1.peptideessentials.ui.sites

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R

const val MIN_REST_DAYS = 1
const val MAX_REST_DAYS = 60

/** "− 7 days +" control for a site's rest period. */
@Composable
fun RestStepper(days: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onChange((days - 1).coerceAtLeast(MIN_REST_DAYS)) }, enabled = days > MIN_REST_DAYS) {
            Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.fewer_days))
        }
        Text(
            pluralStringResource(R.plurals.days_count, days, days),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        FilledTonalIconButton(onClick = { onChange((days + 1).coerceAtMost(MAX_REST_DAYS)) }, enabled = days < MAX_REST_DAYS) {
            Icon(Icons.Filled.Add, stringResource(R.string.more_days))
        }
    }
}
