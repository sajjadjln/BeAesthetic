package app.beaesthetic.domain.workout

import java.time.LocalDate

/** What a set counts. */
enum class ExerciseKind { REPS, SECONDS }

/** A move, shared by name across the week's workouts (D114). */
data class Exercise(val id: Long, val name: String, val kind: ExerciseKind)

/**
 * One line of a weekday's plan (D105). No range ([rangeMin] and [rangeMax] null) = «حداکثر».
 * [kg] is the default load (D103); null = a move without weights.
 */
data class PlanItem(
    val exercise: Exercise,
    val sets: Int,
    val rangeMin: Int? = null,
    val rangeMax: Int? = null,
    val kg: Double? = null,
    val perSide: Boolean = false,
    val targetEffort: Int? = null,
    val restSeconds: Int? = null,
    val note: String? = null,
) {
    val isMax: Boolean get() = rangeMin == null || rangeMax == null
}

/** The plan of one weekday ([weekday]: Saturday = 0 … Friday = 6). Empty [items] = no training that day. */
data class WorkoutPlan(val weekday: Int, val name: String, val items: List<PlanItem>)

/** A logged set. [date] is its session's program date. */
data class LoggedSet(
    val sessionId: Long,
    val date: LocalDate,
    val exerciseId: Long,
    val setIndex: Int,
    val value: Int,
    val kg: Double?,
    val effort: Int,
)

/** What the session screen shows before ✓: the big number, the kg chip and the effort bar. */
data class SetDraft(val value: Int, val kg: Double?, val effort: Int)
