package app.lantern.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lantern.R
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.LocalBaColors
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.lanternColors
import app.lantern.ui.theme.sky
import kotlin.math.sin
import kotlin.random.Random

/** The four meet in a diamond: knight on top, then clockwise (splash.html, "Gather"). */
private val seats = listOf(
    LanternCharacter.KNIGHT to Offset(0f, -1f),
    LanternCharacter.VAMPIRE to Offset(1f, 0f),
    LanternCharacter.GOTH to Offset(0f, 1f),
    LanternCharacter.WIZARD to Offset(-1f, 0f),
)

/** The timeline in ms at full length; the short splash plays the same one faster. */
private object T {
    const val END = 4400f
    const val CORE = 1900f
    const val NAME = 2400f
    const val EN = 2800f
    const val LINE = 3200f
    const val BAR = 3600f
    fun seat(i: Int) = 500f + i * 350f
}

/** 0 before [start], 1 after [start] + [dur], eased in between. */
private fun phase(t: Float, start: Float, dur: Float): Float =
    FastOutSlowInEasing.transform(((t - start) / dur).coerceIn(0f, 1f))

/**
 * The splash (D190, D191 ③): stars come out, the four portraits drift in from the dark one by one in their
 * own glow and meet in one light, then «فانوس», "LANTERN", the line and a pixel bar. [full] is the first
 * launch (~4.4 s); later launches play it at about a third of the time. A tap skips it.
 */
@Composable
fun Splash(full: Boolean, onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val ms = if (full) T.END else T.END * 0.3f
        clock.animateTo(T.END, tween(ms.toInt(), easing = LinearEasing))
        onDone()
    }
    val t = clock.value
    val name = stringResource(R.string.app_name)
    Box(
        Modifier
            .fillMaxSize()
            .sky()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .semantics { contentDescription = name },
    ) {
        Stars(phase(t, 0f, 800f))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight * 0.4f)
            val gap = with(LocalDensity.current) { 96.dp.toPx() }
            val size = 112.dp
            val half = with(LocalDensity.current) { (size / 2).toPx() }
            // The core of light where they meet.
            val core = phase(t, T.CORE, 1600f)
            val swell = if (core < 0.4f) core / 0.4f * 1.4f else 1.4f - (core - 0.4f) / 0.6f * 0.4f
            Box(Modifier.fillMaxSize().drawBehind {
                if (core <= 0f) return@drawBehind
                val r = 70.dp.toPx() * swell
                listOf(Color(0xFFFF5A6A) to 1.6f, Color(0xFFB98CFF) to 1.2f, Color(0xFF5FD3E6) to 0.8f).forEach { (c, k) ->
                    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.35f * core), Color.Transparent), center, r * k), r * k, center)
                }
                drawCircle(Color.White.copy(alpha = 0.9f * core), 5.dp.toPx() * swell, center)
            })
            seats.forEachIndexed { i, (character, dir) ->
                val p = phase(t, T.seat(i), 1000f)
                // From 1.9× further out, small and see-through, to its seat.
                val reach = gap * (1.9f - 0.9f * p)
                CompositionLocalProvider(LocalBaColors provides lanternColors(character)) {
                    CharacterPortrait(
                        character, Mood.CALM,
                        Modifier
                            .offset { androidx.compose.ui.unit.IntOffset((center.x + dir.x * reach - half).toInt(), (center.y + dir.y * reach - half).toInt()) }
                            .size(size)
                            .graphicsLayer { alpha = p; scaleX = 0.5f + 0.5f * p; scaleY = 0.5f + 0.5f * p },
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().layoutTop(0.68f),
        ) {
            val n = phase(t, T.NAME, 900f)
            Text(
                name,
                style = Ba.type.display.copy(
                    fontSize = 46.sp,
                    brush = Brush.linearGradient(listOf(Color(0xFF5FD3E6), Color(0xFFB98CFF), Color(0xFFFF5A6A), Color(0xFFCFD6DD))),
                ),
                modifier = Modifier.graphicsLayer { alpha = n; translationY = (1 - n) * 14.dp.toPx() },
            )
            Text(
                stringResource(R.string.splash_en),
                style = Ba.type.label.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                color = Ba.colors.text2,
                modifier = Modifier.graphicsLayer { alpha = phase(t, T.EN, 800f) },
            )
            Text(
                stringResource(R.string.splash_line),
                style = Ba.type.label,
                color = Ba.colors.text3,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp).graphicsLayer { alpha = phase(t, T.LINE, 800f) },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp).width(120.dp).graphicsLayer { alpha = phase(t, T.BAR - 200f, 400f) },
        ) {
            repeat(6) { i ->
                val on = t >= T.BAR + i * 120f
                Box(Modifier.weight(1f).size(height = 6.dp, width = 0.dp).background(if (on) Color(0xFF5FD3E6) else Color.White.copy(alpha = 0.1f)))
            }
        }
    }
}

/** Places the content [fraction] of the way down the screen. */
private fun Modifier.layoutTop(fraction: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
    layout(placeable.width, height) { placeable.place(0, (height * fraction).toInt()) }
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
            drawRect(Color(0xFFCFEAF2).copy(alpha = a), Offset(x * size.width, y * size.height), androidx.compose.ui.geometry.Size(px, px))
        }
    })
}
