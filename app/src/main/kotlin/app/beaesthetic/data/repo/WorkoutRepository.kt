package app.beaesthetic.data.repo

import androidx.room.withTransaction
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.ExerciseEntity
import app.beaesthetic.data.db.SessionEntity
import app.beaesthetic.data.db.SetLogEntity
import app.beaesthetic.data.db.SetWithDate
import app.beaesthetic.data.db.WorkoutEntity
import app.beaesthetic.data.db.WorkoutExerciseEntity
import app.beaesthetic.domain.tasks.TaskAction
import app.beaesthetic.domain.workout.Exercise
import app.beaesthetic.domain.workout.ExerciseChoice
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.LoggedSet
import app.beaesthetic.domain.workout.PlanForms
import app.beaesthetic.domain.workout.PlanItem
import app.beaesthetic.domain.workout.PlanItemForm
import app.beaesthetic.domain.workout.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** The workout plan (screens.html 10) and sessions (04). Slice 4a. */
class WorkoutRepository(private val db: AppDatabase, private val tasks: TaskRepository) {

    private val dao = db.workouts()

    fun observeExercises(): Flow<List<Exercise>> = dao.observeExercises().map { rows -> rows.map { it.toDomain() } }

    /** All 7 weekdays, Saturday first; a weekday with no rows is an empty plan. */
    fun observePlans(): Flow<List<WorkoutPlan>> = combine(dao.observeWorkouts(), dao.observeItems(), dao.observeExercises()) { workouts, items, exercises ->
        val byId = exercises.associate { it.id to it.toDomain() }
        (0..6).map { weekday ->
            val w = workouts.firstOrNull { it.weekday == weekday }
            val rows = if (w == null) emptyList() else items.filter { it.workoutId == w.id }
            WorkoutPlan(weekday, w?.name.orEmpty(), rows.mapNotNull { row -> byId[row.exerciseId]?.let { row.toDomain(it) } })
        }
    }

    /** Weekdays that have a plan: their workout tile gets the play button. */
    fun observePlannedDays(): Flow<Set<Int>> = observePlans().map { plans -> plans.filter { it.items.isNotEmpty() }.map { it.weekday }.toSet() }

    suspend fun setName(weekday: Int, name: String) = db.withTransaction {
        val clean = name.take(PlanForms.MAX_WORKOUT_NAME)
        val w = dao.workout(weekday)
        if (w == null) dao.insertWorkout(WorkoutEntity(weekday = weekday, name = clean)) else dao.setName(w.id, clean)
    }

    /**
     * Saves the exercise sheet: [editing] is the row being edited (null = a new row at the end). The name
     * picks the move (D114): an existing name shares it, a new one renames a move used nowhere else, or adds one.
     */
    suspend fun saveItem(weekday: Int, form: PlanItemForm, editing: PlanItem?) = db.withTransaction {
        val workoutId = workoutId(weekday)
        val exercises = dao.exercises().map { it.toDomain() }
        val rows = dao.items(workoutId)
        val editingPosition = editing?.let { e -> rows.firstOrNull { it.exerciseId == e.exercise.id }?.position }
        val usedElsewhere = dao.allItems()
            .filterNot { it.workoutId == workoutId && it.position == editingPosition }
            .map { it.exerciseId }.toSet()
        val name = PlanForms.cleanName(form.name)
        val kind = form.kind.name
        val exerciseId = when (val choice = PlanForms.choose(form, editing, exercises, usedElsewhere)) {
            is ExerciseChoice.Existing -> choice.id.also { dao.updateExercise(it, exercises.first { e -> e.id == it }.name, kind) }
            is ExerciseChoice.Rename -> choice.id.also { dao.updateExercise(it, name, kind) }
            ExerciseChoice.New -> dao.insertExercise(ExerciseEntity(name = name, kind = kind))
        }
        val item = PlanForms.toItem(form, Exercise(exerciseId, name, form.kind))
        val list = rows.toMutableList()
        val entity = item.toEntity(workoutId, 0)
        if (editingPosition != null) list[rows.indexOfFirst { it.position == editingPosition }] = entity else list += entity
        rewrite(workoutId, list)
    }

    /** Takes a row out of the day. The move and its logged sets stay, so its history is kept. */
    suspend fun removeItem(weekday: Int, exerciseId: Long) = db.withTransaction {
        val w = dao.workout(weekday) ?: return@withTransaction
        rewrite(w.id, dao.items(w.id).filterNot { it.exerciseId == exerciseId })
    }

    /** Drag to reorder: the row at [from] lands at [to]. */
    suspend fun move(weekday: Int, from: Int, to: Int) = db.withTransaction {
        val w = dao.workout(weekday) ?: return@withTransaction
        val rows = dao.items(w.id).toMutableList()
        if (from !in rows.indices || to !in rows.indices) return@withTransaction
        rows.add(to, rows.removeAt(from))
        rewrite(w.id, rows)
    }

    private suspend fun rewrite(workoutId: Long, rows: List<WorkoutExerciseEntity>) {
        dao.deleteItems(workoutId)
        dao.insertItems(rows.mapIndexed { i, row -> row.copy(workoutId = workoutId, position = i) })
    }

    private suspend fun workoutId(weekday: Int): Long =
        dao.workout(weekday)?.id ?: dao.insertWorkout(WorkoutEntity(weekday = weekday, name = ""))

    // ------------------------------------------------------------ sessions

    /** Today's session of [weekday]'s workout, if a set was logged already (D115). */
    suspend fun existingSession(weekday: Int, date: LocalDate): Long? {
        val w = dao.workout(weekday) ?: return null
        return dao.session(w.id, date)?.id
    }

    /** Every set of [exerciseIds], all time, with their session dates. */
    fun observeSets(exerciseIds: List<Long>): Flow<List<SetWithDate>> = dao.observeSets(exerciseIds)

    /**
     * Logs one set (replacing it if it was logged before) and ticks the day's workout task (D115). The
     * session is created with its first set. Returns the session id.
     */
    suspend fun logSet(weekday: Int, date: LocalDate, sessionId: Long?, exerciseId: Long, setIndex: Int, value: Int, kg: Double?, effort: Int, now: Instant): Long =
        db.withTransaction {
            val id = sessionId ?: existingSession(weekday, date) ?: dao.insertSession(SessionEntity(workoutId = workoutId(weekday), date = date, startedAt = now))
            dao.upsertSet(SetLogEntity(id, exerciseId, setIndex, value, kg, effort, now))
            tasks.day(date).items
                .filter { it.task.action == TaskAction.WORKOUT && !it.done }
                .forEach { tasks.complete(it.task.id, date, now) }
            id
        }

    suspend fun finish(sessionId: Long, now: Instant) = dao.finish(sessionId, now)
}

fun ExerciseEntity.toDomain() = Exercise(id, name, runCatching { ExerciseKind.valueOf(kind) }.getOrDefault(ExerciseKind.REPS))

fun SetWithDate.toDomain() = LoggedSet(sessionId, date, exerciseId, setIndex, value, kg, effort)

private fun WorkoutExerciseEntity.toDomain(exercise: Exercise) =
    PlanItem(exercise, sets, rangeMin, rangeMax, kg, perSide, targetEffort, restSeconds, note)

private fun PlanItem.toEntity(workoutId: Long, position: Int) = WorkoutExerciseEntity(
    workoutId = workoutId,
    position = position,
    exerciseId = exercise.id,
    sets = sets,
    rangeMin = rangeMin,
    rangeMax = rangeMax,
    kg = kg,
    perSide = perSide,
    targetEffort = targetEffort,
    restSeconds = restSeconds,
    note = note,
)
