package io.github.deveshb1.doselog.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.repository
import io.github.deveshb1.doselog.schedule.SiteRotation
import java.time.Instant
import java.time.LocalDate

/** Handles the notification's "Mark taken" action without opening the app. */
class MarkTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_TAKEN) return
        val medicationId = intent.getStringExtra(Notifications.EXTRA_MEDICATION_ID) ?: return
        val date = intent.getStringExtra(Notifications.EXTRA_DATE)?.let(LocalDate::parse) ?: return
        Notifications.cancel(context, medicationId, date)
        runAsync {
            val repo = context.repository
            val data = repo.ensureLoaded()
            val med = data.medication(medicationId) ?: return@runAsync
            val site = if (med.usesInjectionSites) SiteRotation.suggest(med.id, data.entries) else null
            repo.logDose(med, date, DoseStatus.TAKEN, Instant.now(), site = site)
            ReminderScheduler.scheduleAll(context, repo.data.value)
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "io.github.deveshb1.doselog.action.MARK_TAKEN"
    }
}
