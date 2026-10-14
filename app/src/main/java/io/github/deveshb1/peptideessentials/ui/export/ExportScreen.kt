package io.github.deveshb1.peptideessentials.ui.export

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.export.Backup
import io.github.deveshb1.peptideessentials.export.CsvExport
import io.github.deveshb1.peptideessentials.export.CsvTable
import io.github.deveshb1.peptideessentials.export.PdfReport
import io.github.deveshb1.peptideessentials.export.ShareFile
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.DateDialog
import io.github.deveshb1.peptideessentials.ui.components.PickerRow
import io.github.deveshb1.peptideessentials.ui.components.SectionCard
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private enum class ReportRange(val days: Long?) { D30(30), D90(90), D180(180), CUSTOM(null) }

@Composable
fun ExportScreen(data: AppData, today: LocalDate) {
    val context = LocalContext.current
    val res = LocalResources.current
    val actions = rememberActions()
    val scope = rememberCoroutineScope()
    var reportRange by rememberSaveable { mutableStateOf(ReportRange.D90) }
    var customFrom by rememberSaveable { mutableLongStateOf(today.minusDays(90).toEpochDay()) }
    var customTo by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<AppData?>(null) }

    val from = reportRange.days?.let { today.minusDays(it - 1) } ?: LocalDate.ofEpochDay(customFrom)
    val to = if (reportRange == ReportRange.CUSTOM) LocalDate.ofEpochDay(customTo) else today

    fun share(block: () -> Unit) {
        try {
            block()
        } catch (_: ActivityNotFoundException) {
            actions.message(res.getString(R.string.export_failed))
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching { Backup.write(context, uri, actions.repository.data.value) }.isSuccess
                actions.message(res.getString(if (ok) R.string.backup_saved else R.string.backup_failed))
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { Backup.read(context, uri) }
                    .onSuccess { pendingRestore = it }
                    .onFailure { actions.message(res.getString(R.string.restore_failed)) }
            }
        }
    }

    SubScreen(title = stringResource(R.string.export_and_backup)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = stringResource(R.string.report_section)) {
                Text(stringResource(R.string.report_section_body), style = MaterialTheme.typography.bodyMedium)
                ChipChoice(ReportRange.entries, reportRange, {
                    when (it) {
                        ReportRange.D30 -> stringResource(R.string.last_30_days)
                        ReportRange.D90 -> stringResource(R.string.last_90_days)
                        ReportRange.D180 -> stringResource(R.string.last_6_months)
                        ReportRange.CUSTOM -> stringResource(R.string.custom_range)
                    }
                }) { reportRange = it }
                if (reportRange == ReportRange.CUSTOM) {
                    PickerRow(rememberVectorPainter(Icons.Filled.DateRange), stringResource(R.string.from), Format.longDate(from, today), { dialog = "from" })
                    PickerRow(rememberVectorPainter(Icons.Filled.DateRange), stringResource(R.string.to), Format.longDate(to, today), { dialog = "to" })
                }
                Button(
                    enabled = !busy && from <= to,
                    onClick = {
                        busy = true
                        scope.launch {
                            val file = runCatching {
                                withContext(Dispatchers.IO) { PdfReport.create(context, data, from, to, today) }
                            }.getOrNull()
                            busy = false
                            if (file == null) {
                                actions.message(res.getString(R.string.report_failed))
                            } else {
                                share { ShareFile.share(context, file, "application/pdf", res.getString(R.string.report_title)) }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.create_report))
                }
            }

            SectionCard(title = stringResource(R.string.csv_section)) {
                Text(stringResource(R.string.csv_section_body), style = MaterialTheme.typography.bodyMedium)
                CsvTable.entries.forEach { table ->
                    OutlinedButton(
                        onClick = {
                            if (CsvExport.isEmpty(table, data)) {
                                actions.message(res.getString(R.string.nothing_to_export))
                            } else {
                                val file = ShareFile.newFile(context, "peptide-essentials-${table.fileStem}-$today.csv")
                                file.writeText(CsvExport.build(table, data, ZoneId.systemDefault()))
                                share { ShareFile.share(context, file, "text/csv", res.getString(R.string.export_subject)) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Share, null)
                        Text(csvLabel(table), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            SectionCard(title = stringResource(R.string.backup_section)) {
                Text(stringResource(R.string.backup_section_body), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { backupLauncher.launch("peptide-essentials-backup-$today.json") }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.backup))
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.restore)) }
            }
        }
    }

    when (dialog) {
        "from" -> DateDialog(LocalDate.ofEpochDay(customFrom), { customFrom = it.toEpochDay(); dialog = null }, { dialog = null }, latest = today)
        "to" -> DateDialog(LocalDate.ofEpochDay(customTo), { customTo = it.toEpochDay(); dialog = null }, { dialog = null }, latest = today)
    }

    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.restore_body,
                        pluralStringResource(R.plurals.medication_count, backup.medications.size, backup.medications.size),
                        pluralStringResource(R.plurals.entry_count, backup.entries.size, backup.entries.size),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    actions.run { replaceAll(backup) }
                    actions.message(res.getString(R.string.restore_done))
                }) { Text(stringResource(R.string.restore_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun csvLabel(table: CsvTable): String = stringResource(
    when (table) {
        CsvTable.DOSES -> R.string.csv_doses
        CsvTable.VIALS -> R.string.csv_vials
        CsvTable.MEASUREMENTS -> R.string.csv_measurements
        CsvTable.JOURNAL -> R.string.csv_journal
    },
)
