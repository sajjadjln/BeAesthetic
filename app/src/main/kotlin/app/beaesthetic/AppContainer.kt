package app.beaesthetic

import android.content.Context
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.repo.Seed
import app.beaesthetic.data.repo.TaskRepository
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.events.EventLog
import app.beaesthetic.reminders.AlarmScheduler
import app.beaesthetic.reminders.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Manual DI (D33): every long-lived object, built once in [App]. Receivers and workers read `app.container`. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock = ProgramClock()
    val db = AppDatabase.build(appContext)
    val tasks = TaskRepository(db)
    val events = EventLog(db.events(), clock)
    val notifier = Notifier(appContext)
    val scheduler = AlarmScheduler(appContext, db, tasks, clock)

    private val readyLock = Mutex()
    @Volatile private var ready = false

    /** Seeds the database on first run. Every entry point (activity, receivers, workers) awaits this. */
    suspend fun ensureReady() {
        if (ready) return
        readyLock.withLock {
            if (ready) return
            val installDate = ProgramCalendar.programDate(clock.localNow(), ProgramCalendar.DEFAULT_ROLLOVER_HOUR)
            Seed.ensure(db, PersianLocale.wrap(appContext), installDate, clock.now())
            ready = true
        }
    }
}
