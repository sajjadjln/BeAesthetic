package app.beaesthetic.domain.tasks

import app.beaesthetic.domain.time.Weekdays
import java.time.LocalDate

/**
 * One task as it appears on a given day.
 *
 * @property countsInScore whether the task is one of the day's due items (SPEC §6.2). An "every N days" or
 *   one-off task counts only on the first day it was due, so lateness never piles up (D13).
 * @property dueSince for an "every N days" or one-off task that is due and not done: the day it became due.
 *   The tile shows «موعدش رسیده».
 */
data class DayItem(
    val task: Task,
    val done: Boolean,
    val countsInScore: Boolean,
    val dueSince: LocalDate? = null,
) {
    /** Part of the minimum-day floor. */
    val inFloor: Boolean get() = task.inMinimumDay || task.repeat == Repeat.MinDayOnly

    /** Shown only in minimum-day mode. */
    val minDayOnly: Boolean get() = task.repeat == Repeat.MinDayOnly
}

object DueRules {

    /**
     * Every task that appears on [date], in sort order. Minimum-day-only tasks are included (they are part
     * of the floor); the screen hides them outside minimum-day mode.
     *
     * @param completions every completion date per task id, at least up to [date].
     */
    fun itemsFor(date: LocalDate, tasks: List<Task>, completions: Map<Long, Collection<LocalDate>>): List<DayItem> =
        tasks
            .sortedWith(compareBy({ it.part }, { it.sortOrder }, { it.id }))
            .mapNotNull { task -> itemFor(date, task, completions[task.id].orEmpty()) }

    fun itemFor(date: LocalDate, task: Task, doneDates: Collection<LocalDate>): DayItem? {
        val doneToday = date in doneDates
        return when (val repeat = task.repeat) {
            Repeat.Daily ->
                if (date >= task.startDate) DayItem(task, doneToday, countsInScore = true) else null

            is Repeat.OnWeekdays ->
                if (date >= task.startDate && Weekdays.contains(repeat.bits, date.dayOfWeek)) {
                    DayItem(task, doneToday, countsInScore = true)
                } else {
                    null
                }

            is Repeat.EveryNDays -> {
                val lastBefore = doneDates.filter { it < date }.maxOrNull()
                val nextDue = lastBefore?.plusDays(repeat.n.toLong()) ?: task.startDate
                when {
                    date < nextDue -> null
                    else -> DayItem(
                        task = task,
                        done = doneToday,
                        countsInScore = date == nextDue,
                        dueSince = if (doneToday) null else nextDue,
                    )
                }
            }

            is Repeat.Once -> {
                val doneBefore = doneDates.any { it < date }
                when {
                    date < repeat.date || doneBefore -> null
                    else -> DayItem(
                        task = task,
                        done = doneToday,
                        countsInScore = date == repeat.date,
                        dueSince = if (doneToday) null else repeat.date,
                    )
                }
            }

            Repeat.MinDayOnly ->
                if (date >= task.startDate) DayItem(task, doneToday, countsInScore = false) else null

            // Never a day item (D127): see [anytime].
            Repeat.Anytime -> null
        }
    }

    /**
     * Today's «هر وقت» list (D127): every undated task not done before [date], open ones first. One done
     * on [date] stays, ticked, until the day ends, so a wrong tick can be undone.
     */
    fun anytime(date: LocalDate, tasks: List<Task>, completions: Map<Long, Collection<LocalDate>>): List<DayItem> =
        tasks
            .filter { it.repeat == Repeat.Anytime }
            .mapNotNull { task ->
                val done = completions[task.id].orEmpty()
                if (done.any { it < date }) null else DayItem(task, done = date in done, countsInScore = false)
            }
            .sortedWith(compareBy({ it.done }, { it.task.sortOrder }, { it.task.id }))
}
