package app.lantern.domain.widget

import app.lantern.domain.tasks.DayItem
import app.lantern.domain.tasks.Repeat
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.time.Part
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WidgetContentTest {

    private val day = LocalDate.of(2026, 10, 17)
    private fun item(id: Long, part: Part, done: Boolean, important: Boolean = false, floor: Boolean = false, repeat: Repeat = Repeat.Daily) =
        DayItem(Task(id, "t$id", TaskIcon.DROP, part, repeat, day, isImportant = important, inMinimumDay = floor), done, countsInScore = true)

    @Test
    fun `shows the current part's pending tasks, important first, at most three`() {
        val items = listOf(
            item(1, Part.MORNING, done = false), // an earlier part: never listed
            item(2, Part.EVENING, done = false),
            item(3, Part.EVENING, done = false, important = true),
            item(4, Part.EVENING, done = true),
            item(5, Part.EVENING, done = false),
            item(6, Part.EVENING, done = false),
        )
        val w = WidgetContent.of(items, minimumMode = false, part = Part.EVENING)
        assertEquals(listOf(3L, 2L, 5L), w.pending.map { it.taskId })
        assertEquals(1, w.more)
        assertEquals(1, w.done)
        assertEquals(6, w.total)
    }

    @Test
    fun `a minimum day counts only the floor`() {
        val items = listOf(item(1, Part.MORNING, done = true, floor = true), item(2, Part.MORNING, done = false))
        val w = WidgetContent.of(items, minimumMode = true, part = Part.MORNING)
        assertEquals(1, w.total)
        assertTrue(w.dayComplete)
        assertTrue(w.partComplete)
    }

    @Test
    fun `a finished part shows the next part's tasks, never an earlier one's`() {
        val items = listOf(item(1, Part.MORNING, done = false), item(2, Part.MIDDAY, done = true), item(3, Part.EVENING, done = false))
        val w = WidgetContent.of(items, minimumMode = false, part = Part.MIDDAY)
        assertEquals(Part.EVENING, w.part)
        assertTrue(w.upcoming)
        assertEquals(listOf(3L), w.pending.map { it.taskId })
    }
}
