package app.beaesthetic.domain.week

import app.beaesthetic.domain.tasks.DueRules
import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.Task
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What Today offers for one task's reminders (D57). One card at a time, the strongest routine first. */
sealed interface FadeOffer {
    val task: Task

    /** A routine that stayed strong for 4+ weeks: «یادآورش رو خاموش کنیم؟». */
    data class Silence(override val task: Task) : FadeOffer

    /** A silenced routine done on under 80 % of its due days this past week: «یادآورش برگرده؟». */
    data class Restore(override val task: Task) : FadeOffer
}

/**
 * The reminder fade (D57): less dependency on reminders once a routine has stuck. Silenced tasks stay on
 * Today and in the score; only block reminders and follow-ups leave them out.
 */
object ReminderFade {

    /** Offers start in program week 8 (D57, ~28 Nov for this program). */
    const val FROM_WEEK = 8

    /** Strong = routine strength at or above this on every day of the last [STRONG_DAYS]. 0.6 takes ~17 straight due days, so a routine kept since Day 0 is offered around week 7–8. */
    const val STRONG = 0.6
    const val STRONG_DAYS = 28L

    /** Restore when the done share of the last [WEAK_DAYS] due days is under [WEAK]. */
    const val WEAK = 0.8
    const val WEAK_DAYS = 7L

    /** A declined restore offer comes back at most once a week. */
    const val RESTORE_AGAIN_DAYS = 7L

    /**
     * Strength as it stood each day from [from] to [to]; the value on a day counts the due days up to it.
     * Steps only over due days, like [RoutineStrength].
     */
    fun strengthByDay(task: Task, done: Collection<LocalDate>, from: LocalDate, to: LocalDate): Map<LocalDate, Double> {
        val doneSet = done.toSet()
        val start = minOf(task.startDate, from)
        val out = LinkedHashMap<LocalDate, Double>()
        var s = 0.0
        val m = Math.pow(0.5, 1.0 / 13.0)
        var d = start
        while (d <= to) {
            val prior = doneSet.filter { it <= d }
            val due = DueRules.itemFor(d, task, prior)?.let { it.countsInScore && !it.minDayOnly } == true
            if (due) s = s * m + if (d in doneSet) 1 - m else 0.0
            if (d >= from) out[d] = s
            d = d.plusDays(1)
        }
        return out
    }

    /** Strong on every one of the last 28 days, and old enough to have 28 days behind it. */
    fun isStrong(task: Task, done: Collection<LocalDate>, today: LocalDate): Boolean {
        val from = today.minusDays(STRONG_DAYS - 1)
        if (task.startDate > from) return false
        return strengthByDay(task, done, from, today).values.all { it >= STRONG }
    }

    /** Done share over the due days of the last 7 days before today (today is still running). Null with none due. */
    fun recentShare(task: Task, done: Collection<LocalDate>, today: LocalDate): Double? {
        val days = (1L..WEAK_DAYS).map { today.minusDays(it) }.filter { it >= task.startDate }
        val doneSet = done.toSet()
        val due = days.filter { d -> DueRules.itemFor(d, task, doneSet.filter { it <= d })?.let { it.countsInScore && !it.minDayOnly } == true }
        if (due.isEmpty()) return null
        return due.count { it in doneSet }.toDouble() / due.size
    }

    /**
     * The one offer for Today, or null. [offeredAt] is when an offer for the task was last declined or
     * answered (`task.fadeOfferedAt`): a declined silence is never offered again; a restore at most weekly.
     */
    fun offer(
        week: Int,
        today: LocalDate,
        now: Instant,
        tasks: List<Task>,
        completions: Map<Long, Collection<LocalDate>>,
        offeredAt: Map<Long, Instant?>,
    ): FadeOffer? {
        if (week < FROM_WEEK) return null
        val eligible = tasks.filter { it.repeat != Repeat.Anytime && it.repeat != Repeat.MinDayOnly && it.repeat !is Repeat.Once }
        eligible.filter { it.remindersSilenced }.firstOrNull { t ->
            val last = offeredAt[t.id]
            val quiet = last == null || ChronoUnit.DAYS.between(last, now) >= RESTORE_AGAIN_DAYS
            quiet && (recentShare(t, completions[t.id].orEmpty(), today) ?: 1.0) < WEAK
        }?.let { return FadeOffer.Restore(it) }
        return eligible.filter { !it.remindersSilenced && offeredAt[it.id] == null }
            .filter { isStrong(it, completions[it.id].orEmpty(), today) }
            .maxByOrNull { completions[it.id]?.size ?: 0 }
            ?.let { FadeOffer.Silence(it) }
    }
}
