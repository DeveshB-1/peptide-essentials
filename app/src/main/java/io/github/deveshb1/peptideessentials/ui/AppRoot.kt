package io.github.deveshb1.peptideessentials.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.calc.drawFor
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.VialStatus
import io.github.deveshb1.peptideessentials.reminders.Notifications
import io.github.deveshb1.peptideessentials.repository
import io.github.deveshb1.peptideessentials.sites.SiteRotation
import io.github.deveshb1.peptideessentials.ui.about.AboutScreen
import io.github.deveshb1.peptideessentials.ui.about.FirstLaunchNotice
import io.github.deveshb1.peptideessentials.ui.calculator.CalculatorScreen
import io.github.deveshb1.peptideessentials.ui.components.DoseSheet
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.export.ExportScreen
import io.github.deveshb1.peptideessentials.ui.history.HistoryScreen
import io.github.deveshb1.peptideessentials.ui.journal.JournalEditorScreen
import io.github.deveshb1.peptideessentials.ui.journal.JournalScreen
import io.github.deveshb1.peptideessentials.ui.medications.MedicationEditorScreen
import io.github.deveshb1.peptideessentials.ui.medications.MedicationsScreen
import io.github.deveshb1.peptideessentials.ui.more.MoreScreen
import io.github.deveshb1.peptideessentials.ui.more.SettingsScreen
import io.github.deveshb1.peptideessentials.ui.nav.LocalAppData
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.LocalNow
import io.github.deveshb1.peptideessentials.ui.nav.LocalSnackbar
import io.github.deveshb1.peptideessentials.ui.nav.Navigator
import io.github.deveshb1.peptideessentials.ui.nav.Route
import io.github.deveshb1.peptideessentials.ui.progress.MeasurementDialog
import io.github.deveshb1.peptideessentials.ui.progress.MetricDetailScreen
import io.github.deveshb1.peptideessentials.ui.progress.ProgressScreen
import io.github.deveshb1.peptideessentials.ui.sites.SiteDetailScreen
import io.github.deveshb1.peptideessentials.ui.sites.SiteUsage
import io.github.deveshb1.peptideessentials.ui.sites.SitesScreen
import io.github.deveshb1.peptideessentials.ui.today.TodayScreen
import io.github.deveshb1.peptideessentials.ui.vials.VialEditorScreen
import io.github.deveshb1.peptideessentials.ui.vials.VialsScreen
import io.github.deveshb1.peptideessentials.vials.VialMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private enum class Tab(@param:StringRes val label: Int) {
    TODAY(R.string.tab_today),
    VIALS(R.string.tab_vials),
    PROGRESS(R.string.tab_progress),
    JOURNAL(R.string.tab_journal),
    MORE(R.string.tab_more),
}

private const val PREFS = "settings"
private const val KEY_NOTICE_ACCEPTED = "notice_accepted"
private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
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
    // Pushed screens as newline-separated routes, so the stack survives recreation.
    var stackText by rememberSaveable { mutableStateOf("") }
    val navigator = remember {
        Navigator(read = { stackText.split('\n').filter(String::isNotEmpty) }, write = { stackText = it.joinToString("\n") })
    }
    // "new:<medicationId>:<date>" to log a due dose, "edit:<entryId>" to edit history.
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var addMeasurement by rememberSaveable { mutableStateOf(false) }
    var noticeAccepted by remember { mutableStateOf(prefs.getBoolean(KEY_NOTICE_ACCEPTED, false)) }
    var notificationsAllowed by remember { mutableStateOf(Notifications.canPost(context)) }

    val now by produceState(ZonedDateTime.now()) {
        repository.ensureLoaded()
        while (true) {
            value = ZonedDateTime.now()
            delay(30_000)
        }
    }
    val today = now.toLocalDate()
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = Notifications.canPost(context)
        onPauseOrDispose { }
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(res.getString(R.string.notifications_denied)) }
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

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalSnackbar provides snackbar,
        LocalAppData provides data,
        LocalNow provides now,
    ) {
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@CompositionLocalProvider
        }

        val top = navigator.stack.lastOrNull()
        if (top != null) {
            BackHandler { navigator.pop() }
            val (name, args) = Route.parse(top)
            when (name) {
                Route.MEDICATIONS -> SubScreen(
                    title = stringResource(R.string.tab_medications),
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { navigator.push(Route.editMedication(null)) },
                            icon = { Icon(Icons.Filled.Add, null) },
                            text = { Text(stringResource(R.string.add_medication)) },
                        )
                    },
                ) { padding ->
                    MedicationsScreen(
                        data = data,
                        now = now,
                        contentPadding = padding,
                        onEdit = { navigator.push(Route.editMedication(it.id)) },
                        onSetPaused = { med, paused -> scope.launch { repository.setPaused(med.id, paused) } },
                        onAdd = { navigator.push(Route.editMedication(null)) },
                    )
                }
                "medication" -> MedicationEditorScreen(
                    existing = args.getOrNull(0)?.let(data::medication),
                    snackbarHostState = snackbar,
                    onSave = { med ->
                        navigator.pop()
                        scope.launch {
                            repository.saveMedication(med)
                            if (med.remindersEnabled && !med.paused) enableNotifications(fromBanner = false)
                        }
                    },
                    onDelete = { med ->
                        navigator.pop()
                        scope.launch {
                            repository.deleteMedication(med.id)
                            snackbar.showSnackbar(res.getString(R.string.medication_deleted, med.name))
                        }
                    },
                    onClose = navigator::pop,
                )
                Route.CALCULATOR -> CalculatorScreen(data)
                Route.SITES -> SitesScreen(data, today)
                "site" -> InjectionSite.fromName(args.getOrNull(0))?.let { SiteDetailScreen(data, it, today) }
                Route.HISTORY -> SubScreen(title = stringResource(R.string.tab_history)) { padding ->
                    HistoryScreen(data = data, today = today, contentPadding = padding, onOpenEntry = { sheet = "edit:${it.id}" })
                }
                Route.EXPORT -> ExportScreen(data, today)
                Route.SETTINGS -> SettingsScreen(data)
                Route.ABOUT -> AboutScreen(onBack = navigator::pop)
                "vial" -> VialEditorScreen(data, args.getOrNull(0), args.getOrNull(1))
                "metric" -> args.getOrNull(0)?.let { MetricDetailScreen(data, it, today) }
                "journal" -> JournalEditorScreen(data, args.getOrNull(0)?.let(LocalDate::parse) ?: today)
                else -> LaunchedEffect(top) { navigator.pop() }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(if (tab == Tab.TODAY) R.string.app_name else tab.label)) },
                        actions = {
                            if (tab == Tab.TODAY || tab == Tab.VIALS) {
                                IconButton(onClick = { navigator.push(Route.CALCULATOR) }) {
                                    Icon(painterResource(R.drawable.ic_syringe), stringResource(R.string.calculator))
                                }
                            }
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
                                label = { Text(stringResource(item.label), maxLines = 1) },
                            )
                        }
                    }
                },
                floatingActionButton = { TabFab(tab, data, today, onAddMeasurement = { addMeasurement = true }) },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                when (tab) {
                    Tab.TODAY -> TodayScreen(
                        data = data,
                        now = now,
                        contentPadding = padding,
                        showNotificationBanner = !notificationsAllowed && data.medications.any { it.remindersEnabled && !it.paused },
                        onEnableNotifications = { enableNotifications(fromBanner = true) },
                        onTake = { sheet = "new:${it.medication.id}:${it.date}" },
                        onSkip = { dose ->
                            scope.launch {
                                val entry = repository.logDose(dose.medication, dose.date, DoseStatus.SKIPPED, Instant.now()) ?: return@launch
                                val result = snackbar.showSnackbar(
                                    res.getString(R.string.dose_skipped, dose.medication.name),
                                    actionLabel = res.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) repository.deleteEntry(entry.id)
                            }
                        },
                        onOpenEntry = { sheet = "edit:${it.id}" },
                        onAddMedication = { navigator.push(Route.editMedication(null)) },
                        onFinishVial = { vial -> scope.launch { repository.closeVial(vial.id, VialStatus.FINISHED) } },
                        onNewVial = { navigator.push(Route.editVial(null, it)) },
                        onOpenVials = { tab = Tab.VIALS },
                    )
                    Tab.VIALS -> VialsScreen(data, today, padding)
                    Tab.PROGRESS -> ProgressScreen(data, today, padding)
                    Tab.JOURNAL -> JournalScreen(data, today, padding)
                    Tab.MORE -> MoreScreen(padding)
                }
            }
        }

        sheet?.let { target ->
            DoseSheetHost(
                target = target,
                data = data,
                today = today,
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

        if (addMeasurement) MeasurementDialog(data, onDismiss = { addMeasurement = false })

        if (!noticeAccepted) {
            FirstLaunchNotice(onAccept = {
                prefs.edit { putBoolean(KEY_NOTICE_ACCEPTED, true) }
                noticeAccepted = true
            })
        }
    }
}

@Composable
private fun TabFab(tab: Tab, data: AppData, today: LocalDate, onAddMeasurement: () -> Unit) {
    val nav = LocalNavigator.current
    when (tab) {
        Tab.VIALS -> if (data.medications.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { nav.push(Route.editVial(null)) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_vial)) },
            )
        }
        Tab.PROGRESS -> ExtendedFloatingActionButton(
            onClick = onAddMeasurement,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text(stringResource(R.string.add_measurement)) },
        )
        Tab.JOURNAL -> ExtendedFloatingActionButton(
            onClick = { nav.push(Route.journal(today)) },
            icon = { Icon(Icons.Filled.Create, null) },
            text = { Text(stringResource(if (data.journalFor(today) == null) R.string.write_today else R.string.edit_today)) },
        )
        else -> Unit
    }
}

/** Resolves a sheet target string into the logging or editing sheet. */
@Composable
private fun DoseSheetHost(
    target: String,
    data: AppData,
    today: LocalDate,
    onDone: () -> Unit,
    onDeleted: (DoseEntry) -> Unit,
) {
    val context = LocalContext.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    val repository = context.repository
    val zone = ZoneId.systemDefault()
    val parts = target.split(":")

    when (parts.firstOrNull()) {
        "new" -> {
            val med = data.medication(parts.getOrNull(1).orEmpty())
            val date = parts.getOrNull(2)?.let(LocalDate::parse)
            if (med == null || date == null) {
                LaunchedEffect(target) { onDone() }
                return
            }
            val suggestion = if (med.usesInjectionSites) SiteRotation.suggest(data.entries, data.settings, today, zone) else null
            val vial = data.activeVial(med.id)
            DoseSheet(
                title = med.name,
                subtitle = stringResource(
                    R.string.scheduled_for,
                    Format.relativeDay(res, date, today),
                    Format.time(context, med.timeOfDay),
                ),
                unit = med.unit,
                initialStatus = DoseStatus.TAKEN,
                initialTime = Instant.now(),
                initialAmount = med.doseAmount,
                initialSite = suggestion?.site,
                initialNote = "",
                showSites = med.usesInjectionSites,
                suggestion = suggestion,
                usage = SiteUsage(data.entries, data, today),
                drawText = data.drawFor(med)?.let { Format.draw(res, it) },
                vialText = vial?.let {
                    val left = VialMath.remaining(it, data.entries)
                    stringResource(R.string.from_vial, it.label.ifBlank { Format.vialAmount(res, it.amount, it.unit) }, Format.vialAmount(res, left, it.unit))
                },
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
                    stringResource(R.string.scheduled_for_date, Format.relativeDay(res, it, today))
                } ?: stringResource(R.string.unscheduled_dose),
                unit = entry.unit,
                initialStatus = entry.status,
                initialTime = entry.time,
                initialAmount = entry.amount,
                initialSite = entry.site,
                initialNote = entry.note,
                showSites = showSites,
                suggestion = if (showSites) SiteRotation.suggest(others, data.settings, today, zone) else null,
                usage = SiteUsage(others, data, today),
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
private fun tabIcon(tab: Tab): Painter = when (tab) {
    Tab.TODAY -> rememberVectorPainter(Icons.Filled.DateRange)
    Tab.VIALS -> painterResource(R.drawable.ic_vial)
    Tab.PROGRESS -> painterResource(R.drawable.ic_chart)
    Tab.JOURNAL -> rememberVectorPainter(Icons.Filled.Create)
    Tab.MORE -> rememberVectorPainter(Icons.Filled.Menu)
}
