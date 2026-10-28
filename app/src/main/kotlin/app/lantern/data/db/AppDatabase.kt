
package app.lantern.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task WHERE archivedAt IS NULL ORDER BY part, sortOrder, id")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE archivedAt IS NULL ORDER BY part, sortOrder, id")
    suspend fun active(): List<TaskEntity>

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @androidx.room.Update
    suspend fun update(task: TaskEntity)

    /** Deleting archives: the row stays so history keeps the task's name (ARCHITECTURE §8). */
    @Query("UPDATE task SET archivedAt = :at WHERE id = :id")
    suspend fun archive(id: Long, at: Instant)

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    /** D57: [silencedAt] null turns the task's block reminders back on; [offeredAt] records the answer. */
    @Query("UPDATE task SET remindersSilencedAt = :silencedAt, fadeOfferedAt = :offeredAt WHERE id = :id")
    suspend fun setFade(id: Long, silencedAt: Instant?, offeredAt: Instant?)

    @Query("UPDATE task SET fadeOfferedAt = :at WHERE id = :id")
    suspend fun setFadeOffered(id: Long, at: Instant)

    @Query("UPDATE task SET alarmEnabled = :enabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean)
}

@Dao
interface CompletionDao {
    /** Every completion up to [date]. Small: at most ~20 tasks × 160 days. Needed for "every N days" tasks. */
    @Query("SELECT * FROM completion WHERE date <= :date")
    fun observeUpTo(date: LocalDate): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completion WHERE date <= :date")
    suspend fun upTo(date: LocalDate): List<CompletionEntity>

    @Query("SELECT * FROM completion")
    fun observeAll(): Flow<List<CompletionEntity>>

    /** The tasks done on [date], archived ones too, so a past day keeps its names (the day sheet, D1). */
    @Query("SELECT task.* FROM task JOIN completion ON completion.taskId = task.id WHERE completion.date = :date ORDER BY task.part, task.sortOrder, task.id")
    fun observeDoneOn(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM completion WHERE taskId = :taskId AND date = :date")
    suspend fun count(taskId: Long, date: LocalDate): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity)

    @Query("DELETE FROM completion WHERE taskId = :taskId AND date = :date")
    suspend fun delete(taskId: Long, date: LocalDate)
}

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE date = :date")
    suspend fun get(date: LocalDate): DayEntity?

    @Query("SELECT * FROM day WHERE date = :date")
    fun observe(date: LocalDate): Flow<DayEntity?>

    @Query("SELECT * FROM day WHERE date BETWEEN :from AND :to")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayEntity>>

    @Query("UPDATE day SET special = :special WHERE date = :date")
    suspend fun setSpecial(date: LocalDate, special: String?)

    @Upsert
    suspend fun upsert(day: DayEntity)
}

@Dao
interface ReminderSlotDao {
    @Query("SELECT * FROM reminder_slot")
    suspend fun all(): List<ReminderSlotEntity>

    @Query("SELECT * FROM reminder_slot")
    fun observeAll(): Flow<List<ReminderSlotEntity>>

    @Upsert
    suspend fun upsertAll(slots: List<ReminderSlotEntity>)

    @Upsert
    suspend fun upsert(slot: ReminderSlotEntity)
}

@Dao
interface ReminderLogDao {
    /** Alarms that are set and still ahead. */
    @Query("SELECT * FROM reminder_log WHERE outcome = 'SCHEDULED' AND firedAt IS NULL AND scheduledAt > :now ORDER BY scheduledAt")
    suspend fun upcoming(now: Instant): List<ReminderLogEntity>

    @Query("SELECT * FROM reminder_log WHERE outcome = 'SCHEDULED' AND firedAt IS NULL AND scheduledAt > :now ORDER BY scheduledAt")
    fun observeUpcoming(now: Instant): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_log WHERE key = :key AND scheduledAt = :scheduledAt ORDER BY id DESC LIMIT 1")
    suspend fun find(key: String, scheduledAt: Instant): ReminderLogEntity?

    @Query("SELECT * FROM reminder_log ORDER BY scheduledAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReminderLogEntity>>

    @Insert
    suspend fun insert(row: ReminderLogEntity): Long

    @Query("UPDATE reminder_log SET outcome = :outcome WHERE id = :id")
    suspend fun setOutcome(id: Long, outcome: String)

    @Query("UPDATE reminder_log SET firedAt = :firedAt, outcome = :outcome WHERE id = :id")
    suspend fun setFired(id: Long, firedAt: Instant, outcome: String)

    @Query("DELETE FROM reminder_log WHERE scheduledAt < :before")
    suspend fun deleteBefore(before: Instant)

    @Query("SELECT * FROM reminder_log WHERE scheduledAt >= :since ORDER BY scheduledAt")
    suspend fun since(since: Instant): List<ReminderLogEntity>

    @Query("SELECT * FROM reminder_log WHERE scheduledAt >= :since ORDER BY scheduledAt")
    fun observeSince(since: Instant): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_log WHERE key = :key ORDER BY id DESC LIMIT 1")
    fun observeLatest(key: String): Flow<ReminderLogEntity?>

    /** The last time the app did anything before [before]: a sign the phone was still on. */
    @Query("SELECT MAX(firedAt) FROM reminder_log WHERE firedAt < :before")
    suspend fun lastFiredBefore(before: Instant): Instant?

    @Query("UPDATE reminder_log SET outcome = 'DEVICE_OFF' WHERE id IN (:ids)")
    suspend fun markDeviceOff(ids: List<Long>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("UPDATE settings SET lastOpenedAt = :at WHERE id = 1")
    suspend fun setLastOpenedAt(at: Instant)

    @Query("UPDATE settings SET anytimeOpen = :open WHERE id = 1")
    suspend fun setAnytimeOpen(open: Boolean)

    @Query("UPDATE settings SET character = :character WHERE id = 1")
    suspend fun setCharacter(character: String)

    @Query("UPDATE settings SET todayLayout = :layout WHERE id = 1")
    suspend fun setTodayLayout(layout: String)

    @Query("UPDATE settings SET backupTreeUri = :uri WHERE id = 1")
    suspend fun setBackupTreeUri(uri: String?)

    @Query("UPDATE settings SET afterFinishMode = :mode WHERE id = 1")
    suspend fun setAfterFinishMode(mode: String)

    @Query("UPDATE settings SET onboardingDone = :done WHERE id = 1")
    suspend fun setOnboardingDone(done: Boolean)

    @Query("UPDATE settings SET autoBackup = :on WHERE id = 1")
    suspend fun setAutoBackup(on: Boolean)

    @Query("UPDATE settings SET lastBackupAt = :at WHERE id = 1")
    suspend fun setLastBackupAt(at: Instant)
}

@Dao
interface BackupRecordDao {
    @Insert
    suspend fun insert(row: BackupRecordEntity): Long

    @Query("SELECT * FROM backup_record ORDER BY at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_record WHERE ok = 1 ORDER BY at DESC LIMIT 1")
    suspend fun lastOk(): BackupRecordEntity?

    @Query("SELECT * FROM backup_record WHERE ok = 1 ORDER BY at DESC LIMIT 1")
    fun observeLastOk(): Flow<BackupRecordEntity?>

    @Query("SELECT * FROM backup_record ORDER BY at DESC LIMIT 1")
    fun observeLast(): Flow<BackupRecordEntity?>

    @Query("SELECT * FROM backup_record WHERE ok = 1 ORDER BY at DESC LIMIT :limit")
    fun observeRecentOk(limit: Int): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_record ORDER BY at DESC LIMIT 1")
    suspend fun last(): BackupRecordEntity?
}

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity)

    @Query("SELECT * FROM event WHERE type = :type AND at >= :since ORDER BY at DESC LIMIT 1")
    suspend fun latest(type: String, since: Instant): EventEntity?

    @Query("SELECT COUNT(*) FROM event WHERE type = :type AND ref = :ref")
    suspend fun count(type: String, ref: String): Int

    @Query("SELECT * FROM event ORDER BY at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<EventEntity>>

    @Query("DELETE FROM event WHERE at < :before")
    suspend fun deleteBefore(before: Instant)

    @Query("SELECT MAX(at) FROM event WHERE at < :before")
    suspend fun lastBefore(before: Instant): Instant?

    @Query("SELECT * FROM event WHERE type = :type AND at >= :since ORDER BY at")
    suspend fun ofTypeSince(type: String, since: Instant): List<EventEntity>

    @Query("SELECT COUNT(*) FROM event WHERE type = :type AND ref = :ref AND at >= :since")
    suspend fun countSince(type: String, ref: String, since: Instant): Int
}

@Database(
    entities = [
        TaskEntity::class,
        CompletionEntity::class,
        DayEntity::class,
        ReminderSlotEntity::class,
        ReminderLogEntity::class,
        SettingsEntity::class,
        EventEntity::class,
        BackupRecordEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun completions(): CompletionDao
    abstract fun days(): DayDao
    abstract fun slots(): ReminderSlotDao
    abstract fun reminderLog(): ReminderLogDao
    abstract fun settings(): SettingsDao
    abstract fun events(): EventDao
    abstract fun backups(): BackupRecordDao

    companion object {
        const val NAME = "lantern.db"

        // Never fallbackToDestructiveMigration: schema changes must never wipe data (ARCHITECTURE §8).
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
