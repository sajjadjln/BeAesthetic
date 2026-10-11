package app.beaesthetic.domain.buddy

import app.beaesthetic.domain.time.Part
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The 17 poses of «پیکر» (D82), drawn by the rig in `:app`. */
enum class Pose { FLEX, WAVE, CHEER, THUMBS, POINT, STRETCH, TEA, SLEEP, THINK, SELFIE, HUG, LETTER, READ, WOW, CURL, SABZEH, FINISH }

/** One outfit per chapter, from the calendar only (D83). Nothing is earned or lost. */
enum class Outfit { NONE, BAND, WINTER, SHADES, SPROUT }

/** What a line is about. Each moment has its own pool of lines in string resources. */
enum class Moment { MORNING, START, TICK, PART_DONE, DAY_DONE, COMEBACK, MINIMUM, SPECIAL, NIGHT, TAP, CHAPTER_START, LETTER_DAY }

/** What Today knows about the day, for the character. */
data class TodayState(
    val minimumMode: Boolean,
    val special: Boolean,
    /** A comeback day (D53) whose first tick has just happened. */
    val comebackFirstTick: Boolean,
    val done: Int,
    val total: Int,
    val selectedPartDone: Boolean,
    val currentPart: Part,
    val nightDone: Boolean,
)

data class BuddyChoice(val pose: Pose, val moment: Moment)

object BuddyPicker {

    /** Lines shown in the last 7 days are skipped while another line is left (D84). */
    const val NO_REPEAT_DAYS = 7L

    /**
     * Pose and moment from the state of the day (SPEC §6.8), in this order: minimum or special day → tea;
     * comeback → welcome; 100% → flex; night done → sleep; the selected part done → cheer; no tick yet →
     * stretch in the morning, a wave later; otherwise thumbs up with the real count.
     * Never a sad pose, and nothing about a missed day: there is no such moment.
     */
    fun today(state: TodayState): BuddyChoice = when {
        state.special -> BuddyChoice(Pose.TEA, Moment.SPECIAL)
        state.minimumMode -> BuddyChoice(Pose.TEA, Moment.MINIMUM)
        state.comebackFirstTick -> BuddyChoice(Pose.HUG, Moment.COMEBACK)
        state.total > 0 && state.done == state.total -> BuddyChoice(Pose.FLEX, Moment.DAY_DONE)
        state.currentPart == Part.NIGHT && state.nightDone -> BuddyChoice(Pose.SLEEP, Moment.NIGHT)
        state.done > 0 && state.selectedPartDone -> BuddyChoice(Pose.CHEER, Moment.PART_DONE)
        state.done == 0 && state.currentPart == Part.MORNING -> BuddyChoice(Pose.STRETCH, Moment.MORNING)
        state.done == 0 -> BuddyChoice(Pose.WAVE, Moment.START)
        else -> BuddyChoice(Pose.THUMBS, Moment.TICK)
    }

    /**
     * Which line of a pool of [poolSize] to show today. [lastShown] maps a line index to the last day it was
     * shown. Lines shown in the last [NO_REPEAT_DAYS] days are skipped while another is left; among the rest
     * the least recently shown wins (never shown first), then the lowest index.
     */
    fun line(poolSize: Int, lastShown: Map<Int, LocalDate>, today: LocalDate): Int {
        require(poolSize > 0)
        fun recent(i: Int) = lastShown[i]?.let { ChronoUnit.DAYS.between(it, today) < NO_REPEAT_DAYS } ?: false
        val candidates = (0 until poolSize).filterNot(::recent).ifEmpty { (0 until poolSize).toList() }
        return candidates.minWith(compareBy<Int>({ lastShown[it] ?: LocalDate.MIN }, { it }))
    }

    /** ① none · ② sweatband · ③ beanie and scarf · ④ sunglasses · ⑤ sprout. */
    fun outfit(chapterNumber: Int): Outfit = when (chapterNumber) {
        2 -> Outfit.BAND
        3 -> Outfit.WINTER
        4 -> Outfit.SHADES
        5 -> Outfit.SPROUT
        else -> Outfit.NONE
    }
}
