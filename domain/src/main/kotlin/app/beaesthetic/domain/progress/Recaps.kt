package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.workout.LoggedSet
import app.beaesthetic.domain.workout.WorkoutRules
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** After the finish line (D61): nothing chosen yet, keep the routines going, or a new program was started. */
enum class AfterFinish { NONE, MAINTENANCE, NEW_PROGRAM }

/** The halfway and finish recaps (D52, flows R1–R3, F1): when they open and the numbers only they need. */
object Recaps {

    const val HALFWAY = "HALFWAY"
    const val FINISH = "FINISH"

    /** The settings date (the seed sets Yalda, 30 Azar), else the program's midpoint (ARCHITECTURE §3). */
    fun halfway(setting: LocalDate?, calendar: ProgramCalendar): LocalDate =
        setting ?: calendar.day0.plusDays(ChronoUnit.DAYS.between(calendar.day0, calendar.endDate) / 2)

    /** From the halfway day on, the recap can be opened again from Progress. */
    fun available(today: LocalDate, halfway: LocalDate): Boolean = today >= halfway

    /** Today's card: from the halfway day until it's seen, and never after the finish day. */
    fun cardOpen(today: LocalDate, halfway: LocalDate, calendar: ProgramCalendar, seen: Boolean): Boolean =
        !seen && today >= halfway && today <= calendar.endDate

    /** The finish recap's card on Today: from the finish day until it's opened (D52). */
    fun finishCardOpen(today: LocalDate, calendar: ProgramCalendar, seen: Boolean): Boolean = !seen && today >= calendar.endDate

    /**
     * «بعدش چی؟» (D61): once the finish recap was seen and nothing was chosen, from the finish day on, Today
     * offers the choice until it's made.
     */
    fun choiceOpen(today: LocalDate, calendar: ProgramCalendar, finishSeen: Boolean, mode: AfterFinish): Boolean =
        finishSeen && mode == AfterFinish.NONE && today >= calendar.endDate

    /**
     * A new program reuses check-in numbers 0…n. The old program's check-ins and photos move up by this
     * offset, so they stay stored (and in backups) as history without clashing.
     */
    fun archiveOffset(maxIdx: Int?): Int = if (maxIdx == null) 0 else (maxIdx / ARCHIVE_STEP + 1) * ARCHIVE_STEP

    const val ARCHIVE_STEP = 100

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
