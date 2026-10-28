package app.lantern.ui.today

import app.lantern.domain.time.Part
import app.lantern.ui.theme.Mood

/** Pure rules for the list layout (D186), kept apart so they're unit-tested. */
object TodayPick {

    /**
     * The character's look follows the day, never a miss (D186): happy when everything is done, calm at
     * night (and before the first tick), busy once something is ticked.
     */
    fun mood(done: Int, total: Int, part: Part): Mood = when {
        total > 0 && done == total -> Mood.HAPPY
        part == Part.NIGHT -> Mood.CALM
        done > 0 -> Mood.BUSY
        else -> Mood.CALM
    }

    /** Which pool of lines the character speaks from. Night has its own lines with the calm look. */
    enum class Lines { CALM, BUSY, HAPPY, NIGHT }

    fun lines(done: Int, total: Int, part: Part): Lines = when {
        total > 0 && done == total -> Lines.HAPPY
        part == Part.NIGHT -> Lines.NIGHT
        done > 0 -> Lines.BUSY
        else -> Lines.CALM
    }

    /**
     * «الان»: the first open task in the current part, else in a later part, else one left from an
     * earlier part (no shame: it is simply still open). Null when everything is done.
     */
    fun now(tiles: Map<Part, List<TileUi>>, current: Part): TileUi? {
        val order = Part.entries.drop(current.ordinal) + Part.entries.take(current.ordinal)
        return order.firstNotNullOfOrNull { part -> tiles[part].orEmpty().firstOrNull { !it.done } }
    }
}
