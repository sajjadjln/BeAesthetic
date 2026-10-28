
package app.lantern.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.lantern.domain.score.DayStatus
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.BaMotion

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
            .clip(app.lantern.ui.theme.Px.Small)
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
            modifier = Modifier.size(48.dp).clip(app.lantern.ui.theme.Px.Small).clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = backDescription },
        ) {
            Icon(icon, contentDescription = null, tint = Ba.colors.text1, modifier = Modifier.size(24.dp))
        }
        Text(title, style = Ba.type.title, color = Ba.colors.text1, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun BaCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(app.lantern.ui.theme.Px.Card).background(Ba.colors.surface).padding(padding),
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
            .clip(app.lantern.ui.theme.Px.Card)
            .background(if (enabled) c.glow else c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        icon?.let { Icon(it, null, tint = if (enabled) c.onGlow else c.text3, modifier = Modifier.size(20.dp).padding(end = 2.dp)) }
        Text(text, style = Ba.type.subtitle, color = if (enabled) c.onGlow else c.text3, modifier = Modifier.padding(start = if (icon != null) 6.dp else 0.dp))
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
            .clip(app.lantern.ui.theme.Px.Card)
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
            checkedThumbColor = c.onGlow, checkedTrackColor = c.glow, checkedBorderColor = c.glow,
            uncheckedThumbColor = c.text3, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.border,
        ),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Ba.type.label, color = Ba.colors.text2, modifier = modifier.padding(top = 8.dp, start = 4.dp))
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

/**
 * The scroll of a sheet's content. A gesture that scrolled the content never carries on into the sheet:
 * scrolling back up in one long drag or fling stops at the top instead of pulling the sheet down and closing
 * it (friends' feedback, D147). A drag that starts with the content already at the top still closes it.
 */
fun Modifier.sheetScroll(state: ScrollState): Modifier = composed {
    val keepOpen = remember { KeepSheetOpen() }
    nestedScroll(keepOpen).verticalScroll(state)
}

private class KeepSheetOpen : NestedScrollConnection {
    /** The current gesture has moved the content; cleared when the gesture ends (every drag ends in a fling). */
    private var scrolled = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (consumed.y != 0f) scrolled = true
        return if (scrolled || source == NestedScrollSource.SideEffect) available else Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val keep = scrolled
        scrolled = false
        return if (keep) available else Velocity.Zero
    }
}

/**
 * The end of something (a workout, a check-in, an onboarding step, a recap card): the content sits in the
 * middle of the screen and the buttons at the bottom, in thumb reach, the main one last (friends' feedback,
 * D147). Content taller than the screen scrolls above the buttons.
 */
@Composable
fun EndLayout(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    buttons: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewport)
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                content = content,
            )
        }
        if (buttons != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 16.dp),
                content = buttons,
            )
        }
    }
}
