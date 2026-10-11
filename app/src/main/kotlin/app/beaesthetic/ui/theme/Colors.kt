package app.beaesthetic.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The user-chosen accent (D47). It always means done or progress, and nothing else uses it. */
enum class Accent { GREEN, BLUE, TURQUOISE, VIOLET, ORANGE }

/**
 * Every color the app draws with (DESIGN.md → Tokens). Code reads these tokens, never literals:
 * no screen says "green".
 */
@Immutable
data class BaColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentSubtle: Color,
    val onAccentSubtle: Color,
    /** Text and icons on a filled accent background. */
    val onAccent: Color,
    /** Dashboard and week-dot levels 0–4; level 0 is the gray gap. */
    val grid: List<Color>,
    /** Due and important («موعدش رسیده», «مهم»). Amber, or yellow with the orange accent. */
    val amber: Color,
    val amberSubtle: Color,
    /** Special days and the minimum day. Indigo, shifted for blue and violet. */
    val special: Color,
    val specialSubtle: Color,
    val onSpecialSubtle: Color,
)

private data class AccentPalette(
    val accent: Long,
    val subtle: Long,
    val onSubtle: Long,
    val grid: List<Long>,
    val special: Triple<Long, Long, Long>? = null,
    val amber: Pair<Long, Long>? = null,
)

private val darkPalettes = mapOf(
    Accent.GREEN to AccentPalette(0xFF34C47A, 0xFF0F2E1F, 0xFF5BD994, listOf(0xFF114029, 0xFF176B43, 0xFF22A565, 0xFF5BD994)),
    Accent.BLUE to AccentPalette(
        0xFF4D9BF0, 0xFF0F2440, 0xFF82BBFA, listOf(0xFF112E52, 0xFF1A4D8A, 0xFF2F7AD6, 0xFF82BBFA),
        special = Triple(0xFF9A66D4, 0xFF2B1C42, 0xFFC6A2F0),
    ),
    Accent.TURQUOISE to AccentPalette(0xFF2CC2B4, 0xFF0C2E2B, 0xFF62DCCF, listOf(0xFF0E3C38, 0xFF13675F, 0xFF1EA195, 0xFF62DCCF)),
    Accent.VIOLET to AccentPalette(
        0xFF9B7BEA, 0xFF241B3E, 0xFFC2AAFA, listOf(0xFF2B2150, 0xFF46358A, 0xFF7458D0, 0xFFC2AAFA),
        special = Triple(0xFF3A9FC2, 0xFF0E2A33, 0xFF7CCBE4),
    ),
    Accent.ORANGE to AccentPalette(
        0xFFF28B3C, 0xFF3A220F, 0xFFFFB27A, listOf(0xFF3D2410, 0xFF6E3D16, 0xFFC2661F, 0xFFFFB27A),
        amber = 0xFFE3D04E to 0xFF302C0B,
    ),
)

private val lightPalettes = mapOf(
    Accent.GREEN to AccentPalette(0xFF1D8A55, 0xFFE4F7EC, 0xFF176B43, listOf(0xFFC4F0D6, 0xFF93E6B6, 0xFF22A565, 0xFF176B43)),
    Accent.BLUE to AccentPalette(
        0xFF1F6BD0, 0xFFE2EDFC, 0xFF1A56A8, listOf(0xFFCDE0FA, 0xFF98C1F3, 0xFF3C86E0, 0xFF1A56A8),
        special = Triple(0xFFB08BE6, 0xFFEADCFA, 0xFF6A3DAE),
    ),
    Accent.TURQUOISE to AccentPalette(0xFF0E8379, 0xFFDDF4F1, 0xFF0A665E, listOf(0xFFC0ECE7, 0xFF8BDCD3, 0xFF1FA195, 0xFF0A665E)),
    Accent.VIOLET to AccentPalette(
        0xFF6644C8, 0xFFEDE7FC, 0xFF5233AA, listOf(0xFFDCD1FA, 0xFFB8A4F2, 0xFF8466DE, 0xFF5233AA),
        special = Triple(0xFF6CC0DC, 0xFFD4EEF6, 0xFF1D6A84),
    ),
    Accent.ORANGE to AccentPalette(
        0xFFC2570F, 0xFFFCEADB, 0xFF9A440C, listOf(0xFFFBD9BE, 0xFFF6B485, 0xFFE07B2E, 0xFF9A440C),
        amber = 0xFF776804 to 0xFFF6F0C6,
    ),
)

fun baColors(accent: Accent, dark: Boolean): BaColors {
    val p = (if (dark) darkPalettes else lightPalettes).getValue(accent)
    val special = p.special ?: if (dark) Triple(0xFF5560C9, 0xFF1F2448, 0xFF8C95EA) else Triple(0xFF8C95EA, 0xFFD3D7F8, 0xFF3E479E)
    val amber = p.amber ?: if (dark) 0xFFE8B04B to 0xFF36280D else 0xFFB07610 to 0xFFFBEFD6
    val gap = if (dark) 0xFF262C2A else 0xFFDCE1DE
    return BaColors(
        isDark = dark,
        bg = Color(if (dark) 0xFF0E1110 else 0xFFF6F7F6),
        surface = Color(if (dark) 0xFF151918 else 0xFFFFFFFF),
        surface2 = Color(if (dark) 0xFF1C211F else 0xFFEDF0EE),
        border = Color(gap),
        text1 = Color(if (dark) 0xFFEDF0EE else 0xFF0E1110),
        text2 = Color(if (dark) 0xFF909A96 else 0xFF5F6965),
        text3 = Color(if (dark) 0xFF6B7571 else 0xFF8A948F),
        accent = Color(p.accent),
        accentSubtle = Color(p.subtle),
        onAccentSubtle = Color(p.onSubtle),
        // Dark mode: dark text on the bright accent; light mode: white on the deep accent (contrast ≥ 4.3:1).
        onAccent = Color(if (dark) 0xFF0E1110 else 0xFFFFFFFF),
        grid = listOf(Color(gap)) + p.grid.map(::Color),
        amber = Color(amber.first),
        amberSubtle = Color(amber.second),
        special = Color(special.first),
        specialSubtle = Color(special.second),
        onSpecialSubtle = Color(special.third),
    )
}
