package io.github.deveshb1.doselog.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.DoseEntry
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.export.Backup
import io.github.deveshb1.doselog.export.CsvExport
import io.github.deveshb1.doselog.reminders.Notifications
import io.github.deveshb1.doselog.repository
import io.github.deveshb1.doselog.schedule.SiteRotation
import io.github.deveshb1.doselog.ui.about.AboutScreen
import io.github.deveshb1.doselog.ui.about.FirstLaunchNotice
import io.github.deveshb1.doselog.ui.components.DoseSheet
import io.github.deveshb1.doselog.ui.history.HistoryScreen
import io.github.deveshb1.doselog.ui.medications.MedicationEditorScreen
import io.github.deveshb1.doselog.ui.medications.MedicationsScreen
import io.github.deveshb1.doselog.ui.today.TodayScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

private enum class Tab(@param:StringRes val label: Int) { TODAY(R.string.tab_today), HISTORY(R.string.tab_history), MEDICATIONS(R.string.tab_medications) }

private const val PREFS = "settings"
private const val KEY_NOTICE_ACCEPTED = "notice_accepted"
private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
private const val NEW_MEDICATION = ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseLogRoot() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val res = LocalResources.current
    val repository = context.repository
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val data by repository.data.collectAsStateWithLifecycle()
    val loaded by repository.loaded.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    // "new:<medicationId>:<date>" to log a due dose, "edit:<entryId>" to edit history.
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var noticeAccepted by remember { mutableStateOf(prefs.getBoolean(KEY_NOTICE_ACCEPTED, false)) }
    var pendingRestore by remember { mutableStateOf<AppData?>(null) }
    var notificationsAllowed by remember { mutableStateOf(Notifications.canPost(context)) }

    val now by produceState(ZonedDateTime.now()) {
        repository.ensureLoaded()
        while (true) {
            value = ZonedDateTime.now()
            delay(30_000)
        }
    }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = Notifications.canPost(context)
        onPauseOrDispose { }
    }

    fun message(@StringRes id: Int) {
        scope.launch { snackbar.showSnackbar(res.getString(id)) }
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            message(R.string.notifications_denied)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = Notifications.canPost(context)
        if (!granted) {
            scope.launch {
                val result = snackbar.showSnackbar(
                    res.getString(R.string.notifications_denied),
                    actionLabel = res.getString(R.string.settings),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) openNotificationSettings()
            }
        }
    }

    /** Asks for notification permission the first time reminders are needed, or points to Settings. */
    fun enableNotifications(fromBanner: Boolean) {
        if (Notifications.canPost(context)) return
        val askedBefore = prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)
        val canAsk = Build.VERSION.SDK_INT >= 33 && (
            !askedBefore || activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
            )
        when {
            canAsk -> {
                prefs.edit { putBoolean(KEY_ASKED_NOTIFICATIONS, true) }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            fromBanner -> openNotificationSettings()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching { Backup.write(context, uri, repository.data.value) }.isSuccess
                message(if (ok) R.string.backup_saved else R.string.backup_failed)
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { Backup.read(context, uri) }
                    .onSuccess { pendingRestore = it }
                    .onFailure { message(R.string.restore_failed) }
            }
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when {
        editingId != null -> {
            val id = editingId!!
            BackHandler { editingId = null }
            MedicationEditorScreen(
                existing = data.medication(id).takeIf { id != NEW_MEDICATION },
                snackbarHostState = snackbar,
                onSave = { med ->
                    editingId = null
                    scope.launch {
                        repository.saveMedication(med)
                        if (med.remindersEnabled && !med.paused) enableNotifications(fromBanner = false)
                    }
                },
                onDelete = { med ->
                    editingId = null
                    scope.launch {
                        repository.deleteMedication(med.id)
                        snackbar.showSnackbar(res.getString(R.string.medication_deleted, med.name))
                    }
                },
                onClose = { editingId = null },
            )
        }
        showAbout -> {
            BackHandler { showAbout = false }
            AboutScreen(onBack = { showAbout = false })
        }
        else -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (tab == Tab.TODAY) R.string.app_name else tab.label)) },
                    actions = {
                        OverflowMenu(
                            onExport = {
                                if (data.entries.isEmpty()) {
                                    message(R.string.nothing_to_export)
                                } else {
                                    try {
                                        CsvExport.share(context, data)
                                    } catch (_: ActivityNotFoundException) {
                                        message(R.string.export_failed)
                                    }
                                }
                            },
                            onBackup = { backupLauncher.launch("dose-log-backup-${LocalDate.now()}.json") },
                            onRestore = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                            onAbout = { showAbout = true },
                        )
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(tabIcon(item), contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == Tab.MEDICATIONS) {
                    ExtendedFloatingActionButton(
                        onClick = { editingId = NEW_MEDICATION },
                        icon = { Icon(Icons.Filled.Add, null) },
                        text = { Text(stringResource(R.string.add_medication)) },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            when (tab) {
                Tab.TODAY -> TodayScreen(
                    data = data,
                    now = now,
                    contentPadding = padding,
                    showNotificationBanner = !notificationsAllowed &&
                        data.medications.any { it.remindersEnabled && !it.paused },
                    onEnableNotifications = { enableNotifications(fromBanner = true) },
                    onTake = { sheet = "new:${it.medication.id}:${it.date}" },
                    onSkip = { dose ->
                        scope.launch {
                            val entry = repository.logDose(dose.medication, dose.date, DoseStatus.SKIPPED, Instant.now())
                                ?: return@launch
                            val result = snackbar.showSnackbar(
                                res.getString(R.string.dose_skipped, dose.medication.name),
                                actionLabel = res.getString(R.string.undo),
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) repository.deleteEntry(entry.id)
                        }
                    },
                    onOpenEntry = { sheet = "edit:${it.id}" },
                    onAddMedication = { tab = Tab.MEDICATIONS; editingId = NEW_MEDICATION },
                )
                Tab.HISTORY -> HistoryScreen(
                    data = data,
                    today = now.toLocalDate(),
                    contentPadding = padding,
                    onOpenEntry = { sheet = "edit:${it.id}" },
                )
                Tab.MEDICATIONS -> MedicationsScreen(
                    data = data,
                    now = now,
                    contentPadding = padding,
                    onEdit = { editingId = it.id },
                    onSetPaused = { med, paused -> scope.launch { repository.setPaused(med.id, paused) } },
                    onAdd = { editingId = NEW_MEDICATION },
                )
            }
        }
    }

    sheet?.let { target ->
        DoseSheetHost(
            target = target,
            data = data,
            onDone = { sheet = null },
            onDeleted = { entry ->
                sheet = null
                scope.launch {
                    repository.deleteEntry(entry.id)
                    val result = snackbar.showSnackbar(
                        res.getString(R.string.entry_deleted),
                        actionLabel = res.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) repository.saveEntry(entry)
                }
            },
        )
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
                    scope.launch {
                        repository.replaceAll(backup)
                        message(R.string.restore_done)
                    }
                }) { Text(stringResource(R.string.restore_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (!noticeAccepted) {
        FirstLaunchNotice(onAccept = {
            prefs.edit { putBoolean(KEY_NOTICE_ACCEPTED, true) }
            noticeAccepted = true
        })
    }
}

/** Resolves a sheet target string into the logging or editing sheet. */
@Composable
private fun DoseSheetHost(
    target: String,
    data: AppData,
    onDone: () -> Unit,
    onDeleted: (DoseEntry) -> Unit,
) {
    val context = LocalContext.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    val repository = context.repository
    val parts = target.split(":")

    when (parts.firstOrNull()) {
        "new" -> {
            val med = data.medication(parts.getOrNull(1).orEmpty())
            val date = parts.getOrNull(2)?.let(LocalDate::parse)
            if (med == null || date == null) {
                LaunchedEffect(target) { onDone() }
                return
            }
            val suggested = if (med.usesInjectionSites) SiteRotation.suggest(med.id, data.entries) else null
            DoseSheet(
                title = med.name,
                subtitle = stringResource(
                    R.string.scheduled_for,
                    Format.relativeDay(res, date, LocalDate.now()),
                    Format.time(context, med.timeOfDay),
                ),
                unit = med.unit,
                initialStatus = DoseStatus.TAKEN,
                initialTime = Instant.now(),
                initialAmount = med.doseAmount,
                initialSite = suggested,
                initialNote = "",
                showSites = med.usesInjectionSites,
                suggestedSite = suggested,
                lastUsed = SiteRotation.lastUsed(med.id, data.entries),
                onSave = { result ->
                    onDone()
                    scope.launch {
                        repository.logDose(med, date, result.status, result.time, result.amount, result.site, result.note)
                    }
                },
                onDismiss = onDone,
            )
        }
        "edit" -> {
            val entry = data.entries.firstOrNull { it.id == parts.getOrNull(1) }
            if (entry == null) {
                LaunchedEffect(target) { onDone() }
                return
            }
            val med = data.medication(entry.medicationId)
            // Exclude the entry being edited so it doesn't count as the latest use of its own site.
            val others = data.entries.filterNot { it.id == entry.id }
            val showSites = entry.site != null || med?.usesInjectionSites == true
            DoseSheet(
                title = entry.medicationName,
                subtitle = entry.scheduledDate?.let {
                    stringResource(R.string.scheduled_for_date, Format.relativeDay(res, it, LocalDate.now()))
                } ?: stringResource(R.string.unscheduled_dose),
                unit = entry.unit,
                initialStatus = entry.status,
                initialTime = entry.time,
                initialAmount = entry.amount,
                initialSite = entry.site,
                initialNote = entry.note,
                showSites = showSites,
                suggestedSite = if (showSites) SiteRotation.suggest(entry.medicationId, others) else null,
                lastUsed = SiteRotation.lastUsed(entry.medicationId, others),
                onSave = { result ->
                    onDone()
                    scope.launch {
                        repository.saveEntry(
                            entry.copy(
                                status = result.status,
                                time = result.time,
                                amount = result.amount,
                                site = result.site,
                                note = result.note,
                            ),
                        )
                    }
                },
                onDismiss = onDone,
                onDelete = { onDeleted(entry) },
            )
        }
        else -> LaunchedEffect(target) { onDone() }
    }
}

@Composable
private fun OverflowMenu(onExport: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onAbout: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                R.string.export_csv to onExport,
                R.string.backup to onBackup,
                R.string.restore to onRestore,
                R.string.about to onAbout,
            ).forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun tabIcon(tab: Tab): Painter = when (tab) {
    Tab.TODAY -> rememberVectorPainter(Icons.Filled.DateRange)
    Tab.HISTORY -> rememberVectorPainter(Icons.AutoMirrored.Filled.List)
    Tab.MEDICATIONS -> painterResource(R.drawable.ic_pill)
}
