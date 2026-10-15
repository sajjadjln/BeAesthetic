package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.JalaliDate
import app.beaesthetic.domain.time.ProgramCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** A monthly check-in (D28): index 0 is Day 0, the last one is the finish line. */
data class CheckIn(val idx: Int, val date: LocalDate) {
    val isDay0: Boolean get() = idx == 0
}

/**
 * The check-in schedule (D101): Day 0, then the Friday closest to the 17th of each Jalali month inside the
 * program, then the finish line. For the first program: 9 Oct, 6 Nov, 11 Dec, … 5 Mar, 20 Mar (7 in all).
 */
object CheckIns {

    /** A check-in can still be done this many days after its date (a busy Friday moves to Saturday). */
    const val WINDOW_DAYS = 7

    fun schedule(calendar: ProgramCalendar): List<CheckIn> {
        val months = mutableListOf<LocalDate>()
        var jalali = Jalali.fromGregorian(calendar.day0).let { JalaliDate(it.year, it.month, 1) }
        while (true) {
            val date = fridayNear17(jalali.year, jalali.month)
            if (date >= calendar.endDate) break
            if (date > calendar.day0) months += date
            jalali = if (jalali.month == 12) JalaliDate(jalali.year + 1, 1, 1) else JalaliDate(jalali.year, jalali.month + 1, 1)
        }
        val dates = listOf(calendar.day0) + months + calendar.endDate
        return dates.mapIndexed { i, date -> CheckIn(i, date) }
    }

    /** The Friday closest to the 17th of a Jalali month. The 17th is never exactly between two Fridays. */
    fun fridayNear17(jy: Int, jm: Int): LocalDate {
        val the17th = Jalali.toGregorian(jy, jm, 17)
        return (-3L..3L).map { the17th.plusDays(it) }.first { it.dayOfWeek == DayOfWeek.FRIDAY }
    }

    /**
     * The check-in Today offers on [today]: on its date or in the days after, until it's done, the window
     * ends or the next one starts. Day 0 is never offered on Today; Progress asks for it instead.
     */
    fun open(today: LocalDate, schedule: List<CheckIn>, done: Set<Int>): CheckIn? =
        schedule.withIndex().lastOrNull { (_, c) -> c.date <= today }?.let { (i, c) ->
            val next = schedule.getOrNull(i + 1)?.date
            val closes = minOf(c.date.plusDays(WINDOW_DAYS.toLong()), next ?: LocalDate.MAX)
            c.takeIf { !it.isDay0 && it.idx !in done && today < closes }
        }

    /** The next check-in not done yet, from [today] on: open now, or the first one ahead. */
    fun next(today: LocalDate, schedule: List<CheckIn>, done: Set<Int>): CheckIn? =
        open(today, schedule, done) ?: schedule.firstOrNull { it.date > today && !it.isDay0 }

    fun daysUntil(today: LocalDate, checkIn: CheckIn): Int = ChronoUnit.DAYS.between(today, checkIn.date).toInt()

    /** The check-in a photo set or a reading on [date] belongs to: the nearest one by date. */
    fun nearest(date: LocalDate, schedule: List<CheckIn>): CheckIn =
        schedule.minBy { abs(ChronoUnit.DAYS.between(it.date, date)) }
}
