package app.beaesthetic.domain.week

import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Part
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderFadeTest {

    private val start = LocalDate.of(2026, 10, 10)
    private val today = LocalDate.of(2026, 12, 5) // week 9
    private val now = Instant.parse("2026-12-05T08:00:00Z")
    private fun task(id: Long, silenced: Boolean = false, repeat: Repeat = Repeat.Daily) =
        Task(id, "t$id", TaskIcon.DROP, Part.MORNING, repeat, start, remindersSilenced = silenced)
    private fun days(from: LocalDate, to: LocalDate) = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.toList()

    @Test
    fun `a routine done every day for weeks is strong, a recent gap run is not`() {
        val all = days(start, today.minusDays(1))
        assertTrue(ReminderFade.isStrong(task(1), all, today))
        val gappy = all.filterNot { it in days(today.minusDays(14), today.minusDays(3)) }
        assertFalse(ReminderFade.isStrong(task(1), gappy, today))
    }

    @Test
    fun `offers start in week 8, once per task`() {
        val all = mapOf(1L to days(start, today.minusDays(1)))
        assertNull(ReminderFade.offer(7, today, now, listOf(task(1)), all, emptyMap()))
        assertEquals(FadeOffer.Silence(task(1)), ReminderFade.offer(9, today, now, listOf(task(1)), all, emptyMap()))
        assertNull(ReminderFade.offer(9, today, now, listOf(task(1)), all, mapOf(1L to now.minusSeconds(86_400L * 30))))
    }

    @Test
    fun `a silenced routine that slipped this week gets the restore offer, at most weekly`() {
        val slipped = mapOf(1L to days(start, today.minusDays(8)) + today.minusDays(2))
        val t = task(1, silenced = true)
        assertEquals(FadeOffer.Restore(t), ReminderFade.offer(9, today, now, listOf(t), slipped, emptyMap()))
        assertNull(ReminderFade.offer(9, today, now, listOf(t), slipped, mapOf(1L to now.minusSeconds(86_400L * 3))))
        val steady = mapOf(1L to days(start, today.minusDays(1)))
        assertNull(ReminderFade.offer(9, today, now, listOf(t), steady, emptyMap()))
    }

    @Test
    fun `anytime and one-off tasks are never offered`() {
        val all = mapOf(1L to days(start, today.minusDays(1)))
        assertNull(ReminderFade.offer(9, today, now, listOf(task(1, repeat = Repeat.Anytime)), all, emptyMap()))
    }
}
