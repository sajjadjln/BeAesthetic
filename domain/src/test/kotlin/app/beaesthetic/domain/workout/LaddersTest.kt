package app.beaesthetic.domain.workout

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaddersTest {

    // knee push-up (1) → push-up (2) → decline (3); plank (9) alone.
    private val links = mapOf(1L to 2L, 2L to 3L)

    @Test
    fun `a chain runs easiest to hardest from any of its moves`() {
        assertEquals(listOf(1L, 2L, 3L), Ladders.chain(2, links))
        assertEquals(listOf(1L, 2L, 3L), Ladders.chain(3, links))
        assertEquals(listOf(9L), Ladders.chain(9, links))
        assertEquals(1L, Ladders.root(3, links))
    }

    @Test
    fun `a stored loop doesn't hang`() {
        assertEquals(setOf(1L, 2L), Ladders.chain(1, mapOf(1L to 2L, 2L to 1L)).toSet())
    }

    @Test
    fun `linking back to the move or an easier one is a loop`() {
        assertTrue(Ladders.makesLoop(2, 2, links))
        assertTrue(Ladders.makesLoop(3, 1, links))
        assertFalse(Ladders.makesLoop(3, 9, links))
        assertFalse(Ladders.makesLoop(1, 3, links)) // skipping a rung is allowed
    }
}

class ProgressionTest {

    private val pushUp = Exercise(2, "شنا", ExerciseKind.REPS)
    private val decline = Exercise(3, "شنا شیب منفی", ExerciseKind.REPS)
    private val row = Exercise(4, "زیربغل", ExerciseKind.REPS)
    private val plank = Exercise(9, "پلانک", ExerciseKind.SECONDS)
    private val day = LocalDate.of(2026, 10, 14)

    private fun sets(ex: Exercise, vararg values: Int) = values.mapIndexed { i, v -> LoggedSet(1, day, ex.id, i, v, null, 8) }

    @Test
    fun `the top is every planned set at the top of the range`() {
        val item = PlanItem(pushUp, sets = 3, rangeMin = 8, rangeMax = 15)
        assertTrue(Progression.hitTop(item, sets(pushUp, 15, 15, 16)))
        assertFalse(Progression.hitTop(item, sets(pushUp, 15, 15, 14)))
        assertFalse(Progression.hitTop(item, sets(pushUp, 15, 15)))
        assertFalse(Progression.hitTop(PlanItem(pushUp, sets = 2), sets(pushUp, 30, 30))) // «حداکثر»
    }

    @Test
    fun `a harder variant is offered, else the range moves up`() {
        val items = listOf(PlanItem(pushUp, 2, 8, 15), PlanItem(row, 2, 8, 12), PlanItem(plank, 2, 30, 45))
        val logged = sets(pushUp, 15, 15) + sets(row, 12, 13) + sets(plank, 45, 45)
        val harder = mapOf(pushUp.id to decline)
        assertEquals(
            listOf(Offer.NextRung(pushUp.id, decline), Offer.RaiseRange(row.id, 9, 13), Offer.RaiseRange(plank.id, 35, 50)),
            Progression.offers(items, logged, harder::get),
        )
    }

    @Test
    fun `no offer when the harder variant is already in the day`() {
        val items = listOf(PlanItem(pushUp, 1, 8, 15), PlanItem(decline, 1, 6, 10))
        assertEquals(emptyList(), Progression.offers(items, sets(pushUp, 15), mapOf(pushUp.id to decline)::get))
    }
}

class RoundNumbersTest {

    private val day = LocalDate.of(2026, 10, 14)
    private val kinds = mapOf(1L to ExerciseKind.REPS, 2L to ExerciseKind.REPS, 9L to ExerciseKind.SECONDS)
    private val roots = mapOf(1L to 1L, 2L to 1L, 9L to 9L)

    private fun set(session: Long, ex: Long, index: Int, value: Int) = LoggedSet(session, day, ex, index, value, null, 8)

    @Test
    fun `keys round-trip`() {
        val reps = RoundNumbers.Crossing(RoundNumbers.Kind.REPS, 3, 1_000)
        assertEquals("reps:ladder:3:1000", reps.key)
        assertEquals(reps, RoundNumbers.parse(reps.key))
        val days = RoundNumbers.Crossing(RoundNumbers.Kind.DAYS, null, 30)
        assertEquals("days:30", days.key)
        assertEquals(days, RoundNumbers.parse("days:30"))
        assertEquals(null, RoundNumbers.parse("nonsense:1"))
    }

    @Test
    fun `reps add up across a ladder's rungs, seconds per timed ladder, workouts per session`() {
        // 60 knee push-ups + 45 push-ups = 105 on ladder 1; 600 s of plank; 10 sessions.
        val sets = (1L..10L).map { set(it, 9, 0, 60) } + listOf(set(1, 1, 0, 30), set(1, 1, 1, 30), set(2, 2, 0, 45))
        val crossings = RoundNumbers.workoutCrossings(sets, kinds::get, { roots.getValue(it) })
        assertEquals(
            setOf("reps:ladder:1:100", "seconds:ladder:9:600", "workouts:10"),
            crossings.map { it.key }.toSet(),
        )
    }

    @Test
    fun `only new crossings, and only the highest of each total`() {
        val all = RoundNumbers.reached(RoundNumbers.Kind.REPS, 1, 1_200) + RoundNumbers.reached(RoundNumbers.Kind.DAYS, null, 30)
        val fresh = RoundNumbers.new(all, setOf("reps:ladder:1:100", "days:30"))
        assertEquals(listOf("reps:ladder:1:500", "reps:ladder:1:1000"), fresh.map { it.key })
        assertEquals(listOf(1_000), RoundNumbers.highestEach(fresh).map { it.value })
    }
}
