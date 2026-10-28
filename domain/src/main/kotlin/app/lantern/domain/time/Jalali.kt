package app.lantern.domain.time

import java.time.LocalDate

/** A date in the Iranian (Jalali, Solar Hijri) calendar. Months are 1–12, Farvardin = 1. */
data class JalaliDate(val year: Int, val month: Int, val day: Int)

/**
 * Gregorian ↔ Jalali conversion: a Kotlin port of jalaali-js (MIT, https://github.com/jalaali/jalaali-js).
 * It uses the official break-year table, so it matches the Iranian calendar, not only the 33-year cycle.
 * Valid for Jalali years -61 to 3177.
 */
object Jalali {

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    fun fromGregorian(date: LocalDate): JalaliDate {
        val epochDay = date.toEpochDay()
        val gy = date.year
        var jy = gy - 621
        val cal = jalCal(jy)
        val farvardin1 = LocalDate.of(gy, 3, cal.march).toEpochDay()
        var k = (epochDay - farvardin1).toInt()
        if (k >= 0) {
            if (k <= 185) return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (cal.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        require(jm in 1..12 && jd in 1..monthLength(jy, jm)) { "Invalid Jalali date $jy/$jm/$jd" }
        val cal = jalCal(jy)
        val farvardin1 = LocalDate.of(jy + 621, 3, cal.march)
        return farvardin1.plusDays(((jm - 1) * 31 - jm / 7 * (jm - 7) + jd - 1).toLong())
    }

    fun toGregorian(date: JalaliDate): LocalDate = toGregorian(date.year, date.month, date.day)

    fun isLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeap(jy) -> 30
        else -> 29
    }

    private class Cal(val leap: Int, val march: Int)

    /** Leap state of [jy] (0 = leap) and the day in March of the Gregorian year jy+621 on which Farvardin 1 falls. */
    private fun jalCal(jy: Int): Cal {
        require(jy >= breaks.first() && jy < breaks.last()) { "Jalali year $jy out of range" }
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += jump / 33 * 8 + jump % 33 / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += n / 33 * 8 + (n % 33 + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - (gy / 100 + 1) * 3 / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, march)
    }
}
