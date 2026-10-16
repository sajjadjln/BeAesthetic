package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.workout.Exercise
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.LoggedSet
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProofTest {

    private val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    private fun d(m: Int, day: Int) = LocalDate.of(if (m >= 10) 2026 else 2027, m, day)
    private val pullups = Metric(1, MetricKind.STRENGTH, "pullups", "بارفیکس", MetricUnit.REPS, true)
    private val waist = Metric(2, MetricKind.TAPE, "waist", "کمر", MetricUnit.CM, false)

    @Test
    fun `a linked milestone is reached on the first reading that meets it, and stays reached`() {
        val m = Milestone(1, "۱۰ بارفیکس", 1, 10.0, null)
        val readings = listOf(Reading(d(10, 9), 8.0), Reading(d(12, 11), 10.0), Reading(d(1, 8), 9.0))
        val v = Milestones.view(m, pullups, readings)
        assertEquals(d(12, 11), v.reachedOn)
        assertEquals(10.0, v.best)
    }

    @Test
    fun `lower is better milestones and open ones`() {
        val m = Milestone(2, "کمر ۸۵", 2, 85.0, null)
        assertNull(Milestones.view(m, waist, listOf(Reading(d(10, 9), 88.0))).reachedOn)
        assertEquals(d(11, 6), Milestones.view(m, waist, listOf(Reading(d(10, 9), 88.0), Reading(d(11, 6), 85.0))).reachedOn)
    }

    @Test
    fun `a hand milestone keeps its tick, reached ones come first`() {
        val hand = Milestones.view(Milestone(3, "کوه", null, null, d(11, 1)), null, emptyList())
        val open = Milestones.view(Milestone(4, "x", null, null, null), null, emptyList())
        assertEquals(d(11, 1), hand.reachedOn)
        assertEquals(listOf(3L, 4L), Milestones.ordered(listOf(open, hand)).map { it.milestone.id })
    }

    @Test
    fun `program line from day 0 to the finish with chapter ticks`() {
        val start = ProgramLines.of(calendar, d(10, 9))
        assertEquals(0f, start.today)
        assertEquals(4, start.chapterTicks.size)
        assertEquals(1f, ProgramLines.of(calendar, d(3, 20)).today)
        assertEquals(162, start.totalDays)
    }

    private val easy = Exercise(1, "Split squat", ExerciseKind.REPS)
    private val hard = Exercise(2, "Bulgarian", ExerciseKind.REPS)
    private val plank = Exercise(3, "Plank", ExerciseKind.SECONDS)
    private fun set(session: Long, ex: Long, v: Int) = LoggedSet(session, d(10, 12), ex, 0, v, null, 8)

    @Test
    fun `ladders show the planned rung and the highest logged`() {
        val ladders = ProgressTotals.ladders(listOf(easy, hard, plank), mapOf(1L to 2L), planned = setOf(1L), sets = listOf(set(1, 2, 8)))
        assertEquals(1, ladders.size)
        assertEquals(0, ladders[0].current)
        assertEquals(1, ladders[0].reached)
    }

    @Test
    fun `totals count every rung and every session`() {
        val t = ProgressTotals.totals(listOf(easy, hard, plank), mapOf(1L to 2L), setOf(2L), listOf(set(1, 1, 12), set(1, 2, 8), set(2, 3, 60)), daysShownUp = 5)
        assertEquals(2, t.workouts)
        assertEquals(LadderTotal("Plank", ExerciseKind.SECONDS, 60), t.perLadder[0])
        assertEquals(LadderTotal("Bulgarian", ExerciseKind.REPS, 20), t.perLadder[1])
    }

    @Test
    fun `backfill offers past plan days back to day 0`() {
        // Plans on Saturday (0) and Friday (6). 15 Oct is a Thursday.
        assertEquals(listOf(d(10, 10), d(10, 9)), Backfill.days(calendar, d(10, 15), setOf(0, 6)))
    }

    @Test
    fun `day 0 has nothing to reveal against, so it opens no reveal`() {
        assertNull(Reveals.against(0, listOf(0)))
        assertNull(Reveals.against(0, listOf(0, 1), againstLast = true))
    }

    @Test
    fun `a check-in reveals against day 0, or last month when asked`() {
        assertEquals(0, Reveals.against(2, listOf(0, 1, 2)))
        assertEquals(1, Reveals.against(2, listOf(0, 1, 2), againstLast = true))
        // Only Day 0 is earlier: «ماه قبل» falls back to it, and there's no choice to offer.
        assertEquals(0, Reveals.against(1, listOf(0, 1), againstLast = true))
        assertEquals(false, Reveals.canChoose(1, listOf(0, 1)))
        assertEquals(true, Reveals.canChoose(2, listOf(0, 1, 2)))
    }

    @Test
    fun `without day 0 photos the last earlier set is used, and a first set has none`() {
        assertEquals(1, Reveals.against(3, listOf(1, 3)))
        assertNull(Reveals.against(1, listOf(1)))
        assertEquals(false, Reveals.canChoose(3, listOf(1, 3)))
    }
}
