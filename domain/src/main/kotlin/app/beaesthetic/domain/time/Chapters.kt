package app.beaesthetic.domain.time

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A program chapter (D51): one Jalali month. A short first month is merged into the next one, so the first
 * program's chapters are ① Mehr–Aban, ② Azar, ③ Dey, ④ Bahman, ⑤ Esfand. A chapter start is a fresh start;
 * it never resets any count.
 *
 * @property months the Jalali months the chapter covers (1–12), usually one.
 */
data class Chapter(val number: Int, val start: LocalDate, val end: LocalDate, val months: List<Int>)

/** Days left in the last chapter (D74). [toNowruz] when the day after the finish is 1 Farvardin. */
data class Countdown(val daysLeft: Int, val toNowruz: Boolean) {
    val isFinishDay: Boolean get() = daysLeft == 1
}

object Chapters {

    /** A first month with fewer program days than this joins the next month's chapter. */
    const val MIN_FIRST_CHAPTER_DAYS = 15

    fun of(calendar: ProgramCalendar): List<Chapter> {
        val spans = mutableListOf<Triple<LocalDate, LocalDate, Int>>()
        var date = calendar.day0
        while (date <= calendar.endDate) {
            val j = Jalali.fromGregorian(date)
            val monthEnd = Jalali.toGregorian(j.year, j.month, Jalali.monthLength(j.year, j.month))
            val end = minOf(monthEnd, calendar.endDate)
            spans += Triple(date, end, j.month)
            date = end.plusDays(1)
        }
        val merged = mutableListOf<Pair<LocalDate, MutableList<Triple<LocalDate, LocalDate, Int>>>>()
        spans.forEachIndexed { i, span ->
            val firstIsShort = i == 1 && ChronoUnit.DAYS.between(spans[0].first, spans[0].second) + 1 < MIN_FIRST_CHAPTER_DAYS
            if (firstIsShort) merged.last().second += span else merged += span.first to mutableListOf(span)
        }
        return merged.mapIndexed { i, (start, parts) -> Chapter(i + 1, start, parts.last().second, parts.map { it.third }) }
    }

    fun at(calendar: ProgramCalendar, date: LocalDate): Chapter? = of(calendar).firstOrNull { date in it.start..it.end }

    /** The first day of chapter 2 and later. Chapter 1 starts on Day 0, before the app existed. */
    fun isChapterStart(calendar: ProgramCalendar, date: LocalDate): Boolean = of(calendar).drop(1).any { it.start == date }

    /** Null before the last chapter and after the finish. Never shown earlier (D74). */
    fun countdown(calendar: ProgramCalendar, date: LocalDate): Countdown? {
        val last = of(calendar).last()
        if (date < last.start || date > calendar.endDate) return null
        val dayAfter = calendar.endDate.plusDays(1)
        val j = Jalali.fromGregorian(dayAfter)
        return Countdown(ChronoUnit.DAYS.between(date, dayAfter).toInt(), toNowruz = j.month == 1 && j.day == 1)
    }

    /**
     * When a letter written on [writtenOn] opens (D72): the first letter after install opens on the finish day;
     * a chapter letter opens at the next chapter start, and the last chapter's on the finish day.
     */
    fun letterOpensOn(calendar: ProgramCalendar, writtenOn: LocalDate, firstLetter: Boolean): LocalDate {
        if (firstLetter) return calendar.endDate
        return of(calendar).firstOrNull { it.start > writtenOn }?.start ?: calendar.endDate
    }
}
