package app.beaesthetic.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.beaesthetic.App
import app.beaesthetic.AppContainer
import app.beaesthetic.data.db.ReminderLogEntity
import app.beaesthetic.data.db.ReminderOutcome
import app.beaesthetic.domain.reminders.AlarmKind
import app.beaesthetic.domain.reminders.BlockContent
import app.beaesthetic.domain.reminders.BlockLine
import app.beaesthetic.domain.reminders.PartOfDay
import app.beaesthetic.domain.reminders.ReminderSlot
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.ProgramCalendar
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * An alarm fired. The notification is built now, from the database, not when it was scheduled: only
 * what is still pending is listed, and nothing pending means no notification (ARCHITECTURE §7.1).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val container = (context.applicationContext as App).container
        val pending = goAsync()
        container.scope.launch {
            try {
                container.ensureReady()
                handle(container, intent)
            } catch (e: Exception) {
                Log.e(TAG, "alarm handling failed", e)
            } finally {
                runCatching { container.scheduler.reconcile("fired") }
                pending.finish()
            }
        }
    }

    private suspend fun handle(container: AppContainer, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val kind = intent.getStringExtra(EXTRA_KIND)?.let(AlarmKind::valueOf) ?: return
        val scheduledAt = AlarmScheduler.instantOf(intent.getLongExtra(EXTRA_AT, 0))
        val db = container.db
        val now = container.clock.now()
        val row = db.reminderLog().find(key, scheduledAt)
            ?: ReminderLogEntity(key = key, kind = kind.name, scheduledAt = scheduledAt, outcome = ReminderOutcome.SCHEDULED)
                .let { it.copy(id = db.reminderLog().insert(it)) }
        val notifier = container.notifier

        val outcome = if (kind == AlarmKind.TEST) {
            if (notifier.canNotify()) {
                notifier.showTest(key)
                ReminderOutcome.SHOWN
            } else {
                ReminderOutcome.NOTIFICATIONS_OFF
            }
        } else {
            val date = LocalDate.parse(intent.getStringExtra(EXTRA_DATE))
            val part = intent.getStringExtra(EXTRA_PART)?.let(Part::valueOf)
            val settings = db.settings().get()!!
            val localNow = container.clock.localNow()
            val sameDay = ProgramCalendar.programDate(localNow, settings.rolloverHour) == date
            val day = container.tasks.day(date)
            val lines: List<BlockLine> = when (kind) {
                AlarmKind.BLOCK -> BlockContent.lines(part!!, day.items, day.minimumMode)
                AlarmKind.FOLLOW_UP -> BlockContent.followUpLines(part!!, day.items, day.minimumMode)
                AlarmKind.TASK -> {
                    val taskId = intent.getLongExtra(EXTRA_TASK, 0)
                    day.items.filter { it.task.id == taskId && !it.done }.map { BlockLine(it.task.title, it.task.anchor, it.task.id) }
                }
                else -> emptyList()
            }
            // A block that fires after a later part has started never shows: no catch-up (SPEC §7.2).
            val late = kind == AlarmKind.BLOCK && run {
                val slots = db.slots().all().map { ReminderSlot(it.profile, it.part, it.time, it.enabled) }
                PartOfDay.current(localNow, slots, settings.rolloverHour) != part
            }
            when {
                !sameDay || late -> ReminderOutcome.SKIPPED_LATE
                lines.isEmpty() -> ReminderOutcome.SKIPPED_EMPTY
                !notifier.canNotify() -> ReminderOutcome.NOTIFICATIONS_OFF
                else -> {
                    when (kind) {
                        AlarmKind.BLOCK -> notifier.showBlock(part!!, lines, key)
                        AlarmKind.FOLLOW_UP -> lines.forEach { notifier.showFollowUp(it, date, key) }
                        else -> notifier.showTaskAlarm(lines.single(), date, key)
                    }
                    container.events.notificationShown(key)
                    ReminderOutcome.SHOWN
                }
            }
        }
        db.reminderLog().setFired(row.id, now, outcome)
        Log.d(TAG, "fired $key → $outcome")
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        const val ACTION_FIRE = "app.beaesthetic.action.FIRE"
        const val EXTRA_KEY = "key"
        const val EXTRA_KIND = "kind"
        const val EXTRA_DATE = "date"
        const val EXTRA_PART = "part"
        const val EXTRA_AT = "at"
        const val EXTRA_TASK = "task"
    }
}

/**
 * «انجام شد» on a follow-up or a task alarm: ticks the task without opening the app (SPEC §7.2).
 * Only today and yesterday can change (D17).
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        val container = (context.applicationContext as App).container
        val taskId = intent.getLongExtra(EXTRA_TASK, 0)
        val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: return
        container.notifier.cancel(intent.getIntExtra(EXTRA_NOTIFICATION, 0))
        val pending = goAsync()
        container.scope.launch {
            try {
                container.ensureReady()
                val settings = container.db.settings().get() ?: return@launch
                val today = container.clock.programDate(settings.rolloverHour)
                if (date == today || date == today.minusDays(1)) {
                    container.tasks.complete(taskId, date, container.clock.now())
                    container.events.firstTickIfNeeded(date, settings.rolloverHour)
                    val day = container.tasks.day(date)
                    Part.entries.filter { BlockContent.lines(it, day.items, day.minimumMode).isEmpty() }.forEach(container.notifier::cancelBlock)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "app.beaesthetic.action.DONE"
        const val EXTRA_TASK = "task"
        const val EXTRA_DATE = "date"
        const val EXTRA_NOTIFICATION = "notification"
    }
}
