package app.lantern.domain.time

import java.time.LocalDate

/**
 * One Jalali month as the calendar grid draws it (D129): Saturday is column 0, so [leadingBlanks] empty
 * cells come before day 1.
 */
data class JalaliMonth(val year: Int, val month: Int) {
    init { require(month in 1..12) }

    val length: Int get() = Jalali.monthLength(year, month)

    val firstDay: LocalDate get() = Jalali.toGregorian(year, month, 1)

    val lastDay: LocalDate get() = Jalali.toGregorian(year, month, length)

    val leadingBlanks: Int get() = Weekdays.index(firstDay.dayOfWeek)

    fun date(day: Int): LocalDate = Jalali.toGregorian(year, month, day)

    operator fun contains(date: LocalDate): Boolean = date in firstDay..lastDay

    fun plus(months: Int): JalaliMonth {
        val index = year * 12 + (month - 1) + months
        return JalaliMonth(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1)
    }

    companion object {
        fun of(date: LocalDate): JalaliMonth = Jalali.fromGregorian(date).let { JalaliMonth(it.year, it.month) }
    }
}
