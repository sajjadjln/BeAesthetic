package app.beaesthetic.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeedTest {

    @Test
    fun `a fresh install runs to the next Nowruz eve at least 8 weeks away`() {
        assertEquals(LocalDate.of(2027, 3, 20), Seed.defaultEnd(LocalDate.of(2026, 10, 16)))
        // Too close to Nowruz 1406: the next one.
        assertEquals(LocalDate.of(2028, 3, 19), Seed.defaultEnd(LocalDate.of(2027, 2, 1)))
    }

    @Test
    fun `Yalda is the halfway day only when it falls inside the program`() {
        assertEquals(LocalDate.of(2026, 12, 21), Seed.yaldaWithin(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20)))
        assertNull(Seed.yaldaWithin(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 20)))
    }
}
