package app.beaesthetic.domain.score

import app.beaesthetic.domain.time.ProgramCalendar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardTest {

    // The real program: Day 0 is Friday 9 Oct 2026, week 1 starts Saturday 10 Oct, the finish is Saturday 20 Mar 2027.
    private val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    private val install = LocalDate.of(2026, 10, 10)

    private fun snap(due: Int, done: Int, special: Boolean = false) = DaySnapshot(DayCounts(due, done, false), special)

    @Test
    fun `the grid has every week the program touches, Day 0's and the finish's included`() {
        val cols = Dashboard.columns(calendar)
        assertEquals(LocalDate.of(2026, 10, 3), cols.first()) // Day 0's week
        assertEquals(LocalDate.of(2027, 3, 20), cols.last()) // the finish day is a Saturday
        assertEquals(25, cols.size)
    }

    @Test
    fun `days outside the program have no square`() {
        val today = LocalDate.of(2026, 10, 14)
        assertNull(Dashboard.cell(LocalDate.of(2026, 10, 8), calendar, today, install, emptyMap()))
        assertEquals(DayStatus.BeforeApp, Dashboard.cell(LocalDate.of(2026, 10, 9), calendar, today, install, emptyMap()))
        assertNull(Dashboard.cell(LocalDate.of(2027, 3, 21), calendar, today, install, emptyMap()))
    }

    @Test
    fun `a finished week scores its non-special days, a gap as zero`() {
        val start = LocalDate.of(2026, 10, 10)
        val snapshots = mapOf(
            start to snap(10, 10),
            start.plusDays(1) to snap(10, 5),
            start.plusDays(2) to snap(10, 0, special = true),
            // 13 Oct: no row at all, a gap.
            start.plusDays(4) to snap(10, 8),
            start.plusDays(5) to snap(10, 9),
            start.plusDays(6) to snap(10, 10),
        )
        // (1 + .5 + 0 + .8 + .9 + 1) / 6 = 0.7
        assertEquals(70, Dashboard.weekScore(start, LocalDate.of(2026, 10, 20), install, snapshots))
        // While the week runs there is no score yet («جاری»).
        assertNull(Dashboard.weekScore(start, LocalDate.of(2026, 10, 16), install, snapshots))
    }

    @Test
    fun `days before the app don't count in a week's score`() {
        val week0 = LocalDate.of(2026, 10, 3)
        assertNull(Dashboard.weekScore(week0, LocalDate.of(2026, 10, 20), install, emptyMap()))
    }

    @Test
    fun `week rows are newest first and only started weeks`() {
        val rows = Dashboard.weeks(calendar, LocalDate.of(2026, 10, 18), install, emptyMap())
        assertEquals(listOf(2, 1, 0), rows.map { it.number })
        assertTrue(rows.first().current)
        assertFalse(rows[1].current)
    }

    @Test
    fun `the summary counts shown-up days since install, special days apart`() {
        val today = LocalDate.of(2026, 10, 14)
        val snapshots = mapOf(
            LocalDate.of(2026, 10, 10) to snap(10, 3),
            LocalDate.of(2026, 10, 11) to snap(10, 0),
            LocalDate.of(2026, 10, 12) to snap(10, 4, special = true),
            LocalDate.of(2026, 10, 14) to snap(10, 1),
        )
        // 10 and 14 shown up; 11 and 13 (never seen) are gaps; 12 is special.
        assertEquals(Summary(shownUp = 2, counted = 4, special = 1), Dashboard.summary(today, install, calendar, snapshots))
    }

    @Test
    fun `the day line counts from Day 0`() {
        assertEquals(5 to 162, Dashboard.dayOf(calendar, LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun `two special days a week, then only changing or clearing`() {
        val snapshots = mapOf(LocalDate.of(2026, 10, 10) to snap(10, 0, true), LocalDate.of(2026, 10, 12) to snap(10, 0, true))
        assertFalse(Dashboard.canMarkSpecial(LocalDate.of(2026, 10, 14), snapshots, 2))
        assertTrue(Dashboard.canMarkSpecial(LocalDate.of(2026, 10, 12), snapshots, 2))
        assertTrue(Dashboard.canMarkSpecial(LocalDate.of(2026, 10, 17), snapshots, 2)) // the next week
    }
}
