package app.lantern.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.lantern.App
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Boot, app update, clock and zone changes, exact-alarm permission changes: plan again. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as App).container
        val pending = goAsync()
        container.scope.launch {
            try {
                container.ensureReady()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) container.scheduler.markDeviceOff()
                container.scheduler.reconcile(intent.action ?: "system")
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * The 6-hourly watchdog (ARCHITECTURE §7.1). If a phone cleaner dropped the alarms, this puts them back
 * even when the app isn't opened. It also checks reminder health and, if something new went wrong,
 * posts one quiet System notification a day (§7.5), so a broken setup is noticed without opening the app.
 */
class ReconcileWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as App).container
        container.ensureReady()
        container.scheduler.reconcile("watchdog")
        val settings = container.db.settings().get() ?: return Result.success()
        val today = container.clock.programDate(settings.rolloverHour)
        val health = container.scheduler.health()
        val seen = settings.healthSeenAt
        val newProblems = (health.missed + health.blocked).any { seen == null || it.scheduledAt > seen }
        if (newProblems && settings.healthNotifiedOn != today) {
            container.notifier.showHealthWarning()
            container.db.settings().upsert(settings.copy(healthNotifiedOn = today))
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "reconcile-watchdog"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReconcileWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
