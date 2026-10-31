package app.lantern.domain.score

import app.lantern.domain.tasks.DueRules
import app.lantern.domain.tasks.Repeat
import app.lantern.domain.tasks.Task
import app.lantern.domain.time.JalaliMonth
import app.lantern.domain.time.ProgramCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** One repeating task's square on a day (D198). There is no "missed": a due day not done is a gray gap. */
enum class TaskDay { DONE, GAP, NOT_DUE, BEFORE_APP, FUTURE }

/** Times a task was done in one month; [start] is the month's first day (Jalali or Gregorian). */
data class MonthCount(val start: LocalDate, val count: Int)

/**
 * History, the track record (D198): a GitHub-style grid from install to today, weeks as columns, Saturday on
 * top. Built from the stored completions and day snapshots; nothing new is stored.
 */
object History {

    /** Early on the grid is padded back to this many weeks with «قبل از اپ» dots (D56), so it never looks empty. */
    const val MIN_WEEKS = 20

    /** The Saturday of every week from install's (or [minWeeks] back, whichever is older) to today's, oldest first. */
    fun columns(installDate: LocalDate, today: LocalDate, minWeeks: Int = MIN_WEEKS): List<LocalDate> {
        val last = ProgramCalendar.weekStart(today)
        val first = minOf(ProgramCalendar.weekStart(installDate), last.minusWeeks((minWeeks - 1).toLong()))
        val weeks = ChronoUnit.WEEKS.between(first, last).toInt()
        return (0..weeks).map { first.plusWeeks(it.toLong()) }
    }

    /** Tasks that get a track record: the ones that come back on their own. */
    fun isRepeating(task: Task): Boolean =
        task.repeat == Repeat.Daily || task.repeat is Repeat.OnWeekdays || task.repeat is Repeat.EveryNDays

    /**
     * A task's square. A due day counts as a gap only where it counts in the score, so an "every N days" task
     * left undone shows one gap, not a run of them (D13).
     */
    fun taskDay(task: Task, date: LocalDate, today: LocalDate, installDate: LocalDate, doneDates: Collection<LocalDate>): TaskDay = when {
        date > today -> TaskDay.FUTURE
        date < installDate -> TaskDay.BEFORE_APP
        date in doneDates -> TaskDay.DONE
        DueRules.itemFor(date, task, doneDates)?.countsInScore == true -> TaskDay.GAP
        else -> TaskDay.NOT_DUE
    }

    /** Times done in each of the last [months] months up to [today], oldest first; Jalali months when [jalali]. */
    fun monthCounts(doneDates: Collection<LocalDate>, today: LocalDate, months: Int, jalali: Boolean): List<MonthCount> {
        val starts = if (jalali) {
            val now = JalaliMonth.of(today)
            (months - 1 downTo 0).map { now.plus(-it).firstDay }
        } else {
            val now = YearMonth.from(today)
            (months - 1 downTo 0).map { now.minusMonths(it.toLong()).atDay(1) }
        }
        return starts.mapIndexed { i, start ->
            val end = starts.getOrNull(i + 1) ?: today.plusDays(1)
            MonthCount(start, doneDates.count { it >= start && it < end })
        }
    }
}
