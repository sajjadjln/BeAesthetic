package app.lantern.ui.today

import androidx.annotation.ArrayRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.RoundIconButton
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.rememberHaptics
import app.lantern.ui.theme.sky

/** The character's lines for the moment (strings: lines_<character>_<pool>). */
@ArrayRes
private fun linesRes(character: LanternCharacter, pool: TodayPick.Lines): Int = when (character) {
    LanternCharacter.KNIGHT -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_knight_calm; TodayPick.Lines.BUSY -> R.array.lines_knight_busy; TodayPick.Lines.HAPPY -> R.array.lines_knight_happy; TodayPick.Lines.NIGHT -> R.array.lines_knight_night }
    LanternCharacter.VAMPIRE -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_vampire_calm; TodayPick.Lines.BUSY -> R.array.lines_vampire_busy; TodayPick.Lines.HAPPY -> R.array.lines_vampire_happy; TodayPick.Lines.NIGHT -> R.array.lines_vampire_night }
    LanternCharacter.WIZARD -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_wizard_calm; TodayPick.Lines.BUSY -> R.array.lines_wizard_busy; TodayPick.Lines.HAPPY -> R.array.lines_wizard_happy; TodayPick.Lines.NIGHT -> R.array.lines_wizard_night }
    LanternCharacter.GOTH -> when (pool) { TodayPick.Lines.CALM -> R.array.lines_goth_calm; TodayPick.Lines.BUSY -> R.array.lines_goth_busy; TodayPick.Lines.HAPPY -> R.array.lines_goth_happy; TodayPick.Lines.NIGHT -> R.array.lines_goth_night }
}

private val partNames = listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night)

/**
 * Today, layout 1 (D186, today-v5.html): the date in the character's color, the character with its line
 * in the pixel dialog box, one progress block per task, the «الان» card, then every part of the day and
 * «هر وقت» in one scroll. Today only: no week row.
 */
@Composable
internal fun TodayList(state: TodayUiState, actions: TodayActions) {
    val c = Ba.colors
    val haptics = rememberHaptics()
    val tiles = state.tiles
    val now = TodayPick.now(tiles, state.currentPart)
    val pool = TodayPick.lines(state.done, state.total, state.currentPart)
    var tap by remember(pool) { mutableIntStateOf(0) }
    val lines = stringArrayResource(linesRes(state.character, pool))
    val line = lines[tap.mod(lines.size)].format(now?.title ?: "")

    Box(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.fillMaxSize()) {
            item { Header(state, actions) }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    val name = stringResource(state.character.nameRes)
                    Box(contentAlignment = Alignment.BottomCenter) {
                        CharacterPortrait(
                            state.character,
                            TodayPick.mood(state.done, state.total, state.currentPart),
                            Modifier.size(128.dp).clip(Px.Card).clickable(role = Role.Button) { tap++ }.semantics { contentDescription = name },
                        )
                        Text(name, style = Ba.type.caption, color = c.onGlow, modifier = Modifier.background(c.glow).padding(horizontal = 9.dp, vertical = 3.dp))
                    }
                    PixelDialog(line, Modifier.weight(1f).clickable { tap++ })
                }
            }
            item { Progress(state.done, state.total) }
            item { NowCard(now, state, onTick = { id -> haptics.light(); actions.toggle(id) }) }
            Part.entries.forEach { part ->
                val list = tiles[part].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "h$part") { SectionHeader(stringResource(partNames[part.ordinal]), current = part == state.currentPart) }
                    items(list, key = { it.taskId }) { tile ->
                        PixelTile(tile) { if (!tile.done) haptics.light(); actions.toggle(tile.taskId) }
                    }
                }
            }
            if (state.anytime.isNotEmpty()) {
                item { AnytimeCard(state.anytime, state.anytimeOpen, actions, Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) }
            }
            state.banner?.let { item { HealthBannerRow(it, actions.fixReminders, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) } }
            state.backup?.let { item { BackupRow(it, actions.openBackup, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) } }
        }
        AddButton(actions.addTask, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun Header(state: TodayUiState, actions: TodayActions) {
    val c = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
    ) {
        Text(
            weekdays[Weekdays.index(state.date.dayOfWeek)] + " " + jalaliDayMonth(state.date),
            style = Ba.type.headline,
            color = c.glow,
            modifier = Modifier.weight(1f),
        )
        RoundIconButton(BaIcons.Leaf, stringResource(R.string.cd_minimum_day), actions.toggleMinimum, selected = state.minimumMode)
        RoundIconButton(BaIcons.Gear, stringResource(R.string.cd_settings), actions.openSettings)
    }
}

/** One pixel block per task, green once done, and «۲ از ۷». */
@Composable
private fun Progress(done: Int, total: Int) {
    val c = Ba.colors
    val text = stringResource(R.string.today_count, PersianDigits.format(done), PersianDigits.format(total))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).semantics(mergeDescendants = true) { contentDescription = text },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            repeat(total.coerceAtLeast(1)) { i ->
                val on = i < done
                val fill by animateColorAsState(if (on) c.accent else Color.White.copy(alpha = 0.07f), tween(300), label = "block")
                Box(Modifier.weight(1f).height(10.dp).background(fill).then(if (on) Modifier else Modifier.border(2.dp, Color.White.copy(alpha = 0.12f))))
            }
        }
        Text(text, style = Ba.type.label, color = c.text2)
    }
}

/** «الان»: the one thing to do now, ticked in one tap. Empty when the day is done (or at night). */
@Composable
private fun NowCard(now: TileUi?, state: TodayUiState, onTick: (Long) -> Unit) {
    val c = Ba.colors
    val modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(Px.Card)
        .background(c.glow.copy(alpha = 0.10f))
        .border(2.dp, c.glow.copy(alpha = 0.55f), Px.Card)
    if (now == null || state.currentPart == Part.NIGHT && now.part != Part.NIGHT) {
        val text = stringResource(if (now == null) R.string.today_all_done else R.string.today_night_rest)
        Text(text, style = Ba.type.subtitle, color = c.text2, textAlign = TextAlign.Center, modifier = modifier.padding(16.dp))
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .toggleable(false, role = Role.Checkbox) { onTick(now.taskId) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.today_now), style = Ba.type.caption, color = c.glow)
            Text(now.title, style = Ba.type.title, color = c.text1)
            if (now.due) Text(stringResource(R.string.tag_due), style = Ba.type.caption, color = c.amber)
        }
        PixelCheck(false, Modifier.size(34.dp))
    }
}

@Composable
private fun SectionHeader(text: String, current: Boolean) {
    val c = Ba.colors
    val color = if (current) c.glow else c.text3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(text, style = Ba.type.label, color = color)
        // A dashed pixel line to the end.
        Spacer(Modifier.weight(1f).height(2.dp).drawBehind {
            drawLine(Color.White.copy(alpha = 0.16f), center.copy(x = 0f), center.copy(x = size.width), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        })
    }
}

/** A task row: pixel corners, green when done, amber «موعدش رسیده» when due, «مهم» when important. */
@Composable
private fun PixelTile(tile: TileUi, onToggle: () -> Unit) {
    val c = Ba.colors
    val bg by animateColorAsState(if (tile.done) c.accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f), tween(250), label = "tileBg")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(Px.Card)
            .background(bg)
            .border(2.dp, if (tile.done) c.accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), Px.Card)
            .toggleable(tile.done, role = Role.Checkbox) { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(tile.title, style = Ba.type.tile, color = if (tile.done) c.text2 else c.text1)
            when {
                tile.done -> Unit
                tile.due -> Text(stringResource(R.string.tag_due), style = Ba.type.caption, color = c.amber)
                tile.important -> Text(stringResource(R.string.tag_important), style = Ba.type.caption, color = c.amber)
            }
        }
        PixelCheck(tile.done, Modifier.size(26.dp))
    }
}

/** A square pixel check box; filled green with a pixel tick when done. */
@Composable
private fun PixelCheck(done: Boolean, modifier: Modifier) {
    val c = Ba.colors
    Box(
        modifier
            .clip(Px.Small)
            .background(if (done) c.accent else Color.Transparent)
            .then(if (done) Modifier else Modifier.border(2.dp, Color.White.copy(alpha = 0.28f), Px.Small))
            .drawBehind {
                if (!done) return@drawBehind
                val u = size.width / 8f
                listOf(1f to 4f, 2f to 5f, 3f to 4f, 4f to 3f, 5f to 2f).forEach { (x, y) ->
                    drawRect(c.onAccent, androidx.compose.ui.geometry.Offset(x * u, y * u), androidx.compose.ui.geometry.Size(u * 1.2f, u * 1.2f))
                }
            },
    )
}

/** «+ کار جدید» as a pixel button in the character's color. */
@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier) {
    val c = Ba.colors
    val label = stringResource(R.string.tasks_new)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .clip(Px.Card)
            .background(c.glow)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(BaIcons.Plus, null, tint = c.onGlow, modifier = Modifier.size(24.dp))
    }
}
