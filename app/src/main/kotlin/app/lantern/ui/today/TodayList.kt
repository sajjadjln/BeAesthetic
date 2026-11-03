package app.lantern.ui.today

import androidx.annotation.ArrayRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lantern.R
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import app.lantern.ui.components.ADD_RISE
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.RoundIconButton
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.TodayLayout
import app.lantern.ui.theme.drawTick
import app.lantern.ui.theme.hop
import app.lantern.ui.theme.playHop
import app.lantern.ui.theme.rememberHaptics
import app.lantern.ui.theme.riseIn
import app.lantern.ui.theme.sky
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The character's lines for the moment (strings: lines_<character>_<pool>). */
@ArrayRes
private fun linesRes(character: LanternCharacter, pool: TodayPick.Lines): Int = when (character) {
    LanternCharacter.KNIGHT -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_knight_calm; TodayPick.Lines.BUSY -> R.array.lines_knight_busy; TodayPick.Lines.HAPPY -> R.array.lines_knight_happy; TodayPick.Lines.NIGHT -> R.array.lines_knight_night }
    LanternCharacter.VAMPIRE -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_vampire_calm; TodayPick.Lines.BUSY -> R.array.lines_vampire_busy; TodayPick.Lines.HAPPY -> R.array.lines_vampire_happy; TodayPick.Lines.NIGHT -> R.array.lines_vampire_night }
    LanternCharacter.WIZARD -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_wizard_calm; TodayPick.Lines.BUSY -> R.array.lines_wizard_busy; TodayPick.Lines.HAPPY -> R.array.lines_wizard_happy; TodayPick.Lines.NIGHT -> R.array.lines_wizard_night }
    LanternCharacter.GOTH -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_goth_calm; TodayPick.Lines.BUSY -> R.array.lines_goth_busy; TodayPick.Lines.HAPPY -> R.array.lines_goth_happy; TodayPick.Lines.NIGHT -> R.array.lines_goth_night }
}

internal val partNames = listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night)

/**
 * Today (D186, D188, today-v5.html): the date in the character's color, the character with its line in the
 * pixel dialog box, one progress block per task, then the layout's body: the list (the «الان» card and every
 * part of the day in one scroll) or the parts slider (a 4-part pixel bar over swipeable pages). A minimum day
 * shows only its floor tasks in both. «هر وقت» and the reminder and backup rows close the page. Today only:
 * no week row. The motion is D196 (today-motion.html); [celebrate] counts the 100% moments (T3).
 */
@Composable
internal fun TodayBody(state: TodayUiState, actions: TodayActions, celebrate: Int = 0) {
    val c = Ba.colors
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val tiles = state.tiles
    val now = TodayPick.now(tiles, state.currentPart)
    val pool = TodayPick.lines(state.done, state.total, state.currentPart)
    var tap by remember(pool) { mutableIntStateOf(0) }
    val lines = stringArrayResource(linesRes(state.character, pool))
    val line = lines[tap.mod(lines.size)].format(now?.title ?: "")
    val listState = rememberLazyListState()

    // T5: a task that appears while Today is open (just saved in the sheet) glows once and scrolls into view.
    val ids = tiles.values.flatten().map { it.taskId }.toSet()
    val known = remember(state.date) { arrayOfNulls<Set<Long>>(1) }
    val glowing = remember(state.date) { mutableStateMapOf<Long, Unit>() }
    val arrived = known[0]?.let { ids - it }.orEmpty()
    val fresh = glowing.keys + arrived
    SideEffect {
        known[0] = ids
        arrived.forEach { id -> glowing[id] = Unit; scope.launch { delay(2500); glowing.remove(id) } }
    }
    LaunchedEffect(arrived) {
        val id = arrived.firstOrNull() ?: return@LaunchedEffect
        val index = lazyIndexOf(state, id) ?: return@LaunchedEffect
        delay(250)
        val info = listState.layoutInfo
        val seen = info.visibleItemsInfo.firstOrNull { it.index == index }
        val fits = seen != null && seen.offset >= info.viewportStartOffset && seen.offset + seen.size <= info.viewportEndOffset - 200
        if (!fits) listState.animateScrollToItem(index, -(info.viewportSize.height / 3))
    }

    // T6 and T3: the character hops on a tap and at the 100% moment, with two rings at the moment.
    val hopT = remember { Animatable(1f) }
    var hopHigh by remember { mutableStateOf(10.dp) }
    val rings = remember { Animatable(1f) }
    val firstCelebrate = remember { celebrate }
    LaunchedEffect(celebrate) {
        if (celebrate == firstCelebrate) return@LaunchedEffect
        delay(260L + (state.total * 55 * 0.6).toLong())
        launch { rings.snapTo(0f); rings.animateTo(1f, tween(900, easing = LinearEasing)) }
        hopHigh = 16.dp
        hopT.playHop()
    }
    val tapCharacter = {
        tap++
        scope.launch { hopHigh = 10.dp; hopT.playHop() }
        Unit
    }

    val floor = minimumTiles(state)
    val companion = Ba.companion
    Box(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = ADD_RISE + 16.dp), modifier = Modifier.fillMaxSize()) {
            item(key = "header") { Header(state, actions, Modifier.riseIn(0)) }
            if (!companion) item(key = "summary") { SimpleSummary(state, Modifier.riseIn(1)) }
            else item(key = "character") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().riseIn(1).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    val name = stringResource(state.character.nameRes)
                    Box(contentAlignment = Alignment.BottomCenter) {
                        CharacterPortrait(
                            state.character,
                            TodayPick.mood(state.done, state.total, state.currentPart),
                            // No clip and no ripple: either would show the square the round glow sits in.
                            Modifier.size(128.dp)
                                .drawBehind { drawRings(rings.value, c.glow) }
                                .hop({ hopT.value }, { hopHigh })
                                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = tapCharacter)
                                .semantics { contentDescription = name },
                        )
                        Text(name, style = Ba.type.caption.copy(fontWeight = FontWeight.Bold), color = c.onGlow, modifier = Modifier.background(c.glow).padding(horizontal = 9.dp, vertical = 3.dp))
                    }
                    PixelDialog(line, Modifier.weight(1f).clickable(onClick = tapCharacter))
                }
            }
            item(key = "progress") { Progress(state.done, state.total, celebrate, Modifier.riseIn(2)) }
            val tick = { tile: TileUi -> if (!tile.done) haptics.light(); actions.toggle(tile.taskId) }
            when {
                state.minimumMode -> {
                    item(key = "minimum") { SectionHeader(stringResource(R.string.today_minimum_label), current = true, Modifier.animateItem()) }
                    items(floor, key = { it.taskId }) { tile -> PixelTile(tile, Modifier.animateItem(), fresh = tile.taskId in fresh) { tick(tile) } }
                }
                state.layout == TodayLayout.LIST -> {
                    item(key = "now") { NowCard(now, state, Modifier.riseIn(3).animateItem(), onTick = { id -> haptics.light(); actions.toggle(id) }) }
                    var order = 4
                    Part.entries.forEach { part ->
                        val list = tiles[part].orEmpty()
                        if (list.isNotEmpty()) {
                            val rise = order++
                            item(key = "h$part") { SectionHeader(stringResource(partNames[part.ordinal]), current = part == state.currentPart, Modifier.riseIn(rise).animateItem()) }
                            items(list, key = { it.taskId }) { tile -> PixelTile(tile, Modifier.riseIn(rise).animateItem(), fresh = tile.taskId in fresh) { tick(tile) } }
                        }
                    }
                }
                else -> item(key = "parts") { Box(Modifier.riseIn(3)) { PartsSlider(state, actions.selectPart, fresh, tick) } }
            }
            if (state.anytime.isNotEmpty()) {
                item(key = "anytime") { AnytimeCard(state.anytime, state.anytimeOpen, actions, Modifier.animateItem().padding(start = 16.dp, end = 16.dp, top = 20.dp)) }
            }
            state.banner?.let { item(key = "banner") { HealthBannerRow(it, actions.fixReminders, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) } }
            state.backup?.let { item(key = "backup") { BackupRow(it, actions.openBackup, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) } }
        }
    }
}

/** Where a task sits in Today's lazy list, to scroll a new one into view. Mirrors the items above. */
private fun lazyIndexOf(state: TodayUiState, id: Long): Int? {
    if (state.minimumMode) return state.floorTiles.indexOfFirst { it.taskId == id }.takeIf { it >= 0 }?.plus(4)
    if (state.layout != TodayLayout.LIST) return 3
    var i = 4
    for (part in Part.entries) {
        val list = state.tiles[part].orEmpty()
        if (list.isEmpty()) continue
        i++
        val at = list.indexOfFirst { it.taskId == id }
        if (at >= 0) return i + at
        i += list.size
    }
    return null
}

/** T3: two rings spread from the portrait; [t] runs 0 → 1 over 900 ms. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRings(t: Float, color: Color) {
    if (t <= 0f || t >= 1f) return
    val e = t * 900f
    val r0 = size.minDimension / 2f * 0.88f
    val a = (e / 700f).coerceIn(0f, 1f)
    val b = ((e - 140f) / 760f).coerceIn(0f, 1f)
    val ease = { x: Float -> CubicBezierEasing(0.1f, 0.6f, 0.3f, 1f).transform(x) }
    if (a in 0.001f..0.999f) drawCircle(color.copy(alpha = 0.9f * (1 - a)), r0 * (0.8f + 0.75f * ease(a)), style = Stroke(3.dp.toPx()))
    if (b in 0.001f..0.999f) drawCircle(color.copy(alpha = 0.6f * (1 - b)), r0 * (0.8f + 1.1f * ease(b)), style = Stroke(3.dp.toPx()))
}

/**
 * Simple's top of Today (D200), in place of the character: «۳ از ۹» and one quiet line. Never a list of what
 * was missed; done stays green in the blocks under it.
 */
@Composable
private fun SimpleSummary(state: TodayUiState, modifier: Modifier) {
    val c = Ba.colors
    val line = when {
        state.total > 0 && state.done >= state.total -> R.string.today_all_done
        state.done * 2 >= state.total && state.done > 0 -> R.string.simple_half
        else -> R.string.simple_start
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clip(Px.Card).background(Color.White.copy(alpha = 0.055f)).padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(PersianDigits.format(state.done), style = Ba.type.headline.copy(fontSize = 30.sp, lineHeight = 34.sp), color = c.text1)
            Text(stringResource(R.string.simple_of, PersianDigits.format(state.total)), style = Ba.type.label, color = c.text2, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(stringResource(line), style = Ba.type.label, color = c.text2, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Header(state: TodayUiState, actions: TodayActions, modifier: Modifier) {
    val c = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    // T9: the leaf turns when the minimum day goes on or off.
    val leaf = remember { Animatable(0f) }
    val wasMinimum = remember { booleanArrayOf(state.minimumMode) }
    LaunchedEffect(state.minimumMode) {
        if (state.minimumMode == wasMinimum[0]) return@LaunchedEffect
        wasMinimum[0] = state.minimumMode
        leaf.snapTo(-30f)
        leaf.animateTo(0f, BaMotion.bouncy())
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
    ) {
        Text(
            weekdays[Weekdays.index(state.date.dayOfWeek)] + " " + jalaliDayMonth(state.date),
            style = Ba.type.headline,
            color = c.glow,
            modifier = Modifier.weight(1f).clip(Px.Small).clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_open_history), onClick = actions.openHistory),
        )
        RoundIconButton(
            BaIcons.Leaf, stringResource(R.string.cd_minimum_day), actions.toggleMinimum,
            Modifier.graphicsLayer { rotationZ = leaf.value }, selected = state.minimumMode,
        )
        RoundIconButton(BaIcons.Gear, stringResource(R.string.cd_settings), actions.openSettings)
    }
}

/**
 * One pixel block per task, green once done, and «۲ از ۷». The block that just filled drops in (T2); a new
 * task's block grows in (T5); at the 100% moment a light runs across them in the direction of time (T3).
 */
@Composable
private fun Progress(done: Int, total: Int, celebrate: Int, modifier: Modifier) {
    val c = Ba.colors
    val text = stringResource(R.string.today_count, PersianDigits.format(done), PersianDigits.format(total))
    val count = total.coerceAtLeast(1)
    val shown = remember { intArrayOf(count) }
    val firstCelebrate = remember { celebrate }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).semantics(mergeDescendants = true) { contentDescription = text },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            repeat(count) { i ->
                key(i) { ProgressBlock(on = i < done, isNew = i >= shown[0], wave = if (celebrate == firstCelebrate) 0 else celebrate, index = i) }
            }
        }
        if (Ba.companion) Text(text, style = Ba.type.label, color = c.text2)
    }
    SideEffect { shown[0] = count }
}

@Composable
private fun RowScope.ProgressBlock(on: Boolean, isNew: Boolean, wave: Int, index: Int) {
    val c = Ba.colors
    val fill by animateColorAsState(if (on) c.accent else Color.White.copy(alpha = if (Ba.companion) 0.07f else 0.1f), tween(300), label = "block")
    // Simple (D200): thin rounded segments instead of pixel blocks.
    val flat = !Ba.companion
    val grow = remember { Animatable(if (isNew) 0f else 1f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(350, easing = BaMotion.Standard)) }
    val drop = remember { Animatable(1f) }
    val wasOn = remember { booleanArrayOf(on) }
    LaunchedEffect(on) {
        if (on && !wasOn[0]) { drop.snapTo(0f); drop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
        wasOn[0] = on
    }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(wave) {
        if (wave == 0) return@LaunchedEffect
        delay(260L + index * 55L)
        pulse.animateTo(1f, tween(170))
        pulse.animateTo(0f, tween(250))
    }
    Box(
        Modifier
            .weight(grow.value.coerceAtLeast(0.001f))
            .height(if (flat) 6.dp else 10.dp)
            .graphicsLayer {
                val d = 1 - drop.value
                translationY = -8.dp.toPx() * d
                scaleY = 1 + 0.5f * d + pulse.value
                alpha = grow.value.coerceIn(0f, 1f)
            }
            .then(if (flat) Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)) else Modifier)
            .background(fill)
            .then(if (on || flat) Modifier else Modifier.border(2.dp, Color.White.copy(alpha = 0.12f)))
            .drawWithContent {
                drawContent()
                val light = (0.45f * (1 - drop.value) + 0.5f * pulse.value).coerceIn(0f, 1f)
                if (light > 0f) drawRect(Color.White.copy(alpha = light))
            },
    )
}

/**
 * «الان»: the one thing to do now, ticked in one tap. Empty when the day is done (or at night). T4: the done
 * task slides toward the past and the next comes in from the future side (right to left in Persian), and the
 * card glows once.
 */
@Composable
private fun NowCard(now: TileUi?, state: TodayUiState, modifier: Modifier, onTick: (Long) -> Unit) {
    val c = Ba.colors
    val night = now != null && state.currentPart == Part.NIGHT && now.part != Part.NIGHT
    val shownId: Long = when { now == null -> ALL_DONE; night -> NIGHT_REST; else -> now.taskId }
    val flash = remember { Animatable(0f) }
    val wasId = remember { longArrayOf(shownId) }
    LaunchedEffect(shownId) {
        if (shownId == wasId[0]) return@LaunchedEffect
        wasId[0] = shownId
        delay(90)
        flash.snapTo(1f)
        flash.animateTo(0f, tween(600))
    }
    val toPast = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1 else -1
    val shift = with(LocalDensity.current) { 56.dp.roundToPx() }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(Px.Card)
            .background(c.glow.copy(alpha = 0.10f))
            .drawBehind { if (flash.value > 0f) drawRect(c.glow.copy(alpha = 0.16f * flash.value)) }
            .border(2.dp, c.glow.copy(alpha = 0.55f), Px.Card),
    ) {
        AnimatedContent(
            shownId,
            transitionSpec = {
                (slideInHorizontally(tween(320, delayMillis = 90, easing = BaMotion.Standard)) { -toPast * shift } + fadeIn(tween(320, delayMillis = 90)))
                    .togetherWith(slideOutHorizontally(tween(200, easing = BaMotion.Close)) { toPast * shift } + fadeOut(tween(200)))
                    .using(SizeTransform(clip = false))
            },
            label = "now",
        ) { id ->
            val tile = state.tiles.values.flatten().firstOrNull { it.taskId == id }
            if (id < 0 || tile == null) {
                val text = stringResource(if (id == NIGHT_REST) R.string.today_night_rest else R.string.today_all_done)
                Text(text, style = Ba.type.subtitle, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(tile.done, role = Role.Checkbox) { if (!tile.done) onTick(tile.taskId) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.today_now), style = Ba.type.caption, color = c.glow)
                        Text(tile.title, style = Ba.type.title, color = c.text1)
                        if (tile.due) Text(stringResource(R.string.tag_due), style = Ba.type.caption, color = c.amber)
                    }
                    PixelCheck(tile.done, Modifier.size(34.dp))
                }
            }
        }
    }
}

private const val ALL_DONE = -1L
private const val NIGHT_REST = -2L

@Composable
internal fun SectionHeader(text: String, current: Boolean, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val color = if (current) c.glow else c.text3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(text, style = Ba.type.title, color = color)
        // A dashed pixel line to the end; none in Simple.
        val dashed = Ba.companion
        Spacer(Modifier.weight(1f).height(2.dp).drawBehind {
            if (dashed) drawLine(Color.White.copy(alpha = 0.16f), center.copy(x = 0f), center.copy(x = size.width), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        })
    }
}

/**
 * A task row: pixel corners, green when done, amber «موعدش رسیده» when due, «مهم» when important. A [fresh]
 * one (just added) glows once in the character's color (T5).
 */
@Composable
internal fun PixelTile(tile: TileUi, modifier: Modifier = Modifier, fresh: Boolean = false, onToggle: () -> Unit) {
    val c = Ba.colors
    val bg by animateColorAsState(if (tile.done) c.accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f), tween(250), label = "tileBg")
    val glow = remember { Animatable(if (fresh) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!fresh) return@LaunchedEffect
        delay(550)
        glow.animateTo(0f, tween(1100, easing = LinearOutSlowInEasing))
    }
    val edge = if (tile.done) c.accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(Px.Card)
            .background(bg)
            .drawBehind { if (glow.value > 0f) drawRect(c.glow.copy(alpha = 0.32f * glow.value)) }
            .border(2.dp, lerp(edge, c.glow, glow.value), Px.Card)
            .toggleable(tile.done, role = Role.Checkbox) { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(tile.title, style = Ba.type.tile.copy(fontSize = 17.sp, lineHeight = 26.sp), color = if (tile.done) c.text2 else c.text1)
            when {
                tile.done -> Unit
                tile.due -> Text(stringResource(R.string.tag_due), style = Ba.type.label, color = c.amber)
                tile.important -> Text(stringResource(R.string.tag_important), style = Ba.type.label, color = c.amber)
            }
        }
        PixelCheck(tile.done, Modifier.size(26.dp))
    }
}

/**
 * A square pixel check box; filled green with a pixel tick when done. T1: ticking pops it (shrink, overshoot,
 * settle), draws the tick one pixel at a time and sends eight pixel sparks out in green and the character's
 * color. Unticking is quiet.
 */
@Composable
internal fun PixelCheck(done: Boolean, modifier: Modifier) {
    val c = Ba.colors
    val pixel = Ba.companion
    val pop = remember { Animatable(1f) }
    val tick = remember { Animatable(if (done) 1f else 0f) }
    val sparks = remember { Animatable(1f) }
    val was = remember { booleanArrayOf(done) }
    LaunchedEffect(done) {
        val ticked = done && !was[0]
        was[0] = done
        if (!done) { tick.snapTo(0f); return@LaunchedEffect }
        if (!ticked) { tick.snapTo(1f); return@LaunchedEffect }
        launch { pop.snapTo(0.55f); pop.animateTo(1f, BaMotion.bouncy()) }
        launch { tick.snapTo(0f); delay(60); tick.animateTo(1f, tween(250, easing = LinearEasing)) }
        launch { sparks.snapTo(0f); sparks.animateTo(1f, tween(420, easing = CubicBezierEasing(0.1f, 0.7f, 0.3f, 1f))) }
    }
    Box(
        modifier
            .drawBehind {
                val t = sparks.value
                if (t >= 1f || !pixel) return@drawBehind
                val side = 5.dp.toPx()
                for (i in 0 until 8) {
                    val a = i / 8f * 2 * PI.toFloat() + 0.3f
                    val d = (20 + (i % 3) * 7).dp.toPx() * t
                    val at = center + Offset(cos(a) * d - side / 2, sin(a) * d - side / 2)
                    drawRect((if (i % 2 == 1) c.accent else c.glow).copy(alpha = 1 - t), at, Size(side, side))
                }
            }
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(Px.Small)
            .background(if (done) c.accent else Color.Transparent)
            .then(if (done) Modifier else Modifier.border(2.dp, Color.White.copy(alpha = 0.28f), Px.Small))
            .drawBehind { if (done) drawTick(c.onAccent, tick.value, pixel) },
    )
}

/** The floor tasks of a minimum day, all parts at once; a repeated name («مسواک» morning and night) gets its part. */
@Composable
private fun minimumTiles(state: TodayUiState): List<TileUi> {
    val names = partNames.map { stringResource(it) }
    val repeated = state.floorTiles.groupingBy { it.title }.eachCount().filterValues { it > 1 }.keys
    return state.floorTiles.map { if (it.title in repeated) it.copy(title = it.title + " · " + names[it.part.ordinal]) else it }
}
