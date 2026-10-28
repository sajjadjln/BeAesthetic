package app.lantern.ui.today

import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.time.Part
import app.lantern.ui.theme.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayPickTest {
    private fun tile(id: Long, part: Part, done: Boolean = false) = TileUi(id, "t$id", TaskIcon.entries.first(), done, important = false, due = false, part = part)

    @Test
    fun `mood follows the day, never a miss`() {
        assertEquals(Mood.CALM, TodayPick.mood(0, 5, Part.MORNING))
        assertEquals(Mood.BUSY, TodayPick.mood(2, 5, Part.EVENING))
        assertEquals(Mood.HAPPY, TodayPick.mood(5, 5, Part.MIDDAY))
        // At night: calm, even with tasks left; happy if everything is done.
        assertEquals(Mood.CALM, TodayPick.mood(2, 5, Part.NIGHT))
        assertEquals(Mood.HAPPY, TodayPick.mood(5, 5, Part.NIGHT))
        // No tasks at all is never "happy".
        assertEquals(Mood.CALM, TodayPick.mood(0, 0, Part.MORNING))
    }

    @Test
    fun `night has its own lines`() {
        assertEquals(TodayPick.Lines.NIGHT, TodayPick.lines(1, 3, Part.NIGHT))
        assertEquals(TodayPick.Lines.HAPPY, TodayPick.lines(3, 3, Part.NIGHT))
        assertEquals(TodayPick.Lines.BUSY, TodayPick.lines(1, 3, Part.MORNING))
    }

    @Test
    fun `now is the first open task from the current part on, then an earlier one`() {
        val tiles = mapOf(
            Part.MORNING to listOf(tile(1, Part.MORNING, done = true), tile(2, Part.MORNING)),
            Part.EVENING to listOf(tile(3, Part.EVENING, done = true), tile(4, Part.EVENING)),
        )
        assertEquals(2L, TodayPick.now(tiles, Part.MORNING)?.taskId)
        assertEquals(4L, TodayPick.now(tiles, Part.MIDDAY)?.taskId)
        assertEquals(4L, TodayPick.now(tiles, Part.EVENING)?.taskId)
        // At night nothing is left there, so the open morning task comes back round.
        assertEquals(2L, TodayPick.now(tiles, Part.NIGHT)?.taskId)
    }

    @Test
    fun `now is empty when everything is done`() {
        assertNull(TodayPick.now(mapOf(Part.MORNING to listOf(tile(1, Part.MORNING, done = true))), Part.MORNING))
        assertNull(TodayPick.now(emptyMap(), Part.EVENING))
    }
}
