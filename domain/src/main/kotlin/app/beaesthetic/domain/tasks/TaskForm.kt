package app.beaesthetic.domain.tasks

import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Weekdays
import java.time.LocalDate

/** The repeat choices of the task editor (screens.html 08), plus the minimum-day-only kind. */
enum class RepeatKind { DAILY, WEEKDAYS, EVERY_N_DAYS, ONCE, MIN_DAY_ONLY }

enum class FormError {
    NO_TITLE,
    /** «روزهای خاص» with no day picked. */
    NO_WEEKDAYS,
    /** A one-off task needs a day, and a new one can't be in the past. */
    BAD_DATE,
    /** The alarm is on but rings on no day. */
    NO_ALARM_DAYS,
}

/**
 * What the task editor holds while the user edits (slice 3). Every repeat field is kept while the user
 * switches between repeat kinds, so going back to «هر ۳ روز» doesn't lose the 3.
 */
data class TaskForm(
    val title: String = "",
    val icon: TaskIcon = TaskIcon.DROP,
    val part: Part = Part.MORNING,
    val repeat: RepeatKind = RepeatKind.DAILY,
    val weekdays: Int = 0,
    val everyNDays: Int = TaskForms.DEFAULT_EVERY_N,
    val onceDate: LocalDate? = null,
    /** The «بعد از…» cue (D55), e.g. «بعد از صبحانه». */
    val anchor: String = "",
    val important: Boolean = false,
    val inMinimumDay: Boolean = false,
    val alarmOn: Boolean = false,
    val alarmMinute: Int = TaskForms.DEFAULT_ALARM_MINUTE,
    val alarmWeekdays: Int = Weekdays.ALL,
)

object TaskForms {
    const val DEFAULT_EVERY_N = 3
    const val MIN_EVERY_N = 2
    const val MAX_EVERY_N = 90
    const val MAX_TITLE = 40
    const val MAX_ANCHOR = 30
    const val DEFAULT_ALARM_MINUTE = 9 * 60

    /** A new task in [part]; a one-off defaults to [today]. */
    fun blank(part: Part, today: LocalDate): TaskForm = TaskForm(part = part, onceDate = today)

    fun from(task: Task): TaskForm {
        val base = TaskForm(
            title = task.title,
            icon = task.icon,
            part = task.part,
            anchor = task.anchor.orEmpty(),
            important = task.isImportant,
            inMinimumDay = task.inMinimumDay,
            alarmOn = task.alarm?.enabled == true,
            alarmMinute = task.alarm?.minuteOfDay ?: DEFAULT_ALARM_MINUTE,
            alarmWeekdays = task.alarm?.weekdays?.takeIf { it != 0 } ?: Weekdays.ALL,
            onceDate = task.startDate,
        )
        return when (val r = task.repeat) {
            Repeat.Daily -> base.copy(repeat = RepeatKind.DAILY)
            is Repeat.OnWeekdays -> base.copy(repeat = RepeatKind.WEEKDAYS, weekdays = r.bits)
            is Repeat.EveryNDays -> base.copy(repeat = RepeatKind.EVERY_N_DAYS, everyNDays = r.n)
            is Repeat.Once -> base.copy(repeat = RepeatKind.ONCE, onceDate = r.date)
            Repeat.MinDayOnly -> base.copy(repeat = RepeatKind.MIN_DAY_ONLY)
        }
    }

    /**
     * What stops the form from being saved. [original] is the task being edited, or null for a new one:
     * a one-off that already had a past date may keep it, a new one may not.
     */
    fun errors(form: TaskForm, today: LocalDate, original: Task? = null): Set<FormError> = buildSet {
        if (form.title.isBlank()) add(FormError.NO_TITLE)
        if (form.repeat == RepeatKind.WEEKDAYS && form.weekdays and Weekdays.ALL == 0) add(FormError.NO_WEEKDAYS)
        if (form.repeat == RepeatKind.ONCE) {
            val date = form.onceDate
            val keptPast = date != null && (original?.repeat as? Repeat.Once)?.date == date
            if (date == null || (date < today && !keptPast)) add(FormError.BAD_DATE)
        }
        if (form.alarmOn && form.alarmWeekdays and Weekdays.ALL == 0) add(FormError.NO_ALARM_DAYS)
    }

    /**
     * The task the form describes. A new task starts [today]; an edited one keeps its id, start date,
     * action and silenced reminders, so its history and "every N days" clock stay as they were.
     * The caller has checked [errors] first.
     */
    fun toTask(form: TaskForm, original: Task?, today: LocalDate, sortOrder: Int): Task {
        val repeat = when (form.repeat) {
            RepeatKind.DAILY -> Repeat.Daily
            RepeatKind.WEEKDAYS -> Repeat.OnWeekdays(form.weekdays and Weekdays.ALL)
            RepeatKind.EVERY_N_DAYS -> Repeat.EveryNDays(form.everyNDays.coerceIn(MIN_EVERY_N, MAX_EVERY_N))
            RepeatKind.ONCE -> Repeat.Once(requireNotNull(form.onceDate))
            RepeatKind.MIN_DAY_ONLY -> Repeat.MinDayOnly
        }
        // The alarm's time and days are kept when it's switched off, so switching it back on restores them.
        val keepAlarm = form.alarmOn || original?.alarm != null
        return Task(
            id = original?.id ?: 0,
            title = form.title.trim().take(MAX_TITLE),
            icon = form.icon,
            part = form.part,
            repeat = repeat,
            startDate = original?.startDate ?: today,
            isImportant = form.important,
            // A minimum-day-only task is part of the floor by definition.
            inMinimumDay = form.inMinimumDay || form.repeat == RepeatKind.MIN_DAY_ONLY,
            action = original?.action ?: TaskAction.NONE,
            anchor = form.anchor.trim().take(MAX_ANCHOR).ifBlank { null },
            alarm = if (keepAlarm) TaskAlarm(form.alarmMinute, form.alarmWeekdays and Weekdays.ALL, form.alarmOn) else null,
            remindersSilenced = original?.remindersSilenced ?: false,
            sortOrder = sortOrder,
        )
    }

    /** The alarm days that fit the repeat when the alarm is first switched on: the task's own days, else all. */
    fun defaultAlarmDays(form: TaskForm): Int =
        if (form.repeat == RepeatKind.WEEKDAYS && form.weekdays != 0) form.weekdays else Weekdays.ALL

    /** A new task, or one moved to another part, goes to the end of its part. */
    fun sortOrderFor(form: TaskForm, original: Task?, tasks: List<Task>): Int =
        if (original != null && original.part == form.part) {
            original.sortOrder
        } else {
            (tasks.filter { it.part == form.part && it.id != original?.id }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        }

    /** Anchors already in use, most used first: one tap fills the field. The current one is left out. */
    fun anchorSuggestions(tasks: List<Task>, current: String, limit: Int = 6): List<String> =
        tasks.mapNotNull { it.anchor?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
            .filter { it != current.trim() }
            .take(limit)

    /** The list (screens.html 07) hides a one-off task once it was done before [today]: it's finished. */
    fun isListed(task: Task, doneDates: Collection<LocalDate>, today: LocalDate): Boolean =
        task.repeat !is Repeat.Once || doneDates.none { it < today }
}
