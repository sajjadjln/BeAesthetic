package app.beaesthetic.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import app.beaesthetic.MainActivity
import app.beaesthetic.ProgramClock
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.ReminderLogEntity
import app.beaesthetic.data.db.ReminderOutcome
import app.beaesthetic.data.repo.TaskRepository
import app.beaesthetic.domain.reminders.AlarmKind
import app.beaesthetic.domain.reminders.Health
import app.beaesthetic.domain.reminders.LogRow
import app.beaesthetic.domain.reminders.Outcome
import app.beaesthetic.domain.reminders.PlannerInput
import app.beaesthetic.domain.reminders.ReminderHealth
import app.beaesthetic.domain.reminders.PlannedAlarm
import app.beaesthetic.domain.reminders.ReminderPlanner
import app.beaesthetic.domain.reminders.ReminderSlot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

/**
 * Applies the planner's output to AlarmManager (ARCHITECTURE §7.1). [reconcile] is idempotent and runs on
 * app start, every alarm fire, boot, update, time and zone changes, and the 6-hourly watchdog.
 *
 * Every planned alarm is set again on each run, even if the log says it is already set: Xiaomi's
 * "force stop" and some cleaners silently drop an app's alarms, and setting the same PendingIntent again
 * only replaces it.
 */
class AlarmScheduler(
    context: Context,
    private val db: AppDatabase,
    private val tasks: TaskRepository,
    private val clock: ProgramClock,
) {
    private val context = context.applicationContext
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    suspend fun reconcile(reason: String) = mutex.withLock {
        val settings = db.settings().get() ?: return@withLock
        val now = clock.now()
        val slots = db.slots().all().map { ReminderSlot(it.profile, it.part, it.time, it.enabled) }
        val (taskList, completions) = tasks.plannerData(clock.programDate(settings.rolloverHour).plusDays(3))
        val plan = ReminderPlanner.plan(
            PlannerInput(
                now = now,
                zone = clock.zone(),
                slots = slots,
                rolloverHour = settings.rolloverHour,
                lastOpenedAt = settings.lastOpenedAt,
                followUpMinutes = settings.followUpMinutes,
                tasks = taskList,
                completions = completions,
                ritualMinute = settings.ritualTime,
                ritualsDone = db.weeks().done().toSet(),
            ),
        )
        val planned = plan.associateBy { it.key }
        val log = db.reminderLog()

        val upcoming = log.upcoming(now).filter { it.kind != AlarmKind.TEST.name }
        for (row in upcoming) {
            val alarm = planned[row.key]
            if (alarm == null) cancel(row.key)
            if (alarm == null || alarm.at != row.scheduledAt) log.setOutcome(row.id, ReminderOutcome.CANCELLED)
        }
        val alreadyLogged = upcoming.mapTo(HashSet()) { it.key to it.scheduledAt }
        for (alarm in plan) {
            set(alarm, settings.alarmMode)
            if ((alarm.key to alarm.at) !in alreadyLogged) {
                log.insert(ReminderLogEntity(key = alarm.key, kind = alarm.kind.name, scheduledAt = alarm.at, outcome = ReminderOutcome.SCHEDULED))
            }
        }
        log.deleteBefore(now - Duration.ofDays(30))
        Log.d(TAG, "reconcile($reason): ${plan.size} alarms, next ${plan.firstOrNull()?.key}")
    }

    /**
     * A real alarm through the same path as every reminder, [delay] from now (the setup screen's test).
     * Returns its key, so the screen can watch the log for the fire.
     */
    suspend fun scheduleTest(delay: Duration): String = mutex.withLock {
        val at = clock.now() + delay
        val alarm = PlannedAlarm("test:${at.toEpochMilli()}", at, AlarmKind.TEST, clock.programDate(4))
        set(alarm, db.settings().get()?.alarmMode ?: MODE_ALARM_CLOCK)
        db.reminderLog().insert(ReminderLogEntity(key = alarm.key, kind = alarm.kind.name, scheduledAt = at, outcome = ReminderOutcome.SCHEDULED))
        alarm.key
    }

    /** Reminder health over the last 7 days (ARCHITECTURE §7.5). */
    suspend fun health(): Health {
        val now = clock.now()
        return ReminderHealth.check(db.reminderLog().since(now - ReminderHealth.WINDOW).map { it.toLogRow() }, now)
    }

    /**
     * After a boot: alarms that fell while the phone was off are marked DEVICE_OFF, so they don't count as
     * missed. "Off" starts at the app's last sign of life before the boot.
     */
    suspend fun markDeviceOff() {
        val now = clock.now()
        val bootAt = now - Duration.ofMillis(SystemClock.elapsedRealtime())
        val lastAlive = listOfNotNull(db.reminderLog().lastFiredBefore(bootAt), db.events().lastBefore(bootAt)).maxOrNull()
        val rows = db.reminderLog().since(now - ReminderHealth.WINDOW).map { it.toLogRow() }
        val off = ReminderHealth.deviceOff(rows, lastAlive, bootAt).map { it.id }
        if (off.isNotEmpty()) db.reminderLog().markDeviceOff(off)
        Log.d(TAG, "boot: ${off.size} alarms fell while the phone was off")
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun set(alarm: PlannedAlarm, mode: String) {
        val operation = PendingIntent.getBroadcast(
            context, 0, fireIntent(alarm), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val at = alarm.at.toEpochMilli()
        try {
            when {
                !canScheduleExact() ->
                    // Fallback only; slice 2's health banner says "Fix reminders" when this happens.
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                mode == MODE_EXACT_IDLE ->
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                else ->
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent()), operation)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "exact alarm refused, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun cancel(key: String) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE).setData(dataUri(key))
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            ?.let { alarmManager.cancel(it); it.cancel() }
    }

    private fun fireIntent(alarm: PlannedAlarm): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            // The data URI makes each key its own PendingIntent, so request codes never collide.
            .setData(dataUri(alarm.key))
            .putExtra(ReminderReceiver.EXTRA_KEY, alarm.key)
            .putExtra(ReminderReceiver.EXTRA_KIND, alarm.kind.name)
            .putExtra(ReminderReceiver.EXTRA_DATE, alarm.programDate.toString())
            .putExtra(ReminderReceiver.EXTRA_PART, alarm.part?.name)
            .putExtra(ReminderReceiver.EXTRA_AT, alarm.at.toEpochMilli())
            .putExtra(ReminderReceiver.EXTRA_TASK, alarm.taskId ?: 0L)

    /** What the system opens when the user taps the "next alarm" on the lock screen. */
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun dataUri(key: String): Uri = "peykar://alarm/$key".toUri()

    companion object {
        private const val TAG = "AlarmScheduler"

        fun ReminderLogEntity.toLogRow() = LogRow(
            id = id,
            kind = runCatching { AlarmKind.valueOf(kind) }.getOrDefault(AlarmKind.BLOCK),
            scheduledAt = scheduledAt,
            firedAt = firedAt,
            outcome = runCatching { Outcome.valueOf(outcome) }.getOrDefault(Outcome.SCHEDULED),
            key = key,
        )
        const val MODE_ALARM_CLOCK = "ALARM_CLOCK"
        const val MODE_EXACT_IDLE = "EXACT_IDLE"

        fun instantOf(millis: Long): Instant = Instant.ofEpochMilli(millis)
    }
}
