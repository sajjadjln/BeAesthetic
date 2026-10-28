
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
import app.lantern.ui.theme.ThemeMode
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
    val colors = Ba.colors
    val haptics = rememberHaptics()
    // The 100% moment (D76): once, when the day reaches 100% while the screen is open.
    var wasComplete by remember { mutableStateOf<Boolean?>(null) }
    var pulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(state?.dayComplete, state?.date) {
        val complete = state?.dayComplete ?: return@LaunchedEffect
        if (wasComplete == false && complete) {
            haptics.medium()
            pulse++
        }
        wasComplete = complete
    }

    Box(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding()) {
        if (state == null) return@Box
        val partNames = listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night).map { stringResource(it) }
        val tiles = if (state.minimumMode) {
            // All parts show at once, so a repeated name («مسواک» morning and night) gets its part.
            val repeated = state.floorTiles.groupingBy { it.title }.eachCount().filterValues { it > 1 }.keys
            state.floorTiles.map { if (it.title in repeated) it.copy(title = it.title + " " + partNames[it.part.ordinal]) else it }
        } else {
            state.tiles[state.selectedPart].orEmpty()
        }
        // The parts of the day are pages (D147): swipe the tiles, or tap the switcher; both move the other.
        val pager = rememberPagerState(initialPage = state.selectedPart.ordinal) { Part.entries.size }
        LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { actions.selectPart(Part.entries[it]) } }
        LaunchedEffect(state.selectedPart) {
            if (pager.currentPage != state.selectedPart.ordinal) pager.animateScrollToPage(state.selectedPart.ordinal)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header(state, actions) }
            item(span = { GridItemSpan(maxLineSpan) }) { Hero(state, pulse) }
            if (state.minimumMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.today_minimum_label),
                        style = Ba.type.subtitle,
                        color = colors.onSpecialSubtle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (!state.minimumMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PartSwitcher(
                        Part.entries[pager.currentPage],
                        { pager.currentPage + pager.currentPageOffsetFraction },
                        state.completeParts,
                        actions.selectPart,
                        Modifier.padding(vertical = 6.dp),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) { PartPager(state, pager, actions) }
            } else {
                if (tiles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyPart() }
                }
                items(tiles, key = { it.taskId }) { tile -> TaskTile(tile, onToggle = { actions.toggle(tile.taskId) }) }
            }
            if (!state.minimumMode && state.anytime.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnytimeCard(state.anytime, state.anytimeOpen, actions, Modifier.padding(top = 6.dp))
                }
            }
            state.banner?.let { banner ->
                item(span = { GridItemSpan(maxLineSpan) }) { HealthBannerRow(banner, actions.fixReminders, Modifier.padding(top = 8.dp)) }
            }
            state.backup?.let { backup ->
                item(span = { GridItemSpan(maxLineSpan) }) { BackupRow(backup, actions.openBackup, Modifier.padding(top = 8.dp)) }
            }
        }
        // «+ کار جدید» (today-v3/v4 A): where the thumb already is; bottom End = the left edge in RTL.
        if (!state.minimumMode) NewTaskFab(actions.addTask, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun NewTaskFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(c.accent)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .height(52.dp)
            .padding(start = 18.dp, end = 20.dp),
    ) {
        Icon(BaIcons.Plus, null, tint = c.onAccent, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.tasks_new), style = Ba.type.label, color = c.onAccent)
    }
}

@Composable
private fun Header(state: TodayUiState, actions: TodayActions) {
    val colors = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    val line = jalaliDayMonth(state.date)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(weekdays[Weekdays.index(state.date.dayOfWeek)], style = Ba.type.headline, color = colors.text1)
            Text(line, style = Ba.type.label, color = colors.text2)
        }
        RoundIconButton(BaIcons.Leaf, stringResource(R.string.cd_minimum_day), actions.toggleMinimum, selected = state.minimumMode)
        RoundIconButton(BaIcons.Gear, stringResource(R.string.cd_settings), actions.openSettings)
    }
}

@Composable
private fun Hero(state: TodayUiState, pulse: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        DayRing(
            done = state.done,
            total = state.total,
            centerBig = PersianDigits.format(state.done),
            centerSmall = stringResource(R.string.ring_of, PersianDigits.format(state.total)),
            description = stringResource(R.string.cd_ring, PersianDigits.format(state.done), PersianDigits.format(state.total)),
            pulse = pulse,
        )
    }
}

@Composable
private fun HealthBannerRow(banner: HealthBanner, onFix: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .clickable(onClick = onFix)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Bell, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(
            stringResource(if (banner == HealthBanner.LATE) R.string.banner_late else R.string.banner_blocked),
            style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.banner_fix), style = Ba.type.label, color = c.accent)
    }
}

@Composable
private fun BackupRow(status: BackupStatus, onOpen: () -> Unit, modifier: Modifier = Modifier) {
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
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Cloud, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(text, style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.banner_backup_look), style = Ba.type.label, color = c.accent)
    }
}

/**
 * The parts of the day: one tile each, Morning on the right. The indicator follows [position] (the pager's
 * page plus how far it has been dragged), so it slides with the finger and settles with the page (D76, D147).
 */
@Composable
private fun PartSwitcher(selected: Part, position: () -> Float, complete: Set<Part>, onSelect: (Part) -> Unit, modifier: Modifier = Modifier) {
    val colors = Ba.colors
    val names = mapOf(
        Part.MORNING to stringResource(R.string.part_morning),
        Part.MIDDAY to stringResource(R.string.part_midday),
        Part.EVENING to stringResource(R.string.part_evening),
        Part.NIGHT to stringResource(R.string.part_night),
    )
    val gap = 8.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(6.dp),
    ) {
        val cell = (maxWidth - gap * (Part.entries.size - 1)) / Part.entries.size
        // offset {} follows the layout direction, so a positive step moves toward the left in RTL.
        Box(
            Modifier
                .offset { IntOffset((position() * (cell + gap).toPx()).roundToInt(), 0) }
                .width(cell)
                .height(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface2),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth().selectableGroup()) {
            for (part in Part.entries) {
                val isSelected = part == selected
                val tint by animateColorAsState(if (isSelected) colors.text1 else colors.text3, tween(BaMotion.SHORT, easing = BaMotion.Standard), label = "partTint")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(part) })
                        .semantics { contentDescription = names.getValue(part) },
                ) {
                    Icon(BaIcons.of(part), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    PartDot(part in complete, Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp))
                }
            }
        }
    }
}

/**
 * The tiles of each part as a page. The pager reaches the screen edges (past the list's 16 dp padding), so a
 * page slides off the screen rather than vanishing at the padding. Time flows right → left: the next part
 * is to the left, the same side as in the switcher.
 */
@Composable
private fun PartPager(state: TodayUiState, pager: PagerState, actions: TodayActions) {
    val bleed = 16.dp
    HorizontalPager(
        pager,
        contentPadding = PaddingValues(horizontal = bleed),
        pageSpacing = bleed,
        verticalAlignment = Alignment.Top,
        key = { it },
        modifier = Modifier.layout { measurable, constraints ->
            val extra = (bleed * 2).roundToPx()
            val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
            layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
        },
    ) { page ->
        val tiles = state.tiles[Part.entries[page]].orEmpty()
        if (tiles.isEmpty()) {
            EmptyPart()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { tile ->
                            Box(Modifier.weight(1f), propagateMinConstraints = true) {
                                key(tile.taskId) { TaskTile(tile, onToggle = { actions.toggle(tile.taskId) }) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPart() {
    Text(stringResource(R.string.today_part_empty), style = Ba.type.body, color = Ba.colors.text3, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
}

/** A finished part's dot pops in (D76). */
@Composable
private fun PartDot(visible: Boolean, modifier: Modifier) {
    AnimatedVisibility(visible, enter = scaleIn(BaMotion.bouncy()), modifier = modifier) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(Ba.colors.accent))
    }
}

/**
 * A task tile. A tick spreads the accent fill from the touch point, the check appears and the tile dips to
 * 96% and springs back, with a light vibration. An untick drains back without vibrating (D76).
 */
@Composable
private fun TaskTile(tile: TileUi, onToggle: () -> Unit) {
    val colors = Ba.colors
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, BaMotion.spring(), label = "tileScale")
    var touch by remember { mutableStateOf<Offset?>(null) }
    val fill = remember { Animatable(if (tile.done) 1f else 0f) }
    LaunchedEffect(tile.done) { fill.animateTo(if (tile.done) 1f else 0f, tween(BaMotion.SHORT + 50, easing = BaMotion.Standard)) }
    val checkBg by animateColorAsState(if (tile.done) colors.accent else colors.surface, tween(BaMotion.SHORT), label = "checkBg")
    val shape = RoundedCornerShape(20.dp)
    val outlined = !tile.done && (tile.due || tile.important)

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 108.dp)
            .clip(shape)
            .drawBehind {
                drawRect(colors.surface)
                if (fill.value > 0f) {
                    val origin = touch ?: center
                    val reach = max(
                        max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
                        max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
                    )
                    drawCircle(colors.accentSubtle, reach * fill.value, origin)
                }
            }
            .then(if (outlined) Modifier.border(1.5.dp, colors.amber, shape) else Modifier)
            .pointerInput(Unit) { awaitEachGesture { touch = awaitFirstDown(requireUnconsumed = false).position } }
            .toggleable(
                value = tile.done,
                interactionSource = interaction,
                indication = ripple(color = colors.accent),
                role = Role.Checkbox,
                onValueChange = { nowDone ->
                    if (nowDone) haptics.light()
                    onToggle()
                },
            )
            .padding(16.dp),
    ) {
        Icon(
            BaIcons.of(tile.icon),
            contentDescription = null,
            tint = if (tile.done) colors.onAccentSubtle else colors.text2,
            modifier = Modifier.align(Alignment.TopStart).size(28.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(checkBg)
                .then(if (tile.done) Modifier else Modifier.border(2.dp, colors.border, CircleShape)),
        ) {
            Icon(BaIcons.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp).graphicsLayer { alpha = fill.value; scaleX = .6f + .4f * fill.value; scaleY = .6f + .4f * fill.value })
        }
        // Label and tag share the bottom row; a long label wraps instead of running under the tag.
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(top = 44.dp),
        ) {
            Text(tile.title, style = Ba.type.tile, color = colors.text1, modifier = Modifier.weight(1f))
            when {
                tile.due && !tile.done -> Text(
                    stringResource(R.string.tag_due),
                    style = Ba.type.caption,
                    color = colors.amber,
                    maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.amberSubtle).padding(horizontal = 8.dp),
                )
                tile.important -> Text(
                    stringResource(R.string.tag_important),
                    style = Ba.type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = if (tile.done) colors.onAccentSubtle.copy(alpha = 0.7f) else colors.amber,
                )
            }
        }
    }
}

/**
 * «هر وقت» (D127): folded at the bottom, it opens with one tap and remembers it. A ticked item dims and
 * stays until the day ends, so a wrong tick can be undone; it never counts in the ring.
 */
@Composable
private fun AnytimeCard(items: List<AnytimeUi>, open: Boolean, actions: TodayActions, modifier: Modifier = Modifier) {
    val colors = Ba.colors
    val haptics = rememberHaptics()
    val chevron by animateFloatAsState(if (open) 180f else 0f, tween(BaMotion.SHORT), label = "anytimeChevron")
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)) {
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
private fun CheckCircle(done: Boolean, modifier: Modifier = Modifier) {
    val colors = Ba.colors
    val bg by animateColorAsState(if (done) colors.accent else colors.surface, tween(BaMotion.SHORT), label = "checkCircle")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(CircleShape).background(bg).then(if (done) Modifier else Modifier.border(2.dp, colors.border, CircleShape)),
    ) {
        if (done) Icon(BaIcons.Check, null, tint = colors.onAccent, modifier = Modifier.fillMaxSize(0.6f))
    }
}
