# Shared UI components

Stack: native Android, Kotlin, **Jetpack Compose** (Material 3 base, custom `Ba` theme), Navigation Compose with type-safe routes. No web framework, no Tailwind, no CSS. The HTML mockups in `docs/design/*.html` mirror the Compose UI with CSS tokens and are the design reference.

Shared primitives live in `app/src/main/kotlin/app/beaesthetic/ui/components/`. `Common.kt` holds the buttons, card, chip, list row, switch, section title, day ring and day dot; `BaIcons.kt` holds every icon (line icons, 24 dp, 1.8 stroke).

### `app/src/main/kotlin/app/beaesthetic/ui/components/Common.kt`

```kotlin
package app.beaesthetic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.beaesthetic.domain.score.DayStatus
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.ui.theme.BaMotion

@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val colors = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) colors.specialSubtle else colors.surface)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) colors.onSpecialSubtle else colors.text2, modifier = Modifier.size(22.dp))
    }
}

/** A screen's top bar: back (mirrored by RTL, so it points right) and a title. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, backDescription: String, icon: ImageVector = BaIcons.Back) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = backDescription },
        ) {
            Icon(icon, contentDescription = null, tint = Ba.colors.text1, modifier = Modifier.size(24.dp))
        }
        Text(title, style = Ba.type.title, color = Ba.colors.text1, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun BaCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ba.colors.surface).padding(padding),
        content = content,
    )
}

enum class ChipKind { ACCENT, AMBER, SPECIAL, NEUTRAL }

@Composable
fun Chip(text: String, kind: ChipKind, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ba.colors
    val (bg, fg) = when (kind) {
        ChipKind.ACCENT -> c.accentSubtle to c.onAccentSubtle
        ChipKind.AMBER -> c.amberSubtle to c.amber
        ChipKind.SPECIAL -> c.specialSubtle to c.onSpecialSubtle
        ChipKind.NEUTRAL -> c.surface2 to c.text2
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(14.dp)) }
        Text(text, style = Ba.type.caption, color = fg, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, height: Dp = 52.dp) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) c.accent else c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        icon?.let { Icon(it, null, tint = if (enabled) c.onAccent else c.text3, modifier = Modifier.size(20.dp).padding(end = 2.dp)) }
        Text(text, style = Ba.type.subtitle, color = if (enabled) c.onAccent else c.text3, modifier = Modifier.padding(start = if (icon != null) 6.dp else 0.dp))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 52.dp) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        icon?.let { Icon(it, null, tint = c.text1, modifier = Modifier.size(20.dp)) }
        Text(text, style = Ba.type.subtitle, color = c.text1, modifier = Modifier.padding(start = if (icon != null) 6.dp else 0.dp))
    }
}

/** A list row inside a card. */
@Composable
fun ListRow(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
fun BaSwitch(checked: Boolean, onChange: (Boolean) -> Unit, description: String) {
    val c = Ba.colors
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.semantics { contentDescription = description },
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onAccent, checkedTrackColor = c.accent, checkedBorderColor = c.accent,
            uncheckedThumbColor = c.text3, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.border,
        ),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Ba.type.label, color = Ba.colors.text2, modifier = modifier.padding(top = 8.dp, start = 4.dp))
}

/**
 * The day ring: done / total (D27). The arc starts at the top and springs to its new value (D76).
 */
@Composable
fun DayRing(done: Int, total: Int, centerBig: String, centerSmall: String, description: String, modifier: Modifier = Modifier, pulse: Int = 0, ringSize: Dp = 164.dp) {
    val c = Ba.colors
    val fraction by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, BaMotion.spring(), label = "ring")
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse > 0) {
            pulseScale.animateTo(1.06f, BaMotion.spring())
            pulseScale.animateTo(1f, BaMotion.bouncy())
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(ringSize).semantics { contentDescription = description }) {
        Canvas(Modifier.size(ringSize)) {
            val stroke = ringSize.toPx() * 10f / 164f
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            scale(pulseScale.value) {
                drawArc(c.surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (fraction > 0f) {
                    val sweep = 360f * fraction
                    drawArc(c.accent, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val big = if (ringSize < 120.dp) Ba.type.headline else Ba.type.display
            Text(centerBig, style = big, color = c.text1, textAlign = TextAlign.Center)
            if (centerSmall.isNotEmpty()) Text(centerSmall, style = Ba.type.label, color = c.text2)
        }
    }
}

/** One dot of the week row, by level; before the app = dotted; future = an outline (D56). */
@Composable
fun DayDot(status: DayStatus, isToday: Boolean, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val c = Ba.colors
    val fill: Color? = when (status) {
        is DayStatus.Level -> c.grid[status.level.coerceIn(0, 4)]
        DayStatus.Special -> c.special
        DayStatus.BeforeApp, DayStatus.Future -> null
    }
    Box(
        modifier = modifier
            .size(size + if (isToday) 8.dp else 0.dp)
            .then(if (isToday) Modifier.border(2.dp, c.accent, CircleShape).padding(4.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            if (fill != null) {
                drawCircle(fill)
            } else {
                val dashed = status == DayStatus.BeforeApp
                drawCircle(
                    c.border,
                    radius = this.size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(1.5.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())) else null),
                )
            }
        }
    }
}
```

### `app/src/main/kotlin/app/beaesthetic/ui/components/BaIcons.kt`

```kotlin
package app.beaesthetic.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Part

/**
 * The line icons of the mockups (`docs/design/screens.html`, 24×24, stroke 1.8, round caps and joins),
 * as Compose vectors. Paths are copied from the mockup's sprite; circles and rects are written as paths.
 */
object BaIcons {
    val Check = stroke("M20 6 9 17l-5-5")
    val Gear = stroke(
        circle(12f, 12f, 3f),
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
    )
    val Leaf = stroke(
        "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z",
        "M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12",
    )
    val Sunrise = stroke("M12 2v6M4.93 10.93l1.41 1.41M2 18h2M20 18h2M19.07 10.93l-1.41 1.41M22 22H2M16 18a4 4 0 0 0-8 0M8 6l4-4 4 4")
    val Sun = stroke(
        circle(12f, 12f, 4f),
        "M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41",
    )
    val Sunset = stroke("M12 10V2M4.93 10.93l1.41 1.41M2 18h2M20 18h2M19.07 10.93l-1.41 1.41M22 22H2M16 18a4 4 0 0 0-8 0M16 6l-4 4-4-4")
    val Moon = stroke("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Drop = stroke("M12 2.7s6 6.3 6 11.3a6 6 0 0 1-12 0c0-5 6-11.3 6-11.3z", "M9.5 14.5a2.5 2.5 0 0 0 2.5 2.5")
    val Tooth = stroke("M7 3C5 3 3.5 4.8 3.5 7.2c0 3 1.5 4.3 2 7 .5 3 1 7 2.8 7 1.6 0 1.6-4.5 3.7-4.5s2.1 4.5 3.7 4.5c1.8 0 2.3-4 2.8-7 .5-2.7 2-4 2-7C20.5 4.8 19 3 17 3c-2 0-3 1-5 1S9 3 7 3z")
    val Pill = stroke("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7")
    val Egg = stroke("M12 22c-4.4 0-7-3-7-7.5C5 9 8.5 2 12 2s7 7 7 12.5c0 4.5-2.6 7.5-7 7.5z")
    val Dumbbell = stroke("M6 7v10M18 7v10M3 9.5v5M21 9.5v5M6 12h12")
    val Scissors = stroke(circle(6f, 6f, 3f), circle(6f, 18f, 3f), "M20 4 8.12 15.88M14.47 14.48 20 20M8.12 8.12 12 12")
    val Apple = stroke("M12 7c-2-1.5-7-1.5-7 4.5S8 21 10 21c1 0 1.3-.5 2-.5s1 .5 2 .5c2 0 5-3.5 5-9.5S14 5.5 12 7z", "M12 7c0-2 1-3.5 3-4")
    val Shower = stroke("M4 21V9a5 5 0 0 1 9-3", "M10 8h10", "M12 12v1M15 12v1M18 12v1M13.5 16v1M16.5 16v1M15 20v1")
    val Mountain = stroke("m8 3 4 8 5-5 5 15H2L8 3z")
    /** A bathroom scale: not in the mockup sprite; drawn in the same style. */
    val Scale = stroke(roundRect(3f, 3f, 18f, 18f, 4f), "M8 10a4 4 0 0 1 8 0", "M12 10l1.6-2.2")
    val Play = filled("M8 5.5v13l11-6.5z")
    /** Points "back": drawn left, mirrored to point right in RTL. */
    val Back = stroke("M19 12H5M12 19l-7-7 7-7", autoMirror = true)
    val X = stroke("M18 6 6 18M6 6l12 12")
    val Bell = stroke("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val List = stroke("M9 6h11M9 12h11M9 18h11", "m3 6 1.5 1.5L7 5M3 12l1.5 1.5L7 11M3 18l1.5 1.5L7 17")
    val Cloud = stroke("M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z")
    val Palette = stroke(
        circle(13.5f, 6.5f, 1.5f), circle(17.5f, 10.5f, 1.5f), circle(8.5f, 7.5f, 1.5f), circle(6.5f, 12.5f, 1.5f),
        "M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.9 0 1.7-.8 1.7-1.7 0-.4-.2-.8-.4-1.1-.3-.3-.4-.7-.4-1.1 0-.9.8-1.7 1.7-1.7h2c3 0 5.5-2.5 5.5-5.5C22 6 17.5 2 12 2z",
    )
    val Phone = stroke(roundRect(6f, 2f, 12f, 20f, 2f), "M11 18h2")
    val Shield = stroke("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z", "m9 12 2 2 4-4")
    val Lock = stroke(roundRect(4f, 11f, 16f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4")
    val Mail = stroke(roundRect(3f, 5f, 18f, 14f, 2f), "m3 7 9 6 9-6")
    val Repeat = stroke("m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3")
    val Plus = stroke("M12 5v14M5 12h14")
    val Minus = stroke("M5 12h14")
    val Flag = stroke("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z", "M4 22v-7")
    val CheckCircle = stroke(circle(12f, 12f, 9f), "m8.5 12.5 2.5 2.5 5-5.5")
    val Grid = stroke(roundRect(4f, 4f, 7f, 7f, 1.5f), roundRect(13f, 4f, 7f, 7f, 1.5f), roundRect(4f, 13f, 7f, 7f, 1.5f), roundRect(13f, 13f, 7f, 7f, 1.5f))
    val Grip = stroke(circle(9f, 6f, 1f), circle(15f, 6f, 1f), circle(9f, 12f, 1f), circle(15f, 12f, 1f), circle(9f, 18f, 1f), circle(15f, 18f, 1f))
    val Pen = stroke("M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    val Folder = stroke("M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z")
    val Upload = stroke("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m17 8-5-5-5 5", "M12 3v12")
    val Download = stroke("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5", "M12 15V3")
    val Calendar = stroke(roundRect(3f, 4f, 18f, 18f, 3f), "M16 2v4M8 2v4M3 10h18")
    val ChevronDown = stroke("m6 9 6 6 6-6")
    /** Points "forward" (later in time): right in LTR, left in RTL (time flows right to left). */
    val ChevronForward = stroke("m9 18 6-6-6-6", autoMirror = true)
    val ChevronBackward = stroke("m15 18-6-6 6-6", autoMirror = true)

    fun of(icon: TaskIcon): ImageVector = when (icon) {
        TaskIcon.DROP -> Drop
        TaskIcon.TOOTH -> Tooth
        TaskIcon.PILL -> Pill
        TaskIcon.EGG -> Egg
        TaskIcon.DUMBBELL -> Dumbbell
        TaskIcon.MOUNTAIN -> Mountain
        TaskIcon.SCALE -> Scale
        TaskIcon.SHOWER -> Shower
        TaskIcon.SCISSORS -> Scissors
        TaskIcon.APPLE -> Apple
        TaskIcon.SUN -> Sun
        TaskIcon.CHECK -> CheckCircle
    }

    fun of(part: Part): ImageVector = when (part) {
        Part.MORNING -> Sunrise
        Part.MIDDAY -> Sun
        Part.EVENING -> Sunset
        Part.NIGHT -> Moon
    }

    private fun stroke(vararg paths: String, autoMirror: Boolean = false): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = autoMirror).apply {
            for (d in paths) {
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(d: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            .build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"
}
```
