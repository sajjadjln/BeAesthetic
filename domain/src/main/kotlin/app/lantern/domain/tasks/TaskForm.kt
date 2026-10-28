package app.lantern.domain.tasks

import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import java.time.LocalDate

/** The repeat choices of the task editor (screens.html 08), plus the minimum-day-only kind and «هر وقت» (D127). */
enum class RepeatKind { DAILY, WEEKDAYS, EVERY_N_DAYS, ONCE, MIN_DAY_ONLY, ANYTIME }

/** «کِی؟» in the simple sheet (D128): هر وقت · امروز · فردا · یه روز… · هر روز. */
enum class When { ANYTIME, TODAY, TOMORROW, DAY, DAILY }

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
    val icon: TaskIcon = TaskIcon.CHECK,
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
    /** «تمرین روز»: the tile opens the workout session (D117). */
    val opensWorkout: Boolean = false,
)

object TaskForms {
    const val DEFAULT_EVERY_N = 3
    const val MIN_EVERY_N = 2
    const val MAX_EVERY_N = 90
    const val MAX_TITLE = 40
    const val MAX_ANCHOR = 30
    const val DEFAULT_ALARM_MINUTE = 9 * 60

    /** The repeat choices of the full form, in screen order. */
    val FULL_KINDS = listOf(RepeatKind.ANYTIME, RepeatKind.ONCE, RepeatKind.DAILY, RepeatKind.WEEKDAYS, RepeatKind.EVERY_N_DAYS, RepeatKind.MIN_DAY_ONLY)

    /** A new task in [part], starting on [repeat]; a one-off defaults to [today]. */
    fun blank(part: Part, today: LocalDate, repeat: RepeatKind = RepeatKind.DAILY): TaskForm =
        TaskForm(part = part, onceDate = today, repeat = repeat)

    /** The alarm rings on chosen weekdays only for these; a one-off rings on its day, «هر N روز» on the days it's due (D130). */
    fun alarmUsesDays(kind: RepeatKind): Boolean = kind == RepeatKind.DAILY || kind == RepeatKind.WEEKDAYS || kind == RepeatKind.MIN_DAY_ONLY

    /** «هر وقت» has no part, anchor, alarm, floor or workout link (D127). */
    fun isAnytime(form: TaskForm): Boolean = form.repeat == RepeatKind.ANYTIME

    /** What «کِی؟» shows for [form], or null when its repeat isn't one of the simple choices. */
    fun whenOf(form: TaskForm, today: LocalDate): When? = when (form.repeat) {
        RepeatKind.ANYTIME -> When.ANYTIME
        RepeatKind.DAILY -> When.DAILY
        RepeatKind.ONCE -> when (form.onceDate) {
            today -> When.TODAY
            today.plusDays(1) -> When.TOMORROW
            else -> When.DAY
        }
        else -> null
    }

    /** Picks a «کِی؟» choice. [day] is the calendar's pick for [When.DAY]. */
    fun withWhen(form: TaskForm, choice: When, today: LocalDate, day: LocalDate? = null): TaskForm = when (choice) {
        When.ANYTIME -> form.copy(repeat = RepeatKind.ANYTIME)
        When.DAILY -> form.copy(repeat = RepeatKind.DAILY)
        When.TODAY -> form.copy(repeat = RepeatKind.ONCE, onceDate = today)
        When.TOMORROW -> form.copy(repeat = RepeatKind.ONCE, onceDate = today.plusDays(1))
        When.DAY -> form.copy(repeat = RepeatKind.ONCE, onceDate = day ?: form.onceDate ?: today)
    }

    /**
     * Whether the simple sheet can show [task] without hiding anything (D128): a name, a simple «کِی؟», and
     * none of anchor, «مهم», floor, an alarm that's on, or an action. Icon and part are kept as they are.
     */
    fun fitsSimple(task: Task): Boolean =
        (task.repeat == Repeat.Anytime || task.repeat == Repeat.Daily || task.repeat is Repeat.Once) &&
            task.anchor.isNullOrBlank() && !task.isImportant && !task.inMinimumDay &&
            task.alarm?.enabled != true && task.action == TaskAction.NONE

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
            opensWorkout = task.action == TaskAction.WORKOUT,
        )
        return when (val r = task.repeat) {
            Repeat.Daily -> base.copy(repeat = RepeatKind.DAILY)
            is Repeat.OnWeekdays -> base.copy(repeat = RepeatKind.WEEKDAYS, weekdays = r.bits)
            is Repeat.EveryNDays -> base.copy(repeat = RepeatKind.EVERY_N_DAYS, everyNDays = r.n)
            is Repeat.Once -> base.copy(repeat = RepeatKind.ONCE, onceDate = r.date)
            Repeat.MinDayOnly -> base.copy(repeat = RepeatKind.MIN_DAY_ONLY)
            Repeat.Anytime -> base.copy(repeat = RepeatKind.ANYTIME)
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
        if (form.alarmOn && alarmUsesDays(form.repeat) && form.alarmWeekdays and Weekdays.ALL == 0) add(FormError.NO_ALARM_DAYS)
    }

    /**
     * The task the form describes. A new task starts [today]; an edited one keeps its id, start date,
     * other actions (slices 7 and 8 add their switches) and silenced reminders, so its history and "every N days" clock stay as they were.
     * The caller has checked [errors] first.
     */
    fun toTask(form: TaskForm, original: Task?, today: LocalDate, sortOrder: Int): Task {
        val repeat = when (form.repeat) {
            RepeatKind.DAILY -> Repeat.Daily
            RepeatKind.WEEKDAYS -> Repeat.OnWeekdays(form.weekdays and Weekdays.ALL)
            RepeatKind.EVERY_N_DAYS -> Repeat.EveryNDays(form.everyNDays.coerceIn(MIN_EVERY_N, MAX_EVERY_N))
            RepeatKind.ONCE -> Repeat.Once(requireNotNull(form.onceDate))
            RepeatKind.MIN_DAY_ONLY -> Repeat.MinDayOnly
            RepeatKind.ANYTIME -> Repeat.Anytime
        }
        val anytime = isAnytime(form)
        // The alarm's time and days are kept when it's switched off, so switching it back on restores them.
        // «هر وقت» never rings (D127), but keeps them too.
        val alarmOn = form.alarmOn && !anytime
        val keepAlarm = alarmOn || original?.alarm != null
        return Task(
            id = original?.id ?: 0,
            title = form.title.trim().take(MAX_TITLE),
            icon = form.icon,
            part = form.part,
            repeat = repeat,
            startDate = original?.startDate ?: today,
            isImportant = form.important,
            // A minimum-day-only task is part of the floor by definition; «هر وقت» never is.
            inMinimumDay = !anytime && (form.inMinimumDay || form.repeat == RepeatKind.MIN_DAY_ONLY),
            action = when {
                form.opensWorkout && !anytime -> TaskAction.WORKOUT
                original?.action == TaskAction.WORKOUT -> TaskAction.NONE
                else -> original?.action ?: TaskAction.NONE
            },
            anchor = if (anytime) null else form.anchor.trim().take(MAX_ANCHOR).ifBlank { null },
            alarm = if (keepAlarm) TaskAlarm(form.alarmMinute, form.alarmWeekdays and Weekdays.ALL, alarmOn) else null,
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

    /** The list (screens.html 07) hides a one-off or «هر وقت» task once it was done before [today]: it's finished. */
    fun isListed(task: Task, doneDates: Collection<LocalDate>, today: LocalDate): Boolean =
        (task.repeat !is Repeat.Once && task.repeat != Repeat.Anytime) || doneDates.none { it < today }
}
