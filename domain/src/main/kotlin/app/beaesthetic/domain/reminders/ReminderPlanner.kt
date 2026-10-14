package app.beaesthetic.domain.reminders

import app.beaesthetic.domain.tasks.DayItem
import app.beaesthetic.domain.tasks.DueRules
import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.time.Profile
import app.beaesthetic.domain.time.Weekdays
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class AlarmKind { BLOCK, FOLLOW_UP, TASK, RITUAL, TEST }

/**
 * One alarm the scheduler should have set. [key] is stable: re-planning the same alarm gives the same key,
 * so it is replaced, never duplicated.
 */
data class PlannedAlarm(
    val key: String,
    val at: Instant,
    val kind: AlarmKind,
    val programDate: LocalDate,
    val part: Part? = null,
    val taskId: Long? = null,
)

/** Everything the planner reads. All of it comes from the database; the planner itself is pure. */
data class PlannerInput(
    val now: Instant,
    val zone: ZoneId,
    val slots: List<ReminderSlot>,
    val rolloverHour: Int,
    val lastOpenedAt: Instant?,
    val followUpMinutes: Int = 45,
    val tasks: List<Task> = emptyList(),
    /** Completion dates per task id; only "every N days" and one-off tasks need them, to know what shows. */
    val completions: Map<Long, Collection<LocalDate>> = emptyMap(),
)

object ReminderPlanner {

    val HORIZON: Duration = Duration.ofHours(48)

    /** After this long without opening the app, only Morning blocks are planned (D19). */
    val ABSENCE: Duration = Duration.ofDays(3)

    /**
     * The alarms between now (exclusive) and now + [HORIZON]: block reminders, one follow-up for each block
     * that holds an important task, and tasks' own alarms. Nothing in the past is ever planned, so there is
     * no catch-up. What a notification shows is decided when it fires.
     */
    fun plan(input: PlannerInput): List<PlannedAlarm> {
        val now = input.now
        val until = now + HORIZON
        val absent = input.lastOpenedAt != null && Duration.between(input.lastOpenedAt, now) > ABSENCE
        val today = ProgramCalendar.programDate(now.atZone(input.zone).toLocalDateTime(), input.rolloverHour)
        val alarms = mutableListOf<PlannedAlarm>()
        fun add(alarm: PlannedAlarm) {
            if (alarm.at > now && alarm.at <= until) alarms += alarm
        }

        for (offset in -1L..3L) {
            val date = today.plusDays(offset)
            val profile = Profile.of(date)
            val items by lazy { DueRules.itemsFor(date, input.tasks, input.completions) }
            for (slot in input.slots) {
                if (slot.profile != profile || !slot.enabled) continue
                if (absent && slot.part != Part.MORNING) continue
                val at = slot.dateTimeFor(date, input.rolloverHour).atZone(input.zone).toInstant()
                add(PlannedAlarm(blockKey(date, slot.part), at, AlarmKind.BLOCK, date, slot.part))
                if (!absent && hasImportant(items, slot.part)) {
                    val followAt = at + Duration.ofMinutes(input.followUpMinutes.toLong())
                    add(PlannedAlarm(followUpKey(date, slot.part), followAt, AlarmKind.FOLLOW_UP, date, slot.part))
                }
            }
            // A task's own alarm (D25, D130); it is skipped at fire time if already done.
            for (task in input.tasks) {
                val alarm = task.alarm ?: continue
                if (!alarm.enabled || date < task.startDate || !alarmRings(task, alarm.weekdays, date, input.completions)) continue
                val time = LocalTime.of(alarm.minuteOfDay / 60, alarm.minuteOfDay % 60)
                val calendarDate = if (alarm.minuteOfDay < input.rolloverHour * 60) date.plusDays(1) else date
                val at = LocalDateTime.of(calendarDate, time).atZone(input.zone).toInstant()
                add(PlannedAlarm(taskKey(task.id, date), at, AlarmKind.TASK, date, task.part, task.id))
            }
        }
        return alarms.sortedWith(compareBy({ it.at }, { it.key }))
    }

    /** Slice-1 signature, kept for callers that only plan blocks. */
    fun plan(now: Instant, zone: ZoneId, slots: List<ReminderSlot>, rolloverHour: Int, lastOpenedAt: Instant?): List<PlannedAlarm> =
        plan(PlannerInput(now, zone, slots, rolloverHour, lastOpenedAt))

    /**
     * Whether a task's alarm rings on [date] (D130): a one-off on its own day only; «هر N روز» on the days it
     * shows (due and not done when planned); «هر وقت» never; the rest on the alarm's weekdays.
     */
    private fun alarmRings(task: Task, weekdays: Int, date: LocalDate, completions: Map<Long, Collection<LocalDate>>): Boolean =
        when (val repeat = task.repeat) {
            is Repeat.Once -> date == repeat.date
            is Repeat.EveryNDays -> DueRules.itemFor(date, task, completions[task.id].orEmpty())?.let { !it.done } == true
            Repeat.Anytime -> false
            else -> Weekdays.contains(weekdays, date.dayOfWeek)
        }

    private fun hasImportant(items: List<DayItem>, part: Part): Boolean =
        items.any { it.task.part == part && it.task.isImportant && !it.task.remindersSilenced && !it.minDayOnly }

    fun blockKey(date: LocalDate, part: Part): String = "block:$date:$part"
    fun followUpKey(date: LocalDate, part: Part): String = "follow:$date:$part"
    fun taskKey(taskId: Long, date: LocalDate): String = "task:$taskId:$date"
}

/** One line of a block notification: a pending task and its «بعد از…» cue. */
data class BlockLine(val title: String, val anchor: String?, val taskId: Long = 0)

object BlockContent {
    /**
     * What a block notification lists, built when it fires: the tasks of [part] that are still pending,
     * leaving out tasks whose reminders are silenced (D57). Empty means no notification.
     */
    fun lines(part: Part, items: List<DayItem>, minimumMode: Boolean): List<BlockLine> =
        pending(part, items, minimumMode).map { BlockLine(it.task.title, it.task.anchor, it.task.id) }

    /** A follow-up shows only the important tasks of its block that are still not done. */
    fun followUpLines(part: Part, items: List<DayItem>, minimumMode: Boolean): List<BlockLine> =
        pending(part, items, minimumMode).filter { it.task.isImportant }.map { BlockLine(it.task.title, it.task.anchor, it.task.id) }

    private fun pending(part: Part, items: List<DayItem>, minimumMode: Boolean): List<DayItem> =
        items.filter { it.task.part == part && !it.done && !it.task.remindersSilenced }
            .filter { if (minimumMode) it.inFloor else !it.minDayOnly }
}
