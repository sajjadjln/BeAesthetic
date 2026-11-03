package app.lantern.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
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
import app.lantern.ui.theme.Px
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
 * key for the knight, a coffin for the vampire, a crystal orb for the wizard, a bitten moon for the goth girl,
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
                LanternCharacter.KNIGHT -> KeyAdd(press, Px.Card, Modifier.size(56.dp))
                LanternCharacter.VAMPIRE -> KeyAdd(press, Coffin, Modifier.size(50.dp, 62.dp), rim = true)
                LanternCharacter.WIZARD -> OrbAdd(press)
                LanternCharacter.GOTH -> MoonAdd(press)
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

/**
 * A key standing on its own darker shadow that sinks 3 dp onto it: the knight's pixel key (T8), and the
 * vampire's coffin with a carved inner rim.
 */
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

/** The vampire's coffin: wide at the shoulders, narrow at the head and the feet. */
private val Coffin = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.3f, 0f); lineTo(w * 0.7f, 0f); lineTo(w, h * 0.26f); lineTo(w * 0.8f, h); lineTo(w * 0.2f, h); lineTo(0f, h * 0.26f); close()
}

/** The wizard's crystal orb: lit from the top corner, a soft halo and a small twinkling star. */
@Composable
private fun OrbAdd(press: Float) {
    val c = Ba.colors
    val twinkle = if (rememberAnimationsOn()) {
        rememberInfiniteTransition(label = "orb").animateFloat(0.35f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "star").value
    } else 1f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer { scaleX = 1 - 0.08f * press; scaleY = 1 - 0.08f * press }
            .drawBehind {
                drawCircle(c.glow.copy(alpha = 0.16f), radius = size.minDimension / 2 + 5.dp.toPx())
                drawCircle(c.glow.copy(alpha = 0.08f), radius = size.minDimension / 2 + 10.dp.toPx())
            }
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(Color.White, c.glow, lerp(c.glow, Color.Black, 0.55f)), center = Offset(size.width * 0.34f, size.height * 0.3f), radius = size.width * 0.85f))
            },
    ) { PlusIcon(c.onGlow) }
    // The star sits on the orb's top corner, outside its clip.
    Box(Modifier.size(64.dp).drawBehind {
        val r = 6.dp.toPx() * (0.8f + 0.2f * twinkle)
        val at = Offset(size.width - 9.dp.toPx(), 7.dp.toPx())
        drawPath(star(at, r), c.glow.copy(alpha = twinkle))
    })
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

/** The goth girl's moon with a bite out of its top corner, and two small stars beside it. */
@Composable
private fun MoonAdd(press: Float) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer { scaleX = 1 - 0.08f * press; scaleY = 1 - 0.08f * press }
            .clip(BittenMoon)
            .background(c.glow),
    ) { PlusIcon(c.onGlow, Modifier.offset(x = (-3).dp, y = 3.dp)) }
    Box(Modifier.size(64.dp).drawBehind {
        drawCircle(c.glow.copy(alpha = 0.8f), 2.5.dp.toPx(), Offset(size.width - 3.dp.toPx(), 3.dp.toPx()))
        drawCircle(c.glow.copy(alpha = 0.6f), 1.8.dp.toPx(), Offset(size.width - 12.dp.toPx(), 7.dp.toPx()))
    })
}

private val BittenMoon = GenericShape { size, _ ->
    val full = Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) }
    val r = size.width * 0.24f
    val bite = Path().apply { addOval(Rect(Offset(size.width * 0.8f, size.height * 0.18f), r)) }
    addPath(Path().apply { op(full, bite, PathOperation.Difference) })
}
