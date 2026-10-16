package app.beaesthetic.data.repo

import androidx.room.withTransaction
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.ReminderSlotEntity
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Profile
import java.time.Instant
import java.time.LocalDate

/**
 * First-run data: the program dates (editable in onboarding) and the block times. No tasks: the app starts
 * empty (D100, D142). Nothing is hard-coded in the rules (ARCHITECTURE §15).
 */
object Seed {

    val PROGRAM_START: LocalDate = LocalDate.of(2026, 10, 9) // Day 0, 17 Mehr 1405
    val PROGRAM_END: LocalDate = LocalDate.of(2027, 3, 20) // 29 Esfand 1405
    val HALFWAY: LocalDate = LocalDate.of(2026, 12, 21) // Yalda, 30 Azar

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
                    programStartDate = PROGRAM_START,
                    programEndDate = PROGRAM_END,
                    installDate = installDate,
                    halfwayDate = HALFWAY,
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
