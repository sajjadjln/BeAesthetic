package app.beaesthetic.data.repo

import androidx.room.withTransaction
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.ReminderSlotEntity
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Profile
import java.time.Instant
import java.time.LocalDate

/**
 * First-run data: the program dates (editable in onboarding) and the block times. No tasks: the app starts
 * empty (D100, D142). Nothing is hard-coded in the rules (ARCHITECTURE §15).
 */
object Seed {

    /**
     * Default dates for a fresh install, edited in onboarding: Day 0 is the install day, the finish is the eve
     * of the next Nowruz at least 8 weeks away. Nothing personal is hard-coded (D49, D143).
     */
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

    /** Seeds once. Returns false if the database already had settings. */
    suspend fun ensure(db: AppDatabase, installDate: LocalDate, now: Instant): Boolean =
        db.withTransaction {
            db.settings().get()?.let { existing ->
                // An install from before slice 9 is already set up: it never sees onboarding (D142).
                if (!existing.onboardingDone && db.tasks().active().isNotEmpty()) db.settings().upsert(existing.copy(onboardingDone = true))
                return@withTransaction false
            }
            db.settings().upsert(
                SettingsEntity(
                    programStartDate = installDate,
                    programEndDate = defaultEnd(installDate),
                    installDate = installDate,
                    halfwayDate = yaldaWithin(installDate, defaultEnd(installDate)),
                    lastOpenedAt = now,
                ),
            )
            db.slots().upsertAll(slots())
            true
        }

    private fun minutes(h: Int, m: Int = 0) = h * 60 + m

    /**
     * Block times (SPEC §7.1). Wake 06:45; training in the evening; bedtime 23:30, so the Night block is
     * half an hour before it (flows.html O3). Friday has no Evening block: it folds into the ritual.
     */
    fun slots(): List<ReminderSlotEntity> = Profile.entries.flatMap { profile ->
        listOf(
            ReminderSlotEntity(profile, Part.MORNING, minutes(7), true),
            ReminderSlotEntity(profile, Part.MIDDAY, minutes(13, 30), true),
            ReminderSlotEntity(profile, Part.EVENING, minutes(19), profile == Profile.NORMAL),
            ReminderSlotEntity(profile, Part.NIGHT, minutes(23), true),
        )
    }
}
