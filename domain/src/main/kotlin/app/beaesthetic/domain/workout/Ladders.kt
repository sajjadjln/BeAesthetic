package app.beaesthetic.domain.workout

/**
 * Ladders (D70) as chains of «حرکت سخت‌تر» links (D119): each move points to its next harder variant. A move
 * has at most one easier move; the repository keeps it that way and refuses loops ([makesLoop]).
 */
object Ladders {

    /** The chain [id] is on, easiest first. A move without links is a chain of one. Safe against loops. */
    fun chain(id: Long, harderOf: Map<Long, Long>): List<Long> {
        val easierOf = harderOf.entries.associate { (easier, harder) -> harder to easier }
        val seen = mutableSetOf(id)
        val down = ArrayDeque<Long>()
        var cur = id
        while (true) {
            val e = easierOf[cur] ?: break
            if (!seen.add(e)) break
            down.addFirst(e)
            cur = e
        }
        val up = mutableListOf<Long>()
        cur = id
        while (true) {
            val h = harderOf[cur] ?: break
            if (!seen.add(h)) break
            up += h
            cur = h
        }
        return down + id + up
    }

    /** The easiest move of [id]'s chain: the key its totals and round numbers are kept under. */
    fun root(id: Long, harderOf: Map<Long, Long>): Long = chain(id, harderOf).first()

    /** Linking [harder] as [id]'s next would close a loop: it's [id] itself or one of its easier moves. */
    fun makesLoop(id: Long, harder: Long, harderOf: Map<Long, Long>): Boolean =
        harder == id || harder in chain(id, harderOf).takeWhile { it != id }
}

/** What the end of a session offers for one move (D120). */
sealed interface Offer {
    val exerciseId: Long

    /** «پله بعد: [harder]؟» */
    data class NextRung(override val exerciseId: Long, val harder: Exercise) : Offer

    /** «بازه بشه [from]–[to]؟» */
    data class RaiseRange(override val exerciseId: Long, val from: Int, val to: Int) : Offer
}

object Progression {

    /** Every planned set of [item] was logged, each at the top of its range or above. «حداکثر» has no top. */
    fun hitTop(item: PlanItem, sessionSets: List<LoggedSet>): Boolean {
        val top = item.rangeMax ?: return false
        val mine = sessionSets.filter { it.exerciseId == item.exercise.id && it.setIndex < item.sets }
        return mine.size >= item.sets && mine.all { it.value >= top }
    }

    /**
     * The offers after a session, in plan order. A harder variant wins over raising the range; if that variant
     * is already in the day, there's nothing to offer (a move is in a day once).
     */
    fun offers(items: List<PlanItem>, sessionSets: List<LoggedSet>, harderOf: (Long) -> Exercise?): List<Offer> =
        items.filter { hitTop(it, sessionSets) }.mapNotNull { item ->
            val harder = harderOf(item.exercise.id)
            when {
                harder != null && items.none { it.exercise.id == harder.id } -> Offer.NextRung(item.exercise.id, harder)
                harder != null -> null
                else -> {
                    val step = WorkoutRules.step(item.exercise.kind)
                    val max = PlanForms.maxValue(item.exercise.kind)
                    val to = (item.rangeMax!! + step).coerceAtMost(max)
                    if (to == item.rangeMax) null else Offer.RaiseRange(item.exercise.id, (item.rangeMin!! + step).coerceAtMost(to), to)
                }
            }
        }
}
