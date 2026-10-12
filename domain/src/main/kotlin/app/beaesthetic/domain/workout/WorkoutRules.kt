package app.beaesthetic.domain.workout

/** Session rules: pre-fill (D26, D103), «رکورد» (D54, D103) and where the session is. */
object WorkoutRules {
    const val MIN_EFFORT = 1
    const val MAX_EFFORT = 10
    const val DEFAULT_EFFORT = 7
    const val KG_STEP = 0.5
    const val MAX_KG = 300.0
    private const val FIRST_REPS = 10
    private const val FIRST_SECONDS = 30

    /**
     * The sets of the last session before [currentSessionId] that logged [exerciseId], by set index:
     * the «قبلی ۱۲ · ۱۲ · ۱۰» line and the pre-fill source.
     */
    fun previous(history: List<LoggedSet>, exerciseId: Long, currentSessionId: Long): List<LoggedSet> {
        val earlier = history.filter { it.exerciseId == exerciseId && it.sessionId != currentSessionId }
        val last = earlier.maxWithOrNull(compareBy<LoggedSet> { it.date }.thenBy { it.sessionId }) ?: return emptyList()
        return earlier.filter { it.sessionId == last.sessionId }.sortedBy { it.setIndex }
    }

    /**
     * The draft for set [setIndex] (0-based). Reps or seconds and effort come from the same set last time,
     * then from this session's last set, then from the plan. kg follows this session's last set first, since
     * a changed dumbbell stays changed for the next set, then last time, then the plan's kg. A move without
     * kg in the plan has no kg.
     */
    fun prefill(item: PlanItem, previous: List<LoggedSet>, thisSession: List<LoggedSet>, setIndex: Int): SetDraft {
        val same = previous.firstOrNull { it.setIndex == setIndex }
        val lastHere = thisSession.filter { it.setIndex < setIndex }.maxByOrNull { it.setIndex }
        val value = same?.value ?: lastHere?.value ?: previous.lastOrNull()?.value ?: firstValue(item)
        val effort = same?.effort ?: lastHere?.effort ?: item.targetEffort ?: DEFAULT_EFFORT
        val kg = if (item.kg == null) null else lastHere?.kg ?: same?.kg ?: previous.lastOrNull()?.kg ?: item.kg
        return SetDraft(value, kg, effort.coerceIn(MIN_EFFORT, MAX_EFFORT))
    }

    private fun firstValue(item: PlanItem): Int =
        item.rangeMin ?: if (item.exercise.kind == ExerciseKind.SECONDS) FIRST_SECONDS else FIRST_REPS

    /**
     * Whether a set of [value] at [kg] beats every earlier set of the move (D54, D103): more reps or seconds
     * than any set at the same or a higher kg, or a load never lifted before. The very first set of a move
     * has nothing to beat, so it isn't a record. [earlier] holds only that move's sets logged before this one.
     */
    fun isRecord(value: Int, kg: Double?, earlier: List<LoggedSet>): Boolean {
        if (value <= 0 || earlier.isEmpty()) return false
        val load = kg ?: 0.0
        val atOrAbove = earlier.filter { (it.kg ?: 0.0) >= load }
        return atOrAbove.isEmpty() || value > atOrAbove.maxOf { it.value }
    }

    /**
     * The sets of a session that were records when they were logged, as (exerciseId, setIndex). [session] is in
     * the order the sets were logged; [before] holds the moves' sets from earlier sessions. Worked out again
     * each time instead of stored, so a reopened session still shows its records.
     */
    fun records(session: List<LoggedSet>, before: List<LoggedSet>): Set<Pair<Long, Int>> {
        val seen = before.groupBy { it.exerciseId }.mapValues { it.value.toMutableList() }.toMutableMap()
        val records = mutableSetOf<Pair<Long, Int>>()
        for (set in session) {
            val earlier = seen.getOrPut(set.exerciseId) { mutableListOf() }
            if (isRecord(set.value, set.kg, earlier)) records += set.exerciseId to set.setIndex
            earlier += set
        }
        return records
    }

    /**
     * The exercise the session opens on: the first one with sets still to log. Null when every planned set
     * is logged (the summary shows).
     */
    fun firstOpen(items: List<PlanItem>, logged: List<LoggedSet>): Int? {
        val counts = logged.groupingBy { it.exerciseId }.eachCount()
        return items.indexOfFirst { (counts[it.exercise.id] ?: 0) < it.sets }.takeIf { it >= 0 }
    }

    /** The set to log next for [item]: the first index not logged yet (capped at the planned count). */
    fun nextSetIndex(item: PlanItem, logged: List<LoggedSet>): Int {
        val done = logged.filter { it.exerciseId == item.exercise.id }.map { it.setIndex }.toSet()
        return (0 until item.sets).firstOrNull { it !in done } ?: item.sets
    }

    /** kg − / + on the chip: 0.5 steps, never below 0. */
    fun stepKg(kg: Double, up: Boolean): Double =
        (Math.round(kg / KG_STEP) + if (up) 1 else -1).coerceAtLeast(0).times(KG_STEP).coerceAtMost(MAX_KG)

    /** The big number's − / + step: 1 rep, or 5 seconds. */
    fun step(kind: ExerciseKind): Int = if (kind == ExerciseKind.SECONDS) 5 else 1
}
