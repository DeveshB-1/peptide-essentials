package io.github.deveshb1.peptideessentials.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(colorResource(R.color.icon_bg)),
                ) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
                }
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.not_medical_advice_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        stringResource(R.string.not_medical_advice_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            AboutSection(R.string.about_data_title, R.string.about_data_body)
            AboutSection(R.string.about_reminders_title, R.string.about_reminders_body)
            AboutSection(R.string.about_sites_title, R.string.about_sites_body)
            AboutSection(R.string.about_calc_title, R.string.about_calc_body)
            AboutSection(R.string.about_backup_title, R.string.about_backup_body)
            Column {
                SectionHeader(stringResource(R.string.privacy_policy))
                SelectionContainer {
                    Text(stringResource(R.string.privacy_policy_url), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AboutSection(title: Int, body: Int) {
    Column {
        SectionHeader(stringResource(title))
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Shown once on first launch; can't be dismissed without acknowledging. */
@Composable
fun FirstLaunchNotice(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        icon = { Icon(Icons.Filled.Info, null) },
        title = { Text(stringResource(R.string.welcome_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.welcome_body))
                Text(stringResource(R.string.welcome_privacy))
            }
        },
        confirmButton = { TextButton(onClick = onAccept) { Text(stringResource(R.string.i_understand)) } },
    )
}
