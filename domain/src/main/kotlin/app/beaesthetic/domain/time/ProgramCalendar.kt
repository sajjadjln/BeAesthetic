package app.beaesthetic.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** The four parts of a day, in time order. */
enum class Part { MORNING, MIDDAY, EVENING, NIGHT }

/** Reminder profile: Friday has its own block times. */
enum class Profile {
    NORMAL, FRIDAY;

    companion object {
        fun of(date: LocalDate): Profile = if (date.dayOfWeek == DayOfWeek.FRIDAY) FRIDAY else NORMAL
    }
}

/** 7-bit weekday sets with Saturday = bit 0, matching the UI order (the week starts on Saturday). */
object Weekdays {
    const val ALL = 0b111_1111

    fun bit(day: DayOfWeek): Int = 1 shl index(day)

    /** Saturday = 0 … Friday = 6. */
    fun index(day: DayOfWeek): Int = (day.value + 1) % 7

    fun contains(bits: Int, day: DayOfWeek): Boolean = bits and bit(day) != 0

    fun of(vararg days: DayOfWeek): Int = days.fold(0) { acc, d -> acc or bit(d) }
}

/**
 * Date rules of one program. Day 0 is the baseline day; week 1 starts on the first Saturday after it.
 * Everything comes from settings, so a new program gets its own weeks.
 */
data class ProgramCalendar(
    val day0: LocalDate,
    val endDate: LocalDate,
    val rolloverHour: Int = DEFAULT_ROLLOVER_HOUR,
) {
    val week1Start: LocalDate = day0.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    /** Number of full program weeks (23 for the first program). */
    val totalWeeks: Int = ((ChronoUnit.DAYS.between(week1Start, endDate) + 6) / 7).toInt()

    /** The program date of a wall-clock time: the day rolls over at [rolloverHour], not at midnight. */
    fun programDate(now: LocalDateTime): LocalDate = programDate(now, rolloverHour)

    /** 0 for Day 0 and before, then 1…[totalWeeks]. The finish line belongs to the last week. */
    fun weekNumber(date: LocalDate): Int {
        if (date < week1Start) return 0
        val week = (ChronoUnit.DAYS.between(week1Start, date) / 7 + 1).toInt()
        return week.coerceAtMost(totalWeeks)
    }

    companion object {
        const val DEFAULT_ROLLOVER_HOUR = 4

        fun programDate(now: LocalDateTime, rolloverHour: Int): LocalDate =
            now.minusHours(rolloverHour.toLong()).toLocalDate()

        /** The Saturday that starts the week containing [date]. */
        fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
    }
}
