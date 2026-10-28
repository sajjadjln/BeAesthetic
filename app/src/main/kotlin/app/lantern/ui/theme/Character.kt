package app.lantern.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.lantern.R

/** The four characters (D184); the picked one themes the app (D187). */
enum class LanternCharacter(@StringRes val nameRes: Int) {
    KNIGHT(R.string.char_knight),
    VAMPIRE(R.string.char_vampire),
    WIZARD(R.string.char_wizard),
    GOTH(R.string.char_goth);

    companion object {
        /** The stored name, or the knight for anything unknown. */
        fun of(name: String?): LanternCharacter = entries.firstOrNull { it.name == name } ?: KNIGHT
    }
}

/** The looks Today uses (D186): never nervous, cross or asleep. Night uses [CALM]. */
enum class Mood { CALM, BUSY, HAPPY }

/** The portrait for a mood, cut from the character's sheet by `scripts/cut-character.swift`. */
@DrawableRes
fun LanternCharacter.art(mood: Mood): Int = when (this) {
    LanternCharacter.KNIGHT -> when (mood) { Mood.CALM -> R.drawable.char_knight_calm; Mood.BUSY -> R.drawable.char_knight_busy; Mood.HAPPY -> R.drawable.char_knight_happy }
    LanternCharacter.VAMPIRE -> when (mood) { Mood.CALM -> R.drawable.char_vampire_calm; Mood.BUSY -> R.drawable.char_vampire_busy; Mood.HAPPY -> R.drawable.char_vampire_happy }
    LanternCharacter.WIZARD -> when (mood) { Mood.CALM -> R.drawable.char_wizard_calm; Mood.BUSY -> R.drawable.char_wizard_busy; Mood.HAPPY -> R.drawable.char_wizard_happy }
    LanternCharacter.GOTH -> when (mood) { Mood.CALM -> R.drawable.char_goth_calm; Mood.BUSY -> R.drawable.char_goth_busy; Mood.HAPPY -> R.drawable.char_goth_happy }
}

/** Today's layout (D188): one list, or the parts-of-the-day slider. */
enum class TodayLayout {
    LIST, PARTS;

    companion object {
        fun of(name: String?): TodayLayout = entries.firstOrNull { it.name == name } ?: LIST
    }
}
