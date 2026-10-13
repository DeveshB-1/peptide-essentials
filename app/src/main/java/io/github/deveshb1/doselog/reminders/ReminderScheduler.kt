package io.github.deveshb1.doselog.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.schedule.ScheduleMath
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Keeps one inexact alarm per medication, set for its next unlogged dose.
 * Uses setAndAllowWhileIdle so no exact-alarm permission is needed; delivery may be a few minutes late.
 */
object ReminderScheduler {
    private const val PREFS = "reminders"
    private const val KEY_SCHEDULED = "scheduled_ids"

    fun scheduleAll(context: Context, data: AppData) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now()
        val scheduled = mutableSetOf<String>()

        for (med in data.medications) {
            if (med.paused || !med.remindersEnabled) continue
            val logged = data.entries.asSequence()
                .filter { it.medicationId == med.id }
                .mapNotNull { it.scheduledDate }
                .toSet()
            val next = ScheduleMath.nextReminder(med, logged, now) ?: continue
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                reminderIntent(context, med.id, next.toLocalDate()),
            )
            scheduled += med.id
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
        (previous - scheduled).forEach { id -> alarms.cancel(reminderIntent(context, id, null)) }
        prefs.edit { putStringSet(KEY_SCHEDULED, scheduled) }
        Notifications.cancelStale(context, data)
    }

    private fun reminderIntent(context: Context, medicationId: String, date: LocalDate?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            // A per-medication data URI keeps each medication's PendingIntent distinct.
            .setData("doselog://reminder/$medicationId".toUri())
            .putExtra(Notifications.EXTRA_MEDICATION_ID, medicationId)
            .putExtra(Notifications.EXTRA_DATE, date?.toString())
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
