package app.lantern.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Every color the app draws with (DESIGN.md → Tokens). Code reads these tokens, never literals.
 * The theme comes from the picked character (D187); done stays green and due stays amber in every theme.
 */
@Immutable
data class BaColors(
    val isDark: Boolean,
    /** The night sky: [bg] at the edges, [sky2] where it glows. */
    val bg: Color,
    val sky2: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    /** The character's color: the date, the dialog box, «الان», buttons, the current part. Never "done". */
    val glow: Color,
    /** Text and icons on a filled [glow]. */
    val onGlow: Color,
    /** The dialog box's two inner borders (D185): a light rim and a deep one. */
    val rimLight: Color,
    val rimDeep: Color,
    /** Done and progress. Always green. */
    val accent: Color,
    val accentSubtle: Color,
    val onAccentSubtle: Color,
    /** Text and icons on a filled accent background. */
    val onAccent: Color,
    /** Dashboard and week-dot levels 0–4; level 0 is the gray gap. */
    val grid: List<Color>,
    /** Due and important («موعدش رسیده», «مهم»). */
    val amber: Color,
    val amberSubtle: Color,
    /** Special days and the minimum day. */
    val special: Color,
    val specialSubtle: Color,
    val onSpecialSubtle: Color,
)

/** One character's palette (D187), the same values as the mockups. */
private data class Palette(
    val sky1: Long, val sky2: Long, val glow: Long, val box1: Long, val box2: Long,
    val rimDeep: Long, val rimLight: Long, val ink: Long, val t2: Long, val t3: Long,
)

private val palettes = mapOf(
    LanternCharacter.KNIGHT to Palette(0xFF0B1622, 0xFF12283A, 0xFF5FD3E6, 0xFF0F2232, 0xFF0A1520, 0xFF2A6478, 0xFFCDEFF6, 0xFF06202A, 0xFFA9BCCB, 0xFF6F8799),
    LanternCharacter.VAMPIRE to Palette(0xFF14070B, 0xFF2E0F17, 0xFFFF5A6A, 0xFF2A1016, 0xFF17080C, 0xFF7A2433, 0xFFFFD6DC, 0xFF2A0509, 0xFFCBAAB0, 0xFF92707A),
    LanternCharacter.WIZARD to Palette(0xFF0E1013, 0xFF23272D, 0xFFCFD6DD, 0xFF23272E, 0xFF121417, 0xFF5B636E, 0xFFEEF2F6, 0xFF15181C, 0xFFB4BAC2, 0xFF7C838C),
    LanternCharacter.GOTH to Palette(0xFF100A1C, 0xFF24173C, 0xFFB98CFF, 0xFF21153A, 0xFF120B20, 0xFF563A8A, 0xFFE7DBFF, 0xFF1A0B33, 0xFFBDB0D2, 0xFF85799C),
)

fun lanternColors(character: LanternCharacter): BaColors {
    val p = palettes.getValue(character)
    val gap = 0xFF2C333A
    return BaColors(
        isDark = true,
        bg = Color(p.sky1),
        sky2 = Color(p.sky2),
        surface = Color(p.box2),
        surface2 = Color(p.box1),
        border = Color(0x29FFFFFF),
        text1 = Color(0xFFF2F7FA),
        text2 = Color(p.t2),
        text3 = Color(p.t3),
        glow = Color(p.glow),
        onGlow = Color(p.ink),
        rimLight = Color(p.rimLight),
        rimDeep = Color(p.rimDeep),
        accent = Color(0xFF3FBF7F),
        accentSubtle = Color(0xFF1E4232),
        onAccentSubtle = Color(0xFF6FE0A6),
        onAccent = Color(0xFF06210F),
        grid = listOf(Color(gap), Color(0xFF23513A), Color(0xFF28754E), Color(0xFF2FA066), Color(0xFF5BD994)),
        amber = Color(0xFFF2B84B),
        amberSubtle = Color(0xFF4C4630),
        special = Color(0xFF5560C9),
        specialSubtle = Color(0xFF313953),
        onSpecialSubtle = Color(0xFFA3AAF2),
    )
}
