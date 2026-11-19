package app.lantern.ui.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.R
import app.lantern.domain.goals.GoalPlans
import app.lantern.domain.goals.GoalStatus
import app.lantern.domain.text.PersianDigits
import app.lantern.ui.components.*
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.*
import java.time.LocalDate

@Composable
fun GoalsWallRoute(vm: GoalsViewModel, onCreate: () -> Unit, onDetail: (Long) -> Unit, onToday: (() -> Unit)? = null) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return
    val active = s.notes.filter { it.goal.status == GoalStatus.ACTIVE.name }
    val achieved = s.notes.filter { it.goal.status == GoalStatus.ACHIEVED.name }
    val aside = s.notes.filter { it.goal.status == GoalStatus.SET_ASIDE.name }
    val quiet = if (Ba.companion) active.firstOrNull { note ->
        val offered = note.goal.whyOfferedAt?.atZone(java.time.ZoneId.systemDefault())?.minusHours(4)?.toLocalDate()
        val tick = note.actions.flatMap { note.completions[it.id].orEmpty() }.filter { it <= s.today }.maxOrNull()
        GoalPlans.quiet(note.goal.startDate, tick, offered, s.today)
    } else null
    var whyId by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize().sky().statusBarsPadding()) {
        Text(stringResource(R.string.tab_goals), style = Ba.type.headline, color = Ba.colors.text1, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        if (s.notes.isEmpty()) {
            EndLayout(buttons = { PrimaryButton(stringResource(R.string.goal_first), onCreate, Modifier.fillMaxWidth()) }) {
                GoalQuestion(stringResource(R.string.goal_empty))
                Text(stringResource(R.string.goal_empty_help), style = Ba.type.body, color = Ba.colors.text2)
            }
        } else {
            Text(stringResource(R.string.goal_wall_summary, PersianDigits.format(active.size), PersianDigits.format(active.sumOf { it.weekCount(s.today) })), style = Ba.type.body, color = Ba.colors.text2, modifier = Modifier.padding(horizontal = 24.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(155.dp), contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (quiet != null) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    BaCard {
                        Text(stringResource(R.string.goal_why_reminder), style = Ba.type.body, color = Ba.colors.text1)
                        Text(quiet.goal.title, style = Ba.type.subtitle, color = Ba.colors.glow)
                        Row {
                            TextButton(onClick = { whyId = quiet.goal.id; vm.whyOffered(quiet.goal.id) }) { Text(stringResource(R.string.goal_read_why)) }
                            TextButton(onClick = { vm.whyOffered(quiet.goal.id) }) { Text(stringResource(R.string.goal_later)) }
                        }
                    }
                }
                items(active, key = { it.goal.id }) { note ->
                    GoalWallNote(note, s.today, onDetail = { onDetail(note.goal.id) }, onWhy = { whyId = note.goal.id })
                }
                item {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 160.dp).clip(Px.Card).border(1.dp, Ba.colors.border, Px.Card)
                            .combinedClickable(role = Role.Button, onClick = onCreate).padding(16.dp), contentAlignment = Alignment.Center,
                    ) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(BaIcons.Plus, null, tint = Ba.colors.glow, modifier = Modifier.size(28.dp))
                        Text(stringResource(R.string.goal_create), style = Ba.type.subtitle, color = Ba.colors.glow)
                    } }
                }
                if (active.isNotEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.goal_flip_help), style = Ba.type.caption, color = Ba.colors.text3)
                }
                if (achieved.isNotEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.goal_achieved_shelf)) }
                items(achieved, key = { it.goal.id }) { note -> GoalShelfNote(note, onDetail) }
                if (aside.isNotEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.goal_aside_shelf)) }
                items(aside, key = { it.goal.id }) { note -> GoalShelfNote(note, onDetail) }
            }
        }
    }
    s.notes.firstOrNull { it.goal.id == whyId }?.let { note ->
        GoalWhyDialog(note, onDismiss = { whyId = null }, onStep = { whyId = null; if (onToday != null) onToday() else onDetail(note.goal.id) })
    }
}

@Composable
private fun GoalShelfNote(note: GoalNote, onDetail: (Long) -> Unit) {
    BaCard(Modifier.combinedClickable(role = Role.Button, onClick = { onDetail(note.goal.id) })) {
        Text(note.goal.title, style = Ba.type.subtitle, color = Ba.colors.text1)
        Text(stringResource(if (note.goal.status == GoalStatus.ACHIEVED.name) R.string.goal_achieved else R.string.goal_aside), style = Ba.type.caption, color = Ba.colors.text2)
    }
}

@Composable
private fun GoalWallNote(note: GoalNote, today: LocalDate, onDetail: () -> Unit, onWhy: () -> Unit) {
    var flipped by rememberSaveable(note.goal.id) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (flipped) 180f else 0f, tween(BaMotion.MEDIUM), label = "noteFlip")
    val back = turn > 90f
    val c = Ba.colors
    val companion = Ba.companion
    Column(
        Modifier.fillMaxWidth().graphicsLayer {
            rotationY = turn
            cameraDistance = 12 * density
            rotationZ = if (companion) (if (note.goal.id % 2 == 0L) -1.4f else 1.2f) else 0f
        }.clip(Px.Card).background(c.surface).border(1.dp, c.border, Px.Card)
            .combinedClickable(role = Role.Button, onClickLabel = stringResource(R.string.goal_flip), onLongClickLabel = stringResource(R.string.goal_read_why), onLongClick = onWhy, onClick = { flipped = !flipped })
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.graphicsLayer { if (back) rotationY = 180f }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(9.dp).clip(Px.Small).background(c.glow).align(Alignment.CenterHorizontally))
            Text(stringResource(R.string.goal_until, jalaliDayMonth(note.goal.finishDate)), style = Ba.type.caption, color = c.text3)
            Text(note.goal.title, style = Ba.type.title, color = c.text1)
            if (back) {
                Text(stringResource(R.string.goal_this_week), style = Ba.type.caption, color = c.glow)
                note.activeActions.forEach { Text(it.title, style = Ba.type.body, color = c.text2) }
                GoalWeekStrip(note, today)
                Text(stringResource(R.string.goal_week_progress, PersianDigits.format(note.weekCount(today)), PersianDigits.format(note.goal.weeklyLow), PersianDigits.format(note.goal.weeklyHigh)), style = Ba.type.caption, color = c.glow)
            } else {
                val month = note.month(today)
                Text(stringResource(if (note.milestones.isEmpty()) R.string.goal_whole else R.string.goal_this_month), style = Ba.type.caption, color = c.glow)
                Text(month?.statement?.ifBlank { null } ?: if (note.milestones.isEmpty()) note.goal.title else stringResource(R.string.goal_not_sure), style = Ba.type.body, color = c.text2)
                GoalProgress(note.progress(today))
            }
            TextButton(onClick = onDetail, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.goal_open_detail), style = Ba.type.label, color = c.glow) }
        }
    }
}

@Composable
internal fun GoalProgress(progress: Float) {
    Box(Modifier.fillMaxWidth().height(5.dp).clip(Px.Small).background(Ba.colors.surface2)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Ba.colors.glow))
    }
}

@Composable
internal fun GoalWeekStrip(note: GoalNote, today: LocalDate) {
    val names = stringArrayResource(R.array.weekdays_short)
    val days = GoalPlans.weekDays(today)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        days.forEachIndexed { index, day ->
            val done = note.sessions(day, day) > 0
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(names[index], style = Ba.type.caption, color = Ba.colors.text3)
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(Px.Small)
                    .background(if (done) Ba.colors.accent else if (day <= today && day >= note.goal.startDate) Ba.colors.surface2 else androidx.compose.ui.graphics.Color.Transparent)
                    .border(1.dp, if (done) Ba.colors.accent else Ba.colors.border, Px.Small))
            }
        }
    }
}

@Composable
internal fun GoalWhyDialog(note: GoalNote, onDismiss: () -> Unit, onStep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_why_reminder)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(note.goal.title, style = Ba.type.subtitle, color = Ba.colors.glow)
            Text(note.goal.why, style = Ba.type.body, color = Ba.colors.text1)
        } },
        confirmButton = { TextButton(onClick = onStep) { Text(stringResource(R.string.goal_small_step)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.goal_later)) } },
    )
}
