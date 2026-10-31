package app.lantern.domain.score

import app.lantern.domain.tasks.Repeat
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryTest {

    private val install = LocalDate.of(2026, 10, 9) // a Friday
    private val today = LocalDate.of(2026, 10, 31) // a Saturday

    private fun task(repeat: Repeat, start: LocalDate = install) = Task(1, "t", TaskIcon.TOOTH, Part.NIGHT, repeat, start)

    @Test
    fun `early on the grid pads back to 20 weeks and ends on today's week`() {
        val cols = History.columns(install, today)
        assertEquals(20, cols.size)
        assertEquals(LocalDate.of(2026, 10, 31), cols.last())
        assertTrue(cols.all { it.dayOfWeek == DayOfWeek.SATURDAY })
    }

    @Test
    fun `after a long time the grid starts on install's week`() {
        val later = LocalDate.of(2027, 10, 30)
        val cols = History.columns(install, later)
        assertEquals(LocalDate.of(2026, 10, 3), cols.first())
        assertEquals(LocalDate.of(2027, 10, 30), cols.last())
    }

    @Test
    fun `only tasks that come back get a track record`() {
        assertTrue(History.isRepeating(task(Repeat.Daily)))
        assertTrue(History.isRepeating(task(Repeat.OnWeekdays(Weekdays.of(DayOfWeek.FRIDAY)))))
        assertTrue(History.isRepeating(task(Repeat.EveryNDays(2))))
        assertFalse(History.isRepeating(task(Repeat.Once(today))))
        assertFalse(History.isRepeating(task(Repeat.Anytime)))
        assertFalse(History.isRepeating(task(Repeat.MinDayOnly)))
    }

    @Test
    fun `a daily task is done, a gap, before the app or in the future`() {
        val t = task(Repeat.Daily)
        val done = listOf(LocalDate.of(2026, 10, 20))
        assertEquals(TaskDay.DONE, History.taskDay(t, LocalDate.of(2026, 10, 20), today, install, done))
        assertEquals(TaskDay.GAP, History.taskDay(t, LocalDate.of(2026, 10, 21), today, install, done))
        assertEquals(TaskDay.BEFORE_APP, History.taskDay(t, LocalDate.of(2026, 10, 1), today, install, done))
        assertEquals(TaskDay.FUTURE, History.taskDay(t, today.plusDays(1), today, install, done))
    }

    @Test
    fun `a weekday task is not due on other days`() {
        val t = task(Repeat.OnWeekdays(Weekdays.of(DayOfWeek.FRIDAY)))
        assertEquals(TaskDay.GAP, History.taskDay(t, LocalDate.of(2026, 10, 30), today, install, emptyList()))
        assertEquals(TaskDay.NOT_DUE, History.taskDay(t, LocalDate.of(2026, 10, 29), today, install, emptyList()))
    }

    @Test
    fun `an every-N task left undone shows one gap, not a run`() {
        val t = task(Repeat.EveryNDays(2))
        val done = listOf(LocalDate.of(2026, 10, 10))
        assertEquals(TaskDay.NOT_DUE, History.taskDay(t, LocalDate.of(2026, 10, 11), today, install, done))
        assertEquals(TaskDay.GAP, History.taskDay(t, LocalDate.of(2026, 10, 12), today, install, done))
        assertEquals(TaskDay.NOT_DUE, History.taskDay(t, LocalDate.of(2026, 10, 13), today, install, done))
    }

    @Test
    fun `a task added later is not due before its start`() {
        val t = task(Repeat.Daily, start = LocalDate.of(2026, 10, 25))
        assertEquals(TaskDay.NOT_DUE, History.taskDay(t, LocalDate.of(2026, 10, 20), today, install, emptyList()))
    }

    @Test
    fun `month counts follow Jalali months in Persian and Gregorian in English`() {
        // 1 Aban 1405 = 23 Oct 2026; 1 Mehr = 23 Sep.
        val done = listOf(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 30))
        val jalali = History.monthCounts(done, today, 2, jalali = true)
        assertEquals(listOf(MonthCount(LocalDate.of(2026, 9, 23), 1), MonthCount(LocalDate.of(2026, 10, 23), 2)), jalali)
        val greg = History.monthCounts(done, today, 2, jalali = false)
        assertEquals(listOf(MonthCount(LocalDate.of(2026, 9, 1), 0), MonthCount(LocalDate.of(2026, 10, 1), 3)), greg)
    }
}
