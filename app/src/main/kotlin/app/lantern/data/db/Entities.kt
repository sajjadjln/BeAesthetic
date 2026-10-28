
package app.lantern.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.lantern.domain.time.Part
import app.lantern.domain.time.Profile
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

// v3 (slice 4): the workout plan and logged sets (ARCHITECTURE §8, D103–D105, D114).

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
