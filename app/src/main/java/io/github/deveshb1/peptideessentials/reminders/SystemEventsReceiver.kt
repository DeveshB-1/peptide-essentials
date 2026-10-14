package io.github.deveshb1.peptideessentials.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.deveshb1.peptideessentials.repository

/** Alarms are cleared on reboot and drift when the clock or time zone changes, so re-plan them. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        runAsync {
            ReminderScheduler.scheduleAll(context, context.repository.ensureLoaded())
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
