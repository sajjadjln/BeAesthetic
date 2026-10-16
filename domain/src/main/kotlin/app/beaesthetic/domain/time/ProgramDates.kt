package app.beaesthetic.domain.time

import java.time.LocalDate

/** Default program dates: a fresh install (D143) and a new program after the finish (D61, D145). */
object ProgramDates {

    /** The finish: the eve of the next Nowruz at least 8 weeks after [day0]. */
    fun defaultEnd(day0: LocalDate): LocalDate {
        var year = Jalali.fromGregorian(day0).year + 1
        while (Jalali.toGregorian(year, 1, 1).minusDays(1) < day0.plusWeeks(8)) year++
        return Jalali.toGregorian(year, 1, 1).minusDays(1)
    }

    /** Yalda (30 Azar) is the halfway day when it falls inside the program, else the midpoint is used. */
    fun yaldaWithin(day0: LocalDate, end: LocalDate): LocalDate? =
        listOf(Jalali.fromGregorian(day0).year, Jalali.fromGregorian(day0).year + 1)
            .map { Jalali.toGregorian(it, 9, 30) }
            .firstOrNull { it > day0 && it < end }

    /** A new program's Day 0: today, or the day after the finish when chosen on the finish day itself. */
    fun newDay0(today: LocalDate, previousEnd: LocalDate): LocalDate = maxOf(today, previousEnd.plusDays(1))
}
