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
