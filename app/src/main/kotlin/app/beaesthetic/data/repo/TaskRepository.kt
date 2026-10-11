package app.beaesthetic.data.repo

import androidx.room.withTransaction
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.CompletionEntity
import app.beaesthetic.data.db.DayEntity
import app.beaesthetic.data.db.RepeatType
import app.beaesthetic.data.db.TaskEntity
import app.beaesthetic.domain.score.DayCounts
import app.beaesthetic.domain.score.DayScore
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.tasks.DayItem
import app.beaesthetic.domain.tasks.DueRules
import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskAction
import app.beaesthetic.domain.tasks.TaskAlarm
import app.beaesthetic.domain.tasks.TaskIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** What Today needs for one program date. */
data class DayView(val date: LocalDate, val items: List<DayItem>, val minimumMode: Boolean)

class TaskRepository(private val db: AppDatabase) {

    fun observeDay(date: LocalDate): Flow<DayView> = combine(
        db.tasks().observeActive(),
        db.completions().observeUpTo(date),
        db.days().observe(date),
    ) { tasks, completions, day ->
        DayView(date, items(date, tasks, completions), day?.minimumMode ?: false)
    }

    suspend fun day(date: LocalDate): DayView {
        val items = items(date, db.tasks().active(), db.completions().upTo(date))
        return DayView(date, items, db.days().get(date)?.minimumMode ?: false)
    }

    /**
     * Ticks or unticks [taskId] on [date] and rewrites the day snapshot. Returns true when the task is now
     * done. Only today and yesterday are editable (D17); the caller passes one of them.
     */
    suspend fun toggle(taskId: Long, date: LocalDate, now: Instant): Boolean = db.withTransaction {
        val completions = db.completions()
        val nowDone = if (completions.count(taskId, date) > 0) {
            completions.delete(taskId, date)
            false
        } else {
            completions.insert(CompletionEntity(taskId, date, now))
            true
        }
        writeSnapshot(date)
        nowDone
    }

    /** Marks [taskId] done (from a notification's «انجام شد»). Doing it twice changes nothing. */
    suspend fun complete(taskId: Long, date: LocalDate, now: Instant) = db.withTransaction {
        db.completions().insert(CompletionEntity(taskId, date, now))
        writeSnapshot(date)
    }

    /** The leaf button (M3): only the floor tasks show; the floor done gives the day at least level 1. */
    suspend fun setMinimumMode(date: LocalDate, on: Boolean) = db.withTransaction {
        val existing = db.days().get(date) ?: DayEntity(date)
        db.days().upsert(existing.copy(minimumMode = on))
        writeSnapshot(date)
    }

    /** Stored snapshots for a date range (week dots, comebacks). */
    fun observeSnapshots(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DaySnapshot>> =
        db.days().observeRange(from, to).map { rows ->
            rows.associate { it.date to DaySnapshot(DayCounts(it.dueCount, it.doneCount, it.floorDone), special = it.special != null) }
        }

    /** Tasks and completions as the reminder planner needs them. */
    suspend fun plannerData(upTo: LocalDate): Pair<List<Task>, Map<Long, List<LocalDate>>> {
        val tasks = db.tasks().active().map { it.toDomain() }
        val completions = db.completions().upTo(upTo).groupBy({ it.taskId }, { it.date })
        return tasks to completions
    }

    /**
     * Recomputes the stored due/done counts for [date] (D37). Called only for today and yesterday, so a day
     * that has left the edit window is never rewritten and editing a task never recolors history.
     */
    suspend fun writeSnapshot(date: LocalDate) {
        val counts = DayScore.counts(day(date).items)
        val existing = db.days().get(date) ?: DayEntity(date)
        db.days().upsert(existing.copy(dueCount = counts.due, doneCount = counts.done, floorDone = counts.floorDone))
    }

    private fun items(date: LocalDate, tasks: List<TaskEntity>, completions: List<CompletionEntity>): List<DayItem> {
        val byTask = completions.groupBy({ it.taskId }, { it.date })
        return DueRules.itemsFor(date, tasks.map { it.toDomain() }, byTask)
    }
}

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    icon = runCatching { TaskIcon.valueOf(icon) }.getOrDefault(TaskIcon.DROP),
    part = part,
    repeat = when (repeat) {
        RepeatType.DAILY -> Repeat.Daily
        RepeatType.WEEKDAYS -> Repeat.OnWeekdays(weekdays)
        RepeatType.EVERY_N_DAYS -> Repeat.EveryNDays(everyNDays ?: 1)
        RepeatType.ONCE -> Repeat.Once(onceDate ?: startDate)
        RepeatType.MIN_DAY_ONLY -> Repeat.MinDayOnly
    },
    startDate = startDate,
    isImportant = isImportant,
    inMinimumDay = inMinimumDay,
    action = runCatching { TaskAction.valueOf(action) }.getOrDefault(TaskAction.NONE),
    anchor = anchor,
    alarm = alarmTime?.let { TaskAlarm(it, alarmWeekdays, alarmEnabled) },
    remindersSilenced = remindersSilencedAt != null,
    sortOrder = sortOrder,
)
