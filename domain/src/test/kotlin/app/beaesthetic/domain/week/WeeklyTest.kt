package app.beaesthetic.domain.week

import app.beaesthetic.domain.score.DayCounts
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.ProgramCalendar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyTest {

    // Day 0 is Friday 9 Oct 2026; week 1 is Sat 10 – Fri 16 Oct.
    private val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    private val week1 = LocalDate.of(2026, 10, 10)
    private val friday = LocalDate.of(2026, 10, 16)
    private val saturday = LocalDate.of(2026, 10, 17)

    private fun snap(due: Int, done: Int, special: Boolean = false) = DaySnapshot(DayCounts(due, done, false), special)

    @Test
    fun `Friday reviews its own week, Saturday catches up on the last one, other days have no ritual`() {
        assertEquals(week1, Ritual.open(friday, calendar, emptySet()))
        assertEquals(week1, Ritual.open(saturday, calendar, emptySet()))
        assertNull(Ritual.open(saturday, calendar, setOf(week1)))
        assertNull(Ritual.open(LocalDate.of(2026, 10, 15), calendar, emptySet()))
        // Day 0's own Friday is not a ritual in the app.
        assertNull(Ritual.open(LocalDate.of(2026, 10, 9), calendar, emptySet()))
    }

    @Test
    fun `the plan is for next week on Friday and for this week otherwise`() {
        assertEquals(saturday, Ritual.planWeek(friday))
        assertEquals(saturday, Ritual.planWeek(saturday))
        assertEquals(week1, Ritual.planWeek(LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun `strength grows with each done day and halves in about 13 missed days`() {
        val days = (0L until 30L).map { week1.plusDays(it) }
        val all = RoutineStrength.score(days, days.toSet())
        assertTrue(all > 0.75)
        val after = RoutineStrength.score(days + (30L until 43L).map { week1.plusDays(it) }, days.toSet())
        assertEquals(all / 2, after, 0.01)
        assertEquals(0.0, RoutineStrength.score(days, emptySet()))
    }

    @Test
    fun `best routines are the ones done on every due day, strongest first, at most three`() {
        val r = listOf(
            Routine(1, "آب", 7, 7, 0.4),
            Routine(2, "مسواک", 7, 6, 0.9),
            Routine(3, "اسکین", 7, 7, 0.6),
            Routine(4, "تمرین", 4, 4, 0.3),
            Routine(5, "کتاب", 7, 7, 0.5),
        )
        assertEquals(listOf(3L, 5L, 1L), WeekReview.best(r).map { it.taskId })
    }

    @Test
    fun `routines count due and done days in the week and skip floor-only tasks`() {
        val daily = Task(1, "آب", TaskIcon.DROP, Part.MORNING, Repeat.Daily, week1)
        val floor = Task(2, "کف", TaskIcon.DROP, Part.MORNING, Repeat.MinDayOnly, week1)
        val done = (0L..4L).map { week1.plusDays(it) }
        val routines = WeekReview.routines(listOf(daily, floor), mapOf(1L to done, 2L to done), week1, week1, friday)
        assertEquals(1, routines.size)
        assertEquals(7, routines[0].due)
        assertEquals(5, routines[0].done)
    }

    @Test
    fun `wins count comebacks and shown-up days, and today counts live in the score`() {
        val s = mapOf(
            week1 to snap(4, 4),
            week1.plusDays(1) to snap(4, 0), // a gap
            week1.plusDays(2) to snap(4, 2), // a comeback
            week1.plusDays(3) to snap(4, 4),
            week1.plusDays(4) to snap(4, 0, special = true),
            week1.plusDays(5) to snap(4, 4),
            friday to snap(4, 2),
        )
        val w = WeekReview.wins(week1, friday, week1, calendar, s, emptyList(), records = 0, workouts = 2)
        assertEquals(5, w.shownUp)
        assertEquals(1, w.comebacks)
        // (1 + 0 + .5 + 1 + 1 + .5) / 6, the special day left out.
        assertEquals(67, w.score)
        assertEquals(Win.COMEBACKS, w.headline)
        assertEquals(Win.RECORDS, w.copy(records = 1).headline)
    }

    @Test
    fun `reserves are the cap minus the special days already set`() {
        val s = mapOf(saturday.plusDays(2) to snap(0, 0, special = true))
        assertEquals(1, WeekReview.reserves(saturday, s, 2))
        assertEquals(2, WeekReview.reserves(week1, s, 2))
    }
}
