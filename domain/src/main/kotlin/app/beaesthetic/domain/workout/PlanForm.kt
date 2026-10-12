package app.beaesthetic.domain.workout

/** What the exercise sheet of the plan editor (screens.html 10, D105) holds while the user edits. */
data class PlanItemForm(
    val name: String = "",
    val kind: ExerciseKind = ExerciseKind.REPS,
    val sets: Int = PlanForms.DEFAULT_SETS,
    /** «حداکثر»: no range. */
    val isMax: Boolean = false,
    val from: Int = 8,
    val to: Int = 12,
    val hasKg: Boolean = false,
    val kg: Double = 5.0,
    val perSide: Boolean = false,
    /** 0 = no target effort. */
    val effort: Int = 0,
    /** Seconds; 0 = no rest shown. */
    val rest: Int = 0,
    val note: String = "",
    /** «حرکت سخت‌تر» (D119): the next harder variant's name; empty = none. */
    val harder: String = "",
)

enum class PlanError {
    NO_NAME,
    /** The move is already in this day's list; set logs are per move, so it can't be there twice. */
    DUPLICATE,

    /** «حرکت سخت‌تر» is the move itself or one of its easier variants. */
    HARDER_LOOP,
}

/** Which exercise row a saved form points to (D114). */
sealed interface ExerciseChoice {
    /** The typed name is an existing move: share its history. */
    data class Existing(val id: Long) : ExerciseChoice

    /** A new name for a move used nowhere else: fix the name in place and keep its history (a typo). */
    data class Rename(val id: Long) : ExerciseChoice

    /** A new move. */
    data object New : ExerciseChoice
}

object PlanForms {
    const val DEFAULT_SETS = 3
    const val MIN_SETS = 1
    const val MAX_SETS = 10
    const val MAX_REPS = 100
    const val MAX_SECONDS = 600
    const val REST_STEP = 15
    const val MAX_REST = 600
    const val MAX_NAME = 40
    const val MAX_NOTE = 60
    const val MAX_WORKOUT_NAME = 40

    /** [harder] is the name of the move's next harder variant, if it has one. */
    fun from(item: PlanItem, harder: String = ""): PlanItemForm {
        val blank = PlanItemForm()
        return PlanItemForm(
            name = item.exercise.name,
            kind = item.exercise.kind,
            sets = item.sets,
            isMax = item.isMax,
            from = item.rangeMin ?: blank.from,
            to = item.rangeMax ?: blank.to,
            hasKg = item.kg != null,
            kg = item.kg ?: blank.kg,
            perSide = item.perSide,
            effort = item.targetEffort ?: 0,
            rest = item.restSeconds ?: 0,
            note = item.note.orEmpty(),
            harder = harder,
        )
    }

    /**
     * The key two names are compared by: trimmed, single spaces, lower case, and Arabic «ي» «ك» as Persian
     * «ی» «ک», since keyboards mix them and «پرس سرشانه» must match however it was typed.
     */
    fun key(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").replace('ي', 'ی').replace('ك', 'ک').lowercase()

    fun match(name: String, exercises: List<Exercise>): Exercise? {
        val k = key(name)
        return if (k.isEmpty()) null else exercises.firstOrNull { key(it.name) == k }
    }

    /**
     * [editing] is the row being edited (null for a new one); [dayItems] is the day's whole list. [exercises]
     * and [harderOf] (move → its harder move) catch a «حرکت سخت‌تر» that would close a loop.
     */
    fun errors(
        form: PlanItemForm,
        dayItems: List<PlanItem>,
        editing: PlanItem?,
        exercises: List<Exercise> = emptyList(),
        harderOf: Map<Long, Long> = emptyMap(),
    ): Set<PlanError> = buildSet {
        val k = key(form.name)
        if (k.isEmpty()) add(PlanError.NO_NAME)
        if (k.isNotEmpty() && dayItems.any { it.exercise.id != editing?.exercise?.id && key(it.exercise.name) == k }) add(PlanError.DUPLICATE)
        val h = key(form.harder)
        if (h.isNotEmpty()) {
            // The move being saved: the one the name matches, else the row's own move (a rename keeps it).
            val self = match(form.name, exercises)?.id ?: editing?.exercise?.id
            val harder = match(form.harder, exercises)?.id
            if (h == k || (self != null && harder != null && Ladders.makesLoop(self, harder, harderOf))) add(PlanError.HARDER_LOOP)
        }
    }

    /** Moves for the «حرکت سخت‌تر» field: never the move itself or its easier variants; the text filters. */
    fun harderSuggestions(typed: String, name: String, exercises: List<Exercise>, harderOf: Map<Long, Long>, limit: Int = 8): List<Exercise> {
        val self = match(name, exercises)?.id
        val k = key(typed)
        return exercises
            .filter { key(it.name) != key(name) && (self == null || !Ladders.makesLoop(self, it.id, harderOf)) }
            .filter { (k.isEmpty() || key(it.name).contains(k)) && key(it.name) != k }
            .sortedBy { it.name }
            .take(limit)
    }

    /** Existing moves for the name field, the ones already in this day left out; the typed text filters. */
    fun suggestions(typed: String, exercises: List<Exercise>, dayItems: List<PlanItem>, limit: Int = 8): List<Exercise> {
        val inDay = dayItems.map { it.exercise.id }.toSet()
        val k = key(typed)
        return exercises
            .filter { it.id !in inDay && (k.isEmpty() || key(it.name).contains(k)) && key(it.name) != k }
            .sortedBy { it.name }
            .take(limit)
    }

    /** [usedElsewhere]: moves in any plan row other than [editing]. */
    fun choose(form: PlanItemForm, editing: PlanItem?, exercises: List<Exercise>, usedElsewhere: Set<Long>): ExerciseChoice {
        match(form.name, exercises)?.let { return ExerciseChoice.Existing(it.id) }
        if (editing != null && editing.exercise.id !in usedElsewhere) return ExerciseChoice.Rename(editing.exercise.id)
        return ExerciseChoice.New
    }

    /** The plan row the form describes, for [exercise] (already resolved by [choose]). */
    fun toItem(form: PlanItemForm, exercise: Exercise): PlanItem = PlanItem(
        exercise = exercise,
        sets = form.sets.coerceIn(MIN_SETS, MAX_SETS),
        rangeMin = if (form.isMax) null else form.from,
        rangeMax = if (form.isMax) null else form.to,
        kg = if (form.hasKg) form.kg else null,
        perSide = form.perSide,
        targetEffort = form.effort.takeIf { it in WorkoutRules.MIN_EFFORT..WorkoutRules.MAX_EFFORT },
        restSeconds = form.rest.takeIf { it > 0 },
        note = form.note.trim().take(MAX_NOTE).ifBlank { null },
    )

    fun cleanName(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)

    fun maxValue(kind: ExerciseKind): Int = if (kind == ExerciseKind.SECONDS) MAX_SECONDS else MAX_REPS

    /** «از» moved: «تا» follows so the range never turns around. */
    fun withFrom(form: PlanItemForm, from: Int): PlanItemForm {
        val f = from.coerceIn(1, maxValue(form.kind))
        return form.copy(from = f, to = maxOf(form.to, f))
    }

    /** «تا» moved: «از» follows. */
    fun withTo(form: PlanItemForm, to: Int): PlanItemForm {
        val t = to.coerceIn(1, maxValue(form.kind))
        return form.copy(to = t, from = minOf(form.from, t))
    }

    /** Switching reps ↔ seconds gives the range a sensible start instead of «۸–۱۲ ثانیه». */
    fun withKind(form: PlanItemForm, kind: ExerciseKind): PlanItemForm = when {
        kind == form.kind -> form
        kind == ExerciseKind.SECONDS -> form.copy(kind = kind, from = 30, to = 45)
        else -> form.copy(kind = kind, from = 8, to = 12)
    }
}
