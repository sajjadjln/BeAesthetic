package app.beaesthetic.domain.workout

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutRulesTest {

    private val pushUp = Exercise(1, "شنا", ExerciseKind.REPS)
    private val row = Exercise(2, "زیربغل دمبل", ExerciseKind.REPS)
    private val plank = Exercise(3, "پلانک", ExerciseKind.SECONDS)
    private val mon = LocalDate.of(2026, 10, 12)
    private val wed = LocalDate.of(2026, 10, 14)

    private fun set(session: Long, date: LocalDate, ex: Exercise, index: Int, value: Int, kg: Double? = null, effort: Int = 8) =
        LoggedSet(session, date, ex.id, index, value, kg, effort)

    @Test
    fun `previous is the last other session that logged the move, in set order`() {
        val history = listOf(
            set(1, mon, pushUp, 1, 11), set(1, mon, pushUp, 0, 12),
            set(2, wed, row, 0, 10),
            set(3, wed, pushUp, 0, 13), // the session being logged now
        )
        assertEquals(listOf(12, 11), WorkoutRules.previous(history, pushUp.id, currentSessionId = 3).map { it.value })
        assertEquals(emptyList(), WorkoutRules.previous(history, plank.id, currentSessionId = 3))
    }

    @Test
    fun `the same set last time fills value and effort`() {
        val item = PlanItem(pushUp, sets = 3, rangeMin = 8, rangeMax = 15)
        val previous = listOf(set(1, mon, pushUp, 0, 12, effort = 7), set(1, mon, pushUp, 1, 12, effort = 8), set(1, mon, pushUp, 2, 10, effort = 9))
        assertEquals(SetDraft(10, null, 9), WorkoutRules.prefill(item, previous, emptyList(), 2))
    }

    @Test
    fun `a set past last time's count follows this session's last set`() {
        val item = PlanItem(pushUp, sets = 4, rangeMin = 8, rangeMax = 15)
        val previous = listOf(set(1, mon, pushUp, 0, 12), set(1, mon, pushUp, 1, 11))
        val now = listOf(set(2, wed, pushUp, 0, 13, effort = 6), set(2, wed, pushUp, 1, 12, effort = 9), set(2, wed, pushUp, 2, 10, effort = 10))
        assertEquals(SetDraft(10, null, 10), WorkoutRules.prefill(item, previous, now, 3))
    }

    @Test
    fun `the first time uses the bottom of the range, or a start for max sets`() {
        assertEquals(8, WorkoutRules.prefill(PlanItem(pushUp, 3, 8, 15), emptyList(), emptyList(), 0).value)
        assertEquals(10, WorkoutRules.prefill(PlanItem(pushUp, 3), emptyList(), emptyList(), 0).value)
        assertEquals(30, WorkoutRules.prefill(PlanItem(plank, 2), emptyList(), emptyList(), 0).value)
    }

    @Test
    fun `effort falls back to the target, then 7`() {
        assertEquals(9, WorkoutRules.prefill(PlanItem(pushUp, 3, targetEffort = 9), emptyList(), emptyList(), 0).effort)
        assertEquals(7, WorkoutRules.prefill(PlanItem(pushUp, 3), emptyList(), emptyList(), 0).effort)
    }

    @Test
    fun `kg comes from this session first, then last time, then the plan`() {
        val item = PlanItem(row, sets = 4, rangeMin = 8, rangeMax = 12, kg = 12.5)
        assertEquals(12.5, WorkoutRules.prefill(item, emptyList(), emptyList(), 0).kg)
        val previous = listOf(set(1, mon, row, 0, 10, kg = 15.0), set(1, mon, row, 1, 9, kg = 15.0))
        assertEquals(15.0, WorkoutRules.prefill(item, previous, emptyList(), 0).kg)
        // Set 1 was logged with a lighter dumbbell today: set 2 follows it.
        val now = listOf(set(2, wed, row, 0, 12, kg = 13.0))
        assertEquals(13.0, WorkoutRules.prefill(item, previous, now, 1).kg)
    }

    @Test
    fun `a move without kg in the plan has no kg, even if it was logged with some`() {
        val previous = listOf(set(1, mon, pushUp, 0, 12, kg = 5.0))
        assertNull(WorkoutRules.prefill(PlanItem(pushUp, 3), previous, emptyList(), 0).kg)
    }

    @Test
    fun `the first set of a move is never a record`() {
        assertFalse(WorkoutRules.isRecord(12, null, emptyList()))
    }

    @Test
    fun `more reps than ever is a record, the same is not`() {
        val earlier = listOf(set(1, mon, pushUp, 0, 12), set(1, mon, pushUp, 1, 11))
        assertTrue(WorkoutRules.isRecord(13, null, earlier))
        assertFalse(WorkoutRules.isRecord(12, null, earlier))
        assertFalse(WorkoutRules.isRecord(0, null, earlier))
    }

    @Test
    fun `reps count against sets at the same or a higher kg only`() {
        val earlier = listOf(set(1, mon, row, 0, 12, kg = 10.0), set(1, mon, row, 1, 8, kg = 15.0))
        // 10 reps at 15 kg beats the 8 at 15 kg; the 12 at 10 kg is lighter, so it doesn't count.
        assertTrue(WorkoutRules.isRecord(10, 15.0, earlier))
        // 12 at 12.5 kg: only the 15 kg set is at or above, and 12 > 8.
        assertTrue(WorkoutRules.isRecord(12, 12.5, earlier))
        // 12 at 10 kg ties the 12 at 10 kg.
        assertFalse(WorkoutRules.isRecord(12, 10.0, earlier))
        // A load never lifted before is a record.
        assertTrue(WorkoutRules.isRecord(5, 17.0, earlier))
    }

    @Test
    fun `the session opens on the first move with sets left`() {
        val items = listOf(PlanItem(pushUp, 2), PlanItem(row, 3), PlanItem(plank, 2))
        assertEquals(0, WorkoutRules.firstOpen(items, emptyList()))
        val logged = listOf(set(2, wed, pushUp, 0, 12), set(2, wed, pushUp, 1, 12), set(2, wed, row, 0, 10))
        assertEquals(1, WorkoutRules.firstOpen(items, logged))
        val all = logged + listOf(set(2, wed, row, 1, 10), set(2, wed, row, 2, 9), set(2, wed, plank, 0, 40), set(2, wed, plank, 1, 40))
        assertNull(WorkoutRules.firstOpen(items, all))
    }

    @Test
    fun `the next set is the first gap`() {
        val item = PlanItem(row, 3)
        assertEquals(0, WorkoutRules.nextSetIndex(item, emptyList()))
        assertEquals(1, WorkoutRules.nextSetIndex(item, listOf(set(2, wed, row, 0, 10), set(2, wed, row, 2, 9))))
        assertEquals(3, WorkoutRules.nextSetIndex(item, (0..2).map { set(2, wed, row, it, 10) }))
    }

    @Test
    fun `kg steps by a half and stops at zero`() {
        assertEquals(9.0, WorkoutRules.stepKg(8.5, up = true))
        assertEquals(8.0, WorkoutRules.stepKg(8.5, up = false))
        assertEquals(0.0, WorkoutRules.stepKg(0.0, up = false))
        assertEquals(9.0, WorkoutRules.stepKg(8.4, up = true)) // a stray value snaps to the grid (8.5) first
    }

    @Test
    fun `records in a session count earlier sets of the same session`() {
        val before = listOf(set(1, mon, pushUp, 0, 12), set(1, mon, pushUp, 1, 11))
        val now = listOf(set(2, wed, pushUp, 0, 13), set(2, wed, pushUp, 1, 13), set(2, wed, pushUp, 2, 14), set(2, wed, row, 0, 10))
        // 13 beats 12; the second 13 only ties it; 14 beats 13. The row has no history yet.
        assertEquals(setOf(pushUp.id to 0, pushUp.id to 2), WorkoutRules.records(now, before))
    }
}
