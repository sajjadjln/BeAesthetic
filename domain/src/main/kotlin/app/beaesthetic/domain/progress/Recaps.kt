package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.workout.LoggedSet
import app.beaesthetic.domain.workout.WorkoutRules
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The halfway recap (D52, flows R1–R3): when it opens and the numbers only it needs. */
object Recaps {

    const val HALFWAY = "HALFWAY"

    /** The settings date (the seed sets Yalda, 30 Azar), else the program's midpoint (ARCHITECTURE §3). */
    fun halfway(setting: LocalDate?, calendar: ProgramCalendar): LocalDate =
        setting ?: calendar.day0.plusDays(ChronoUnit.DAYS.between(calendar.day0, calendar.endDate) / 2)

    /** From the halfway day on, the recap can be opened again from Progress. */
    fun available(today: LocalDate, halfway: LocalDate): Boolean = today >= halfway

    /** Today's card: from the halfway day until it's seen, and never after the finish day. */
    fun cardOpen(today: LocalDate, halfway: LocalDate, calendar: ProgramCalendar, seen: Boolean): Boolean =
        !seen && today >= halfway && today <= calendar.endDate

    /** «رکورد» moments so far (D54): each set that beat that move's best, counted once per move and day. */
    fun records(sets: List<LoggedSet>, upTo: LocalDate): Int {
        val byDate = sets.filter { it.date <= upTo }.groupBy { it.date }.toSortedMap()
        val before = mutableListOf<LoggedSet>()
        var count = 0
        for ((_, day) in byDate) {
            count += WorkoutRules.records(day, before).map { it.first }.distinct().size
            before += day
        }
        return count
    }
}
