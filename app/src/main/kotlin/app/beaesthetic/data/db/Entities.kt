package app.beaesthetic.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Profile
import java.time.Instant
import java.time.LocalDate

// Physical schema: ARCHITECTURE §8. Dates are ISO strings, times are minutes after midnight,
// instants are epoch milliseconds (see Converters). Tables for later slices are added by migrations.

enum class RepeatType { DAILY, WEEKDAYS, EVERY_N_DAYS, ONCE, MIN_DAY_ONLY, ANYTIME }

@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val icon: String,
    val part: Part,
    val repeat: RepeatType,
    val weekdays: Int = 0,
    val everyNDays: Int? = null,
    val onceDate: LocalDate? = null,
    val startDate: LocalDate,
    val isImportant: Boolean = false,
    val inMinimumDay: Boolean = false,
    val action: String = "NONE",
    val alarmTime: Int? = null,
    val alarmWeekdays: Int = 0,
    val alarmEnabled: Boolean = false,
    val anchor: String? = null,
    val remindersSilencedAt: Instant? = null,
    val fadeOfferedAt: Instant? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val archivedAt: Instant? = null,
)

@Entity(tableName = "completion", primaryKeys = ["taskId", "date"], indices = [Index("date")])
data class CompletionEntity(
    val taskId: Long,
    val date: LocalDate,
    val completedAt: Instant,
)

/** The day snapshot (D37): due/done counts frozen once the day leaves the edit window. */
@Entity(tableName = "day")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val special: String? = null,
    val minimumMode: Boolean = false,
    val dueCount: Int = 0,
    val doneCount: Int = 0,
    val floorDone: Boolean = false,
)

@Entity(tableName = "reminder_slot", primaryKeys = ["profile", "part"])
data class ReminderSlotEntity(
    val profile: Profile,
    val part: Part,
    val time: Int,
    val enabled: Boolean,
)

object ReminderOutcome {
    const val SCHEDULED = "SCHEDULED"
    const val SHOWN = "SHOWN"
    const val SKIPPED_EMPTY = "SKIPPED_EMPTY"
    /** Fired after a later part of the day had started; never shown, so a block never repeats (no catch-up). */
    const val SKIPPED_LATE = "SKIPPED_LATE"
    /** Fired, but the app's notifications are turned off. */
    const val NOTIFICATIONS_OFF = "NOTIFICATIONS_OFF"
    const val CANCELLED = "CANCELLED"
    const val DEVICE_OFF = "DEVICE_OFF"
}

@Entity(tableName = "reminder_log", indices = [Index("key"), Index("scheduledAt")])
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val kind: String,
    val scheduledAt: Instant,
    val firedAt: Instant? = null,
    val outcome: String,
)

/** The single settings row (D38). */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val programStartDate: LocalDate,
    val programEndDate: LocalDate,
    val installDate: LocalDate,
    val halfwayDate: LocalDate? = null,
    val rolloverHour: Int = 4,
    val specialCapPerWeek: Int = 2,
    val followUpMinutes: Int = 45,
    val ritualTime: Int = 19 * 60,
    /** ALARM_CLOCK (setAlarmClock, default) or EXACT_IDLE (setExactAndAllowWhileIdle), ARCHITECTURE §7.2. */
    val alarmMode: String = "ALARM_CLOCK",
    val accentColor: String = "GREEN",
    val themeMode: String = "SYSTEM",
    val showBuddy: Boolean = true,
    val backupTreeUri: String? = null,
    val lastOpenedAt: Instant? = null,
    val lastBackupAt: Instant? = null,
    val onboardingDone: Boolean = false,
    val identitySentence: String? = null,
    val afterFinishMode: String = "NONE",
    val experimentFlags: Long = 0,
    // v2 (slice 2)
    /** The first letter (D72) is offered once after install; set when written or put off. */
    @ColumnInfo(defaultValue = "0") val firstLetterOfferDone: Boolean = false,
    /** «بعداً» on the chapter-start card hides it for that day. */
    val cardDismissedOn: LocalDate? = null,
    /** Reminder problems before this were seen on the phone screen; the Today banner ignores them. */
    val healthSeenAt: Instant? = null,
    /** The watchdog posts at most one "reminders may be blocked" notification a day. */
    val healthNotifiedOn: LocalDate? = null,
    /** Xiaomi settings no API can read: the user confirms them with a tick. */
    @ColumnInfo(defaultValue = "0") val autostartConfirmed: Boolean = false,
    @ColumnInfo(defaultValue = "0") val recentsLockConfirmed: Boolean = false,
    // v5 (slice 5b)
    /** Today's «هر وقت» card is open (D127); remembered as the user left it. */
    @ColumnInfo(defaultValue = "0") val anytimeOpen: Boolean = false,
    // v6 (slice 6)
    /** The automatic backup (D133); on by default. Kept on this phone through a restore. */
    @ColumnInfo(defaultValue = "1") val autoBackup: Boolean = true,
)

object EventType {
    /** Which character line was shown (ref = "MOMENT:index"), so a line doesn't come back within 7 days (D84). */
    const val BUDDY_LINE = "BUDDY_LINE"
    const val APP_OPEN = "APP_OPEN"
    const val SCREEN = "SCREEN"
    const val NOTIF_SHOWN = "NOTIF_SHOWN"
    const val NOTIF_TAP = "NOTIF_TAP"
    const val FIRST_TICK = "FIRST_TICK"
}

/** The local usage log (D59). Never leaves the phone except inside the user's own backup. */
@Entity(tableName = "event", indices = [Index("at"), Index("type")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Instant,
    val type: String,
    val ref: String? = null,
    val valueMinutes: Int? = null,
)

/**
 * A sealed letter to the future self (D72). The UI never shows [text] before [opensOn]. Included in backups.
 */
@Entity(tableName = "letter")
data class LetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val writtenOn: LocalDate,
    val writtenAt: Instant,
    val opensOn: LocalDate,
    val text: String,
    val openedAt: Instant? = null,
    val isFirst: Boolean = false,
)

// v3 (slice 4): the workout plan and logged sets (ARCHITECTURE §8, D103–D105, D114).

/**
 * A move, shared by name across the week (D114). Ladders are chains of [harderId] links (D119, v4); the v3
 * columns `ladderId` and `rung` stay empty.
 */
@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** REPS or SECONDS. */
    val kind: String,
    val ladderId: Long? = null,
    val rung: Int? = null,
    val archivedAt: Instant? = null,
    /** «حرکت سخت‌تر»: the next harder variant (v4). */
    val harderId: Long? = null,
)

/** One per weekday (Saturday = 0). A weekday without rows in `workout_exercise` is a rest day. */
@Entity(tableName = "workout", indices = [Index(value = ["weekday"], unique = true)])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekday: Int,
    val name: String,
)

/** A plan row (D105). No range = «حداکثر». `kg` is the default load (D103). */
@Entity(tableName = "workout_exercise", primaryKeys = ["workoutId", "position"], indices = [Index("exerciseId")])
data class WorkoutExerciseEntity(
    val workoutId: Long,
    val position: Int,
    val exerciseId: Long,
    val sets: Int,
    val rangeMin: Int? = null,
    val rangeMax: Int? = null,
    val kg: Double? = null,
    val perSide: Boolean = false,
    val targetEffort: Int? = null,
    val restSeconds: Int? = null,
    val note: String? = null,
)

/** A workout done on a program date. Created with its first set; opening it again the same day continues it (D115). */
@Entity(tableName = "session", indices = [Index(value = ["workoutId", "date"]), Index("date")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
)

/** A logged set (D27, D103). Logging the same set again replaces it. */
@Entity(tableName = "set_log", primaryKeys = ["sessionId", "exerciseId", "setIndex"], indices = [Index("exerciseId")])
data class SetLogEntity(
    val sessionId: Long,
    val exerciseId: Long,
    val setIndex: Int,
    val value: Int,
    val kg: Double? = null,
    val effort: Int,
    val loggedAt: Instant,
)

/** A set with its session's date, for history and records. */
data class SetWithDate(
    val sessionId: Long,
    val exerciseId: Long,
    val setIndex: Int,
    val value: Int,
    val kg: Double?,
    val effort: Int,
    val loggedAt: Instant,
    val date: LocalDate,
)

/**
 * A round number crossed (D71, D121), e.g. `reps:ladder:3:1000`, `workouts:50`, `days:100`. Written once,
 * when first crossed, so editing a log never takes it back. `seenAt` = the Today card was dismissed (v4).
 */
@Entity(tableName = "round_number")
data class RoundNumberEntity(
    @PrimaryKey val key: String,
    val reachedOn: LocalDate,
    val seenAt: Instant? = null,
)

/**
 * One backup try (slice 6, v6). Kept on this phone only: a restore never replaces it (D132). [reason] is a
 * BackupReason name; [error] is a short technical note when [ok] is false.
 */
@Entity(tableName = "backup_record", indices = [Index("at")])
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Instant,
    val fileName: String,
    val sizeBytes: Long,
    val photoCount: Int,
    val reason: String,
    val ok: Boolean,
    val error: String? = null,
)

// v7 (slice 7a): progress (ARCHITECTURE §8, D100, D106, D73).

/**
 * Weight, a tape site or a strength test. Added by the user; the suggested ones carry a `Suggestions` key
 * (D100). Removing one archives it, so a backup still restores its readings.
 */
@Entity(tableName = "metric")
data class MetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** BODY, TAPE or STRENGTH. */
    val kind: String,
    val key: String? = null,
    val name: String,
    /** KG, CM, REPS or SECONDS. */
    val unit: String,
    val higherIsBetter: Boolean,
    val sortOrder: Int = 0,
    val archivedAt: Instant? = null,
)

/** One reading a day per metric: entering the same day again replaces it. `guess` = the test guess (D73). */
@Entity(tableName = "metric_entry", indices = [Index(value = ["metricId", "date"], unique = true)])
data class MetricEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricId: Long,
    val date: LocalDate,
    val value: Double,
    val guess: Double? = null,
    val note: String? = null,
)

/** A finished monthly check-in (D28): idx 0 = Day 0 … 6 = the finish line. [date] = the day it was done for. */
@Entity(tableName = "check_in")
data class CheckInEntity(
    @PrimaryKey val idx: Int,
    val date: LocalDate,
    val doneAt: Instant? = null,
)

/** A check-in photo (ARCHITECTURE §10): `files/photos/<idx>-<pose>.jpg`. Taking it again replaces it. */
@Entity(tableName = "photo", primaryKeys = ["checkInIdx", "pose"])
data class PhotoEntity(
    val checkInIdx: Int,
    /** FRONT, SIDE or BACK. */
    val pose: String,
    val fileName: String,
    val takenAt: Instant,
)
