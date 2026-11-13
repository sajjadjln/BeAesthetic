package app.lantern.domain.goals

import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import java.time.LocalDate

/** One weekly action as typed in step ④: what, on which weekdays, in which part of the day. */
data class ActionDraft(val title: String = "", val weekdays: Int = 0, val part: Part = Part.EVENING)

/** Why it stopped last time (step ③). [OWN] means the user wrote their own reason. */
enum class StopReason { BORED, NO_CHANGE, ROUTINE, NO_TIME, OWN }

/**
 * The goal flow's answers (goal-step1…6, D221). Each step's `ok` says whether «بعدی» is on; nothing is
 * scored or judged. Step ④'s months: index 0 is this month (required), the rest may stay empty.
 */
data class GoalDraft(
    val title: String = "",
    val endDate: LocalDate? = null,
    val triedBefore: Boolean? = null,
    val minutesPerWeek: Int = 0,
    val timeTouched: Boolean = false,
    val triedWhat: String = "",
    val stopReason: StopReason? = null,
    val ownReason: String = "",
    val months: List<String> = emptyList(),
    val actions: List<ActionDraft> = listOf(ActionDraft()),
    val flexDays: Int = 1,
    val why: String = "",
) {
    val step1Ok: Boolean get() = title.isNotBlank()

    fun step2Ok(today: LocalDate): Boolean = endDate != null && endDate > today

    val step3Ok: Boolean get() = when (triedBefore) {
        null -> false
        false -> true
        true -> timeTouched && triedWhat.isNotBlank() && stopReason != null && (stopReason != StopReason.OWN || ownReason.isNotBlank())
    }

    val step4Ok: Boolean get() =
        months.firstOrNull()?.isNotBlank() == true &&
            actions.isNotEmpty() && actions.size <= GoalRules.MAX_ACTIONS &&
            actions.all { it.title.isNotBlank() && it.weekdays and Weekdays.ALL != 0 }

    val step6Ok: Boolean get() = why.isNotBlank()

    /** The stop reason as stored: the enum name, or the user's own words. */
    val storedReason: String? get() = when (stopReason) {
        null -> null
        StopReason.OWN -> ownReason.trim()
        else -> stopReason.name
    }

    fun range(): Pair<Int, Int> = GoalRules.range(actions.map { GoalAction(0, it.weekdays) }, flexDays)

    /** Keeps one month row per Jalali month from [today] to [endDate], keeping what was typed. */
    fun withMonthsFor(today: LocalDate, calendar: MonthCalendar = MonthCalendar.JALALI): GoalDraft {
        val n = endDate?.let { GoalRules.months(today, it, calendar).size } ?: 1
        return copy(months = List(n) { months.getOrElse(it) { "" } })
    }

    fun ready(today: LocalDate): Boolean = step1Ok && step2Ok(today) && step3Ok && step4Ok && step6Ok
}
