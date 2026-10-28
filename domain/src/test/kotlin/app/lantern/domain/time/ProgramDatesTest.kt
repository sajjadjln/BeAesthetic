package app.lantern.domain.time

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgramDatesTest {

    @Test
    fun `the finish is the next Nowruz eve at least 8 weeks away`() {
        assertEquals(LocalDate.of(2027, 3, 20), ProgramDates.defaultEnd(LocalDate.of(2026, 10, 16)))
        assertEquals(LocalDate.of(2028, 3, 19), ProgramDates.defaultEnd(LocalDate.of(2027, 2, 1)))
        // A new program started on Nowruz 1406 runs to the eve of Nowruz 1407.
        assertEquals(LocalDate.of(2028, 3, 19), ProgramDates.defaultEnd(LocalDate.of(2027, 3, 21)))
    }

    @Test
    fun `Yalda is halfway only inside the program, and a new program starts after the finish`() {
        assertEquals(LocalDate.of(2026, 12, 21), ProgramDates.yaldaWithin(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20)))
        assertNull(ProgramDates.yaldaWithin(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 20)))
        assertEquals(LocalDate.of(2027, 3, 21), ProgramDates.newDay0(LocalDate.of(2027, 3, 20), LocalDate.of(2027, 3, 20)))
        assertEquals(LocalDate.of(2027, 3, 25), ProgramDates.newDay0(LocalDate.of(2027, 3, 25), LocalDate.of(2027, 3, 20)))
    }
}
