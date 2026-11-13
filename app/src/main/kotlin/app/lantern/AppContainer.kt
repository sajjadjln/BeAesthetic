package app.lantern

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import app.lantern.backup.BackupWorker
import app.lantern.backup.BackupService
import app.lantern.crash.crashLog
import app.lantern.data.db.AppDatabase
import app.lantern.data.repo.Seed
import app.lantern.data.repo.GoalRepository
import app.lantern.data.repo.TaskRepository
import app.lantern.domain.reminders.BlockContent
import app.lantern.domain.time.Part
import app.lantern.domain.time.ProgramCalendar
import app.lantern.events.EventLog
import app.lantern.reminders.AlarmScheduler
import app.lantern.reminders.Notifier
import app.lantern.store.StorePrefs
import app.lantern.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Manual DI (D33): every long-lived object, built once in [App]. Receivers and workers read `app.container`. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** The application context, for widget refreshes from receivers. */
    val app: Context get() = appContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock = ProgramClock()
    val db = AppDatabase.build(appContext)
    val tasks = TaskRepository(db)
    val goals = GoalRepository(db, tasks)
    val events = EventLog(db.events(), clock)
    val notifier = Notifier(appContext)
    val scheduler = AlarmScheduler(appContext, db, tasks, clock)
    val crashes = crashLog(appContext)
    val backup = BackupService(appContext, db, clock, crashes)
    val store = StorePrefs(appContext)

    private val readyLock = Mutex()
    @Volatile private var ready = false

    /** Seeds the database on first run. Every entry point (activity, receivers, workers) awaits this. */
    suspend fun ensureReady() {
        if (ready) return
        readyLock.withLock {
            if (ready) return
            val installDate = ProgramCalendar.programDate(clock.localNow(), ProgramCalendar.DEFAULT_ROLLOVER_HOUR)
            Seed.ensure(db, installDate, clock.now())
            ready = true
        }
    }

    /**
     * Settings → «پاک کردن همه‌چیز» (B3, D213): back to a fresh install. Alarms, notifications, a queued backup,
     * the database, the crash log, private safety copies, the cache and the app's preferences go, and the
     * folder permission is let go. Backups already in the user's folder are never touched. The fresh
     * settings row is seeded again, so the next screen is onboarding.
     */
    suspend fun eraseEverything() = withContext(Dispatchers.IO) {
        backup.exclusive {
            scheduler.cancelAll()
            BackupWorker.cancel(appContext)
            NotificationManagerCompat.from(appContext).cancelAll()
            db.settings().get()?.backupTreeUri?.let { backup.folder.releasePermission(it.toUri()) }
            db.clearAllTables()
            crashes.clear()
            listOf(File(appContext.filesDir, "photos"), File(appContext.filesDir, "safety")).forEach { it.deleteRecursively() }
            appContext.cacheDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
            AppLanguage.reset(appContext)
            store.reset()
            readyLock.withLock { ready = false }
        }
        ensureReady()
        scheduler.reconcile("erase")
        TodayWidget.refresh(appContext)
    }

    /** After a tick anywhere: a block notification goes away once its part has nothing pending, and the widget redraws. */
    suspend fun cancelEmptyBlocks(date: LocalDate) {
        TodayWidget.refresh(appContext)
        val day = tasks.day(date)
        Part.entries
            .filter { BlockContent.lines(it, day.items, day.minimumMode).isEmpty() }
            .forEach(notifier::cancelBlock)
    }
}
