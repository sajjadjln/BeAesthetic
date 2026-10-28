package app.lantern.domain.score

import java.time.LocalDate

/** How a day shows on the week dots and, later, the dashboard. */
sealed interface DayStatus {
    data class Level(val level: Int) : DayStatus
    data object Special : DayStatus

    /** Before `installDate`: not a gap (D56). */
    data object BeforeApp : DayStatus
    data object Future : DayStatus
}

/** What is stored for a past day: its snapshot counts and whether it was a special day. */
data class DaySnapshot(val counts: DayCounts, val special: Boolean = false)

object Days {

    fun status(
        date: LocalDate,
        today: LocalDate,
        installDate: LocalDate,
        snapshot: DaySnapshot?,
        thresholds: LevelThresholds = LevelThresholds(),
    ): DayStatus = when {
        date > today -> DayStatus.Future
        date < installDate -> DayStatus.BeforeApp
        snapshot?.special == true -> DayStatus.Special
        snapshot == null -> DayStatus.Level(0) // a day the app never saw is a gray gap
        else -> DayStatus.Level(DayScore.level(snapshot.counts, thresholds))
    }

    /** The 7 days of [today]'s week, Saturday first. */
    fun week(
        weekStart: LocalDate,
        today: LocalDate,
        installDate: LocalDate,
        snapshots: Map<LocalDate, DaySnapshot>,
    ): List<DayStatus> = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        status(date, today, installDate, snapshots[date])
    }

    /** Days shown up (D121): days up to [today] at level 1 or more. Special days and days before the app don't count. */
    fun shownUp(snapshots: Map<LocalDate, DaySnapshot>, today: LocalDate, installDate: LocalDate): Int =
        snapshots.count { (date, snapshot) ->
            val s = status(date, today, installDate, snapshot)
            s is DayStatus.Level && s.level >= 1
        }

    /**
     * A comeback (D53): a day at level 1 or more right after a day at level 0. Special days and days before
     * the app are not level 0, so they never make the next day a comeback.
     */
    fun isComeback(today: DayStatus, yesterday: DayStatus): Boolean =
        today is DayStatus.Level && today.level >= 1 && yesterday == DayStatus.Level(0)
}
