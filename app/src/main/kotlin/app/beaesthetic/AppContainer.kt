package app.beaesthetic

import android.content.Context
import app.beaesthetic.backup.BackupService
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.data.repo.ProgressRepository
import app.beaesthetic.data.repo.Seed
import app.beaesthetic.data.repo.TaskRepository
import app.beaesthetic.data.repo.WorkoutRepository
import app.beaesthetic.domain.reminders.BlockContent
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.events.EventLog
import app.beaesthetic.reminders.AlarmScheduler
import app.beaesthetic.reminders.Notifier
import app.beaesthetic.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val workouts = WorkoutRepository(db, tasks)
    val events = EventLog(db.events(), clock)
    val notifier = Notifier(appContext)
    val scheduler = AlarmScheduler(appContext, db, tasks, clock)
    val backup = BackupService(appContext, db, clock)
    val photos = PhotoStore(appContext)
    val progress = ProgressRepository(db, photos)

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

    /** After a tick anywhere: a block notification goes away once its part has nothing pending, and the widget redraws. */
    suspend fun cancelEmptyBlocks(date: LocalDate) {
        TodayWidget.refresh(appContext)
        val day = tasks.day(date)
        Part.entries
            .filter { BlockContent.lines(it, day.items, day.minimumMode).isEmpty() }
            .forEach(notifier::cancelBlock)
    }
}
