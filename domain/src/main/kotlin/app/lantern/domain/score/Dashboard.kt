package app.lantern.domain.score

import app.lantern.domain.time.ProgramCalendar
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Why a day was special (SPEC §6.2). Stored as the name in `day.special`. */
enum class SpecialKind { SICK, TRAVEL, OFF }

/** One week row of «هفته به هفته»: its 7 statuses and score. [score] is null while the week is still running. */
data class WeekRow(val start: LocalDate, val number: Int, val days: List<DayStatus?>, val score: Int?, val current: Boolean)

/** The dashboard's top line: «۲۷ از ۲۸ روز حضور» and «۲ روز ویژه». */
data class Summary(val shownUp: Int, val counted: Int, val special: Int)

/**
 * The dashboard (screens.html 02, M6). Columns are the program's weeks, Saturday on top. Day 0 and the finish
 * day can fall in a week of their own (Day 0 is the Friday before week 1), so the grid shows every week the
 * program touches; days outside the program are null and not drawn.
 */
object Dashboard {

    /** The Saturday of every week from Day 0's to the finish day's, oldest first. */
    fun columns(calendar: ProgramCalendar): List<LocalDate> {
        val first = ProgramCalendar.weekStart(calendar.day0)
        val last = ProgramCalendar.weekStart(calendar.endDate)
        val weeks = ChronoUnit.WEEKS.between(first, last).toInt()
        return (0..weeks).map { first.plusWeeks(it.toLong()) }
    }

    /** A day's square: null outside the program, else its status (before the app, a level, special, future). */
    fun cell(date: LocalDate, calendar: ProgramCalendar, today: LocalDate, installDate: LocalDate, snapshots: Map<LocalDate, DaySnapshot>): DayStatus? =
        if (date < calendar.day0 || date > calendar.endDate) null else Days.status(date, today, installDate, snapshots[date])

    /**
     * The week's score (SPEC §6.2): the average done share over its past, non-special days the app saw, as a
     * whole percent. Days before the app don't count. Null when no day counts or the week isn't over.
     */
    fun weekScore(weekStart: LocalDate, today: LocalDate, installDate: LocalDate, snapshots: Map<LocalDate, DaySnapshot>): Int? {
        if (weekStart.plusDays(6) >= today) return null
        val pcts = (0L..6L).map { weekStart.plusDays(it) }
            .filter { it >= installDate }
            .mapNotNull { d ->
                val s = snapshots[d]
                when {
                    s == null -> 0.0 // a day the app never saw is a gap
                    s.special -> null
                    else -> s.counts.pct
                }
            }
        if (pcts.isEmpty()) return null
        return Math.round(pcts.average() * 100).toInt()
    }

    /** Week rows, newest on top; only weeks that have started. Week 0 is Day 0's week. */
    fun weeks(calendar: ProgramCalendar, today: LocalDate, installDate: LocalDate, snapshots: Map<LocalDate, DaySnapshot>): List<WeekRow> =
        columns(calendar).filter { it <= today }.mapIndexed { i, start ->
            WeekRow(
                start = start,
                number = i,
                days = (0L..6L).map { cell(start.plusDays(it), calendar, today, installDate, snapshots) },
                score = weekScore(start, today, installDate, snapshots),
                current = !start.plusDays(6).isBefore(today),
            )
        }.reversed()

    /**
     * Days shown up out of the days that count: from the install day to today, special days left out (they
     * are neither shown up nor missed).
     */
    fun summary(today: LocalDate, installDate: LocalDate, calendar: ProgramCalendar, snapshots: Map<LocalDate, DaySnapshot>): Summary {
        val from = maxOf(installDate, calendar.day0)
        if (today < from) return Summary(0, 0, 0)
        val days = (0..ChronoUnit.DAYS.between(from, today)).map { from.plusDays(it) }
        val statuses = days.map { Days.status(it, today, installDate, snapshots[it]) }
        return Summary(
            shownUp = statuses.count { it is DayStatus.Level && it.level >= 1 },
            counted = statuses.count { it is DayStatus.Level },
            special = statuses.count { it == DayStatus.Special },
        )
    }

    /** «روز ۳۱ از ۱۶۲»: today's day number (Day 0 = 0) and the finish day's. */
    fun dayOf(calendar: ProgramCalendar, today: LocalDate): Pair<Int, Int> =
        ChronoUnit.DAYS.between(calendar.day0, today).toInt().coerceIn(0, total(calendar)) to total(calendar)

    private fun total(calendar: ProgramCalendar) = ChronoUnit.DAYS.between(calendar.day0, calendar.endDate).toInt()

    /** A special day can be set only within the week's cap (2 by default); clearing or changing one is always fine. */
    fun canMarkSpecial(date: LocalDate, snapshots: Map<LocalDate, DaySnapshot>, cap: Int): Boolean {
        if (snapshots[date]?.special == true) return true
        val start = ProgramCalendar.weekStart(date)
        val used = (0L..6L).count { snapshots[start.plusDays(it)]?.special == true }
        return used < cap
    }
}
