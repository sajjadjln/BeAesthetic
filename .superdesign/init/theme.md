# Theme

## Part 1 — token summary

Stack: native Android, Kotlin, **Jetpack Compose** (Material 3 base, custom `Ba` theme), Navigation Compose with type-safe routes. No web framework, no Tailwind, no CSS. The HTML mockups in `docs/design/*.html` mirror the Compose UI with CSS tokens and are the design reference.

Dark first ("slate", D136); accent chosen by the user, green by default. Values for dark / light:

| Token | Dark | Light |
|---|---|---|
| bg | #1B201E | #F6F7F6 |
| surface | #252B29 | #FFFFFF |
| surface-2 | #2F3633 | #EDF0EE |
| border | #3C4441 | #DCE1DE |
| text-1/2/3 | #EEF1EF / #A1ABA7 / #828C88 | #0E1110 / #5F6965 / #8A948F |
| accent (green) | #34C47A | #1D8A55 |
| accent-subtle | #29503C | #E4F7EC |
| on-accent-subtle | #5BD994 | #176B43 |
| on-accent | #0E1110 | #FFFFFF |
| grid 0–4 | #3C4441 #23513A #28754E #2FA066 #5BD994 | #DCE1DE #C4F0D6 #93E6B6 #22A565 #176B43 |
| amber / subtle | #E8B04B / #4C4630 | #B07610 / #FBEFD6 |
| special / subtle / on | #5560C9 / #313953 / #A3AAF2 | #8C95EA / #D3D7F8 / #3E479E |

Font: Estedad (400–800). Type: display 52/800, headline 26/800, title 18/700, subtitle 16/600, body 15/400, tile 15/600, label 13/500, caption 12/400.
Spacing 4·8·12·16·24·32. Radius 6 cells · 12 controls · 16 cards · 20 tiles · 28 sheets · full. No shadows on cards. No red anywhere.
The same tokens as CSS variables: `:root` block of `.superdesign/replica_html_template/today.html` (--bg, --sf, --sf2, --bd, --t1..3, --acc, --acc-sub, --acc-on, --on-acc, --w0..4, --nt, --nt-sub, --sp…).

## Part 2 — raw sources

### `app/src/main/kotlin/app/beaesthetic/ui/theme/Colors.kt`

```kotlin
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
    Accent.GREEN to AccentPalette(0xFF34C47A, 0xFF29503C, 0xFF5BD994, listOf(0xFF23513A, 0xFF28754E, 0xFF2FA066, 0xFF5BD994)),
    Accent.BLUE to AccentPalette(
        0xFF4D9BF0, 0xFF2F4659, 0xFF82BBFA, listOf(0xFF2A455D, 0xFF35608B, 0xFF4280C2, 0xFF82BBFA),
        special = Triple(0xFF9A66D4, 0xFF433A55, 0xFFC6A2F0),
    ),
    Accent.TURQUOISE to AccentPalette(0xFF2CC2B4, 0xFF274F4A, 0xFF62DCCF, listOf(0xFF20514B, 0xFF24746C, 0xFF289E93, 0xFF62DCCF)),
    Accent.VIOLET to AccentPalette(
        0xFF9B7BEA, 0xFF413E57, 0xFFC2AAFA, listOf(0xFF413B5B, 0xFF5E4F88, 0xFF7F67BD, 0xFFC2AAFA),
        special = Triple(0xFF3A9FC2, 0xFF2A4951, 0xFF7CCBE4),
    ),
    Accent.ORANGE to AccentPalette(
        0xFFF28B3C, 0xFF56422E, 0xFFFFB27A, listOf(0xFF5C4027, 0xFF8B582E, 0xFFC37335, 0xFFFFB27A),
        amber = 0xFFE3D04E to 0xFF4B4C30,
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
    val special = p.special ?: if (dark) Triple(0xFF5560C9, 0xFF313953, 0xFFA3AAF2) else Triple(0xFF8C95EA, 0xFFD3D7F8, 0xFF3E479E)
    val amber = p.amber ?: if (dark) 0xFFE8B04B to 0xFF4C4630 else 0xFFB07610 to 0xFFFBEFD6
    val gap = if (dark) 0xFF3C4441 else 0xFFDCE1DE
    return BaColors(
        isDark = dark,
        bg = Color(if (dark) 0xFF1B201E else 0xFFF6F7F6),
        surface = Color(if (dark) 0xFF252B29 else 0xFFFFFFFF),
        surface2 = Color(if (dark) 0xFF2F3633 else 0xFFEDF0EE),
        border = Color(gap),
        text1 = Color(if (dark) 0xFFEEF1EF else 0xFF0E1110),
        text2 = Color(if (dark) 0xFFA1ABA7 else 0xFF5F6965),
        text3 = Color(if (dark) 0xFF828C88 else 0xFF8A948F),
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
```

### `app/src/main/kotlin/app/beaesthetic/ui/theme/Theme.kt`

```kotlin
package app.beaesthetic.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.beaesthetic.R

/** Estedad is the only font (D47), bundled in the APK. */
val Estedad = FontFamily(
    Font(R.font.estedad_regular, FontWeight.Normal),
    Font(R.font.estedad_medium, FontWeight.Medium),
    Font(R.font.estedad_semibold, FontWeight.SemiBold),
    Font(R.font.estedad_bold, FontWeight.Bold),
    Font(R.font.estedad_extrabold, FontWeight.ExtraBold),
)

/** The type scale from DESIGN.md. */
@Immutable
data class BaType(
    val display: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 56.sp),
    val headline: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp),
    val title: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    val subtitle: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    val body: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    val tile: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    val label: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    val caption: TextStyle = TextStyle(fontFamily = Estedad, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
)

/**
 * Motion tokens (D76, DESIGN.md → Motion). Compose's duration scale follows the phone's
 * "Remove animations" setting, so all of these become instant there.
 */
object BaMotion {
    const val TAP = 100
    const val SHORT = 200
    const val MEDIUM = 350
    const val LONG = 600
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> spring(): SpringSpec<T> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
    fun <T> settle(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
}

val LocalBaColors = staticCompositionLocalOf { baColors(Accent.GREEN, dark = true) }
val LocalBaType = staticCompositionLocalOf { BaType() }

object Ba {
    val colors: BaColors @Composable get() = LocalBaColors.current
    val type: BaType @Composable get() = LocalBaType.current
}

enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun PeykarTheme(
    accent: Accent = Accent.GREEN,
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colors = baColors(accent, dark)
    // M3 components (sheets, switches) read colorScheme; map it from our tokens so they match.
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.text1,
            surface = colors.surface, onSurface = colors.text1, surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.text2, outline = colors.border, error = colors.amber,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.text1,
            surface = colors.surface, onSurface = colors.text1, surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.text2, outline = colors.border, error = colors.amber,
        )
    }
    val type = BaType()
    CompositionLocalProvider(LocalBaColors provides colors, LocalBaType provides type) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.copy(bodyLarge = type.body), content = content)
    }
}
```
