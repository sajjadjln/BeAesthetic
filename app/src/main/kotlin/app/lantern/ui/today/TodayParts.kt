package app.lantern.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.domain.time.Part
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.Px
import kotlinx.coroutines.launch

/** The four parts' pixel icons on a 12×12 grid (today-v5.html): sunrise, sun, sunset, moon. */
private val icons: Map<Part, List<List<Int>>> = mapOf(
    Part.MORNING to listOf(listOf(1, 9, 10, 2), listOf(4, 6, 4, 3), listOf(5, 2, 2, 2), listOf(1, 4, 2, 2), listOf(9, 4, 2, 2)),
    Part.MIDDAY to listOf(listOf(4, 4, 4, 4), listOf(5, 0, 2, 2), listOf(5, 10, 2, 2), listOf(0, 5, 2, 2), listOf(10, 5, 2, 2)),
    Part.EVENING to listOf(listOf(1, 9, 10, 2), listOf(3, 6, 6, 3), listOf(1, 3, 2, 1), listOf(9, 3, 2, 1)),
    Part.NIGHT to listOf(listOf(4, 1, 4, 2), listOf(2, 3, 3, 6), listOf(4, 9, 4, 2), listOf(8, 8, 2, 2), listOf(9, 2, 1, 1)),
)

@Composable
private fun PartIcon(part: Part, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 12f
        icons.getValue(part).forEach { (x, y, w, h) -> drawRect(color, Offset(x * u, y * u), Size(w * u, h * u)) }
    }
}

/**
 * Today, layout 2 (D188): a pixel bar of the 4 parts over swipeable pages, one per part. It opens on the
 * part the clock is in; a swipe or a tap moves both, and the indicator follows the finger. A later part
 * comes in from the left in RTL. A small green square under a part means it's all done.
 */
@Composable
internal fun PartsSlider(state: TodayUiState, onSelect: (Part) -> Unit, onTick: (TileUi) -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = state.selectedPart.ordinal) { Part.entries.size }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { onSelect(Part.entries[it]) } }
    LaunchedEffect(state.selectedPart) {
        if (pager.currentPage != state.selectedPart.ordinal) pager.animateScrollToPage(state.selectedPart.ordinal)
    }
    Column {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(56.dp)
                .clip(Px.Card)
                .background(c.surface)
                .border(2.dp, Color.White.copy(alpha = 0.08f), Px.Card),
        ) {
            val slot = maxWidth / 4
            val position = pager.currentPage + pager.currentPageOffsetFraction
            Box(
                Modifier
                    .offset(x = slot * position)
                    .width(slot)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(Px.Card)
                    .background(c.glow.copy(alpha = 0.18f))
                    .border(2.dp, c.glow, Px.Card),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                Part.entries.forEach { part ->
                    val on = part.ordinal == pager.currentPage
                    val color = if (on) c.glow else c.text3
                    val full = state.completeParts.contains(part) && state.tiles[part].orEmpty().isNotEmpty()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(role = Role.Tab) { scope.launch { pager.animateScrollToPage(part.ordinal) } }
                            .semantics { selected = on },
                    ) {
                        PartIcon(part, color, Modifier.size(20.dp))
                        Text(stringResource(partNames[part.ordinal]), style = Ba.type.caption, color = color)
                        Box(Modifier.size(4.dp).background(if (full) c.accent else Color.Transparent))
                    }
                }
            }
        }
        HorizontalPager(pager, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) { page ->
            val list = state.tiles[Part.entries[page]].orEmpty()
            Column(Modifier.fillMaxWidth()) {
                if (list.isEmpty()) {
                    Text(
                        stringResource(R.string.today_part_empty),
                        style = Ba.type.label,
                        color = c.text3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
                list.forEach { tile -> PixelTile(tile) { onTick(tile) } }
            }
        }
    }
}
