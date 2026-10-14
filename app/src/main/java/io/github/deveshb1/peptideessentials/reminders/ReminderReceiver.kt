package io.github.deveshb1.peptideessentials.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.deveshb1.peptideessentials.repository
import io.github.deveshb1.peptideessentials.calc.drawFor
import io.github.deveshb1.peptideessentials.sites.SiteRotation
import java.time.LocalDate
import java.time.ZoneId

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
                val site = if (med.usesInjectionSites) {
                    SiteRotation.suggest(data.entries, data.settings, LocalDate.now(), ZoneId.systemDefault()).site
                } else {
                    null
                }
                Notifications.showReminder(context, med, date, site, data.drawFor(med))
            }
            ReminderScheduler.scheduleAll(context, data)
        }
    }

    companion object {
        const val ACTION_REMIND = "io.github.deveshb1.peptideessentials.action.REMIND"
    }
}
