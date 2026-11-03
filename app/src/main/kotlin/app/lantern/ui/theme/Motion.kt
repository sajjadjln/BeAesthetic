package app.lantern.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Motion shared by Today and the task sheet (D196, docs/design/today-motion.html). */
object Motion {
    /** Set once the splash has handed over (at once when there is none); Today's entrance waits for it. */
    val splashDone = mutableStateOf(false)

    /** Today's entrance plays once per cold start. */
    var entrancePlayed = false

    /**
     * The phone's "Remove animations". Compose's own animations already follow it; the timers it can't
     * reach (the dialog typing, the splash hold) ask here.
     */
    fun enabled(context: Context): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
}

@Composable
fun rememberAnimationsOn(): Boolean {
    val context = LocalContext.current
    return remember { Motion.enabled(context) }
}

/** T10: rises 16 dp and fades in, [order] × 55 ms after the splash hands over. Once per cold start. */
fun Modifier.riseIn(order: Int): Modifier = composed {
    val play = remember { !Motion.entrancePlayed }
    val t = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!play) return@LaunchedEffect
        snapshotFlow { Motion.splashDone.value }.first { it }
        Motion.entrancePlayed = true
        delay(order * 55L)
        t.animateTo(1f, tween(380, easing = BaMotion.Standard))
    }
    graphicsLayer { alpha = t.value; translationY = (1 - t.value) * 16.dp.toPx() }
}

/** S2: a section of the full form rises in after the one before it, when it was opened by a tap. */
fun Modifier.staggerIn(order: Int, play: Boolean): Modifier = composed {
    val t = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!play) return@LaunchedEffect
        delay(40L + order * 55L)
        t.animateTo(1f, tween(320, easing = BaMotion.Standard))
    }
    graphicsLayer { alpha = t.value; translationY = (1 - t.value) * 14.dp.toPx() }
}

/** The character's hop (T3, T6, S7): it squashes, jumps [high] and lands. [t] runs 0 → 1 in 520 ms. */
fun Modifier.hop(t: () -> Float, high: () -> Dp): Modifier = graphicsLayer {
    val p = t()
    fun seg(a: Float, b: Float) = FastOutSlowInEasing.transform(((p - a) / (b - a)).coerceIn(0f, 1f))
    fun lerp(x: Float, y: Float, f: Float) = x + (y - x) * f
    val h = high().toPx()
    val (sx, sy, ty) = when {
        p <= 0f || p >= 1f -> Triple(1f, 1f, 0f)
        p < 0.22f -> seg(0f, 0.22f).let { Triple(lerp(1f, 1.07f, it), lerp(1f, 0.9f, it), 0f) }
        p < 0.55f -> seg(0.22f, 0.55f).let { Triple(lerp(1.07f, 0.97f, it), lerp(0.9f, 1.04f, it), lerp(0f, -h, it)) }
        p < 0.82f -> seg(0.55f, 0.82f).let { Triple(lerp(0.97f, 1.02f, it), lerp(1.04f, 0.98f, it), lerp(-h, 1.dp.toPx(), it)) }
        else -> seg(0.82f, 1f).let { Triple(lerp(1.02f, 1f, it), lerp(0.98f, 1f, it), lerp(1.dp.toPx(), 0f, it)) }
    }
    transformOrigin = TransformOrigin(0.5f, 1f)
    scaleX = sx
    scaleY = sy
    translationY = ty
}

suspend fun Animatable<Float, AnimationVector1D>.playHop() {
    snapTo(0f)
    animateTo(1f, tween(520, easing = LinearEasing))
}
