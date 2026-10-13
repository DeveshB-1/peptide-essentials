package io.github.deveshb1.doselog.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.github.deveshb1.doselog.MainActivity
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.InjectionSite
import io.github.deveshb1.doselog.data.Medication
import io.github.deveshb1.doselog.ui.Format
import io.github.deveshb1.doselog.ui.theme.TagColors
import java.time.LocalDate

object Notifications {
    const val EXTRA_MEDICATION_ID = "medication_id"
    const val EXTRA_DATE = "date"
    private const val CHANNEL_ID = "dose_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_reminders_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED)

    fun showReminder(context: Context, med: Medication, date: LocalDate, site: InjectionSite?) {
        if (!canPost(context)) return
        val res = context.resources
        val dose = Format.dose(res, med.doseAmount, med.unit)
        val time = Format.time(context, med.timeOfDay)
        val text = buildString {
            append(res.getString(R.string.notification_dose_due, dose, time))
            if (site != null) append('\n').append(res.getString(R.string.notification_site, Format.site(res, site)))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(med.name)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(TagColors.argb(med.colorIndex))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.action_mark_taken),
                markTakenIntent(context, med.id, date),
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(med.id, notificationId(date), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; nothing else to do.
        }
    }

    fun cancel(context: Context, medicationId: String, date: LocalDate) {
        NotificationManagerCompat.from(context).cancel(medicationId, notificationId(date))
    }

    /** Removes reminders for doses that were logged elsewhere or whose medication is gone or paused. */
    fun cancelStale(context: Context, data: AppData) {
        val manager = context.getSystemService(NotificationManager::class.java)
        for (shown in manager.activeNotifications) {
            val medId = shown.tag ?: continue
            val date = LocalDate.ofEpochDay(shown.id.toLong())
            val med = data.medication(medId)
            val logged = data.entries.any { it.medicationId == medId && it.scheduledDate == date }
            if (med == null || med.paused || !med.remindersEnabled || logged) manager.cancel(medId, shown.id)
        }
    }

    private fun notificationId(date: LocalDate) = date.toEpochDay().toInt()

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun markTakenIntent(context: Context, medicationId: String, date: LocalDate): PendingIntent {
        val intent = Intent(context, MarkTakenReceiver::class.java)
            .setAction(MarkTakenReceiver.ACTION_MARK_TAKEN)
            .setData("doselog://taken/$medicationId/$date".toUri())
            .putExtra(EXTRA_MEDICATION_ID, medicationId)
            .putExtra(EXTRA_DATE, date.toString())
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
