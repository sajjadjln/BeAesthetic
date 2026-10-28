
package app.lantern.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.R
import app.lantern.backup.BackupStatus
import app.lantern.domain.backup.BackupNotice
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.text.PersianDigits
import androidx.compose.runtime.saveable.rememberSaveable
import app.lantern.domain.time.Jalali
import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.Chip
import app.lantern.ui.components.ChipKind
import app.lantern.domain.tasks.RepeatKind
import app.lantern.ui.tasks.Editing
import app.lantern.ui.tasks.TaskEditorSheet
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.DayDot
import app.lantern.ui.components.DayRing
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.RoundIconButton
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.LanternTheme
import app.lantern.ui.theme.rememberHaptics
import java.time.LocalDate
import kotlin.math.hypot
import kotlin.math.max
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

data class TodayActions(
    val selectPart: (Part) -> Unit = {},
    val toggle: (Long) -> Unit = {},
    val toggleMinimum: () -> Unit = {},
    val openSettings: () -> Unit = {},
    val fixReminders: () -> Unit = {},
    val setAnytimeOpen: (Boolean) -> Unit = {},
    val addAnytime: () -> Unit = {},
    val openBackup: () -> Unit = {},
    /** «+ کار جدید» (today-v3/v4 A, D146): the simple sheet on «امروز» in the part being looked at. */
    val addTask: () -> Unit = {},
)

@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    onOpenSettings: () -> Unit,
    onFixReminders: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // «یه چیز اضافه کن» opens the simple sheet on «هر وقت» in the part being looked at (D128).
    var adding by remember { mutableStateOf<Pair<Part, RepeatKind>?>(null) }
    LaunchedEffect(Unit) { viewModel.screenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshSystemState() }
    TodayScreen(
        state,
        TodayActions(
            selectPart = viewModel::selectPart,
            toggle = viewModel::toggle,
            toggleMinimum = viewModel::toggleMinimumMode,
            openSettings = onOpenSettings,
            fixReminders = onFixReminders,
            setAnytimeOpen = viewModel::setAnytimeOpen,
            addAnytime = { adding = (state?.selectedPart ?: Part.MORNING) to RepeatKind.ANYTIME },
            addTask = { adding = (state?.selectedPart ?: Part.MORNING) to RepeatKind.ONCE },
            openBackup = onOpenBackup,
        ),
    )
    val s = state
    val add = adding
    if (s != null && add != null) {
        val tasks by viewModel.editorTasks.collectAsStateWithLifecycle()
        TaskEditorSheet(
            editing = Editing.New(add.first, add.second),
            today = s.date,
            allTasks = tasks,
            onDismiss = { adding = null },
            onSave = { form -> adding = null; viewModel.saveTask(form) },
        )
    }
}

@Composable
fun TodayScreen(state: TodayUiState?, actions: TodayActions) {
    val haptics = rememberHaptics()
    // The 100% moment (D76): once, when the day reaches 100% while the screen is open.
    var wasComplete by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state?.dayComplete, state?.date) {
        val complete = state?.dayComplete ?: return@LaunchedEffect
        if (wasComplete == false && complete) {
            haptics.medium()
        }
        wasComplete = complete
    }

    if (state != null) TodayBody(state, actions)
}

@Composable
internal fun HealthBannerRow(banner: HealthBanner, onFix: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(app.lantern.ui.theme.Px.Card)
            .background(c.surface)
            .clickable(onClick = onFix)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Bell, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(
            stringResource(if (banner == HealthBanner.LATE) R.string.banner_late else R.string.banner_blocked),
            style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.banner_fix), style = Ba.type.label, color = c.glow)
    }
}

@Composable
internal fun BackupRow(status: BackupStatus, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val text = when {
        status.notice == BackupNotice.NO_FOLDER -> stringResource(R.string.backup_no_folder)
        status.notice == BackupNotice.FAILED -> stringResource(R.string.backup_failed)
        status.daysSince == null -> stringResource(R.string.backup_none_yet)
        else -> stringResource(R.string.backup_last_days, PersianDigits.format(status.daysSince))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(app.lantern.ui.theme.Px.Card)
            .background(c.surface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Cloud, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(text, style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.banner_backup_look), style = Ba.type.label, color = c.glow)
    }
}

/**
 * «هر وقت» (D127): folded at the bottom, it opens with one tap and remembers it. A ticked item dims and
 * stays until the day ends, so a wrong tick can be undone; it never counts in the ring.
 */
@Composable
internal fun AnytimeCard(items: List<AnytimeUi>, open: Boolean, actions: TodayActions, modifier: Modifier = Modifier) {
    val colors = Ba.colors
    val haptics = rememberHaptics()
    val chevron by animateFloatAsState(if (open) 180f else 0f, tween(BaMotion.SHORT), label = "anytimeChevron")
    Column(modifier.fillMaxWidth().clip(app.lantern.ui.theme.Px.Card).background(colors.surface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(open, role = Role.Button) { actions.setAnytimeOpen(it) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(BaIcons.List, null, tint = colors.text2, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.anytime_title), style = Ba.type.subtitle, color = colors.text1, modifier = Modifier.weight(1f))
            Chip(PersianDigits.format(items.count { !it.done }), ChipKind.NEUTRAL)
            Icon(BaIcons.ChevronDown, null, tint = colors.text3, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = chevron })
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn()) {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                items.forEach { item ->
                    val alpha by animateFloatAsState(if (item.done) 0.45f else 1f, tween(BaMotion.SHORT), label = "anytimeAlpha")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(item.done, role = Role.Checkbox) { nowDone -> if (nowDone) haptics.light(); actions.toggle(item.taskId) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .graphicsLayer { this.alpha = alpha },
                    ) {
                        CheckCircle(item.done, Modifier.size(24.dp))
                        Text(item.title, style = Ba.type.body, color = colors.text1, modifier = Modifier.weight(1f))
                        if (item.important && !item.done) Text(stringResource(R.string.tag_important), style = Ba.type.caption, color = colors.amber)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = actions.addAnytime)
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
                ) {
                    Icon(BaIcons.Plus, null, tint = colors.text3, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.anytime_add), style = Ba.type.body, color = colors.text3)
                }
            }
        }
    }
}

/** The round check of a tile, the workout card and «هر وقت» rows. */
@Composable
internal fun CheckCircle(done: Boolean, modifier: Modifier = Modifier) {
    val colors = Ba.colors
    val bg by animateColorAsState(if (done) colors.accent else colors.surface, tween(BaMotion.SHORT), label = "checkCircle")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(CircleShape).background(bg).then(if (done) Modifier else Modifier.border(2.dp, colors.border, CircleShape)),
    ) {
        if (done) Icon(BaIcons.Check, null, tint = colors.onAccent, modifier = Modifier.fillMaxSize(0.6f))
    }
}
