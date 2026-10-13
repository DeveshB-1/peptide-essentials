package io.github.deveshb1.doselog

import android.app.Application
import android.content.Context
import io.github.deveshb1.doselog.data.DoseRepository
import io.github.deveshb1.doselog.reminders.Notifications
import io.github.deveshb1.doselog.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class DoseLogApp : Application() {
    val repository by lazy { DoseRepository(File(filesDir, "doselog.json")) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        scope.launch {
            repository.ensureLoaded()
            // Any change to medications or history can move the next reminder.
            repository.data.collect { ReminderScheduler.scheduleAll(this@DoseLogApp, it) }
        }
    }
}

val Context.repository: DoseRepository
    get() = (applicationContext as DoseLogApp).repository
