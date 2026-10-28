package app.lantern.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A rectangle with stepped "pixel" corners (D185): [levels] steps of [step] each at every corner.
 * One 6 dp step for cards and buttons, one 3 dp step for small chips, two 4 dp steps for the dialog box.
 */
class PixelShape(private val step: Dp, private val levels: Int = 1) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { step.toPx() }
        val w = size.width
        val h = size.height
        // The top-left corner from the left edge to the top edge; the other three are mirrors of it.
        val tl = buildList {
            add(Offset(0f, levels * s))
            for (k in 1..levels) { add(Offset(k * s, (levels - k + 1) * s)); add(Offset(k * s, (levels - k) * s)) }
        }
        val points = tl +
            tl.reversed().map { Offset(w - it.x, it.y) } +
            tl.map { Offset(w - it.x, h - it.y) } +
            tl.reversed().map { Offset(it.x, h - it.y) }
        val p = Path()
        p.moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { p.lineTo(it.x, it.y) }
        p.close()
        return Outline.Generic(p)
    }
}

object Px {
    val Card = PixelShape(6.dp)
    val Small = PixelShape(3.dp)
    val Dialog = PixelShape(4.dp, levels = 2)
}

/** The night sky behind every screen: [BaColors.sky2] glowing from the top, [BaColors.bg] at the edges. */
@Composable
fun Modifier.sky(): Modifier {
    val c = Ba.colors
    return background(c.bg).drawBehind {
        drawRect(Brush.radialGradient(listOf(c.sky2, c.bg), center = Offset(size.width / 2, size.height * 0.1f), radius = size.maxDimension * 0.75f))
    }
}

/**
 * The pixel-game dialog box (D185): a light rim, a dark gap and a deep rim inside stepped corners, faint
 * scanlines. [text] types in letter by letter when [typing] is on; a nudging arrow shows once it's all out.
 */
@Composable
fun PixelDialog(text: String, modifier: Modifier = Modifier, typing: Boolean = true) {
    val c = Ba.colors
    var shown by remember(text) { mutableIntStateOf(if (typing) 0 else text.length) }
    LaunchedEffect(text, typing) {
        while (shown < text.length) { delay(38); shown++ }
    }
    val done = shown >= text.length
    val nudge = rememberInfiniteTransition(label = "nudge").animateFloat(0f, 3f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "y")
    Box(
        modifier
            .clip(Px.Dialog)
            .background(Brush.verticalGradient(listOf(c.surface2, c.surface)))
            .drawBehind {
                val px = 1.dp.toPx()
                fun ring(inset: Float, width: Float, color: Color) =
                    drawRect(color, Offset(inset + width / 2, inset + width / 2), Size(size.width - 2 * inset - width, size.height - 2 * inset - width), style = Stroke(width))
                ring(0f, 4 * px, c.rimLight)
                ring(4 * px, 3 * px, c.surface)
                ring(7 * px, 2 * px, c.rimDeep)
                var y = 9 * px
                while (y < size.height - 9 * px) { drawRect(Color.White.copy(alpha = 0.025f), Offset(9 * px, y + 3 * px), Size(size.width - 18 * px, px)); y += 4 * px }
            }
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
    ) {
        Text(text.take(shown), style = Ba.type.subtitle, color = Color(0xFFE9FBFF))
        if (done && typing) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(bottom = 0.dp).graphicsLayer { translationY = nudge.value * density + 10.dp.toPx() }
                    .size(10.dp, 6.dp).drawBehind {
                        val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }
                        drawPath(p, c.glow)
                    },
            )
        }
    }
}

/**
 * A character's round portrait with its glow (D185): the dark backdrop fades out at the edge, and it
 * breathes gently. Purely decorative unless the caller adds a description.
 */
@Composable
fun CharacterPortrait(character: LanternCharacter, mood: Mood, modifier: Modifier = Modifier, breathe: Boolean = true) {
    val c = Ba.colors
    val t = rememberInfiniteTransition(label = "breathe")
    val lift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lift")
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(Brush.radialGradient(listOf(c.glow.copy(alpha = 0.32f), Color.Transparent), radius = size.minDimension * 0.62f), radius = size.minDimension * 0.62f)
        })
        Image(
            painterResource(character.art(mood)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    if (breathe) translationY = -lift * 4.dp.toPx()
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.radialGradient(0f to Color.Black, 0.78f to Color.Black, 1f to Color.Transparent, radius = size.minDimension / 2),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }
}
