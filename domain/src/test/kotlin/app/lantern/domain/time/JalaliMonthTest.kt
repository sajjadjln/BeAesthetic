package app.lantern.domain.time

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JalaliMonthTest {

    @Test
    fun `the program's months start on the right column (Saturday = 0)`() {
        val mehr = JalaliMonth(1405, 7)
        assertEquals(LocalDate.of(2026, 9, 23), mehr.firstDay) // a Wednesday
        assertEquals(4, mehr.leadingBlanks)
        assertEquals(30, mehr.length)
        assertEquals(6, mehr.plus(1).leadingBlanks) // 1 Aban = Friday 23 Oct
        assertEquals(1, mehr.plus(2).leadingBlanks) // 1 Azar = Sunday 22 Nov
    }

    @Test
    fun `months roll over the year both ways`() {
        val esfand = JalaliMonth(1405, 12)
        assertEquals(JalaliMonth(1406, 1), esfand.plus(1))
        assertEquals(esfand, JalaliMonth(1406, 1).plus(-1))
        assertEquals(JalaliMonth(1404, 11), JalaliMonth(1405, 1).plus(-2))
        assertEquals(LocalDate.of(2027, 3, 20), esfand.lastDay) // 29 Esfand, the finish line
    }

    @Test
    fun `of and contains agree with the conversion`() {
        val today = LocalDate.of(2026, 10, 14) // 22 Mehr
        val month = JalaliMonth.of(today)
        assertEquals(JalaliMonth(1405, 7), month)
        assertEquals(today, month.date(22))
        assertTrue(today in month)
        assertFalse(LocalDate.of(2026, 10, 23) in month)
    }
}
