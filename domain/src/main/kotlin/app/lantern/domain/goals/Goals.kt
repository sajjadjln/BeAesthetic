package app.lantern.domain.goals

import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.DueRules
import app.lantern.domain.time.JalaliMonth
import app.lantern.domain.time.Part
import app.lantern.domain.time.ProgramCalendar
import app.lantern.domain.time.Weekdays
import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class GoalCalendar { JALALI, GREGORIAN }

/** The approved six-step plan (D217–D218). Empty later milestones mean “not sure yet”. */
data class GoalActionDraft(
    val title: String = "",
    val weekdays: Int = 0,
    val part: Part = Part.MORNING,
    val cue: String = "",
    val id: String = java.util.UUID.randomUUID().toString(),
) : Serializable

data class GoalDraft(
    val creationKey: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val start: LocalDate,
    val finish: LocalDate = start.plusDays(90),
    val triedBefore: Boolean? = null,
    val baselineMinutes: Int = 0,
    val triedWhat: String = "",
    val obstacle: String = "",
    val milestones: Map<LocalDate, String> = emptyMap(),
    val actions: List<GoalActionDraft> = listOf(GoalActionDraft()),
    val flexDays: Int = 1,
    val why: String = "",
    val calendar: GoalCalendar = GoalCalendar.JALALI,
) : Serializable

data class GoalMonth(val start: LocalDate, val end: LocalDate)
enum class GoalStatus { ACTIVE, ACHIEVED, SET_ASIDE }

object GoalPlans {
    fun short(start: LocalDate, finish: LocalDate): Boolean = ChronoUnit.DAYS.between(start, finish) <= 42

    /** Calendar boundaries are Jalali in storage; labels follow the app language. No empty final month. */
    fun months(start: LocalDate, finish: LocalDate, calendar: GoalCalendar = GoalCalendar.JALALI): List<GoalMonth> {
        require(finish >= start)
        if (short(start, finish)) return emptyList()
        val result = mutableListOf<GoalMonth>()
        if (calendar == GoalCalendar.GREGORIAN) {
            var first = start.withDayOfMonth(1)
            while (first <= finish) {
                result += GoalMonth(maxOf(start, first), minOf(finish, first.plusMonths(1).minusDays(1)))
                first = first.plusMonths(1)
            }
        } else {
            var month = JalaliMonth.of(start)
            while (month.firstDay <= finish) {
                result += GoalMonth(maxOf(start, month.firstDay), minOf(finish, month.lastDay))
                month = month.plus(1)
            }
        }
        // A tiny first chapter joins the next, as in SPEC §5.1.
        if (result.size > 1 && ChronoUnit.DAYS.between(result[0].start, result[0].end) + 1 < 14) {
            result[1] = result[1].copy(start = start)
            result.removeAt(0)
        }
        return result
    }

    fun validStep(d: GoalDraft, step: Int): Boolean = when (step) {
        0 -> d.title.isNotBlank()
        1 -> d.finish > d.start
        2 -> d.triedBefore == false || (d.triedBefore == true && d.baselineMinutes >= 0 && d.triedWhat.isNotBlank() && d.obstacle.isNotBlank())
        3 -> d.finish > d.start && (months(d.start, d.finish, d.calendar).firstOrNull()?.let { !d.milestones[it.start].isNullOrBlank() } ?: true) &&
            d.actions.size in 1..3 && d.actions.all { it.title.isNotBlank() && it.weekdays in 1..Weekdays.ALL }
        4 -> d.flexDays in 0..2
        5 -> d.why.isNotBlank()
        else -> false
    }

    /** D223: one offer after five logical days; a new tick permits the next quiet spell's offer. */
    fun quiet(start: LocalDate, lastTick: LocalDate?, offeredOn: LocalDate?, today: LocalDate): Boolean {
        val last = lastTick ?: start
        return ChronoUnit.DAYS.between(last, today) >= 5 && (offeredOn == null || lastTick != null && lastTick >= offeredOn)
    }

    fun valid(d: GoalDraft): Boolean = (0..5).all { validStep(d, it) }

    /** Chosen action days are the good-week target; flex days allow a smaller, explicit minimum. */
    fun range(actions: List<GoalActionDraft>, flexDays: Int): IntRange {
        val high = actions.sumOf { Integer.bitCount(it.weekdays and Weekdays.ALL) }
        return maxOf(1, high - flexDays)..maxOf(1, high)
    }

    /** Session counts use the same task ids/completions as Today, including archived actions. */
    fun sessions(taskIds: Set<Long>, completions: Map<Long, Collection<LocalDate>>, from: LocalDate, to: LocalDate): Int =
        taskIds.sumOf { id -> completions[id].orEmpty().toSet().count { it in from..to } }

    fun weekDays(today: LocalDate): List<LocalDate> = ProgramCalendar.weekStart(today).let { start -> List(7) { start.plusDays(it.toLong()) } }

    fun planned(tasks: List<Task>, from: LocalDate, to: LocalDate): Int {
        var date = from
        var count = 0
        while (date <= to) {
            count += tasks.count { DueRules.itemFor(date, it, emptyList()) != null }
            date = date.plusDays(1)
        }
        return count
    }
}
