package app.lantern.domain.score

import app.lantern.domain.tasks.DayItem

/** The numbers stored in a day's snapshot (D37). The level is computed from them at display time. */
data class DayCounts(val due: Int, val done: Int, val floorDone: Boolean) {
    val pct: Double get() = if (due == 0) 0.0 else done.toDouble() / due
}

/** Day level thresholds (D18). Defaults until tuned after two weeks of real use. */
data class LevelThresholds(val level2: Double = 0.5, val level3: Double = 0.8)

object DayScore {

    fun counts(items: List<DayItem>): DayCounts {
        val due = items.filter { it.countsInScore }
        val floor = items.filter { it.inFloor }
        return DayCounts(
            due = due.size,
            done = due.count { it.done },
            floorDone = floor.isNotEmpty() && floor.all { it.done },
        )
    }

    /** 0 = gray gap · 1 = anything done or the floor done · 2 ≥ 50% · 3 ≥ 80% · 4 = 100%. */
    fun level(counts: DayCounts, thresholds: LevelThresholds = LevelThresholds()): Int = when {
        counts.due > 0 && counts.done == counts.due -> 4
        counts.due > 0 && counts.pct >= thresholds.level3 -> 3
        counts.due > 0 && counts.pct >= thresholds.level2 -> 2
        counts.done > 0 || counts.floorDone -> 1
        else -> 0
    }
}
