package app.lantern.domain.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JalaliTest {

    @ParameterizedTest(name = "{0} = {1}/{2}/{3}")
    @CsvSource(
        "2026-10-09, 1405, 7, 17", // Day 0
        "2026-10-10, 1405, 7, 18", // week 1 starts
        "2026-11-22, 1405, 9, 1", // chapter Azar
        "2026-12-21, 1405, 9, 30", // Yalda
        "2027-03-19, 1405, 12, 28",
        "2027-03-20, 1405, 12, 29", // finish line
        "2027-03-21, 1406, 1, 1", // Nowruz 1406
    )
    fun `program anchor dates`(iso: String, jy: Int, jm: Int, jd: Int) {
        val date = LocalDate.parse(iso)
        assertEquals(JalaliDate(jy, jm, jd), Jalali.fromGregorian(date))
        assertEquals(date, Jalali.toGregorian(jy, jm, jd))
    }

    @ParameterizedTest(name = "Nowruz {0} = {1}")
    @CsvSource(
        "1400, 2021-03-21",
        "1401, 2022-03-21",
        "1402, 2023-03-21",
        "1403, 2024-03-20",
        "1404, 2025-03-21",
        "1405, 2026-03-21",
        "1406, 2027-03-21",
        "1407, 2028-03-20",
        "1408, 2029-03-20",
        "1409, 2030-03-21",
        "1410, 2031-03-21",
    )
    fun `nowruz dates`(jy: Int, iso: String) {
        assertEquals(LocalDate.parse(iso), Jalali.toGregorian(jy, 1, 1))
    }

    @Test
    fun `leap years`() {
        assertTrue(Jalali.isLeap(1403))
        assertFalse(Jalali.isLeap(1404))
        assertFalse(Jalali.isLeap(1405))
        assertTrue(Jalali.isLeap(1408))
        assertEquals(29, Jalali.monthLength(1405, 12))
        assertEquals(30, Jalali.monthLength(1403, 12))
    }

    @Test
    fun `round trip every day from 1395 to 1415`() {
        var date = Jalali.toGregorian(1395, 1, 1)
        val end = Jalali.toGregorian(1416, 1, 1)
        var previous: JalaliDate? = null
        while (date < end) {
            val j = Jalali.fromGregorian(date)
            assertEquals(date, Jalali.toGregorian(j), "round trip of $date")
            assertTrue(j.day in 1..Jalali.monthLength(j.year, j.month), "valid day $j")
            previous?.let { p ->
                val consecutive = (j.year == p.year && j.month == p.month && j.day == p.day + 1) ||
                    (j.year == p.year && j.month == p.month + 1 && j.day == 1 && p.day == Jalali.monthLength(p.year, p.month)) ||
                    (j.year == p.year + 1 && j.month == 1 && j.day == 1 && p.month == 12)
                assertTrue(consecutive, "$p → $j")
            }
            previous = j
            date = date.plusDays(1)
        }
    }
}
