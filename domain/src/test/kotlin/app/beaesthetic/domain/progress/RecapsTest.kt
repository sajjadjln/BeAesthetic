package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.workout.LoggedSet
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecapsTest {

    private val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    private val yalda = LocalDate.of(2026, 12, 21)

    @Test
    fun `halfway is the setting, else the midpoint`() {
        assertEquals(yalda, Recaps.halfway(yalda, calendar))
        assertEquals(LocalDate.of(2026, 12, 29), Recaps.halfway(null, calendar))
    }

    @Test
    fun `the card shows from Yalda until it's seen`() {
        assertFalse(Recaps.cardOpen(yalda.minusDays(1), yalda, calendar, seen = false))
        assertTrue(Recaps.cardOpen(yalda, yalda, calendar, seen = false))
        assertTrue(Recaps.cardOpen(yalda.plusDays(5), yalda, calendar, seen = false))
        assertFalse(Recaps.cardOpen(yalda, yalda, calendar, seen = true))
        assertTrue(Recaps.available(yalda, yalda))
    }

    @Test
    fun `records count each move's new best once a day`() {
        val d1 = LocalDate.of(2026, 10, 12)
        val d2 = d1.plusDays(2)
        val sets = listOf(
            LoggedSet(1, d1, 1, 0, 8, null, 8),
            LoggedSet(2, d2, 1, 0, 10, null, 8),
            LoggedSet(2, d2, 1, 1, 11, null, 8),
            LoggedSet(2, d2, 2, 0, 5, null, 8),
        )
        val all = Recaps.records(sets, d2)
        assertTrue(all >= 1)
        assertEquals(all, Recaps.records(sets + LoggedSet(3, d2.plusDays(1), 1, 0, 3, null, 8), d2))
    }

    @Test
    fun `the finish card shows from 29 Esfand until seen, then the choice until made`() {
        val end = LocalDate.of(2027, 3, 20)
        assertFalse(Recaps.finishCardOpen(end.minusDays(1), calendar, seen = false))
        assertTrue(Recaps.finishCardOpen(end, calendar, seen = false))
        assertFalse(Recaps.choiceOpen(end, calendar, finishSeen = false, mode = AfterFinish.NONE))
        assertTrue(Recaps.choiceOpen(end.plusDays(3), calendar, finishSeen = true, mode = AfterFinish.NONE))
        assertFalse(Recaps.choiceOpen(end.plusDays(3), calendar, finishSeen = true, mode = AfterFinish.MAINTENANCE))
    }

    @Test
    fun `old check-ins move up by a hundred per program`() {
        assertEquals(0, Recaps.archiveOffset(null))
        assertEquals(100, Recaps.archiveOffset(6))
        assertEquals(200, Recaps.archiveOffset(106))
    }
}
