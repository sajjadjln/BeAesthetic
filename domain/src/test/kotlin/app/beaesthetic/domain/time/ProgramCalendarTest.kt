package app.beaesthetic.domain.time

import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgramCalendarTest {

    private val calendar = ProgramCalendar(
        day0 = LocalDate.of(2026, 10, 9),
        endDate = LocalDate.of(2027, 3, 20),
    )

    @Test
    fun `day rolls over at 04 00`() {
        assertEquals(LocalDate.of(2026, 10, 10), calendar.programDate(LocalDateTime.of(2026, 10, 11, 0, 30)))
        assertEquals(LocalDate.of(2026, 10, 10), calendar.programDate(LocalDateTime.of(2026, 10, 11, 3, 59)))
        assertEquals(LocalDate.of(2026, 10, 11), calendar.programDate(LocalDateTime.of(2026, 10, 11, 4, 0)))
        assertEquals(LocalDate.of(2026, 10, 11), calendar.programDate(LocalDateTime.of(2026, 10, 11, 23, 59)))
    }

    @Test
    fun `week numbers`() {
        assertEquals(23, calendar.totalWeeks)
        assertEquals(LocalDate.of(2026, 10, 10), calendar.week1Start)
        assertEquals(0, calendar.weekNumber(LocalDate.of(2026, 10, 9)))
        assertEquals(1, calendar.weekNumber(LocalDate.of(2026, 10, 10)))
        assertEquals(1, calendar.weekNumber(LocalDate.of(2026, 10, 16)))
        assertEquals(2, calendar.weekNumber(LocalDate.of(2026, 10, 17)))
        // Week 5 = 16–22 Aban = 7–13 Nov (D86's fix).
        assertEquals(5, calendar.weekNumber(LocalDate.of(2026, 11, 7)))
        assertEquals(5, calendar.weekNumber(LocalDate.of(2026, 11, 13)))
        assertEquals(11, calendar.weekNumber(LocalDate.of(2026, 12, 21))) // Yalda
        assertEquals(23, calendar.weekNumber(LocalDate.of(2027, 3, 19)))
        assertEquals(23, calendar.weekNumber(LocalDate.of(2027, 3, 20))) // finish line
    }

    @Test
    fun `week starts on saturday`() {
        assertEquals(LocalDate.of(2026, 10, 10), ProgramCalendar.weekStart(LocalDate.of(2026, 10, 16)))
        assertEquals(LocalDate.of(2026, 10, 10), ProgramCalendar.weekStart(LocalDate.of(2026, 10, 10)))
        assertEquals(LocalDate.of(2026, 10, 3), ProgramCalendar.weekStart(LocalDate.of(2026, 10, 9)))
    }

    @Test
    fun `weekday bits start at saturday`() {
        assertEquals(0, Weekdays.index(DayOfWeek.SATURDAY))
        assertEquals(6, Weekdays.index(DayOfWeek.FRIDAY))
        val sms = Weekdays.of(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        assertEquals(0b0010101, sms)
        assertTrue(Weekdays.contains(sms, DayOfWeek.MONDAY))
        assertFalse(Weekdays.contains(sms, DayOfWeek.SUNDAY))
    }

    @Test
    fun `friday uses its own profile`() {
        assertEquals(Profile.FRIDAY, Profile.of(LocalDate.of(2026, 10, 16)))
        assertEquals(Profile.NORMAL, Profile.of(LocalDate.of(2026, 10, 17)))
    }
}
