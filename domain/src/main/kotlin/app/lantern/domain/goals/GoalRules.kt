package app.lantern.domain.goals

import app.lantern.domain.time.Jalali
import app.lantern.domain.time.JalaliMonth
import app.lantern.domain.time.ProgramCalendar
import app.lantern.domain.time.Weekdays
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** Where a goal is (D221). An idea has only a title; set aside goes back to the ideas, never "failed". */
enum class GoalState { IDEA, ACTIVE, REACHED, SET_ASIDE }

/**
 * Step ② (goal-step2): a slider 0..[MAX] mapped to 1..365 days on a curve, finer at the short end,
 * that snaps to 3, 6, 9 months, 1 year and the next Nowruz. Past a year, the calendar picks the date.
 */
object GoalTimeline {
    const val MAX = 1000
    const val YEAR = 365
    private const val CURVE = 1.5
    private const val SNAP = 14

    enum class Mark { MONTHS_3, NOWRUZ, MONTHS_6, MONTHS_9, YEAR }

    data class Marker(val mark: Mark, val days: Int)

    data class Pick(val days: Int, val marker: Mark?)

    fun toDays(position: Int): Int = maxOf(1, (YEAR * (position.coerceIn(0, MAX) / MAX.toDouble()).pow(CURVE)).roundToInt())

    fun toPosition(days: Int): Int = (MAX * (days.coerceIn(0, YEAR) / YEAR.toDouble()).pow(1 / CURVE)).roundToInt()

    /** The next 1 Farvardin after [today]. */
    fun nextNowruz(today: LocalDate): LocalDate {
        val jy = Jalali.fromGregorian(today).year
        return Jalali.toGregorian(jy + 1, 1, 1)
    }

    fun markers(today: LocalDate): List<Marker> {
        // "Until Nowruz" ends on the year's last day, so the goal doesn't spill a day into Farvardin.
        val nowruz = ChronoUnit.DAYS.between(today, nextNowruz(today).minusDays(1)).toInt()
        return listOf(
            Marker(Mark.MONTHS_3, 91), Marker(Mark.NOWRUZ, nowruz), Marker(Mark.MONTHS_6, 182),
            Marker(Mark.MONTHS_9, 273), Marker(Mark.YEAR, YEAR),
        ).sortedBy { it.days }
    }

    fun pick(position: Int, today: LocalDate): Pick {
        val hit = markers(today).filter { abs(toPosition(it.days) - position) < SNAP }.minByOrNull { abs(toPosition(it.days) - position) }
        return if (hit != null) Pick(hit.days, hit.mark) else Pick(toDays(position), null)
    }

    /** How the length reads: days under 2 weeks, weeks under 60 days, then months, then a year. */
    sealed interface Length {
        data class Days(val n: Int) : Length
        data class Weeks(val n: Int) : Length
        data class Months(val n: Int) : Length
        data object Year : Length
    }

    fun length(days: Int): Length = when {
        days < 14 -> Length.Days(days)
        days < 60 -> Length.Weeks((days / 7.0).roundToInt())
        days >= YEAR -> Length.Year
        else -> Length.Months((days / 30.4).roundToInt())
    }
}

/** One weekly action (step ④): a task on [weekdays] (Saturday = bit 0). */
data class GoalAction(val taskId: Long, val weekdays: Int)

enum class WeekDay { DONE, GAP, AHEAD }

/** This week of a goal: 7 squares from Saturday, ticks so far, and the calm range (picked − flex … picked). */
data class GoalWeek(val start: LocalDate, val days: List<WeekDay>, val ticks: Int, val low: Int, val high: Int) {
    val inRange: Boolean get() = ticks >= low
}

object GoalRules {
    /** A goal has gone quiet after this many logical days with no tick (D221). */
    const val QUIET_DAYS = 5
    const val MAX_ACTIONS = 3
    const val MAX_FLEX = 2

    fun range(actions: List<GoalAction>, flexDays: Int): Pair<Int, Int> {
        val high = actions.sumOf { Integer.bitCount(it.weekdays and Weekdays.ALL) }
        return maxOf(1, high - flexDays).coerceAtMost(high) to high
    }

    /**
     * [ticks] are completion dates of the goal's actions (a date may repeat, one per action). Days before
     * [goalStart] in the first week are gaps too, drawn gray: never a miss.
     */
    fun week(today: LocalDate, actions: List<GoalAction>, ticks: List<LocalDate>, flexDays: Int): GoalWeek {
        val start = ProgramCalendar.weekStart(today)
        val end = start.plusDays(6)
        val inWeek = ticks.filter { it in start..end }
        val doneDays = inWeek.toSet()
        val days = (0..6).map { i ->
            val d = start.plusDays(i.toLong())
            when {
                d in doneDays -> WeekDay.DONE
                d < today -> WeekDay.GAP
                else -> WeekDay.AHEAD
            }
        }
        val (low, high) = range(actions, flexDays)
        return GoalWeek(start, days, inWeek.size, low, high)
    }

    /** The Jalali months a goal touches, start month first. */
    fun months(start: LocalDate, end: LocalDate): List<JalaliMonth> {
        val first = JalaliMonth.of(start)
        val last = JalaliMonth.of(maxOf(start, end))
        val out = mutableListOf(first)
        while (out.last() != last) out += out.last().plus(1)
        return out
    }

    /** 0-based month of [today] within the goal, clamped to the goal's months. */
    fun monthIndex(start: LocalDate, end: LocalDate, today: LocalDate): Int {
        val months = months(start, end)
        val at = months.indexOfFirst { today in it }
        return when {
            at >= 0 -> at
            today < start -> 0
            else -> months.lastIndex
        }
    }

    /** The share of the goal's days already gone, 0..1. Time, not a score: nothing to fall behind on. */
    fun timeShare(start: LocalDate, end: LocalDate, today: LocalDate): Float {
        val total = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
        return (ChronoUnit.DAYS.between(start, today).toFloat() / total).coerceIn(0f, 1f)
    }

    fun daysLeft(end: LocalDate, today: LocalDate): Int = ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)

    /**
     * Gone quiet (D221): an active goal, at least [QUIET_DAYS] logical days since its last tick (or its
     * start), and not yet offered in this quiet spell.
     */
    fun isQuiet(start: LocalDate, lastTick: LocalDate?, offeredOn: LocalDate?, today: LocalDate): Boolean {
        val since = maxOf(start, lastTick ?: start)
        if (ChronoUnit.DAYS.between(since, today) < QUIET_DAYS) return false
        return offeredOn == null || offeredOn < since
    }
}
