package app.beaesthetic.domain.tasks

import app.beaesthetic.domain.time.Part
import java.time.LocalDate

/** How a task repeats. */
sealed interface Repeat {
    data object Daily : Repeat

    /** [bits] is a weekday set (see [app.beaesthetic.domain.time.Weekdays]). */
    data class OnWeekdays(val bits: Int) : Repeat

    /** Due [n] days after the last time it was done ("every N days from the last time", D13). */
    data class EveryNDays(val n: Int) : Repeat {
        init { require(n >= 1) }
    }

    data class Once(val date: LocalDate) : Repeat

    /** Shows only in minimum-day mode and never counts in the score. */
    data object MinDayOnly : Repeat

    /**
     * «هر وقت» (D127): no day, part, alarm or floor. Listed in Today's «هر وقت» card until done; never a
     * day item, so it never counts in the score, never reminds and never leaves a gap.
     */
    data object Anytime : Repeat
}

/** What tapping the tile does beyond ticking it. Slice 1 only ticks. */
enum class TaskAction { NONE, WORKOUT, HIKE, WEIGH_IN, SNACK_NOTE }

/** Icon keys; `:app` maps each to a vector. New keys go last: the editor's names are listed by ordinal. */
enum class TaskIcon { DROP, TOOTH, PILL, EGG, DUMBBELL, MOUNTAIN, SCALE, SHOWER, SCISSORS, APPLE, SUN, CHECK }

/** A task's own alarm (D25), rung on chosen weekdays. Scheduled from slice 2. */
data class TaskAlarm(val minuteOfDay: Int, val weekdays: Int, val enabled: Boolean)

data class Task(
    val id: Long,
    val title: String,
    val icon: TaskIcon,
    val part: Part,
    val repeat: Repeat,
    val startDate: LocalDate,
    val isImportant: Boolean = false,
    val inMinimumDay: Boolean = false,
    val action: TaskAction = TaskAction.NONE,
    /** The «بعد از…» cue (D55), shown in notifications. */
    val anchor: String? = null,
    val alarm: TaskAlarm? = null,
    /** Left out of notifications, still in Today and the score (D57). */
    val remindersSilenced: Boolean = false,
    val sortOrder: Int = 0,
)
