package app.beaesthetic.domain.tasks

import app.beaesthetic.domain.score.DayCounts
import app.beaesthetic.domain.score.DayScore
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Weekdays
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DueRulesTest {

    private val start = LocalDate.of(2026, 10, 10) // Saturday
    private fun task(id: Long, repeat: Repeat, floor: Boolean = false) =
        Task(id, "t$id", TaskIcon.DROP, Part.MORNING, repeat, start, inMinimumDay = floor)

    @Test
    fun `daily is due every day from its start`() {
        val t = task(1, Repeat.Daily)
        assertNull(DueRules.itemFor(start.minusDays(1), t, emptyList()))
        val item = assertNotNull(DueRules.itemFor(start, t, emptyList()))
        assertTrue(item.countsInScore)
        assertFalse(item.done)
        assertTrue(DueRules.itemFor(start, t, listOf(start))!!.done)
    }

    @Test
    fun `weekdays only on chosen days`() {
        val t = task(1, Repeat.OnWeekdays(Weekdays.of(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        assertNotNull(DueRules.itemFor(start, t, emptyList())) // Sat
        assertNull(DueRules.itemFor(start.plusDays(1), t, emptyList())) // Sun
        assertNotNull(DueRules.itemFor(start.plusDays(2), t, emptyList())) // Mon
        assertNull(DueRules.itemFor(start.plusDays(6), t, emptyList())) // Fri
    }

    @Test
    fun `every N days counts only on the first due day`() {
        val t = task(1, Repeat.EveryNDays(3))
        // Never done: due from its start date.
        val first = DueRules.itemFor(start, t, emptyList())!!
        assertTrue(first.countsInScore)
        assertEquals(start, first.dueSince)
        // Not done on the start day: still visible the next days, but no longer counts.
        val late = DueRules.itemFor(start.plusDays(2), t, emptyList())!!
        assertFalse(late.countsInScore)
        assertEquals(start, late.dueSince)
        // Done late on day 2: shows done that day, then resets the clock from that day.
        val doneLate = DueRules.itemFor(start.plusDays(2), t, listOf(start.plusDays(2)))!!
        assertTrue(doneLate.done)
        assertNull(doneLate.dueSince)
        assertNull(DueRules.itemFor(start.plusDays(3), t, listOf(start.plusDays(2))))
        assertNull(DueRules.itemFor(start.plusDays(4), t, listOf(start.plusDays(2))))
        val next = DueRules.itemFor(start.plusDays(5), t, listOf(start.plusDays(2)))!!
        assertTrue(next.countsInScore)
    }

    @Test
    fun `once shows from its day until done`() {
        val day = start.plusDays(3)
        val t = task(1, Repeat.Once(day))
        assertNull(DueRules.itemFor(day.minusDays(1), t, emptyList()))
        assertTrue(DueRules.itemFor(day, t, emptyList())!!.countsInScore)
        val later = DueRules.itemFor(day.plusDays(1), t, emptyList())!!
        assertFalse(later.countsInScore)
        assertTrue(DueRules.itemFor(day.plusDays(1), t, listOf(day.plusDays(1)))!!.done)
        assertNull(DueRules.itemFor(day.plusDays(2), t, listOf(day.plusDays(1))))
    }

    @Test
    fun `min-day-only never counts but is part of the floor`() {
        val item = DueRules.itemFor(start, task(1, Repeat.MinDayOnly), emptyList())!!
        assertFalse(item.countsInScore)
        assertTrue(item.inFloor)
        assertTrue(item.minDayOnly)
    }

    @Test
    fun `day counts and levels`() {
        val tasks = listOf(
            task(1, Repeat.Daily, floor = true),
            task(2, Repeat.Daily),
            task(3, Repeat.Daily),
            task(4, Repeat.Daily),
            task(5, Repeat.MinDayOnly),
        )
        fun countsWith(vararg done: Long): DayCounts {
            val items = DueRules.itemsFor(start, tasks, done.associateWith { listOf(start) })
            return DayScore.counts(items)
        }
        assertEquals(DayCounts(due = 4, done = 0, floorDone = false), countsWith())
        assertEquals(0, DayScore.level(countsWith()))
        assertEquals(1, DayScore.level(countsWith(2)))
        assertEquals(2, DayScore.level(countsWith(2, 3)))
        assertEquals(3, DayScore.level(countsWith(1, 2, 3, 5).copy(due = 5, done = 4)))
        assertEquals(4, DayScore.level(countsWith(1, 2, 3, 4)))
        // The floor alone gives the lightest shade.
        val floorOnly = countsWith(1, 5)
        assertTrue(floorOnly.floorDone)
        assertEquals(1, DayScore.level(floorOnly.copy(done = 0)))
        assertEquals(0, DayScore.level(DayCounts(0, 0, false)))
    }

    @Test
    fun `anytime is never a day item and never counts (D127)`() {
        val list = task(1, Repeat.Anytime)
        assertNull(DueRules.itemFor(start, list, emptyList()))
        val counts = DayScore.counts(DueRules.itemsFor(start, listOf(list, task(2, Repeat.Daily)), mapOf(1L to listOf(start))))
        assertEquals(DayCounts(due = 1, done = 0, floorDone = false), counts)
    }

    @Test
    fun `the anytime list keeps open items, then today's ticks, and drops older ones`() {
        val tasks = listOf(task(1, Repeat.Anytime), task(2, Repeat.Anytime), task(3, Repeat.Anytime), task(4, Repeat.Daily))
        val day = start.plusDays(5)
        val done = mapOf(1L to listOf(day), 3L to listOf(day.minusDays(1)))
        val list = DueRules.anytime(day, tasks, done)
        assertEquals(listOf(2L, 1L), list.map { it.task.id })
        assertEquals(listOf(false, true), list.map { it.done })
        assertTrue(list.none { it.countsInScore })
    }
}
