package app.lantern.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.LocalCharacter
import app.lantern.ui.theme.rememberAnimationsOn
import kotlin.math.roundToInt

/** The bottom tabs, icons only, RTL from the right: Today · + · Goals (D160, D201). */
enum class Tab(val icon: ImageVector, val label: Int) {
    TODAY(BaIcons.CheckCircle, R.string.tab_today),
    GOALS(BaIcons.Grid, R.string.tab_goals),
}

/** The bar's own height, above the navigation-bar inset; tab screens keep this much clear at the bottom. */
val BOTTOM_BAR_HEIGHT = 61.dp

/** How far «+» rises above the bar; the screen under it keeps this much more clear at the end of its scroll. */
val ADD_RISE = 20.dp

/**
 * The bar with «+ کار جدید» in its middle (D201). The button is the one thing each character changes: a pixel
 * sword for the knight (D208), a coffin for the vampire, a spell book for the wizard (D210), a cat for the goth girl (D209),
 * a rounded square in Simple. Everything else is the same in every look.
 */
@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(top = ADD_RISE).background(c.surface).navigationBarsPadding()) {
            // A line on top: in the character's color with a companion, a hairline in Simple.
            if (Ba.companion) HorizontalDivider(color = c.glow.copy(alpha = 0.25f), thickness = 2.dp)
            else HorizontalDivider(color = c.border, thickness = 1.dp)
            Row(Modifier.fillMaxWidth().height(60.dp).selectableGroup()) {
                TabButton(Tab.TODAY, Tab.TODAY == selected, onSelect, Modifier.weight(1f))
                Spacer(Modifier.width(88.dp))
                TabButton(Tab.GOALS, Tab.GOALS == selected, onSelect, Modifier.weight(1f))
            }
        }
        AddButton(onAdd, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun TabButton(tab: Tab, on: Boolean, onSelect: (Tab) -> Unit, modifier: Modifier) {
    val c = Ba.colors
    val label = stringResource(tab.label)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        modifier = modifier
            .height(60.dp)
            .selectable(on, role = Role.Tab) { onSelect(tab) }
            .semantics { contentDescription = label },
    ) {
        Icon(tab.icon, null, tint = if (on) c.glow else c.text3, modifier = Modifier.size(24.dp))
        // The active tab's small accent mark.
        Box(Modifier.padding(top = 4.dp).size(5.dp).clip(if (Ba.companion) androidx.compose.ui.graphics.RectangleShape else CircleShape).background(if (on) c.glow else c.surface))
    }
}

/** «+ کار جدید» in the picked look (D201). Pressed, it sinks or squashes, and springs back when let go. */
@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.tasks_new)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, if (pressed) tween(70) else BaMotion.bouncy(), label = "addPress")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        when {
            !Ba.companion -> SimpleAdd(press)
            else -> when (LocalCharacter.current) {
                LanternCharacter.KNIGHT -> SwordAdd(press)
                LanternCharacter.VAMPIRE -> KeyAdd(press, Coffin, Modifier.size(50.dp, 62.dp), rim = true)
                LanternCharacter.WIZARD -> BookAdd(press)
                LanternCharacter.GOTH -> CatAdd(press)
            }
        }
    }
}

@Composable
private fun PlusIcon(tint: Color, modifier: Modifier = Modifier) = Icon(BaIcons.Plus, null, tint = tint, modifier = modifier.size(24.dp))

/** Simple: a rounded square in the picked color. */
@Composable
private fun SimpleAdd(press: Float) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer { scaleX = 1 - 0.06f * press; scaleY = 1 - 0.06f * press }
            .clip(RoundedCornerShape(18.dp))
            .background(c.glow),
    ) { PlusIcon(c.onGlow) }
}

/** The vampire's coffin with a carved inner rim, standing on its own darker shadow that sinks 3 dp onto it (T8). */
@Composable
private fun KeyAdd(press: Float, shape: androidx.compose.ui.graphics.Shape, modifier: Modifier, rim: Boolean = false) {
    val c = Ba.colors
    val shadow = lerp(c.glow, Color.Black, 0.55f)
    Box(modifier) {
        Box(Modifier.fillMaxSize().offset { IntOffset(0, (4.dp.toPx() - 3.dp.toPx() * press).roundToInt()) }.clip(shape).background(shadow))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = 3.dp.toPx() * press }
                .clip(shape)
                .background(if (rim) Brush.verticalGradient(listOf(lerp(c.glow, Color.White, 0.3f), c.glow, lerp(c.glow, Color.Black, 0.3f))) else Brush.linearGradient(listOf(c.glow, c.glow))),
        ) {
            if (rim) Box(Modifier.fillMaxSize().padding(4.dp).border(2.dp, lerp(c.glow, Color.Black, 0.45f), shape))
            PlusIcon(c.onGlow, Modifier.offset(y = if (rim) (-3).dp else 0.dp))
        }
    }
}

/**
 * The knight's sword (D208): a pixel sword standing point down in the middle of the bar, so its guard and blade
 * still read as «+». Pressed, it lifts 10 dp like it's being drawn. A shine runs down the blade every 4 s.
 * One sprite pixel is 3 dp; the sword reaches from the top of the button almost to the bottom of the bar.
 */
@Composable
private fun SwordAdd(press: Float) {
    val c = Ba.colors
    val clock = if (rememberAnimationsOn()) {
        rememberInfiniteTransition(label = "sword").animateFloat(0f, SHINE_MS, infiniteRepeatable(tween(SHINE_MS.toInt(), easing = LinearEasing)), label = "shine").value
    } else -1f
    val ink = mapOf(
        'o' to lerp(c.glow, Color.Black, 0.92f), 'p' to c.glow, 'g' to c.glow, 'G' to lerp(c.glow, Color.Black, 0.5f),
        'w' to Color.White, 'h' to c.rimDeep, 'b' to c.rimLight, 'e' to lerp(c.glow, c.rimLight, 0.5f),
    )
    Box(
        Modifier
            .size(64.dp)
            .graphicsLayer { translationY = -10.dp.toPx() * press }
            .drawBehind {
                val s = 3.dp.toPx()
                val left = (size.width - SWORD[0].length * s) / 2
                // The glow behind the hilt and guard.
                drawCircle(Brush.radialGradient(listOf(c.glow.copy(alpha = 0.35f), Color.Transparent), center = Offset(size.width / 2, 8 * s), radius = 15 * s), 15 * s, Offset(size.width / 2, 8 * s))
                SWORD.forEachIndexed { y, row ->
                    row.forEachIndexed { x, k ->
                        if (k == '.') return@forEachIndexed
                        // A hard 3 dp shadow under the sword first, then the pixel.
                        drawRect(Color.Black.copy(alpha = 0.45f), Offset(left + x * s, y * s + s), Size(s, s))
                    }
                }
                SWORD.forEachIndexed { y, row ->
                    row.forEachIndexed { x, k ->
                        if (k == '.') return@forEachIndexed
                        val base = ink.getValue(k)
                        val color = if ((k == 'b' || k == 'w') && clock >= 0f) lerp(base, Color.White, shine(clock - y * 35f)) else base
                        drawRect(color, Offset(left + x * s, y * s), Size(s, s))
                    }
                }
            },
    )
}

private const val SHINE_MS = 4000f

/** How white a blade row is, [ms] after the shine started: up over 120 ms, back down over 200 ms. */
private fun shine(ms: Float): Float {
    val t = ((ms % SHINE_MS) + SHINE_MS) % SHINE_MS
    return when {
        t < 120f -> t / 120f
        t < 320f -> 1 - (t - 120f) / 200f
        else -> 0f
    }
}

/** The sword, point down: o outline, p pommel, w white, h grip, g/G guard and its shade, b/e blade and its edge. */
private val SWORD = listOf(
    "......ooo......",
    ".....opwpo.....",
    ".....opppo.....",
    "......oho......",
    "......oho......",
    "......oho......",
    ".ooooooooooooo.",
    "ogwgggggggggggo",
    "oGGGGGGGGGGGGGo",
    ".ooooooooooooo.",
) + List(12) { ".....owbeo....." } + listOf(
    "......obo......",
    "......obo......",
    ".......o.......",
)

/** The vampire's coffin: wide at the shoulders, narrow at the head and the feet. */
private val Coffin = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.3f, 0f); lineTo(w * 0.7f, 0f); lineTo(w, h * 0.26f); lineTo(w * 0.8f, h); lineTo(w * 0.2f, h); lineTo(0f, h * 0.26f); close()
}

/**
 * The wizard's spell book (D210): an open book with a glowing «+» rune floating above its pages and two small
 * twinkling stars. The rune bobs 4 dp; pressed, the book squashes. Drawn on a 64-unit grid, like the mockup.
 */
@Composable
private fun BookAdd(press: Float) {
    val c = Ba.colors
    val (bob, twinkle) = if (rememberAnimationsOn()) {
        val t = rememberInfiniteTransition(label = "book")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "rune").value to
            t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "star").value
    } else 0f to 1f
    Box(
        Modifier
            .size(64.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.9f)
                scaleX = 1 + 0.06f * press; scaleY = 1 - 0.12f * press
            }
            .drawBehind {
                val u = size.width / 64f
                fun p(x: Float, y: Float) = Offset(x * u, y * u)
                drawOval(c.glow.copy(alpha = 0.1f), p(-2f, 0f), Size(68 * u, 60 * u))
                // The cover, then the two pages, each a curved sheet.
                drawPath(Path().apply {
                    moveTo(2 * u, 40 * u); quadraticTo(16 * u, 34 * u, 32 * u, 40 * u); quadraticTo(48 * u, 34 * u, 62 * u, 40 * u)
                    lineTo(62 * u, 58 * u); quadraticTo(48 * u, 52 * u, 32 * u, 58 * u); quadraticTo(16 * u, 52 * u, 2 * u, 58 * u); close()
                }, c.rimDeep)
                for (x0 in listOf(5f, 33f)) drawPath(Path().apply {
                    moveTo(x0 * u, 37 * u); quadraticTo((x0 + 13) * u, 31 * u, (x0 + 26) * u, 37 * u)
                    lineTo((x0 + 26) * u, 54 * u); quadraticTo((x0 + 13) * u, 48 * u, x0 * u, 54 * u); close()
                }, c.glow)
                // Two lines of writing on each page.
                for (x0 in listOf(10f, 38f)) for (y0 in listOf(42f, 46f)) drawPath(
                    Path().apply { moveTo(x0 * u, y0 * u); quadraticTo((x0 + 8) * u, (y0 - 3) * u, (x0 + 16) * u, y0 * u) },
                    c.rimDeep.copy(alpha = 0.7f), style = Stroke(1.4f * u, cap = StrokeCap.Round),
                )
                // The rune.
                val ry = (17 - 4 * bob) * u
                drawCircle(c.glow.copy(alpha = 0.25f), 13 * u, Offset(32 * u, ry))
                drawLine(c.rimLight, Offset(23 * u, ry), Offset(41 * u, ry), 3.4f * u, StrokeCap.Round)
                drawLine(c.rimLight, Offset(32 * u, ry - 9 * u), Offset(32 * u, ry + 9 * u), 3.4f * u, StrokeCap.Round)
                drawPath(star(p(10f, 14f), 3 * u), c.glow.copy(alpha = twinkle))
                drawPath(star(p(55f, 9f), 3.6f * u), c.glow.copy(alpha = 1.35f - twinkle))
            },
    )
}

private fun star(at: Offset, r: Float): Path = Path().apply {
    val k = r * 0.28f
    moveTo(at.x, at.y - r)
    quadraticTo(at.x + k, at.y - k, at.x + r, at.y)
    quadraticTo(at.x + k, at.y + k, at.x, at.y + r)
    quadraticTo(at.x - k, at.y + k, at.x - r, at.y)
    quadraticTo(at.x - k, at.y - k, at.x, at.y - r)
    close()
}

/**
 * The goth girl's cat (D209): a cat head in her color with the dark «+» on its face and whiskers beside it.
 * Pressed, it squashes from the chin. Every 5 s its right ear twitches. Drawn on a 64-unit grid, like the mockup.
 */
@Composable
private fun CatAdd(press: Float) {
    val c = Ba.colors
    val ear = if (rememberAnimationsOn()) {
        rememberInfiniteTransition(label = "cat").animateFloat(
            0f, 0f,
            infiniteRepeatable(keyframes { durationMillis = 5000; 0f at 4400; 14f at 4550; -4f at 4700; 0f at 4850 }),
            label = "ear",
        ).value
    } else 0f
    val inner = c.rimDeep
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.9f)
                scaleX = 1 + 0.06f * press; scaleY = 1 - 0.12f * press
            }
            .drawBehind {
                val u = size.width / 64f
                fun tri(a: Float, b: Float, x: Float, y: Float, p: Float, q: Float) = Path().apply { moveTo(a * u, b * u); lineTo(x * u, y * u); lineTo(p * u, q * u); close() }
                drawCircle(c.glow.copy(alpha = 0.14f), 34 * u, Offset(32 * u, 36 * u))
                drawPath(tri(9f, 32f, 12f, 4f, 28f, 17f), c.glow)
                drawPath(tri(13f, 25f, 14.5f, 10f, 23f, 17f), inner)
                rotate(ear, Offset(46 * u, 20 * u)) {
                    drawPath(tri(55f, 32f, 52f, 4f, 36f, 17f), c.glow)
                    drawPath(tri(51f, 25f, 49.5f, 10f, 41f, 17f), inner)
                }
                drawOval(c.glow, Offset(6 * u, 14 * u), Size(52 * u, 46 * u))
                val whisker = c.glow.copy(alpha = 0.7f)
                val w = 1.6f * u
                drawLine(whisker, Offset(6 * u, 40 * u), Offset(0f, 38 * u), w, StrokeCap.Round)
                drawLine(whisker, Offset(6 * u, 44 * u), Offset(-1 * u, 44 * u), w, StrokeCap.Round)
                drawLine(whisker, Offset(58 * u, 40 * u), Offset(64 * u, 38 * u), w, StrokeCap.Round)
                drawLine(whisker, Offset(58 * u, 44 * u), Offset(65 * u, 44 * u), w, StrokeCap.Round)
            },
    ) { PlusIcon(c.onGlow, Modifier.offset(y = 6.dp)) }
}
