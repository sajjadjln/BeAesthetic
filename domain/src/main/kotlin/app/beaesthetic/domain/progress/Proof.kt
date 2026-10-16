package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.workout.Exercise
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.Ladders
import app.beaesthetic.domain.workout.LoggedSet
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A real-world milestone (D65). Linked to a metric with a [target], or ticked by hand. */
data class Milestone(val id: Long, val title: String, val metricId: Long?, val target: Double?, val reachedOn: LocalDate?)

/** A milestone with what Progress shows: reached (and when), else plain progress toward the target. */
data class MilestoneView(val milestone: Milestone, val reachedOn: LocalDate?, val best: Double?)

object Milestones {

    /**
     * A linked milestone is reached on the first date a reading meets the target, in the metric's direction;
     * it stays reached even if a later reading drops (nothing is taken back). A hand-ticked one keeps its date.
     */
    fun view(m: Milestone, metric: Metric?, readings: List<Reading>): MilestoneView {
        if (metric == null || m.target == null) return MilestoneView(m, m.reachedOn, null)
        val meets = { r: Reading -> if (metric.higherIsBetter) r.value >= m.target else r.value <= m.target }
        val first = readings.sortedBy { it.date }.firstOrNull(meets)?.date
        return MilestoneView(m, first ?: m.reachedOn, Progress.best(metric, readings)?.value)
    }

    /** Reached ones first (newest first), then open ones in the user's order. */
    fun ordered(views: List<MilestoneView>): List<MilestoneView> =
        views.filter { it.reachedOn != null }.sortedByDescending { it.reachedOn } + views.filter { it.reachedOn == null }

    /** The program caps milestones at 8 (D65). */
    const val MAX = 8
}

/** The program line (D51): Day 0 → the finish, the chapter starts as ticks, today's position. 0..1 each. */
data class ProgramLine(val today: Float, val chapterTicks: List<Float>, val dayNumber: Int, val totalDays: Int)

object ProgramLines {
    fun of(calendar: ProgramCalendar, today: LocalDate): ProgramLine {
        val total = ChronoUnit.DAYS.between(calendar.day0, calendar.endDate).toFloat().coerceAtLeast(1f)
        fun at(d: LocalDate) = (ChronoUnit.DAYS.between(calendar.day0, d) / total).coerceIn(0f, 1f)
        return ProgramLine(
            today = at(today),
            chapterTicks = Chapters.of(calendar).drop(1).map { at(it.start) },
            dayNumber = ChronoUnit.DAYS.between(calendar.day0, today).toInt().coerceIn(0, total.toInt()),
            totalDays = total.toInt(),
        )
    }
}

/**
 * A ladder on Progress (D70, DESIGN 03): the rungs easiest first, the one in the plan now, and the highest
 * rung ever logged ("rungs reached" never goes down). Indexes are 0-based; null = none.
 */
data class LadderView(val rungs: List<Exercise>, val current: Int?, val reached: Int?)

/** Lifetime totals since Day 0 (D71): only go up. */
data class Totals(val perLadder: List<LadderTotal>, val workouts: Int, val daysShownUp: Int)

/** Reps or seconds over every rung of one ladder, named after its current (or hardest logged) move. */
data class LadderTotal(val name: String, val kind: ExerciseKind, val total: Int)

object ProgressTotals {

    /** Ladders with more than one rung, in the order their easiest move was created. */
    fun ladders(exercises: List<Exercise>, harderOf: Map<Long, Long>, planned: Set<Long>, sets: List<LoggedSet>): List<LadderView> {
        val byId = exercises.associateBy { it.id }
        val logged = sets.map { it.exerciseId }.toSet()
        return exercises.map { Ladders.root(it.id, harderOf) }.distinct().sorted().mapNotNull { root ->
            val chain = Ladders.chain(root, harderOf).mapNotNull(byId::get)
            if (chain.size < 2) return@mapNotNull null
            LadderView(
                rungs = chain,
                current = chain.indexOfLast { it.id in planned }.takeIf { it >= 0 },
                reached = chain.indexOfLast { it.id in logged }.takeIf { it >= 0 },
            )
        }
    }

    fun totals(exercises: List<Exercise>, harderOf: Map<Long, Long>, planned: Set<Long>, sets: List<LoggedSet>, daysShownUp: Int): Totals {
        val byId = exercises.associateBy { it.id }
        val perLadder = sets.groupBy { Ladders.root(it.exerciseId, harderOf) }.mapNotNull { (root, ladderSets) ->
            val chain = Ladders.chain(root, harderOf).mapNotNull(byId::get)
            if (chain.isEmpty()) return@mapNotNull null
            val logged = ladderSets.map { it.exerciseId }.toSet()
            val name = (chain.lastOrNull { it.id in planned } ?: chain.last { it.id in logged }).name
            LadderTotal(name, chain.first().kind, ladderSets.sumOf { it.value })
        }.sortedByDescending { it.total }
        return Totals(perLadder, sets.map { it.sessionId }.distinct().size, daysShownUp)
    }
}

/** Workout backfill: past program days with a plan, Day 0 to yesterday, newest first. */
object Backfill {
    fun days(calendar: ProgramCalendar, today: LocalDate, plannedWeekdays: Set<Int>): List<LocalDate> =
        generateSequence(today.minusDays(1)) { it.minusDays(1) }
            .takeWhile { it >= calendar.day0 }
            .filter { app.beaesthetic.domain.time.Weekdays.index(it.dayOfWeek) in plannedWeekdays }
            .toList()
}

/** The reveal (D75): which earlier check-in a check-in's photos are compared with. Indexes as in [CheckIn]. */
object Reveals {

    /**
     * Day 0 by default, the last earlier check-in when [againstLast]; null when no earlier check-in has
     * photos (Day 0 itself, or a first set taken later), so there's nothing to compare and no reveal.
     */
    fun against(idx: Int, withPhotos: Collection<Int>, againstLast: Boolean = false): Int? {
        val earlier = withPhotos.filter { it < idx }
        if (earlier.isEmpty()) return null
        val last = earlier.max()
        return if (againstLast || 0 !in earlier) last else 0
    }

    /** «روز صفر» and «ماه قبل» are both offered only when they differ: Day 0 and a later earlier check-in. */
    fun canChoose(idx: Int, withPhotos: Collection<Int>): Boolean =
        0 in withPhotos && withPhotos.any { it in 1 until idx }
}
