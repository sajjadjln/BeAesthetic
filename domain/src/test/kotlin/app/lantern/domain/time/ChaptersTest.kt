package app.lantern.domain.time

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChaptersTest {

    private val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    private fun d(m: Int, day: Int) = LocalDate.of(if (m >= 10) 2026 else 2027, m, day)

    @Test
    fun `five chapters by jalali month, with mehr merged into aban`() {
        val chapters = Chapters.of(calendar)
        assertEquals(
            listOf(
                Chapter(1, d(10, 9), d(11, 21), listOf(7, 8)),
                Chapter(2, d(11, 22), d(12, 21), listOf(9)),
                Chapter(3, d(12, 22), d(1, 20), listOf(10)),
                Chapter(4, d(1, 21), d(2, 19), listOf(11)),
                Chapter(5, d(2, 20), d(3, 20), listOf(12)),
            ),
            chapters,
        )
    }

    @Test
    fun `chapter starts`() {
        assertTrue(Chapters.isChapterStart(calendar, d(11, 22)))
        assertTrue(Chapters.isChapterStart(calendar, d(2, 20)))
        assertFalse(Chapters.isChapterStart(calendar, d(10, 9))) // Day 0 is not a chapter-start card
        assertFalse(Chapters.isChapterStart(calendar, d(10, 23))) // 1 Aban stays inside chapter 1
        assertEquals(3, Chapters.at(calendar, d(1, 1))!!.number)
    }

    @Test
    fun `countdown only in the last chapter`() {
        assertNull(Chapters.countdown(calendar, d(2, 19)))
        assertEquals(Countdown(29, toNowruz = true), Chapters.countdown(calendar, d(2, 20)))
        val finish = Chapters.countdown(calendar, d(3, 20))!!
        assertEquals(1, finish.daysLeft)
        assertTrue(finish.isFinishDay)
        assertNull(Chapters.countdown(calendar, d(3, 21)))
        // A program that doesn't end on 29 Esfand counts to the finish line, not to Nowruz.
        val other = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 10))
        assertFalse(Chapters.countdown(other, LocalDate.of(2027, 3, 1))!!.toNowruz)
    }

    @Test
    fun `letter opening dates`() {
        assertEquals(d(3, 20), Chapters.letterOpensOn(calendar, d(10, 11), firstLetter = true))
        assertEquals(d(12, 22), Chapters.letterOpensOn(calendar, d(11, 22), firstLetter = false)) // Azar → 1 Dey
        assertEquals(d(3, 20), Chapters.letterOpensOn(calendar, d(2, 20), firstLetter = false)) // Esfand → finish
    }
}
