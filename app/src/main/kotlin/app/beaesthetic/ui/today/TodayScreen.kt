package app.beaesthetic.ui.today

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.domain.buddy.Moment
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.domain.score.DayStatus
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.DayDot
import app.beaesthetic.ui.components.DayRing
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.RoundIconButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.buddy.AnimatedPiker
import app.beaesthetic.ui.components.buddy.Piker
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.linesRes
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.ui.theme.BaMotion
import app.beaesthetic.ui.theme.PeykarTheme
import app.beaesthetic.ui.theme.ThemeMode
import app.beaesthetic.ui.theme.rememberHaptics
import java.time.LocalDate
import kotlin.math.hypot
import kotlin.math.max

data class TodayActions(
    val selectPart: (Part) -> Unit = {},
    val toggle: (Long) -> Unit = {},
    val toggleMinimum: () -> Unit = {},
    val tapBuddy: () -> Unit = {},
    val openSettings: () -> Unit = {},
    val fixReminders: () -> Unit = {},
    val writeLetter: (first: Boolean) -> Unit = {},
    val dismissCard: () -> Unit = {},
    val openLetter: (Long) -> Unit = {},
    val openWorkout: () -> Unit = {},
    val dismissDaysShownUp: () -> Unit = {},
)

@Composable
fun TodayRoute(viewModel: TodayViewModel, onOpenSettings: () -> Unit, onFixReminders: () -> Unit, onWriteLetter: (Boolean) -> Unit, onOpenWorkout: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.screenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshSystemState() }
    TodayScreen(
        state,
        TodayActions(
            selectPart = viewModel::selectPart,
            toggle = viewModel::toggle,
            toggleMinimum = viewModel::toggleMinimumMode,
            tapBuddy = viewModel::tapBuddy,
            openSettings = onOpenSettings,
            fixReminders = onFixReminders,
            writeLetter = onWriteLetter,
            dismissCard = viewModel::dismissCard,
            openLetter = viewModel::openLetter,
            openWorkout = onOpenWorkout,
            dismissDaysShownUp = viewModel::dismissDaysShownUp,
        ),
    )
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
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
            state.cards.forEach { card -> item(span = { GridItemSpan(maxLineSpan) }) { CardFor(card, actions) } }
            state.buddy?.let { buddy -> item(span = { GridItemSpan(maxLineSpan) }) { BuddyRow(buddy, actions.tapBuddy) } }
            if (!state.minimumMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PartSwitcher(state.selectedPart, state.completeParts, actions.selectPart, Modifier.padding(vertical = 6.dp))
                }
            }
            if (tiles.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.today_part_empty), style = Ba.type.body, color = colors.text3, modifier = Modifier.padding(vertical = 24.dp))
                }
            }
            // The workout tile is wide (screens.html 01) and comes first, so it never leaves a hole in the grid.
            val ordered = tiles.sortedByDescending { it.opensWorkout }
            items(ordered, key = { it.taskId }, span = { GridItemSpan(if (it.opensWorkout) maxLineSpan else 1) }) { tile ->
                TaskTile(tile, onToggle = { actions.toggle(tile.taskId) }, onPlay = if (tile.opensWorkout) actions.openWorkout else null)
            }
            state.banner?.let { banner ->
                item(span = { GridItemSpan(maxLineSpan) }) { HealthBannerRow(banner, actions.fixReminders, Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun Header(state: TodayUiState, actions: TodayActions) {
    val colors = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    val months = stringArrayResource(R.array.jalali_months)
    val dayMonth = jalaliDayMonth(state.date)
    val week = PersianDigits.format(state.week)
    val total = PersianDigits.format(state.totalWeeks)
    val tail = state.headerTail
    val line = when {
        state.week == 0 -> stringResource(R.string.today_line_day0, dayMonth)
        tail is HeaderTail.Counting -> {
            val c = tail.countdown
            val text = when {
                c.isFinishDay && c.toNowruz -> stringResource(R.string.countdown_finish_nowruz)
                c.isFinishDay -> stringResource(R.string.countdown_finish)
                c.toNowruz -> stringResource(R.string.countdown_to_nowruz, PersianDigits.format(c.daysLeft))
                else -> stringResource(R.string.countdown_to_finish, PersianDigits.format(c.daysLeft))
            }
            stringResource(R.string.today_line_countdown, dayMonth, week, total, text)
        }
        tail is HeaderTail.InChapter -> stringResource(R.string.today_line_chapter, dayMonth, week, total, months[tail.monthIndex - 1])
        else -> stringResource(R.string.today_line_week, dayMonth, week, total)
    }
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
        val levelWords = stringArrayResource(R.array.day_level_words)
        val weekdays = stringArrayResource(R.array.weekdays)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                // Not color-only (D68): each day is read out with its level as a word.
                contentDescription = state.weekDots.mapIndexed { i, s ->
                    weekdays[i] + " " + when (s) {
                        is DayStatus.Level -> levelWords[s.level.coerceIn(0, 4)]
                        else -> ""
                    }
                }.joinToString("، ")
            },
        ) {
            state.weekDots.forEachIndexed { i, status -> DayDot(status, isToday = i == state.todayIndex) }
        }
    }
}

@Composable
private fun buddyText(buddy: BuddyUi): String {
    val lines = stringArrayResource(buddy.moment.linesRes())
    val possessive = stringArrayResource(R.array.part_possessive)
    val count = if (buddy.moment == Moment.MORNING) buddy.total else buddy.count
    return lines[buddy.lineIndex.mod(lines.size)].format(PersianDigits.format(count), possessive[buddy.part.ordinal])
}

/** «پیکر» under the ring with one line (D82). A tap gives a new line and a flex or a wink. */
@Composable
private fun BuddyRow(buddy: BuddyUi, onTap: () -> Unit) {
    val text = buddyText(buddy)
    val name = stringResource(R.string.cd_piker)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(92.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onTap)
                .semantics { contentDescription = name },
        ) {
            AnimatedPiker(buddy.pose, Ba.colors.accent, buddy.outfit, Modifier.fillMaxWidth())
        }
        Bubble(text, Modifier.weight(1f))
    }
}

/** A speech bubble with a tail pointing at «پیکر» (on the start side). */
@Composable
private fun Bubble(text: String, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Box(modifier) {
        Canvas(Modifier.align(Alignment.CenterStart).size(10.dp, 16.dp)) {
            val rtl = layoutDirection == LayoutDirection.Rtl
            val p = Path().apply {
                if (rtl) {
                    moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height)
                } else {
                    moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height)
                }
                close()
            }
            drawPath(p, c.surface2)
        }
        Text(
            text,
            style = Ba.type.body,
            color = c.text1,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface2)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CardFor(card: TodayCard, actions: TodayActions) {
    when (card) {
        is TodayCard.ChapterStart -> ChapterCard(card, actions)
        is TodayCard.FirstLetter -> FirstLetterCard(card, actions)
        is TodayCard.LetterArrived -> LetterArrivedCard(card, actions)
        is TodayCard.DaysShownUp -> DaysShownUpCard(card, actions.dismissDaysShownUp)
    }
}

/** A days-shown-up round number (D121): «پیکر» cheers, the number lands with a bounce and a medium buzz. */
@Composable
private fun DaysShownUpCard(card: TodayCard.DaysShownUp, onOk: () -> Unit) {
    val c = Ba.colors
    val haptics = rememberHaptics()
    val lines = stringArrayResource(R.array.buddy_round)
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(card.days) {
        haptics.medium()
        pop.animateTo(1f, BaMotion.bouncy())
    }
    BaCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Piker(Pose.CHEER, c.accent, card.outfit, Modifier.width(150.dp))
            Bubble(lines[card.days % lines.size])
            Text(
                stringResource(R.string.round_days, PersianDigits.grouped(card.days)),
                style = Ba.type.headline,
                color = c.onAccentSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
            PrimaryButton(stringResource(R.string.round_ok), onClick = onOk, height = 44.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** L1: the chapter-start card. «پیکر» shows the new outfit, the chapter line moves, a letter is offered. */
@Composable
private fun ChapterCard(card: TodayCard.ChapterStart, actions: TodayActions) {
    val c = Ba.colors
    val months = stringArrayResource(R.array.jalali_months)
    val numbers = stringArrayResource(R.array.chapter_numbers)
    val lines = stringArrayResource(R.array.chapter_lines)
    val chapter = card.chapter
    val days = java.time.temporal.ChronoUnit.DAYS.between(chapter.start, chapter.end).toInt() + 1
    BaCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ChapterChain(chapter.number)
            Piker(Pose.LETTER, c.accent, card.outfit, Modifier.width(150.dp))
            Bubble(lines[(chapter.number - 1).coerceIn(0, lines.size - 1)])
            Text(
                stringResource(R.string.chapter_card_title, numbers[chapter.number - 1], months[chapter.months.last() - 1]),
                style = Ba.type.subtitle, color = c.text1,
            )
            Text(stringResource(R.string.chapter_card_text, PersianDigits.format(days)), style = Ba.type.caption, color = c.text2, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(
                    stringResource(R.string.chapter_letter_button, jalaliDayMonth(card.letterOpensOn)),
                    onClick = { actions.writeLetter(false) },
                    icon = BaIcons.Pen,
                    height = 44.dp,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(stringResource(R.string.later), onClick = actions.dismissCard, height = 44.dp)
            }
        }
    }
}

/** Five steps, one per chapter: past ones in the accent, the current one outlined. */
@Composable
private fun ChapterChain(current: Int) {
    val c = Ba.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { n ->
            Box(
                Modifier
                    .size(width = if (n == current) 22.dp else 14.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (n <= current) c.accent else c.border),
            )
        }
    }
}

@Composable
private fun FirstLetterCard(card: TodayCard.FirstLetter, actions: TodayActions) {
    val c = Ba.colors
    BaCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Piker(Pose.LETTER, c.accent, Outfit.NONE, Modifier.width(92.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.first_letter_title), style = Ba.type.subtitle, color = c.text1)
                Text(stringResource(R.string.first_letter_text, jalaliDayMonth(card.opensOn)), style = Ba.type.caption, color = c.text2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            PrimaryButton(stringResource(R.string.first_letter_button), onClick = { actions.writeLetter(true) }, icon = BaIcons.Pen, height = 44.dp, modifier = Modifier.weight(1f))
            SecondaryButton(stringResource(R.string.later), onClick = actions.dismissCard, height = 44.dp)
        }
    }
}

/** L3: a sealed letter on its day. Tap: it opens and the text unfolds. */
@Composable
private fun LetterArrivedCard(card: TodayCard.LetterArrived, actions: TodayActions) {
    val c = Ba.colors
    BaCard(Modifier.clickable(enabled = !card.opened) { actions.openLetter(card.id) }) {
        if (!card.opened) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(BaIcons.Mail, null, tint = c.accent, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.letter_arrived, jalaliDayMonth(card.writtenOn)), style = Ba.type.subtitle, color = c.text1)
                Text(stringResource(R.string.letter_tap_to_open), style = Ba.type.caption, color = c.text2)
            }
        }
        AnimatedVisibility(card.opened, enter = expandVertically(tween(BaMotion.LONG, easing = BaMotion.Standard)) + fadeIn(tween(BaMotion.LONG))) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.letter_from, jalaliDayMonth(card.writtenOn)), style = Ba.type.caption, color = c.text3)
                Text(card.text, style = Ba.type.body, color = c.text1)
            }
        }
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
private fun PartSwitcher(selected: Part, complete: Set<Part>, onSelect: (Part) -> Unit, modifier: Modifier = Modifier) {
    val colors = Ba.colors
    val names = mapOf(
        Part.MORNING to stringResource(R.string.part_morning),
        Part.MIDDAY to stringResource(R.string.part_midday),
        Part.EVENING to stringResource(R.string.part_evening),
        Part.NIGHT to stringResource(R.string.part_night),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(6.dp)
            .selectableGroup(),
    ) {
        for (part in Part.entries) {
            val isSelected = part == selected
            val bg by animateColorAsState(if (isSelected) colors.surface2 else colors.surface, tween(BaMotion.SHORT, easing = BaMotion.Standard), label = "partBg")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(part) })
                    .semantics { contentDescription = names.getValue(part) },
            ) {
                Icon(BaIcons.of(part), contentDescription = null, tint = if (isSelected) colors.text1 else colors.text3, modifier = Modifier.size(24.dp))
                PartDot(part in complete, Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp))
            }
        }
    }
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
private fun TaskTile(tile: TileUi, onToggle: () -> Unit, onPlay: (() -> Unit)? = null) {
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
            .heightIn(min = if (onPlay != null) 84.dp else 108.dp)
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
        if (onPlay != null) {
            WideTileContent(tile, fill.value, checkBg, onPlay)
            return@Box
        }
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

/** The workout tile: tapping it ticks like any tile; the play button opens the session (DESIGN 01, D117). */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.WideTileContent(tile: TileUi, fill: Float, checkBg: androidx.compose.ui.graphics.Color, onPlay: () -> Unit) {
    val colors = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
    ) {
        Icon(BaIcons.of(tile.icon), null, tint = if (tile.done) colors.onAccentSubtle else colors.text2, modifier = Modifier.size(28.dp))
        Text(tile.title, style = Ba.type.tile, color = colors.text1, modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(checkBg)
                .then(if (tile.done) Modifier else Modifier.border(2.dp, colors.border, CircleShape)),
        ) {
            Icon(BaIcons.Check, null, tint = colors.onAccent, modifier = Modifier.size(14.dp).graphicsLayer { alpha = fill })
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable(role = Role.Button, onClick = onPlay),
        ) {
            Icon(BaIcons.Play, stringResource(R.string.cd_start_workout), tint = colors.onAccent, modifier = Modifier.size(22.dp))
        }
    }
}

@Preview(locale = "fa", showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun TodayPreview() {
    val date = LocalDate.of(2026, 11, 9)
    val tiles = mapOf(
        Part.MORNING to listOf(
            TileUi(1, "مسواک", TaskIcon.TOOTH, done = true, important = false, due = false),
            TileUi(2, "پوست", TaskIcon.DROP, done = true, important = true, due = false),
            TileUi(3, "کراتین", TaskIcon.PILL, done = false, important = false, due = false),
            TileUi(5, "ناخن", TaskIcon.SCISSORS, done = false, important = false, due = true),
        ),
    )
    PeykarTheme(mode = ThemeMode.DARK) {
        TodayScreen(
            TodayUiState(
                date, Jalali.fromGregorian(date), 5, 23, HeaderTail.InChapter(8), Part.MORNING, false, tiles, emptyList(), emptySet(), 2, 12,
                listOf(DayStatus.Level(4), DayStatus.Level(2), DayStatus.Level(1), DayStatus.Future, DayStatus.Future, DayStatus.Future, DayStatus.Future), 2,
                emptyList(), BuddyUi(Pose.THUMBS, Outfit.NONE, Moment.TICK, 0, 2, 12, Part.MORNING), HealthBanner.LATE,
            ),
            TodayActions(),
        )
    }
}
