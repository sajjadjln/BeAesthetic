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
import app.beaesthetic.domain.tasks.TaskForm
import app.beaesthetic.domain.tasks.TaskForms
import app.beaesthetic.domain.tasks.TaskIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** What Today needs for one program date. [anytime] is the «هر وقت» list (D127), never part of [items]. */
data class DayView(val date: LocalDate, val items: List<DayItem>, val minimumMode: Boolean, val anytime: List<DayItem> = emptyList())

class TaskRepository(private val db: AppDatabase) {

    fun observeDay(date: LocalDate): Flow<DayView> = combine(
        db.tasks().observeActive(),
        db.completions().observeUpTo(date),
        db.days().observe(date),
    ) { tasks, completions, day ->
        val domain = tasks.map { it.toDomain() }
        val byTask = completions.groupBy({ it.taskId }, { it.date })
        DayView(date, DueRules.itemsFor(date, domain, byTask), day?.minimumMode ?: false, DueRules.anytime(date, domain, byTask))
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

    /**
     * Marks [date] as a special day (sick, travel, off) or clears it (null). The square takes the special
     * color and the day leaves the weekly score (SPEC §6.2). The caller checks the weekly cap.
     */
    suspend fun setSpecial(date: LocalDate, kind: String?) = db.withTransaction {
        if (db.days().get(date) == null) writeSnapshot(date)
        db.days().setSpecial(date, kind)
    }

    /** The stored special kind of [date], if any. */
    fun observeSpecial(date: LocalDate): Flow<String?> = db.days().observe(date).map { it?.special }

    /** Tasks done on [date] (archived ones too), for the day sheet. */
    fun observeDoneOn(date: LocalDate): Flow<List<Task>> = db.completions().observeDoneOn(date).map { rows -> rows.map { it.toDomain() } }

    /** Stored snapshots for a date range (week dots, comebacks). */
    fun observeSnapshots(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DaySnapshot>> =
        db.days().observeRange(from, to).map { rows ->
            rows.associate { it.date to DaySnapshot(DayCounts(it.dueCount, it.doneCount, it.floorDone), special = it.special != null) }
        }

    /** Active tasks with every completion date, for the tasks list (screens.html 07). */
    fun observeTasks(): Flow<Pair<List<Task>, Map<Long, List<LocalDate>>>> = combine(
        db.tasks().observeActive(),
        db.completions().observeAll(),
    ) { tasks, completions -> tasks.map { it.toDomain() } to completions.groupBy({ it.taskId }, { it.date }) }

    suspend fun task(id: Long): Task? = db.tasks().get(id)?.takeIf { it.archivedAt == null }?.toDomain()

    /**
     * Adds a task (id 0) or updates one, then rewrites the editable days' snapshots: editing a task changes
     * today and yesterday only, never older history (D37). Returns the task's id.
     */
    suspend fun save(task: Task, today: LocalDate, now: Instant): Long = db.withTransaction {
        val id = if (task.id == 0L) {
            db.tasks().insert(task.toEntity(createdAt = now))
        } else {
            val existing = checkNotNull(db.tasks().get(task.id))
            db.tasks().update(task.toEntity(createdAt = existing.createdAt).copy(fadeOfferedAt = existing.fadeOfferedAt, remindersSilencedAt = existing.remindersSilencedAt))
            task.id
        }
        writeEditableSnapshots(today)
        id
    }

    /**
     * Saves the task editor's form (a new task when [original] is null), from the tasks list or Today
     * (D107, D128). The caller re-plans reminders.
     */
    suspend fun saveForm(form: TaskForm, original: Task?, today: LocalDate, now: Instant): Long {
        val tasks = db.tasks().active().map { it.toDomain() }
        val task = TaskForms.toTask(form, original, today, TaskForms.sortOrderFor(form, original, tasks))
        return save(task, today, now)
    }

    /** Removes a task from today on. Its completions stay, so past days keep their colors. */
    suspend fun archive(id: Long, today: LocalDate, now: Instant) = db.withTransaction {
        db.tasks().archive(id, now)
        writeEditableSnapshots(today)
    }

    /** Today always; yesterday only if the app already saw it (a day before install never gets a row, D56). */
    private suspend fun writeEditableSnapshots(today: LocalDate) {
        writeSnapshot(today)
        val yesterday = today.minusDays(1)
        if (db.days().get(yesterday) != null) writeSnapshot(yesterday)
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

fun Task.toEntity(createdAt: Instant): TaskEntity {
    val (type, weekdays, everyN, once) = when (val r = repeat) {
        Repeat.Daily -> Quad(RepeatType.DAILY, 0, null, null)
        is Repeat.OnWeekdays -> Quad(RepeatType.WEEKDAYS, r.bits, null, null)
        is Repeat.EveryNDays -> Quad(RepeatType.EVERY_N_DAYS, 0, r.n, null)
        is Repeat.Once -> Quad(RepeatType.ONCE, 0, null, r.date)
        Repeat.MinDayOnly -> Quad(RepeatType.MIN_DAY_ONLY, 0, null, null)
        Repeat.Anytime -> Quad(RepeatType.ANYTIME, 0, null, null)
    }
    return TaskEntity(
        id = id,
        title = title,
        icon = icon.name,
        part = part,
        repeat = type,
        weekdays = weekdays,
        everyNDays = everyN,
        onceDate = once,
        startDate = startDate,
        isImportant = isImportant,
        inMinimumDay = inMinimumDay,
        action = action.name,
        alarmTime = alarm?.minuteOfDay,
        alarmWeekdays = alarm?.weekdays ?: 0,
        alarmEnabled = alarm?.enabled ?: false,
        anchor = anchor,
        remindersSilencedAt = null,
        sortOrder = sortOrder,
        createdAt = createdAt,
    )
}

private data class Quad(val type: RepeatType, val weekdays: Int, val everyN: Int?, val once: LocalDate?)

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
        RepeatType.ANYTIME -> Repeat.Anytime
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
