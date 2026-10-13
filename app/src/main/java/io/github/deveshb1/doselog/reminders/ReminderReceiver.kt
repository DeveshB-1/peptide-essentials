package io.github.deveshb1.doselog.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.deveshb1.doselog.repository
import io.github.deveshb1.doselog.schedule.SiteRotation
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val medicationId = intent.getStringExtra(Notifications.EXTRA_MEDICATION_ID) ?: return
        val date = intent.getStringExtra(Notifications.EXTRA_DATE)?.let(LocalDate::parse) ?: return
        runAsync {
            val data = context.repository.ensureLoaded()
            val med = data.medication(medicationId)
            val alreadyLogged = data.entries.any { it.medicationId == medicationId && it.scheduledDate == date }
            if (med != null && !med.paused && med.remindersEnabled && !alreadyLogged) {
                val site = if (med.usesInjectionSites) SiteRotation.suggest(med.id, data.entries) else null
                Notifications.showReminder(context, med, date, site)
            }
            ReminderScheduler.scheduleAll(context, data)
        }
    }

    companion object {
        const val ACTION_REMIND = "io.github.deveshb1.doselog.action.REMIND"
    }
}
