package app.lantern.ui.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.data.repo.GoalView
import app.lantern.domain.goals.GoalRules
import app.lantern.domain.goals.GoalState
import app.lantern.domain.goals.WeekDay
import app.lantern.domain.text.PersianDigits
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.goalDate
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LocalCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.rememberAnimationsOn
import app.lantern.ui.theme.sky
import java.time.LocalDate

/**
 * The goals wall (W2, D222): a pinned note per active goal, this month on the front, a tap flips it to
 * this week. A dashed «هدف تازه» note starts the flow; ideas are slips under the wall. With no goal yet,
 * the empty wall: the companion and one button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsWall(
    goals: List<GoalView>,
    today: LocalDate,
    onNew: () -> Unit,
    onOpenIdea: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onHold: (Long) -> Unit,
    offer: GoalView? = null,
    onOfferRead: () -> Unit = {},
    onOfferDismiss: () -> Unit = {},
) {
    val c = Ba.colors
    val active = goals.filter { it.state == GoalState.ACTIVE }
    val ideas = goals.filter { it.state == GoalState.IDEA || it.state == GoalState.SET_ASIDE }
    val reached = goals.filter { it.state == GoalState.REACHED }
    if (active.isEmpty() && ideas.isEmpty() && reached.isEmpty()) return EmptyWall(onNew)

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().sky().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(stringResource(R.string.goals_title), style = Ba.type.headline, color = c.text1)
        if (active.isNotEmpty()) {
            val steps = active.sumOf { it.week?.ticks ?: 0 }
            val line = if (active.size == 1) stringResource(R.string.goals_line_one, PersianDigits.format(steps))
            else stringResource(R.string.goals_line, PersianDigits.format(active.size), PersianDigits.format(steps))
            if (Ba.companion) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CharacterPortrait(LocalCharacter.current, Mood.CALM, Modifier.size(64.dp))
                    PixelDialog(line, Modifier.weight(1f))
                }
            } else {
                Text(line, style = Ba.type.body, color = c.text2)
            }
        }
        offer?.let { QuietOffer(it.goal.title, onOfferRead, onOfferDismiss) }
        // Two notes a row, each tilted a little like a real pinned note (none when animations are off is fine: the tilt is still).
        val cells: List<GoalView?> = active + listOf(null)
        cells.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { col, g ->
                    val tilt = if ((row + col) % 2 == 0) -1.2f else 1.2f
                    Box(Modifier.weight(1f)) {
                        if (g == null) NewNote(onNew) else GoalNote(g, today, tilt, onOpen = { onOpenGoal(g.id) }, onHold = { onHold(g.id) })
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (active.isNotEmpty()) {
            Text(stringResource(R.string.goals_flip_hint), style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        if (ideas.isNotEmpty()) {
            Text(stringResource(R.string.goals_ideas, PersianDigits.format(ideas.size)), style = Ba.type.label, color = c.text2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ideas.forEach { idea -> Slip(idea.goal.title) { onOpenIdea(idea.id) } }
            }
        }
        if (reached.isNotEmpty()) {
            Text(stringResource(R.string.goals_reached, PersianDigits.format(reached.size)), style = Ba.type.label, color = c.text2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reached.forEach { g -> Slip(g.goal.title, done = true) { onOpenGoal(g.id) } }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A note: the front is this month, the back this week. A tap flips it; holding it opens the why (G4). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalNote(g: GoalView, today: LocalDate, tilt: Float, onOpen: () -> Unit, onHold: () -> Unit) {
    val c = Ba.colors
    var back by rememberSaveable(g.id) { mutableStateOf(false) }
    val animate = rememberAnimationsOn()
    val turn by animateFloatAsState(if (back) 180f else 0f, if (animate) tween(420) else tween(0), label = "flip")
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .graphicsLayer { rotationZ = tilt; rotationY = turn; cameraDistance = 12f * density }
            .clip(Px.Card)
            .background(c.surface)
            .border(2.dp, c.rimDeep, Px.Card)
            .combinedClickable(role = Role.Button, onClick = { back = !back }, onLongClick = onHold)
            .padding(12.dp),
    ) {
        // The pin.
        Box(Modifier.align(Alignment.TopCenter).size(10.dp).clip(CircleShape).background(c.glow))
        if (turn <= 90f) NoteFront(g, today) else Box(Modifier.graphicsLayer { rotationY = 180f }) { NoteBack(g, onOpen) }
    }
}

@Composable
private fun NoteFront(g: GoalView, today: LocalDate) {
    val c = Ba.colors
    val end = g.goal.endDate ?: today
    val months = GoalRules.months(g.goal.startDate, end)
    val at = GoalRules.monthIndex(g.goal.startDate, end, today)
    val milestone = g.months.firstOrNull { it.monthIndex == at }?.milestone
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
        Text(
            stringResource(R.string.goals_note_meta, goalDate(end), PersianDigits.format(at + 1), PersianDigits.format(months.size)),
            style = Ba.type.caption,
            color = c.text3,
        )
        Text(g.goal.title, style = Ba.type.subtitle, color = c.text1, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(
            milestone ?: stringResource(R.string.goals_note_no_milestone),
            style = Ba.type.caption,
            color = if (milestone != null) c.text2 else c.text3,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        // Time gone, not a score: the bar never asks to catch up.
        val share = GoalRules.timeShare(g.goal.startDate, end, today)
        Box(Modifier.fillMaxWidth().height(6.dp).background(c.surface2)) {
            Box(Modifier.fillMaxWidth(share.coerceAtLeast(0.02f)).height(6.dp).background(c.glow))
        }
    }
}

@Composable
private fun NoteBack(g: GoalView, onOpen: () -> Unit) {
    val c = Ba.colors
    val week = g.week ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
        Text(stringResource(R.string.goals_week), style = Ba.type.caption, color = c.text3)
        Text(g.actions.joinToString(" · ") { it.title }, style = Ba.type.label, color = c.text1, maxLines = 2, overflow = TextOverflow.Ellipsis)
        WeekSquares(week.days, Modifier.fillMaxWidth())
        Text(
            stringResource(if (week.inRange) R.string.goals_ticks_in else R.string.goals_ticks, PersianDigits.format(week.ticks)),
            style = Ba.type.caption,
            color = if (week.inRange) c.accent else c.text2,
        )
        Text(stringResource(R.string.goals_range, PersianDigits.format(week.low), PersianDigits.format(week.high)), style = Ba.type.caption, color = c.text3)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(Px.Small).clickable(role = Role.Button, onClick = onOpen).padding(vertical = 4.dp),
        ) {
            Text(stringResource(R.string.goals_open), style = Ba.type.label.copy(fontWeight = FontWeight.Bold), color = c.glow)
            Icon(BaIcons.ChevronForward, null, tint = c.glow, modifier = Modifier.size(16.dp))
        }
    }
}

/** Seven squares from Saturday: green done, gray gap, an outline for days ahead. Never red. */
@Composable
internal fun WeekSquares(days: List<WeekDay>, modifier: Modifier = Modifier, letters: Boolean = false) {
    val c = Ba.colors
    val names = androidx.compose.ui.res.stringArrayResource(R.array.weekdays_short)
    val done = stringResource(R.string.cd_goal_day_done)
    val gap = stringResource(R.string.cd_goal_day_gap)
    val ahead = stringResource(R.string.cd_goal_day_ahead)
    val before = stringResource(R.string.cd_goal_day_before)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        days.forEachIndexed { i, d ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                val label = names[i] + ": " + when (d) { WeekDay.DONE -> done; WeekDay.GAP -> gap; WeekDay.AHEAD -> ahead; WeekDay.BEFORE -> before }
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(Px.Small)
                        .then(
                            when (d) {
                                WeekDay.DONE -> Modifier.background(c.accent)
                                WeekDay.GAP -> Modifier.background(c.grid.first())
                                WeekDay.AHEAD -> Modifier.border(1.5.dp, c.text3, Px.Small)
                                WeekDay.BEFORE -> Modifier.border(1.dp, c.text3.copy(alpha = 0.25f), Px.Small)
                            },
                        )
                        .semantics { contentDescription = label },
                )
                if (letters) Text(names[i], style = Ba.type.caption, color = c.text3)
            }
        }
    }
}

/** The dashed «هدف تازه» note: the flow from step ①. «+» in the bar still opens a new task (D201). */
@Composable
private fun NewNote(onNew: () -> Unit) {
    val c = Ba.colors
    val pixel = Ba.companion
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .clip(Px.Card)
            .drawBehind {
                drawRoundRect(
                    c.glow.copy(alpha = 0.6f),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    cornerRadius = CornerRadius(if (pixel) 0f else 16.dp.toPx()),
                )
            }
            .clickable(role = Role.Button, onClick = onNew)
            .padding(12.dp),
    ) {
        Icon(BaIcons.Plus, null, tint = c.glow, modifier = Modifier.size(28.dp))
        Text(stringResource(R.string.goals_new), style = Ba.type.label.copy(fontWeight = FontWeight.Bold), color = c.glow)
    }
}

/** An idea (or a reached goal) as a paper slip under the wall. */
@Composable
private fun Slip(text: String, done: Boolean = false, onClick: () -> Unit) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clip(Px.Small).background(c.surface2).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (done) Icon(BaIcons.CheckCircle, null, tint = c.accent, modifier = Modifier.size(16.dp))
        Text(text, style = Ba.type.label, color = c.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The first visit: the companion (in Simple the tab's icon) and one button. */
@Composable
private fun EmptyWall(onNew: () -> Unit) {
    val c = Ba.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().sky().statusBarsPadding().padding(24.dp),
    ) {
        if (Ba.companion) {
            CharacterPortrait(LocalCharacter.current, Mood.CALM, Modifier.size(180.dp))
            PixelDialog(stringResource(R.string.goals_empty_say), Modifier.fillMaxWidth())
        } else {
            Icon(BaIcons.Grid, null, tint = c.glow, modifier = Modifier.size(64.dp))
            Text(stringResource(R.string.goals_empty_say), style = Ba.type.title, color = c.text1, textAlign = TextAlign.Center)
        }
        Text(stringResource(R.string.goals_empty_sub), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
        PrimaryButton(stringResource(R.string.goals_empty_go), onNew, Modifier.fillMaxWidth(), icon = BaIcons.Plus)
    }
}
