package io.github.deveshb1.peptideessentials

import android.app.Application
import android.content.Context
import io.github.deveshb1.peptideessentials.data.AppRepository
import io.github.deveshb1.peptideessentials.reminders.Notifications
import io.github.deveshb1.peptideessentials.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class PeptideEssentialsApp : Application() {
    val repository by lazy { AppRepository(File(filesDir, "peptide-essentials.json")) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        scope.launch {
            repository.ensureLoaded()
            // Any change to medications or history can move the next reminder.
            repository.data.collect { ReminderScheduler.scheduleAll(this@PeptideEssentialsApp, it) }
        }
    }
}

val Context.repository: AppRepository
    get() = (applicationContext as PeptideEssentialsApp).repository
