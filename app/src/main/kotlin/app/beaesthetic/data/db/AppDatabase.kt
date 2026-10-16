package app.beaesthetic.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
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

@Dao
interface LetterDao {
    @Insert
    suspend fun insert(letter: LetterEntity): Long

    @Query("SELECT * FROM letter ORDER BY opensOn, id")
    fun observeAll(): Flow<List<LetterEntity>>

    @Query("UPDATE letter SET openedAt = :at WHERE id = :id AND openedAt IS NULL")
    suspend fun markOpened(id: Long, at: Instant)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workout_exercise ORDER BY workoutId, position")
    fun observeItems(): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE archivedAt IS NULL ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE archivedAt IS NULL")
    suspend fun exercises(): List<ExerciseEntity>

    @Query("SELECT * FROM workout WHERE weekday = :weekday")
    suspend fun workout(weekday: Int): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("UPDATE workout SET name = :name WHERE id = :id")
    suspend fun setName(id: Long, name: String)

    @Query("SELECT * FROM workout_exercise WHERE workoutId = :workoutId ORDER BY position")
    suspend fun items(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercise")
    suspend fun allItems(): List<WorkoutExerciseEntity>

    @Query("DELETE FROM workout_exercise WHERE workoutId = :workoutId")
    suspend fun deleteItems(workoutId: Long)

    @Insert
    suspend fun insertItems(items: List<WorkoutExerciseEntity>)

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Query("UPDATE exercise SET name = :name, kind = :kind WHERE id = :id")
    suspend fun updateExercise(id: Long, name: String, kind: String)

    @Query("UPDATE exercise SET harderId = :harderId WHERE id = :id")
    suspend fun setHarder(id: Long, harderId: Long?)

    /** A move has at most one easier move (D119): linking a new one unlinks the old. */
    @Query("UPDATE exercise SET harderId = NULL WHERE harderId = :harderId AND id != :keep")
    suspend fun unlinkEasier(harderId: Long, keep: Long)

    @Query("SELECT * FROM exercise")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM session WHERE workoutId = :workoutId AND date = :date ORDER BY id LIMIT 1")
    suspend fun session(workoutId: Long, date: LocalDate): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE session SET finishedAt = :at WHERE id = :id")
    suspend fun finish(id: Long, at: Instant)

    /** Every set of the given moves (all time, small: ~4 sets × 4 sessions a week). */
    @Query(
        "SELECT set_log.sessionId, set_log.exerciseId, set_log.setIndex, set_log.value, set_log.kg, set_log.effort, set_log.loggedAt, session.date " +
            "FROM set_log JOIN session ON session.id = set_log.sessionId WHERE set_log.exerciseId IN (:exerciseIds) ORDER BY set_log.loggedAt",
    )
    fun observeSets(exerciseIds: List<Long>): Flow<List<SetWithDate>>

    @Upsert
    suspend fun upsertSet(set: SetLogEntity)

    /** Every set ever logged, for lifetime totals (small: a few thousand rows over the program). */
    @Query(
        "SELECT set_log.sessionId, set_log.exerciseId, set_log.setIndex, set_log.value, set_log.kg, set_log.effort, set_log.loggedAt, session.date " +
            "FROM set_log JOIN session ON session.id = set_log.sessionId",
    )
    suspend fun allSets(): List<SetWithDate>

    /** The same, observed: Progress's ladders and totals (slice 7b). */
    @Query(
        "SELECT set_log.sessionId, set_log.exerciseId, set_log.setIndex, set_log.value, set_log.kg, set_log.effort, set_log.loggedAt, session.date " +
            "FROM set_log JOIN session ON session.id = set_log.sessionId",
    )
    fun observeAllSets(): Flow<List<SetWithDate>>

    @Query("SELECT * FROM workout_exercise WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun item(workoutId: Long, exerciseId: Long): WorkoutExerciseEntity?

    @androidx.room.Update
    suspend fun updateItem(item: WorkoutExerciseEntity)
}

@Dao
interface RoundNumberDao {
    @Query("SELECT key FROM round_number")
    suspend fun keys(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<RoundNumberEntity>)

    @Query("SELECT * FROM round_number WHERE reachedOn = :date")
    fun observeOn(date: LocalDate): Flow<List<RoundNumberEntity>>

    /** Days-shown-up crossings not dismissed yet: the Today card. */
    @Query("SELECT * FROM round_number WHERE key LIKE 'days:%' AND seenAt IS NULL")
    fun observeUnseenDays(): Flow<List<RoundNumberEntity>>

    /** «دمت گرم»: every days-shown-up crossing so far is seen, so an older one never shows after a newer one. */
    @Query("UPDATE round_number SET seenAt = :at WHERE key LIKE 'days:%' AND seenAt IS NULL")
    suspend fun markDaysSeen(at: Instant)
}

@Dao
interface ProgressDao {
    @Query("SELECT MAX(idx) FROM check_in")
    suspend fun maxCheckInIdx(): Int?

    /** A new program (D145): the old check-ins and photos move up by [offset] and stay as history. */
    @Query("UPDATE check_in SET idx = idx + :offset")
    suspend fun shiftCheckIns(offset: Int)

    @Query("UPDATE photo SET checkInIdx = checkInIdx + :offset")
    suspend fun shiftPhotos(offset: Int)

    @Query("SELECT * FROM metric WHERE archivedAt IS NULL ORDER BY sortOrder, id")
    fun observeMetrics(): Flow<List<MetricEntity>>

    @Query("SELECT * FROM metric WHERE archivedAt IS NULL ORDER BY sortOrder, id")
    suspend fun metrics(): List<MetricEntity>

    @Query("SELECT * FROM metric WHERE id = :id")
    suspend fun metric(id: Long): MetricEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM metric")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insertMetric(metric: MetricEntity): Long

    @androidx.room.Update
    suspend fun updateMetric(metric: MetricEntity)

    @Query("UPDATE metric SET archivedAt = :at WHERE id = :id")
    suspend fun archiveMetric(id: Long, at: Instant)

    /** Readings of active metrics only (small: ~15 metrics × 30 readings). */
    @Query("SELECT metric_entry.* FROM metric_entry JOIN metric ON metric.id = metric_entry.metricId WHERE metric.archivedAt IS NULL ORDER BY metric_entry.date")
    fun observeEntries(): Flow<List<MetricEntryEntity>>

    @Query("SELECT * FROM metric_entry WHERE metricId = :metricId AND date = :date")
    suspend fun entry(metricId: Long, date: LocalDate): MetricEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: MetricEntryEntity)

    @Query("DELETE FROM metric_entry WHERE metricId = :metricId AND date = :date")
    suspend fun deleteEntry(metricId: Long, date: LocalDate)

    @Query("SELECT * FROM check_in")
    fun observeCheckIns(): Flow<List<CheckInEntity>>

    @Upsert
    suspend fun upsertCheckIn(checkIn: CheckInEntity)

    @Query("SELECT * FROM photo ORDER BY checkInIdx")
    fun observePhotos(): Flow<List<PhotoEntity>>

    @Upsert
    suspend fun upsertPhoto(photo: PhotoEntity)

    @Query("DELETE FROM photo WHERE checkInIdx = :idx AND pose = :pose")
    suspend fun deletePhoto(idx: Int, pose: String)

    @Query("SELECT * FROM milestone ORDER BY sortOrder, id")
    fun observeMilestones(): Flow<List<MilestoneEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM milestone")
    suspend fun maxMilestoneOrder(): Int

    @Query("SELECT * FROM milestone WHERE id = :id")
    suspend fun milestone(id: Long): MilestoneEntity?

    @Upsert
    suspend fun upsertMilestone(m: MilestoneEntity)

    @Query("DELETE FROM milestone WHERE id = :id")
    suspend fun deleteMilestone(id: Long)
}

@Dao
interface WeekDao {
    @Query("SELECT weekStart FROM week_plan WHERE ritualDoneAt IS NOT NULL")
    fun observeDone(): Flow<List<LocalDate>>

    @Query("SELECT weekStart FROM week_plan WHERE ritualDoneAt IS NOT NULL")
    suspend fun done(): List<LocalDate>

    @Upsert
    suspend fun upsert(plan: WeekPlanEntity)

    @Query("SELECT * FROM snack_note WHERE date BETWEEN :from AND :to")
    fun observeSnacks(from: LocalDate, to: LocalDate): Flow<List<SnackNoteEntity>>

    @Query("SELECT * FROM snack_note WHERE date = :date")
    fun observeSnack(date: LocalDate): Flow<SnackNoteEntity?>

    @Upsert
    suspend fun upsertSnack(note: SnackNoteEntity)

    @Query("DELETE FROM snack_note WHERE date = :date")
    suspend fun deleteSnack(date: LocalDate)

    @Query("SELECT key FROM recap")
    fun observeRecapsSeen(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markRecapSeen(recap: RecapEntity)

    @Query("DELETE FROM recap")
    suspend fun clearRecaps()
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
        LetterEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SessionEntity::class,
        SetLogEntity::class,
        RoundNumberEntity::class,
        BackupRecordEntity::class,
        MetricEntity::class,
        MetricEntryEntity::class,
        CheckInEntity::class,
        PhotoEntity::class,
        MilestoneEntity::class,
        WeekPlanEntity::class,
        SnackNoteEntity::class,
        RecapEntity::class,
    ],
    version = 10,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7), AutoMigration(from = 7, to = 8), AutoMigration(from = 8, to = 9), AutoMigration(from = 9, to = 10)],
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
    abstract fun letters(): LetterDao
    abstract fun workouts(): WorkoutDao
    abstract fun roundNumbers(): RoundNumberDao
    abstract fun backups(): BackupRecordDao
    abstract fun progress(): ProgressDao
    abstract fun weeks(): WeekDao

    companion object {
        const val NAME = "beaesthetic.db"

        // Never fallbackToDestructiveMigration: schema changes must never wipe data (ARCHITECTURE §8).
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
