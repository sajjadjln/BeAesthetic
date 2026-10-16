package app.beaesthetic.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.data.repo.Pose
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.ui.components.PickPill
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import kotlinx.coroutines.launch

/**
 * The compare slider (D75, flows C5): [after] fills the frame, [before] covers the part right of the divider.
 * Dragging moves the divider. On first show a wipe reveals [after] from the left, as in the motion demo.
 */
@Composable
fun CompareSlider(photos: PhotoStore, before: String?, after: String?, beforeLabel: String, afterLabel: String, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val divider = remember(before, after) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(before, after) { divider.animateTo(0.5f, tween(600)) }
    BoxWithConstraints(modifier.aspectRatio(3f / 4f).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(c.surface2)) {
        val width = constraints.maxWidth.toFloat()
        val dividerDp = with(LocalDensity.current) { (divider.value * width).toDp() }
        PhotoThumb(photos, after, afterLabel, Modifier.fillMaxSize(), maxEdge = 1440)
        // «روز صفر» on the right: drawn only right of the divider (physical sides, so RTL doesn't flip it).
        PhotoThumb(
            photos, before, beforeLabel,
            Modifier.fillMaxSize().drawWithContent {
                clipRect(left = divider.value * size.width, top = 0f, right = size.width, bottom = size.height, clipOp = ClipOp.Intersect) { this@drawWithContent.drawContent() }
            },
            maxEdge = 1440,
        )
        Box(Modifier.align(AbsoluteAlignment.TopLeft).fillMaxHeight().width(3.dp).absoluteOffset(x = dividerDp - 1.5.dp).background(c.surface))
        Box(Modifier.align(AbsoluteAlignment.CenterLeft).absoluteOffset(x = dividerDp - 16.dp).size(32.dp).clip(CircleShape).background(c.surface))
        Box(
            Modifier.fillMaxSize().pointerInput(width) {
                detectHorizontalDragGestures { change, drag ->
                    change.consume()
                    scope.launch { divider.snapTo((divider.value + drag / width).coerceIn(0f, 1f)) }
                }
            },
        )
        Text(afterLabel, style = Ba.type.caption, color = c.text1, modifier = Modifier.align(AbsoluteAlignment.BottomLeft).padding(8.dp).background(c.surface, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp))
        Text(beforeLabel, style = Ba.type.caption, color = c.text1, modifier = Modifier.align(AbsoluteAlignment.BottomRight).padding(8.dp).background(c.surface, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

/**
 * The reveal (D75): a check-in's photos next to Day 0 (or last month), per pose. Opened after a check-in
 * and from Progress's photo rows.
 */
@Composable
fun RevealContent(photos: PhotoStore, rows: List<PhotoRow>, idx: Int) {
    val c = Ba.colors
    val months = stringArrayResource(R.array.jalali_months)
    val current = rows.firstOrNull { it.checkIn.idx == idx } ?: return
    val day0 = rows.firstOrNull { it.checkIn.isDay0 }
    val last = rows.lastOrNull { it.checkIn.idx < idx && !it.checkIn.isDay0 }
    var againstLast by remember { mutableStateOf(false) }
    val against = if (againstLast && last != null) last else day0 ?: last
    val poses = Pose.entries.filter { it in current.photos }
    var pose by remember { mutableStateOf(poses.firstOrNull() ?: Pose.FRONT) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (day0 != null && last != null && day0 !== current) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickPill(stringResource(R.string.reveal_day0), !againstLast) { againstLast = false }
                PickPill(stringResource(R.string.reveal_last), againstLast) { againstLast = true }
            }
        }
        val beforeLabel = against?.let { if (it.checkIn.isDay0) stringResource(R.string.reveal_day0) else months[Jalali.fromGregorian(it.checkIn.date).month - 1] }
            ?: stringResource(R.string.reveal_day0)
        CompareSlider(photos, against?.photos?.get(pose), current.photos[pose], beforeLabel, jalaliDayMonth(current.checkIn.date), Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            poses.forEach { p -> PickPill(stringResource(poseName(p)), pose == p) { pose = p } }
        }
        Text(stringResource(R.string.reveal_drag), style = Ba.type.caption, color = c.text3)
    }
}

@Composable
fun RevealRoute(viewModel: ProgressViewModel, photos: PhotoStore, idx: Int, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Ba.colors
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.reveal_title), onBack, stringResource(R.string.cd_back))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                state?.let { RevealContent(photos, it.photoRows, idx) }
            }
        }
    }
}
