package app.beaesthetic.data.repo

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.withTransaction
import app.beaesthetic.R
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.ReminderSlotEntity
import app.beaesthetic.data.db.RepeatType
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.data.db.TaskEntity
import app.beaesthetic.domain.tasks.TaskAction
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Profile
import app.beaesthetic.domain.time.Weekdays
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * First-run data: the program dates, block times and the default routine (SPEC §5, D55 anchors).
 * Everything here is editable later; nothing is hard-coded in the rules (ARCHITECTURE §15).
 */
object Seed {

    val PROGRAM_START: LocalDate = LocalDate.of(2026, 10, 9) // Day 0, 17 Mehr 1405
    val PROGRAM_END: LocalDate = LocalDate.of(2027, 3, 20) // 29 Esfand 1405
    val HALFWAY: LocalDate = LocalDate.of(2026, 12, 21) // Yalda, 30 Azar

    /** Seeds once. Returns false if the database already had settings. */
    suspend fun ensure(db: AppDatabase, context: Context, installDate: LocalDate, now: Instant): Boolean =
        db.withTransaction {
            if (db.settings().get() != null) return@withTransaction false
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
            db.tasks().insertAll(tasks(context, installDate, now))
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

    private class Spec(
        @param:StringRes val title: Int,
        val icon: TaskIcon,
        val part: Part,
        val repeat: RepeatType,
        val weekdays: Int = 0,
        val everyNDays: Int? = null,
        val important: Boolean = false,
        val floor: Boolean = false,
        val action: TaskAction = TaskAction.NONE,
        @param:StringRes val anchor: Int? = null,
    )

    private fun tasks(context: Context, start: LocalDate, now: Instant): List<TaskEntity> {
        val training = Weekdays.of(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        val friday = Weekdays.of(DayOfWeek.FRIDAY)
        val specs = listOf(
            // Morning
            Spec(R.string.seed_brush, TaskIcon.TOOTH, Part.MORNING, RepeatType.DAILY, floor = true, anchor = R.string.anchor_after_breakfast),
            Spec(R.string.seed_skincare, TaskIcon.DROP, Part.MORNING, RepeatType.DAILY, important = true, anchor = R.string.anchor_after_brushing),
            Spec(R.string.seed_creatine, TaskIcon.PILL, Part.MORNING, RepeatType.DAILY, floor = true, anchor = R.string.anchor_with_breakfast),
            Spec(R.string.seed_protein, TaskIcon.EGG, Part.MORNING, RepeatType.DAILY, anchor = R.string.anchor_with_breakfast),
            Spec(R.string.seed_weigh_in, TaskIcon.SCALE, Part.MORNING, RepeatType.WEEKDAYS, weekdays = friday, action = TaskAction.WEIGH_IN, anchor = R.string.anchor_before_breakfast),
            // The Friday hike is the Friday session (SPEC §2, default until answered).
            Spec(R.string.seed_hike, TaskIcon.MOUNTAIN, Part.MORNING, RepeatType.WEEKDAYS, weekdays = friday, action = TaskAction.HIKE),
            // Midday
            Spec(R.string.seed_vitamin_d, TaskIcon.SUN, Part.MIDDAY, RepeatType.DAILY, anchor = R.string.anchor_with_lunch),
            Spec(R.string.seed_protein, TaskIcon.EGG, Part.MIDDAY, RepeatType.DAILY, anchor = R.string.anchor_with_lunch),
            // Evening
            Spec(R.string.seed_workout, TaskIcon.DUMBBELL, Part.EVENING, RepeatType.WEEKDAYS, weekdays = training, action = TaskAction.WORKOUT),
            Spec(R.string.seed_protein, TaskIcon.EGG, Part.EVENING, RepeatType.DAILY, anchor = R.string.anchor_with_dinner),
            Spec(R.string.seed_shower, TaskIcon.SHOWER, Part.EVENING, RepeatType.EVERY_N_DAYS, everyNDays = 2, anchor = R.string.anchor_after_workout),
            Spec(R.string.seed_shampoo, TaskIcon.SHOWER, Part.EVENING, RepeatType.EVERY_N_DAYS, everyNDays = 3, anchor = R.string.anchor_in_shower),
            Spec(R.string.seed_nails, TaskIcon.SCISSORS, Part.EVENING, RepeatType.EVERY_N_DAYS, everyNDays = 14, anchor = R.string.anchor_after_shower),
            // Beard and haircut intervals are still open questions (TASKS.md Q6); 7 and 30 days until answered.
            Spec(R.string.seed_beard, TaskIcon.SCISSORS, Part.EVENING, RepeatType.EVERY_N_DAYS, everyNDays = 7, anchor = R.string.anchor_before_shower),
            Spec(R.string.seed_haircut, TaskIcon.SCISSORS, Part.EVENING, RepeatType.EVERY_N_DAYS, everyNDays = 30),
            Spec(R.string.seed_min_pushups, TaskIcon.DUMBBELL, Part.EVENING, RepeatType.MIN_DAY_ONLY, floor = true),
            // Night
            Spec(R.string.seed_brush, TaskIcon.TOOTH, Part.NIGHT, RepeatType.DAILY, important = true, floor = true, anchor = R.string.anchor_before_bed),
            Spec(R.string.seed_skincare, TaskIcon.DROP, Part.NIGHT, RepeatType.DAILY, important = true, anchor = R.string.anchor_after_brushing),
            Spec(R.string.seed_snack, TaskIcon.APPLE, Part.NIGHT, RepeatType.DAILY, action = TaskAction.SNACK_NOTE, anchor = R.string.anchor_after_dinner),
        )
        return specs.mapIndexed { index, s ->
            TaskEntity(
                title = context.getString(s.title),
                icon = s.icon.name,
                part = s.part,
                repeat = s.repeat,
                weekdays = s.weekdays,
                everyNDays = s.everyNDays,
                startDate = start,
                isImportant = s.important,
                inMinimumDay = s.floor,
                action = s.action.name,
                anchor = s.anchor?.let(context::getString),
                sortOrder = index,
                createdAt = now,
            )
        }
    }
}
