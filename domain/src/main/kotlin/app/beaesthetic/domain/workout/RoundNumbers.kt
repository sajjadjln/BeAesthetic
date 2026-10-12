package app.beaesthetic.domain.workout

/**
 * Round numbers (D71, D121): lifetime totals that cross a round value are noted once. Totals are computed
 * from the log; only the crossing is stored (`round_number`), so editing a log never takes one back.
 */
object RoundNumbers {

    enum class Kind(val prefix: String, val thresholds: List<Int>) {
        /** Reps per ladder (every rung counts; a move without a ladder is its own). */
        REPS("reps:ladder", listOf(100, 500, 1_000, 2_500, 5_000, 10_000)),

        /** Seconds per timed ladder: 10, 30, 60 and 120 minutes. */
        SECONDS("seconds:ladder", listOf(600, 1_800, 3_600, 7_200)),

        /** Sessions with at least one logged set (D115). */
        WORKOUTS("workouts", listOf(10, 25, 50, 75)),

        /** Days at level 1 or more. */
        DAYS("days", listOf(30, 60, 100, 150)),
    }

    /** [ladder] is the chain's easiest move ([Ladders.root]) for reps and seconds, null otherwise. */
    data class Crossing(val kind: Kind, val ladder: Long?, val value: Int) {
        val key: String get() = if (ladder == null) "${kind.prefix}:$value" else "${kind.prefix}:$ladder:$value"
    }

    fun parse(key: String): Crossing? {
        val kind = Kind.entries.firstOrNull { key.startsWith(it.prefix + ":") } ?: return null
        val rest = key.removePrefix(kind.prefix + ":").split(":")
        return when {
            kind == Kind.REPS || kind == Kind.SECONDS -> if (rest.size == 2) {
                val ladder = rest[0].toLongOrNull() ?: return null
                rest[1].toIntOrNull()?.let { Crossing(kind, ladder, it) }
            } else {
                null
            }
            rest.size == 1 -> rest[0].toIntOrNull()?.let { Crossing(kind, null, it) }
            else -> null
        }
    }

    /** Every threshold of [kind] at or below [total]. */
    fun reached(kind: Kind, ladder: Long?, total: Int): List<Crossing> =
        kind.thresholds.filter { it <= total }.map { Crossing(kind, ladder, it) }

    /**
     * Reps and seconds per ladder from [sets] (D122: a set counts as logged, «هر طرف» too). [kindOf] gives a
     * move's kind (null for an unknown move, which is skipped); [rootOf] its ladder.
     */
    fun workoutCrossings(sets: List<LoggedSet>, kindOf: (Long) -> ExerciseKind?, rootOf: (Long) -> Long): List<Crossing> {
        val totals = mutableMapOf<Pair<Kind, Long>, Int>()
        for (set in sets) {
            val kind = when (kindOf(set.exerciseId)) {
                ExerciseKind.REPS -> Kind.REPS
                ExerciseKind.SECONDS -> Kind.SECONDS
                null -> continue
            }
            val key = kind to rootOf(set.exerciseId)
            totals[key] = (totals[key] ?: 0) + set.value
        }
        val workouts = sets.map { it.sessionId }.distinct().size
        return totals.flatMap { (k, total) -> reached(k.first, k.second, total) } + reached(Kind.WORKOUTS, null, workouts)
    }

    /** The crossings not noted yet. */
    fun new(all: List<Crossing>, noted: Set<String>): List<Crossing> = all.filter { it.key !in noted }

    /** When several values of one total were crossed at once, only the highest is worth saying. */
    fun highestEach(crossings: List<Crossing>): List<Crossing> =
        crossings.groupBy { it.kind to it.ladder }.values.map { group -> group.maxBy { it.value } }
}
