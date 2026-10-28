package app.lantern.domain.score

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DaysTest {

    private val install = LocalDate.of(2026, 10, 10)

    @Test
    fun `week dots`() {
        val weekStart = LocalDate.of(2026, 10, 10)
        val today = LocalDate.of(2026, 10, 13)
        val snapshots = mapOf(
            weekStart to DaySnapshot(DayCounts(10, 10, true)),
            weekStart.plusDays(1) to DaySnapshot(DayCounts(10, 5, false)),
            today to DaySnapshot(DayCounts(10, 1, false)),
        )
        assertEquals(
            listOf(
                DayStatus.Level(4), DayStatus.Level(2), DayStatus.Level(0), DayStatus.Level(1),
                DayStatus.Future, DayStatus.Future, DayStatus.Future,
            ),
            Days.week(weekStart, today, install, snapshots),
        )
    }

    @Test
    fun `days before install are not gaps`() {
        assertEquals(DayStatus.BeforeApp, Days.status(LocalDate.of(2026, 10, 9), install, install, null))
        assertEquals(DayStatus.Special, Days.status(install, install, install, DaySnapshot(DayCounts(5, 0, false), special = true)))
    }

    @Test
    fun `comebacks`() {
        assertTrue(Days.isComeback(DayStatus.Level(1), DayStatus.Level(0)))
        assertFalse(Days.isComeback(DayStatus.Level(0), DayStatus.Level(0)))
        assertFalse(Days.isComeback(DayStatus.Level(2), DayStatus.Level(1)))
        assertFalse(Days.isComeback(DayStatus.Level(2), DayStatus.Special))
        assertFalse(Days.isComeback(DayStatus.Level(2), DayStatus.BeforeApp))
    }

    @Test
    fun `days shown up count level 1 and above, up to today`() {
        val install = LocalDate.of(2026, 10, 10)
        val today = LocalDate.of(2026, 10, 14)
        val snapshots = mapOf(
            LocalDate.of(2026, 10, 9) to DaySnapshot(DayCounts(10, 5, false)), // before the app
            LocalDate.of(2026, 10, 10) to DaySnapshot(DayCounts(10, 1, false)),
            LocalDate.of(2026, 10, 11) to DaySnapshot(DayCounts(10, 0, false)),
            LocalDate.of(2026, 10, 12) to DaySnapshot(DayCounts(10, 0, true)), // floor done
            LocalDate.of(2026, 10, 13) to DaySnapshot(DayCounts(10, 9, false), special = true),
            LocalDate.of(2026, 10, 14) to DaySnapshot(DayCounts(10, 10, false)),
            LocalDate.of(2026, 10, 15) to DaySnapshot(DayCounts(10, 10, false)), // future
        )
        assertEquals(3, Days.shownUp(snapshots, today, install))
    }
}
