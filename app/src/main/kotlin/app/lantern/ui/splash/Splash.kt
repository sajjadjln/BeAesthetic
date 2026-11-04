package app.lantern.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.theme.rememberAnimationsOn
import app.lantern.ui.theme.sky
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** The logo's gold (the user's logo file, 2026-10-31). */
private val Gold = Color(0xFFEBC57E)

/** 0 before [from], 1 after [to] (both fractions of the timeline), eased in between. */
private fun phase(t: Float, from: Float, to: Float): Float =
    FastOutSlowInEasing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

/** The lantern catching: dim, a flicker up, a dip, then full (splash-logo.html, `catch`). */
private fun lampAlpha(t: Float): Float {
    val u = ((t - 0.115f) / 0.385f).coerceIn(0f, 1f)
    return when {
        u < 0.40f -> 0.15f + 0.25f * (u / 0.40f)
        u < 0.55f -> 0.40f - 0.15f * ((u - 0.40f) / 0.15f)
        else -> 0.25f + 0.75f * ((u - 0.55f) / 0.45f)
    }
}

/**
 * Where the lantern sits on each logo, as fractions of the word image (x, y, width, height). In Persian it
 * stands on the «ف»; in English on the «L». The English y is negative: the lantern sits above the word.
 */
private class Logo(val word: Int, val aspect: Float, val widthOfScreen: Float, val lamp: FloatArray)

private val Fa = Logo(R.drawable.splash_word_fa, 780f / 290f, 0.78f, floatArrayOf(0.8487f, 0.0069f, 0.1385f, 0.3448f))
private val En = Logo(R.drawable.splash_word_en, 572f / 122f, 0.72f, floatArrayOf(0.0014f, -0.754f, 0.1566f, 0.682f))

/**
 * The splash, concept 1 "Light the lantern" (splash-logo.html, splash-logo-en.html): only the lantern shows,
 * dim; its flame catches, then its light sweeps across in reading order and writes the name. Persian sweeps
 * right to left, English left to right. Plays on every cold start (2.6 s), holds a moment, then goes. A tap
 * skips it.
 */
@Composable
fun Splash(onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    val animate = rememberAnimationsOn()
    LaunchedEffect(Unit) {
        // The first frames carry the whole app composing underneath; start the clock after them.
        repeat(2) { withFrameNanos { } }
        clock.animateTo(1f, tween(2600, easing = LinearEasing))
        // With "Remove animations" on there's nothing to watch, so no hold.
        if (animate) delay(700)
        onDone()
    }
    val t = clock.value
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val logo = if (rtl) Fa else En
    val flicker by rememberInfiniteTransition(label = "flame").animateFloat(
        0f, 6.28f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "f",
    )
    val name = stringResource(R.string.app_name)
    Box(
        Modifier
            .fillMaxSize()
            .sky()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .semantics { contentDescription = name },
    ) {
        Stars(phase(t, 0f, 0.3f))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth * logo.widthOfScreen
            val h = w / logo.aspect
            val left = (maxWidth - w) / 2
            val top = maxHeight * 0.44f - h / 2
            val (lx, ly, lw, lh) = logo.lamp.let { listOf(it[0], it[1], it[2], it[3]) }
            val sweep = phase(t, 0.42f, 0.96f)
            // The halo behind the flame, and the band of light that runs ahead of the writing.
            val halo = phase(t, 0.35f, 0.5f) * (0.82f + 0.12f * sin(flicker) + 0.05f * sin(flicker * 2.3f))
            val beam = if (sweep in 0.001f..0.999f) 1f - phase(t, 0.85f, 1f) else 0f
            Box(Modifier.fillMaxSize().drawBehind {
                val px = w.toPx()
                val flame = Offset((left + w * (lx + lw / 2)).toPx(), (top + h * (ly + lh * 0.62f)).toPx())
                if (halo > 0f) {
                    val r = 60.dp.toPx() * (1f + 0.06f * sin(flicker))
                    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.55f * halo), Color.Transparent), flame, r), r, flame)
                }
                if (beam > 0f) {
                    val edge = left.toPx() + px * (if (rtl) 1 - sweep else sweep)
                    val band = px * 0.35f
                    val start = if (rtl) edge + band else edge - band
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = 0.22f * beam), Color.Transparent), start, edge + (edge - start) * 0.3f),
                        Offset(0f, 0f), Size(size.width, size.height),
                    )
                }
            })
            Image(
                painterResource(logo.word), null,
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(Gold),
                modifier = Modifier
                    // absoluteOffset: the logo's fractions are measured from the left in both languages; a plain
                    // offset mirrors in RTL and put the Persian lantern at the end of the word (on the «س»).
                    .absoluteOffset(left, top).size(w, h)
                    .drawWithContent {
                        val x = size.width * sweep
                        if (rtl) clipRect(left = size.width - x) { this@drawWithContent.drawContent() }
                        else clipRect(right = x) { this@drawWithContent.drawContent() }
                    },
            )
            Image(
                painterResource(R.drawable.splash_lamp), null,
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(Gold),
                modifier = Modifier
                    .absoluteOffset { IntOffset((left + w * lx).roundToPx(), (top + h * ly).roundToPx()) }
                    .size(w * lw, h * lh)
                    .graphicsLayer { alpha = lampAlpha(t) },
            )
        }
    }
}

/** Fifty stars that come out with [shown] and then twinkle. */
@Composable
private fun Stars(shown: Float) {
    val stars = remember { List(50) { Triple(Random.nextFloat(), Random.nextFloat() * 0.7f, Random.nextFloat() * 6.28f) } }
    val twinkle by rememberInfiniteTransition(label = "stars").animateFloat(0f, 6.28f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Box(Modifier.fillMaxSize().drawBehind {
        val px = 2.dp.toPx()
        stars.forEach { (x, y, seed) ->
            val a = (0.5f + 0.5f * sin(twinkle + seed)) * 0.8f * shown
            drawRect(Color(0xFFCFEAF2).copy(alpha = a), Offset(x * size.width, y * size.height), Size(px, px))
        }
    })
}
