package app.lantern.domain.goals

import app.lantern.domain.tasks.DueRules
import app.lantern.domain.tasks.Repeat
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskAlarm
import app.lantern.domain.tasks.TaskForms
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.time.JalaliMonth
import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.time.LocalDate

class GoalPlansTest {
    private val start = LocalDate.of(2026, 11, 17)
    private fun validDraft(): GoalDraft {
        val finish = start.plusDays(90)
        return GoalDraft(title = "Read", start = start, finish = finish, triedBefore = false,
            milestones = mapOf(GoalPlans.months(start, finish).first().start to "First book"),
            actions = listOf(GoalActionDraft("Read 5 pages", weekdays = 21)), why = "Make room for learning")
    }

    @Test fun `required why and every visible answer gate pinning`() {
        val d = validDraft()
        assertTrue(GoalPlans.valid(d))
        assertFalse(GoalPlans.valid(d.copy(why = " \n")))
        assertFalse(GoalPlans.valid(d.copy(actions = listOf(GoalActionDraft("Read")))))
        assertFalse(GoalPlans.valid(d.copy(milestones = emptyMap())))
        assertFalse(GoalPlans.valid(d.copy(triedBefore = null)))
        assertFalse(GoalPlans.valid(d.copy(triedBefore = true)))
        assertTrue(GoalPlans.valid(d.copy(triedBefore = true, baselineMinutes = 60, triedWhat = "Books", obstacle = "Time")))
    }

    @Test fun `short goals skip months and long chapters cover each date exactly once`() {
        assertTrue(GoalPlans.months(start, start.plusDays(42)).isEmpty())
        val last = start.plusYears(1)
        val months = GoalPlans.months(start, last)
        assertEquals(start, months.first().start)
        assertEquals(last, months.last().end)
        months.zipWithNext().forEach { (a, b) -> assertEquals(a.end.plusDays(1), b.start) }
        // Start late in a Jalali month: the tiny first chapter joins the next.
        val late = JalaliMonth.of(start).lastDay.minusDays(2)
        val merged = GoalPlans.months(late, late.plusDays(100))
        assertEquals(late, merged.first().start)
        assertEquals(JalaliMonth.of(late).plus(1).lastDay, merged.first().end)
    }

    @Test fun `flex range and session counts include multiple actions without double counting dates`() {
        val actions = listOf(GoalActionDraft("Read", 21), GoalActionDraft("Walk", 2))
        assertEquals(3..4, GoalPlans.range(actions, 1))
        assertEquals(1..1, GoalPlans.range(listOf(GoalActionDraft("Read", 1)), 2))
        val dates = mapOf(1L to listOf(start, start, start.plusDays(1)), 2L to listOf(start), 3L to listOf(start))
        assertEquals(3, GoalPlans.sessions(setOf(1L, 2L), dates, start, start.plusDays(1)))
        assertEquals(java.time.DayOfWeek.SATURDAY, GoalPlans.weekDays(start).first().dayOfWeek)
    }

    @Test fun `Gregorian plans use Gregorian boundaries and quiet offers reset after a new tick`() {
        val months = GoalPlans.months(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 2, 15), GoalCalendar.GREGORIAN)
        assertEquals(LocalDate.of(2026, 11, 30), months.first().end)
        assertEquals(LocalDate.of(2026, 12, 1), months[1].start)
        assertFalse(GoalPlans.quiet(start, null, null, start.plusDays(4)))
        assertTrue(GoalPlans.quiet(start, null, null, start.plusDays(5)))
        assertFalse(GoalPlans.quiet(start, null, start.plusDays(5), start.plusDays(9)))
        assertTrue(GoalPlans.quiet(start, start.plusDays(5), start.plusDays(5), start.plusDays(10)))
    }

    @Test fun `goal action stops after finish and task editor keeps its link and deadline`() {
        val task = Task(1, "Read", TaskIcon.CHECK, Part.NIGHT, Repeat.OnWeekdays(Weekdays.ALL), start,
            goalId = 9, endDate = start.plusDays(4), alarm = TaskAlarm(1200, Weekdays.ALL, true))
        assertNotNull(DueRules.itemFor(start.plusDays(4), task, emptyList()))
        assertNull(DueRules.itemFor(start.plusDays(5), task, emptyList()))
        val edited = TaskForms.toTask(TaskForms.from(task).copy(title = "Read more"), task, start, 0)
        assertEquals(9L, edited.goalId)
        assertEquals(task.endDate, edited.endDate)
    }
}
