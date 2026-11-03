package app.lantern.ui.components

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.Px
import kotlin.math.roundToInt

/**
 * S1 (D196): one highlight for a group of choices that slides from the old choice to the new one, across
 * lines too, instead of each choice drawing its own. Choices report where they are with [choiceOf].
 */
@Stable
class ChoiceHighlight {
    internal val bounds = mutableStateMapOf<Any, Rect>()
    /** The group's own box; every choice is measured against it, however deep it sits. */
    internal var group: LayoutCoordinates? = null
}

@Composable
fun rememberChoiceHighlight(): ChoiceHighlight = remember { ChoiceHighlight() }

/**
 * Marks a choice. Its bounds are taken in the [ChoiceGroup]'s own coordinates: bounds in the direct parent were
 * off whenever that parent didn't sit at the group's physical top-left (a wrapping row at the start in RTL).
 */
fun Modifier.choiceOf(highlight: ChoiceHighlight, key: Any): Modifier =
    onGloballyPositioned { child ->
        val group = highlight.group?.takeIf { it.isAttached } ?: return@onGloballyPositioned
        highlight.bounds[key] = group.localBoundingBoxOf(child, clipBounds = false)
    }

@Composable
fun ChoiceGroup(highlight: ChoiceHighlight, selected: Any?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Ba.colors
    Box(modifier.onPlaced { highlight.group = it }) {
        val target = selected?.let { highlight.bounds[it] }
        if (target != null) {
            val r by animateValueAsState(target, Rect.VectorConverter, BaMotion.settle(), label = "choice")
            val density = LocalDensity.current
            Box(
                Modifier
                    // Physical coordinates, not mirrored in RTL: start from the physical top-left, or in RTL the box
                    // would begin at the right edge and the offset would push it off the group.
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                    .clip(Px.Small)
                    .background(c.glow.copy(alpha = 0.18f))
                    .border(2.dp, c.glow, Px.Small),
            )
        }
        content()
    }
}
