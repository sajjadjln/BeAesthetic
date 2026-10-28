package app.lantern.domain.reminders

import app.lantern.domain.time.Part
import app.lantern.domain.time.ProgramCalendar
import app.lantern.domain.time.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One block reminder time. [minuteOfDay] is minutes after midnight. */
data class ReminderSlot(
    val profile: Profile,
    val part: Part,
    val minuteOfDay: Int,
    val enabled: Boolean = true,
) {
    val time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    /**
     * When this slot rings for [programDate]. A time before the rollover hour (say 00:30) belongs to the
     * evening before, so it rings on the next calendar day.
     */
    fun dateTimeFor(programDate: LocalDate, rolloverHour: Int): LocalDateTime {
        val calendarDate = if (minuteOfDay < rolloverHour * 60) programDate.plusDays(1) else programDate
        return LocalDateTime.of(calendarDate, time)
    }
}

object PartOfDay {
    /**
     * The part of the day [now] falls in: Morning until the Midday time, Midday until Evening, Evening until
     * Night, Night until the rollover. A profile without an enabled slot for a part (Friday has no Evening)
     * simply stretches the part before it.
     */
    fun current(now: LocalDateTime, slots: List<ReminderSlot>, rolloverHour: Int): Part {
        val date = ProgramCalendar.programDate(now, rolloverHour)
        val profile = Profile.of(date)
        val sinceRollover = minutesSinceRollover(now.hour * 60 + now.minute, rolloverHour)
        return slots
            .filter { it.profile == profile && it.enabled && it.part != Part.MORNING }
            .filter { minutesSinceRollover(it.minuteOfDay, rolloverHour) <= sinceRollover }
            .maxByOrNull { it.part.ordinal }
            ?.part
            ?: Part.MORNING
    }

    private fun minutesSinceRollover(minuteOfDay: Int, rolloverHour: Int): Int =
        (minuteOfDay - rolloverHour * 60 + MINUTES_PER_DAY) % MINUTES_PER_DAY

    private const val MINUTES_PER_DAY = 24 * 60
}
