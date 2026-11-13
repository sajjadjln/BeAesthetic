package app.lantern.domain.goals

import app.lantern.domain.time.JalaliMonth
import app.lantern.domain.time.Weekdays
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalRulesTest {
    private val today = LocalDate.of(2026, 11, 13) // Friday, 22 Aban 1405

    @Test
    fun `the slider runs 1 to 365 days, finer at the short end`() {
        assertEquals(1, GoalTimeline.toDays(0))
        assertEquals(365, GoalTimeline.toDays(1000))
        assertTrue(GoalTimeline.toDays(250) < 50) // a quarter of the track is under 7 weeks
        for (d in listOf(10, 91, 200, 365)) assertEquals(d, GoalTimeline.toDays(GoalTimeline.toPosition(d)), "round trip $d")
    }

    @Test
    fun `the slider snaps to the markers and Nowruz`() {
        assertEquals(LocalDate.of(2027, 3, 21), GoalTimeline.nextNowruz(today))
        val nowruz = GoalTimeline.pick(GoalTimeline.toPosition(127) + 5, today)
        assertEquals(GoalTimeline.Mark.NOWRUZ, nowruz.marker)
        assertEquals(127, nowruz.days) // 29 Esfand
        assertEquals(GoalTimeline.Pick(91, GoalTimeline.Mark.MONTHS_3), GoalTimeline.pick(GoalTimeline.toPosition(91) - 4, today))
        assertNull(GoalTimeline.pick(GoalTimeline.toPosition(30), today).marker)
    }

    @Test
    fun `lengths read as days, weeks, months, a year`() {
        assertEquals(GoalTimeline.Length.Days(9), GoalTimeline.length(9))
        assertEquals(GoalTimeline.Length.Weeks(3), GoalTimeline.length(21))
        assertEquals(GoalTimeline.Length.Months(3), GoalTimeline.length(91))
        assertEquals(GoalTimeline.Length.Year, GoalTimeline.length(365))
    }

    @Test
    fun `the range is picked days minus flex, never below one`() {
        val a = listOf(GoalAction(1, Weekdays.of(SATURDAY, MONDAY, WEDNESDAY)), GoalAction(2, Weekdays.of(SATURDAY, MONDAY)))
        assertEquals(4 to 5, GoalRules.range(a, 1))
        assertEquals(1 to 1, GoalRules.range(listOf(GoalAction(1, Weekdays.of(MONDAY))), 2))
    }

    @Test
    fun `the week starts Saturday with done, gray gaps and days ahead`() {
        val a = listOf(GoalAction(1, Weekdays.ALL))
        val sat = LocalDate.of(2026, 11, 7)
        val w = GoalRules.week(today, a, listOf(sat, sat.plusDays(2), sat.plusDays(2), sat.minusDays(1)), 1)
        assertEquals(sat, w.start)
        assertEquals(
            listOf(WeekDay.DONE, WeekDay.GAP, WeekDay.DONE, WeekDay.GAP, WeekDay.GAP, WeekDay.GAP, WeekDay.AHEAD),
            w.days,
        )
        assertEquals(3, w.ticks) // two actions ticked on Monday count twice
        assertEquals(6 to 7, w.low to w.high)
    }

    @Test
    fun `days before the goal began are not gaps`() {
        val w = GoalRules.week(today, listOf(GoalAction(1, Weekdays.ALL)), emptyList(), 1, goalStart = today)
        assertEquals(List(6) { WeekDay.BEFORE } + WeekDay.AHEAD, w.days)
    }

    @Test
    fun `months run from the start month to the end month`() {
        val m = GoalRules.months(today, LocalDate.of(2027, 3, 20)) // to 29 Esfand
        assertEquals(listOf(8, 9, 10, 11, 12), m.map { it.month })
        assertEquals(JalaliMonth(1405, 8), m.first())
        assertEquals(1, GoalRules.monthIndex(today, LocalDate.of(2027, 3, 20), LocalDate.of(2026, 11, 25)))
        assertEquals(4, GoalRules.monthIndex(today, LocalDate.of(2027, 3, 20), LocalDate.of(2027, 5, 1)))
        assertEquals(1, GoalRules.months(today, today).size)
    }

    @Test
    fun `time share and days left`() {
        val end = today.plusDays(100)
        assertEquals(0f, GoalRules.timeShare(today, end, today))
        assertEquals(0.5f, GoalRules.timeShare(today, end, today.plusDays(50)))
        assertEquals(1f, GoalRules.timeShare(today, end, end.plusDays(9)))
        assertEquals(0, GoalRules.daysLeft(end, end.plusDays(3)))
    }

    @Test
    fun `quiet after 5 days with no tick, offered once per spell`() {
        val start = today.minusDays(20)
        assertFalse(GoalRules.isQuiet(start, today.minusDays(4), null, today))
        assertTrue(GoalRules.isQuiet(start, today.minusDays(5), null, today))
        assertTrue(GoalRules.isQuiet(start, null, null, today)) // never ticked since the start
        assertFalse(GoalRules.isQuiet(today.minusDays(2), null, null, today)) // a new goal is not quiet
        assertFalse(GoalRules.isQuiet(start, today.minusDays(6), today.minusDays(1), today)) // already offered
        assertTrue(GoalRules.isQuiet(start, today.minusDays(6), today.minusDays(9), today)) // offered in an older spell
    }
}

class GoalDraftTest {
    private val today = LocalDate.of(2026, 11, 13)

    @Test
    fun `each step opens only when its answers are in`() {
        var d = GoalDraft()
        assertFalse(d.step1Ok)
        d = d.copy(title = "IELTS 7")
        assertTrue(d.step1Ok)
        assertFalse(d.step2Ok(today))
        d = d.copy(endDate = LocalDate.of(2027, 3, 20)).withMonthsFor(today)
        assertTrue(d.step2Ok(today))
        assertEquals(5, d.months.size)
        assertFalse(d.step3Ok)
        assertTrue(d.copy(triedBefore = false).step3Ok)
        d = d.copy(triedBefore = true, timeTouched = true, triedWhat = "a class")
        assertFalse(d.step3Ok)
        assertFalse(d.copy(stopReason = StopReason.OWN).step3Ok)
        assertTrue(d.copy(stopReason = StopReason.OWN, ownReason = "moved").step3Ok)
        d = d.copy(stopReason = StopReason.BORED)
        assertEquals("BORED", d.storedReason)
        assertFalse(d.step4Ok)
        d = d.copy(months = d.months.toMutableList().also { it[0] = "words" }, actions = listOf(ActionDraft("25 min", Weekdays.of(SATURDAY, MONDAY))))
        assertTrue(d.step4Ok)
        assertEquals(1 to 2, d.range())
        assertFalse(d.ready(today))
        assertTrue(d.copy(why = "a new life").ready(today))
    }

    @Test
    fun `changing the end date keeps typed months`() {
        val d = GoalDraft(endDate = LocalDate.of(2027, 3, 20)).withMonthsFor(today).let { it.copy(months = it.months.toMutableList().also { m -> m[0] = "a"; m[1] = "b" }) }
        val shorter = d.copy(endDate = today.plusDays(20)).withMonthsFor(today)
        assertEquals(listOf("a", "b"), shorter.months)
    }
}
