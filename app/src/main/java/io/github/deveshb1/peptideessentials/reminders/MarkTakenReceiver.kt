package io.github.deveshb1.peptideessentials.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.repository
import io.github.deveshb1.peptideessentials.sites.SiteRotation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
            val site = if (med.usesInjectionSites) {
                SiteRotation.suggest(data.entries, data.settings, LocalDate.now(), ZoneId.systemDefault()).site
            } else {
                null
            }
            repo.logDose(med, date, DoseStatus.TAKEN, Instant.now(), site = site)
            ReminderScheduler.scheduleAll(context, repo.data.value)
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "io.github.deveshb1.peptideessentials.action.MARK_TAKEN"
    }
}
