package app.beaesthetic.domain.week

import app.beaesthetic.domain.score.Dashboard
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.score.DayStatus
import app.beaesthetic.domain.score.Days
import app.beaesthetic.domain.tasks.DueRules
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.time.ProgramCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * When the Friday ritual (M8) is open, and which week it reviews and plans (D140). Friday reviews its own
 * week and plans the next one. If it wasn't done, Saturday still offers it for the week that just ended,
 * with the same words; after Saturday it's gone without a trace.
 */
object Ritual {

    /** The Saturday of the week the ritual reviews on [today], or null on the other days. */
    fun reviewWeek(today: LocalDate): LocalDate? = when (today.dayOfWeek) {
        DayOfWeek.FRIDAY -> ProgramCalendar.weekStart(today)
        DayOfWeek.SATURDAY -> today.minusDays(7)
        else -> null
    }

    /**
     * The week to review now, if the ritual is open: Friday or Saturday, not done yet, and a week of the
     * program after Day 0's (Day 0 itself was the first ritual, done on paper).
     */
    fun open(today: LocalDate, calendar: ProgramCalendar, done: Set<LocalDate>): LocalDate? {
        val week = reviewWeek(today) ?: return null
        if (week in done) return null
        if (week < calendar.week1Start || week > calendar.endDate) return null
        return week
    }

    /** The week the plan (special days, snack notes) is for: next week on Friday, else the current one. */
    fun planWeek(today: LocalDate): LocalDate =
        ProgramCalendar.weekStart(today).let { if (today.dayOfWeek == DayOfWeek.FRIDAY) it.plusWeeks(1) else it }
}

/**
 * Routine strength (D57): Loop Habit Tracker's score. Each due day moves it toward 1 when done and toward 0
 * when not, with `0.5^(sqrt(freq)/13)` as the step, so a daily task has a ~13-day half-life. We step over
 * due days only, with freq 1, so a task due 3 times a week isn't punished for the days it wasn't due.
 * Internal only: it picks the best routines and is never shown as a number.
 */
object RoutineStrength {
    private val MULTIPLIER = 0.5.pow(sqrt(1.0) / 13.0)

    fun score(dueDays: List<LocalDate>, done: Set<LocalDate>): Double =
        dueDays.sorted().fold(0.0) { s, d -> s * MULTIPLIER + if (d in done) 1 - MULTIPLIER else 0.0 }
}

/** A task's week in the review: its due and done days this week, and its strength at the end of the week. */
data class Routine(val taskId: Long, val title: String, val due: Int, val done: Int, val strength: Double)

/** What the review card shows. Wins only: nothing here counts what was missed (D53, D62). */
data class WeekWins(
    val days: List<DayStatus?>,
    /** The week's score with today counted live, or null when no day counts (special days, before the app). */
    val score: Int?,
    val shownUp: Int,
    val comebacks: Int,
    val records: Int,
    val workouts: Int,
    val bestRoutines: List<Routine>,
) {
    /** The win «پیکر» cheers about at the bottom of the card. */
    val headline: Win get() = when {
        records > 0 -> Win.RECORDS
        comebacks > 0 -> Win.COMEBACKS
        bestRoutines.isNotEmpty() -> Win.ROUTINES
        workouts > 0 -> Win.WORKOUTS
        else -> Win.SHOWED_UP
    }
}

enum class Win { RECORDS, COMEBACKS, ROUTINES, WORKOUTS, SHOWED_UP }

object WeekReview {

    const val MAX_ROUTINES = 3

    /**
     * The review of the week starting [weekStart]. [today] may be the week's Friday (it counts live through
     * [snapshots]) or the Saturday after. [recordsByDay] and [workoutDays] come from the workout log.
     */
    fun wins(
        weekStart: LocalDate,
        today: LocalDate,
        installDate: LocalDate,
        calendar: ProgramCalendar,
        snapshots: Map<LocalDate, DaySnapshot>,
        routines: List<Routine>,
        records: Int,
        workouts: Int,
    ): WeekWins {
        val week = (0L..6L).map { weekStart.plusDays(it) }.filter { it <= today }
        val statuses = (0L..6L).map { Dashboard.cell(weekStart.plusDays(it), calendar, today, installDate, snapshots) }
        val shownUp = week.count { d -> Days.status(d, today, installDate, snapshots[d]).let { it is DayStatus.Level && it.level >= 1 } }
        return WeekWins(
            days = statuses,
            // Counting the week as over from the day after its Friday puts today's live counts in.
            score = Dashboard.weekScore(weekStart, maxOf(today, weekStart.plusDays(7)), installDate, snapshots),
            shownUp = shownUp,
            comebacks = comebacks(week, today, installDate, snapshots),
            records = records,
            workouts = workouts,
            bestRoutines = best(routines),
        )
    }

    /** Days of [week] that came right after a gray gap (D53). The gap itself is never named. */
    fun comebacks(week: List<LocalDate>, today: LocalDate, installDate: LocalDate, snapshots: Map<LocalDate, DaySnapshot>): Int =
        week.count { d ->
            Days.isComeback(Days.status(d, today, installDate, snapshots[d]), Days.status(d.minusDays(1), today, installDate, snapshots[d.minusDays(1)]))
        }

    /** Routines done on every due day this week, strongest first (D57). */
    fun best(routines: List<Routine>, max: Int = MAX_ROUTINES): List<Routine> =
        routines.filter { it.due > 0 && it.done == it.due }
            .sortedWith(compareByDescending<Routine> { it.strength }.thenByDescending { it.done }.thenBy { it.title })
            .take(max)

    /**
     * Each scored task's week: due and done days in the review week, and its strength over every due day from
     * [from] to the week's end (or [today], if sooner). Minimum-day-only tasks and «هر وقت» don't count.
     */
    fun routines(
        tasks: List<Task>,
        completions: Map<Long, Collection<LocalDate>>,
        from: LocalDate,
        weekStart: LocalDate,
        today: LocalDate,
    ): List<Routine> {
        val end = minOf(today, weekStart.plusDays(6))
        if (end < from) return emptyList()
        val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= end }.toList()
        return tasks.mapNotNull { task ->
            val doneDates = completions[task.id].orEmpty()
            // What was due on a day depends only on what was done up to it («هر N روز»).
            val due = dates.filter { d -> DueRules.itemFor(d, task, doneDates.filter { it <= d })?.let { it.countsInScore && !it.minDayOnly } == true }
            if (due.isEmpty()) return@mapNotNull null
            val inWeek = due.filter { it >= weekStart }
            val done = doneDates.toSet()
            Routine(task.id, task.title, inWeek.size, inWeek.count { it in done }, RoutineStrength.score(due, done))
        }
    }

    /** «۲ روز ذخیره»: how many special days are left in the week (D65). */
    fun reserves(weekStart: LocalDate, snapshots: Map<LocalDate, DaySnapshot>, cap: Int): Int =
        (cap - (0L..6L).count { snapshots[weekStart.plusDays(it)]?.special == true }).coerceAtLeast(0)
}
