package app.lantern.ui.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import app.lantern.R
import app.lantern.ui.theme.rememberAnimationsOn
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos

/*
 * The splash (D219, docs/design/splash-smooth.html): a castle on a hill under a big moon, in the knight's
 * colors, drawn with smooth shapes. The scene is laid out on a 108-wide grid, 234 tall; a taller screen
 * gets more sky on top. Times are in seconds from the start (the mockup's CSS timeline).
 */

/** When the last beat (the shine over the name) is over. */
private const val END = 4.7f

private val Void = Color(0xFF050B12)
private val Cyan = Color(0xFF5FD3E6)
private val Glow = Color(0xFF8FE6F2)
private val Pale = Color(0xFFC4F3FA)
private val Ember = Color(0xFF1B3D55)
private val Teal = Color(0xFF25637A)
private val Mid = Color(0xFF3FA6BD)
private val Dusk = Color(0xFF12283A)
private val Ink = Color(0xFF03070C)

private val Out = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
private val InOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** 0 before [start], 1 after [start] + [dur], eased between. */
private fun p(t: Float, start: Float, dur: Float, e: Easing = Out) = e.transform(((t - start) / dur).coerceIn(0f, 1f))

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

private const val ROAD = "M60 234 C 30 228, 16 220, 34 212 S 76 200, 52 192 S 26 180, 46 172 S 66 162, 58 158 L 54 156"
private const val LAND = "M0 192 L8 182 L14 188 L22 170 L30 166 L40 160 L54 158 L68 160 L76 164 L82 154 L86 162 L94 170 L100 166 L108 180 V234 H0z"
private const val RIDGE = "M22 170 L30 166 L40 160 L54 158 L68 160 L76 164 L82 154 L86 162"
private val SHARDS = listOf("M12 204 l7-5 -2 6z", "M88 194 l9-6 -1 7z", "M80 214 l8-5 -2 6z", "M94 222 l7-4 -1 5z")
private val CLOUDS_L = listOf(
    "M-6 96 q4-7 10-5 q4-8 12-4 q6-3 10 3 q6 0 8 6 h22 q-30 2-62 0z",
    "M-10 140 q5-6 10-4 q5-7 11-3 q6 0 8 5 h20 q-28 2-49 2z",
    "M-4 176 q4-5 9-3 q4-5 9-2 q5 0 6 5 h14 q-20 2-38 0z",
)
private val CLOUDS_R = listOf(
    "M116 118 q-4-7-10-5 q-5-8-13-4 q-6-3-10 3 q-6 0-8 6 h-22 q32 2 63 0z",
    "M114 162 q-4-6-10-4 q-5-7-11-3 q-6 0-8 5 h-18 q26 2 47 2z",
)
private const val BAT = "M0 0 q2-2 4 0 q2-2 4 0 q-2 0-4 1.5 q-2-1.5-4-1.5z"
/** x, width, top of the wall, spire height. */
private val TOWERS = listOf(
    intArrayOf(28, 7, 128, 18), intArrayOf(36, 4, 138, 10), intArrayOf(40, 8, 116, 20), intArrayOf(48, 12, 106, 30),
    intArrayOf(60, 8, 118, 18), intArrayOf(68, 4, 136, 10), intArrayOf(72, 8, 130, 18), intArrayOf(44, 3, 114, 8),
    intArrayOf(58, 3, 112, 8),
)
private val WINDOWS = listOf(31 to 136, 31 to 143, 43 to 124, 43 to 134, 53 to 116, 55 to 116, 54 to 126, 64 to 126,
    64 to 136, 75 to 138, 75 to 145, 37 to 146, 69 to 144)
private val CRATERS = listOf(floatArrayOf(44f, 98f, 7f, 5f), floatArrayOf(66f, 104f, 5f, 4f), floatArrayOf(58f, 124f, 9f, 5f),
    floatArrayOf(38f, 118f, 4f, 3f), floatArrayOf(70f, 92f, 3f, 2.4f), floatArrayOf(50f, 136f, 5f, 2.6f))

private class Shapes {
    val road = path(ROAD)
    val roadMeasure = PathMeasure().apply { setPath(road, false) }
    val land = path(LAND)
    val ridge = path(RIDGE)
    val shards = SHARDS.map(::path)
    val cloudsL = CLOUDS_L.map(::path)
    val cloudsR = CLOUDS_R.map(::path)
    val bat = path(BAT)
    val castle = Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(30f, 142f, 78f, 166f))
        for ((x, w, top, s) in TOWERS) {
            addRect(androidx.compose.ui.geometry.Rect(x.toFloat(), top.toFloat(), (x + w).toFloat(), 166f))
            addRect(androidx.compose.ui.geometry.Rect(x - 0.6f, top.toFloat(), x + w + 0.6f, top + 1f))
            moveTo(x - 0.3f, top.toFloat()); lineTo(x + w / 2f, (top - s).toFloat()); lineTo(x + w + 0.3f, top.toFloat()); close()
        }
    }
    val archWindow = path("M52 134 v-5 q2-3 4 0 v5z")
    val door = path("M52 156 v-7 q2-3 4 0 v7z")
}

/**
 * The moon rises, clouds drift in, the hill and castle rise, a path draws itself up to the door with a small
 * light walking it, the windows light one by one, the door glows, then the app's name fades in at the top
 * and a shine passes over it. About 5 s, on every cold start; a tap skips it. With "Remove animations" on,
 * the finished scene shows for a moment instead.
 */
@Composable
fun Splash(onDone: () -> Unit) {
    val animate = rememberAnimationsOn()
    val name = stringResource(R.string.app_name)
    val shapes = remember { Shapes() }
    val measurer = rememberTextMeasurer()
    val font = remember { FontFamily(Font(R.font.estedad_extrabold)) }
    var t by remember { mutableFloatStateOf(if (animate) 0f else END + 2f) }

    LaunchedEffect(Unit) {
        if (!animate) { delay(900); onDone(); return@LaunchedEffect }
        // The first frames carry the whole app composing underneath; start the clock after them.
        repeat(2) { withFrameMillis { } }
        val start = withFrameMillis { it }
        while (t < END) t = (withFrameMillis { it } - start) / 1000f
        delay(300)
        onDone()
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .background(Void)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .semantics { contentDescription = name },
    ) {
        val s = size.width / 108f
        val rows = size.height / s
        val shift = (rows - 234f).coerceAtLeast(0f)

        // Sky: a radial night, brightest behind the moon.
        drawRect(
            Brush.radialGradient(
                0f to Ember, 0.45f to Dusk, 1f to Void,
                center = Offset(54f * s, (112f + shift) * s), radius = 0.7f * 234f * s,
            ),
            alpha = p(t, 0f, 1.1f, InOut),
        )
        stars(t, s, shift)
        title(t, s, shift, name, measurer, font)

        withTransform({ translate(0f, shift * s); scale(s, s, Offset.Zero) }) {
            moon(t)
            clouds(t, shapes)
            bats(t, shapes)
            translate(0f, 22f * (1 - p(t, 0.7f, 1f))) { land(t, shapes) }
        }
    }
}

private fun rand(n: Int): Float { val v = kotlin.math.sin(n * 12.9898f + 78.233f) * 43758.547f; return v - kotlin.math.floor(v) }

private fun DrawScope.stars(t: Float, s: Float, shift: Float) {
    for (i in 0 until 34) {
        val on = p(t, 1f + (i % 7) * 0.08f, 1f, InOut)
        if (on <= 0f) continue
        val tw0 = 2f + (i % 5) * 0.4f
        val tw = if (t > tw0) 1f - 0.65f * (0.5f - 0.5f * cos(2 * PI.toFloat() * (t - tw0) / 3f)) else 1f
        // Scattered, not on a line: a fixed pseudo-random place per star.
        val x = rand(i * 2) * 108f
        val y = rand(i * 2 + 1) * (70f + shift) + 4f
        drawCircle(Color(0xFFCFEAF2), (if (i % 5 == 0) 0.6f else 0.35f) * s, Offset(x * s, y * s), alpha = on * tw)
    }
}

private fun DrawScope.title(t: Float, s: Float, shift: Float, name: String, measurer: androidx.compose.ui.text.TextMeasurer, font: FontFamily) {
    val a = p(t, 3.05f, 0.9f)
    if (a <= 0f) return
    val fontSize = (22f * s).toSp()
    val style = TextStyle(
        fontFamily = font, fontSize = fontSize,
        brush = Brush.verticalGradient(listOf(Color(0xFFE6FBFF), Cyan)),
        shadow = Shadow(Cyan.copy(alpha = 0.8f), Offset.Zero, 4f * s),
    )
    val layout = measurer.measure(name, style)
    val top = (40f - 22f * 0.95f + shift / 2f + 5f * (1 - a)) * s
    val left = (size.width - layout.size.width) / 2f
    drawText(layout, topLeft = Offset(left, top), alpha = a)
    // One shine across the name.
    val sh = p(t, 3.8f, 0.9f, InOut)
    if (sh in 0.001f..0.999f) {
        val cx = left - 20f * s + (layout.size.width + 40f * s) * sh
        val band = 8f * s
        val shine = measurer.measure(
            name,
            style.copy(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                    startX = cx - band - left, endX = cx + band - left,
                ),
                shadow = null,
            ),
        )
        drawText(shine, topLeft = Offset(left, top))
    }
}

private fun DrawScope.moon(t: Float) {
    val a = p(t, 0.05f, 1.3f)
    translate(0f, 34f * (1 - a)) {
        drawCircle(Brush.radialGradient(0.55f to Cyan.copy(alpha = 0.35f), 1f to Cyan.copy(alpha = 0f), center = Offset(54f, 112f), radius = 44f), 44f, Offset(54f, 112f), alpha = a)
        drawCircle(
            Brush.radialGradient(0f to Color(0xFFB9F1F8), 0.55f to Cyan, 1f to Color(0xFF2F97AD), center = Offset(54f - 34f * 0.2f, 112f - 34f * 0.24f), radius = 34f * 1.4f),
            34f, Offset(54f, 112f), alpha = a,
        )
        for ((cx, cy, rx, ry) in CRATERS) drawOval(Color(0xFF2F97AD), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), alpha = 0.35f * a)
    }
}

private fun DrawScope.clouds(t: Float, sh: Shapes) {
    val a = p(t, 0.4f, 1.1f)
    if (a <= 0f) return
    fun drift(period: Float) = if (t > 1.5f) 5f * (0.5f - 0.5f * cos(PI.toFloat() * (t - 1.5f) / period)) else 0f
    translate(-14f * (1 - a) + drift(9f), 0f) { sh.cloudsL.forEach { drawPath(it, Ember, alpha = a) } }
    translate(14f * (1 - a) - drift(11f), 0f) { sh.cloudsR.forEach { drawPath(it, Ember, alpha = a) } }
}

private fun DrawScope.bats(t: Float, sh: Shapes) {
    if (t < 1.9f) return
    val a = p(t, 1.9f, 0.4f, InOut)
    fun bat(x: Float, y: Float) {
        val flap = ((t / 0.14f).toInt() % 2 == 0)
        withTransform({ translate(x, y); if (flap) scale(1f, -0.6f, Offset(4f, 0.5f)) }) { drawPath(sh.bat, Ink, alpha = a) }
    }
    val u1 = ((t - 1.9f) / 7f) % 1f
    bat(-10f + 130f * u1, 40f - 10f * u1)
    val u2 = ((t - 2.2f).coerceAtLeast(0f) / 9f) % 1f
    bat(120f - 130f * u2, 26f + 10f * u2)
}

private fun DrawScope.land(t: Float, sh: Shapes) {
    drawPath(sh.land, Brush.verticalGradient(listOf(Color(0xFF071019), Color(0xFF020509)), startY = 154f, endY = 234f))
    drawPath(sh.ridge, Teal, alpha = 0.9f, style = Stroke(0.7f, join = StrokeJoin.Round))
    sh.shards.forEach { drawPath(it, Dusk) }
    drawPath(sh.castle, Ink)

    // Windows, one by one.
    WINDOWS.forEachIndexed { i, (x, y) ->
        val on = p(t, 2.15f + i * 0.06f, 0.25f, InOut)
        if (on > 0f) drawRoundRect(Cyan, Offset(x - 0.1f, y.toFloat()), Size(1.2f, 3f), CornerRadius(0.6f), alpha = on)
    }
    val arch = p(t, 2.5f, 0.25f, InOut)
    if (arch > 0f) drawPath(sh.archWindow, Mid, alpha = arch)

    // The door, then a slow pulse.
    val door = p(t, 2.65f, 0.5f, InOut)
    if (door > 0f) {
        val pulse = if (t > 3.2f) 1f - 0.3f * (0.5f - 0.5f * cos(2 * PI.toFloat() * (t - 3.2f) / 2.4f)) else 1f
        val glow = door * pulse
        drawOval(Brush.radialGradient(listOf(Pale, Cyan.copy(alpha = 0f)), Offset(54f, 158f), 10f), Offset(44f, 154f), Size(20f, 8f), alpha = 0.6f * glow)
        drawCircle(Brush.radialGradient(listOf(Glow.copy(alpha = 0.6f), Glow.copy(alpha = 0f)), Offset(54f, 152f), 6f), 6f, Offset(54f, 152f), alpha = glow)
        drawPath(sh.door, Glow, alpha = glow)
    }

    // The road draws itself up to the door, with a small light walking it.
    val r = p(t, 1.5f, 1.15f, InOut)
    if (r > 0f) {
        val m = sh.roadMeasure
        val part = Path()
        m.getSegment(0f, m.length * r, part, true)
        drawPath(part, Teal, style = Stroke(2.2f, cap = StrokeCap.Round))
        drawPath(part, Mid, alpha = 0.7f, style = Stroke(0.6f, cap = StrokeCap.Round))
        val walker = 1f - p(t, 2.6f, 0.3f, InOut)
        if (walker > 0f) {
            val at = m.getPosition(m.length * r)
            drawCircle(Brush.radialGradient(listOf(Pale.copy(alpha = 0.7f), Pale.copy(alpha = 0f)), at, 3f), 3f, at, alpha = walker)
            drawCircle(Pale, 1.1f, at, alpha = walker)
        }
    }
}

