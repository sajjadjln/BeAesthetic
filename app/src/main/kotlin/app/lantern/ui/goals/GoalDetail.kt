package app.lantern.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.AppContainer
import app.lantern.R
import app.lantern.data.repo.GoalView
import app.lantern.domain.goals.GoalRules
import app.lantern.domain.goals.GoalState
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.ProgramCalendar
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.InputBox
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.components.TopBar
import app.lantern.ui.goalDate
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.rememberHaptics
import app.lantern.ui.theme.sky
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * A goal's detail (G3, D222): this week, the path of months (a tap writes a month's step), the why on paper,
 * always open, then «رسیدم!» and «بذارش کنار». Setting aside sends it back to the ideas; there is no failed state.
 */
@Composable
fun GoalDetailScreen(container: AppContainer, goalId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val today = remember { container.clock.programDate(ProgramCalendar.DEFAULT_ROLLOVER_HOUR) }
    val view by remember(goalId, today) { container.goals.observe(today).map { all -> all.firstOrNull { it.id == goalId } } }
        .collectAsStateWithLifecycle<GoalView?>(null)
    val g = view ?: return
    GoalDetail(
        g, today, onBack,
        onMilestone = { i, text -> scope.launch { container.goals.setMilestone(goalId, i, text) } },
        onReached = {
            haptics.medium()
            scope.launch { container.goals.reach(goalId, today, container.clock.now()); container.scheduler.reconcile("goal"); onBack() }
        },
        onSetAside = {
            scope.launch { container.goals.setAside(goalId, today, container.clock.now()); container.scheduler.reconcile("goal"); onBack() }
        },
    )
}

@Composable
private fun GoalDetail(
    g: GoalView,
    today: LocalDate,
    onBack: () -> Unit,
    onMilestone: (Int, String) -> Unit,
    onReached: () -> Unit,
    onSetAside: () -> Unit,
) {
    val c = Ba.colors
    val active = g.state == GoalState.ACTIVE
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        TopBar(g.goal.title, onBack, stringResource(R.string.cd_back))
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            g.goal.endDate?.let { end ->
                val bits = buildList {
                    add(stringResource(R.string.goal_detail_by, goalDate(end)))
                    if (active) add(stringResource(R.string.goal_detail_left, PersianDigits.format(GoalRules.daysLeft(end, today))))
                    if (active) add(stringResource(R.string.goal_detail_flex, PersianDigits.format(g.goal.flexDays)))
                }
                Text(bits.joinToString(stringResource(R.string.goal_sep)), style = Ba.type.label, color = c.text2)
            }
            g.goal.reachedAt?.let {
                Text(
                    stringResource(R.string.goal_detail_reached_on, goalDate(it.atZone(ZoneId.systemDefault()).toLocalDate())),
                    style = Ba.type.label.copy(fontWeight = FontWeight.Bold),
                    color = c.accent,
                )
            }
            g.week?.let { week ->
                BaCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.goals_week), style = Ba.type.subtitle, color = c.text1)
                        g.actions.forEach { a -> Text(a.title, style = Ba.type.tile, color = c.text1) }
                        WeekSquares(week.days, Modifier.fillMaxWidth(), letters = true)
                        Text(
                            stringResource(if (week.inRange) R.string.goals_ticks_in else R.string.goals_ticks, PersianDigits.format(week.ticks)) +
                                stringResource(R.string.goal_sep) + stringResource(R.string.goals_range, PersianDigits.format(week.low), PersianDigits.format(week.high)),
                            style = Ba.type.caption,
                            color = if (week.inRange) c.accent else c.text2,
                        )
                    }
                }
            }
            g.goal.endDate?.let { end -> Path(g, end, today, active, onMilestone) }
            g.goal.why?.let { Why(it, g.goal.whyAt?.let { at -> at.atZone(ZoneId.systemDefault()).toLocalDate() }) }
            Spacer(Modifier.height(8.dp))
        }
        if (active) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            ) {
                Text(stringResource(R.string.goal_detail_aside_help), style = Ba.type.caption, color = c.text3)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(stringResource(R.string.goal_detail_aside), onSetAside, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.goal_detail_reached), onReached, Modifier.weight(1f), icon = BaIcons.Flag)
                }
            }
        }
    }
}

/** The months as a path, first at the top; this month marked «الان». A tap opens the month's step for writing. */
@Composable
private fun Path(g: GoalView, end: LocalDate, today: LocalDate, editable: Boolean, onMilestone: (Int, String) -> Unit) {
    val c = Ba.colors
    val cal = monthCalendar()
    val months = GoalRules.months(g.goal.startDate, end, cal)
    val now = if (editable) GoalRules.monthIndex(g.goal.startDate, end, today, cal) else -1
    val names = monthNames()
    var editing by rememberSaveable { mutableIntStateOf(-1) }
    var text by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.goal_detail_path), style = Ba.type.subtitle, color = c.text1)
        if (editable) Text(stringResource(R.string.goal_detail_tap_month), style = Ba.type.caption, color = c.text3)
        Spacer(Modifier.height(6.dp))
        months.forEachIndexed { i, m ->
            val step = g.months.firstOrNull { it.monthIndex == i }?.milestone
            val past = editable && i < now
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                // The path line and its dot: full for this month, outlined ahead, soft behind.
                Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(c.surface2))
                    Box(
                        Modifier.padding(top = 6.dp).size(if (i == now) 14.dp else 10.dp).clip(CircleShape)
                            .background(if (i == now) c.glow else if (past) c.text3 else c.surface2),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f).clip(Px.Small)
                        .clickable(enabled = editable && editing != i, role = Role.Button) { editing = i; text = step.orEmpty() }
                        .padding(start = 8.dp, top = 2.dp, bottom = 12.dp, end = 4.dp),
                ) {
                    Text(
                        names[m.month - 1] + if (i == now) " · " + stringResource(R.string.goal_detail_now) else "",
                        style = Ba.type.label.copy(fontWeight = if (i == now) FontWeight.Bold else FontWeight.Medium),
                        color = if (i == now) c.glow else c.text2,
                    )
                    if (editing == i) {
                        InputBox(text, stringResource(R.string.goal_s4_unknown), onDone = { onMilestone(i, text); editing = -1 }) { text = it }
                        Text(
                            stringResource(R.string.goal_detail_save),
                            style = Ba.type.label.copy(fontWeight = FontWeight.Bold),
                            color = c.glow,
                            modifier = Modifier.clip(Px.Small).clickable(role = Role.Button) { onMilestone(i, text); editing = -1 }.padding(6.dp),
                        )
                    } else {
                        Text(step ?: stringResource(R.string.goal_s4_unknown), style = Ba.type.body, color = if (step != null) c.text1 else c.text3)
                    }
                }
            }
        }
    }
}

/** The why from step ⑥ (D221), on paper, never folded away: the date it was written, then the words. */
@Composable
internal fun Why(why: String, writtenOn: LocalDate?, modifier: Modifier = Modifier, title: Boolean = true) {
    val c = Ba.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().clip(Px.Card).background(c.surface2)
            .padding(16.dp),
    ) {
        if (title) Text(stringResource(R.string.goal_detail_why), style = Ba.type.subtitle, color = c.glow)
        writtenOn?.let { Text(goalDate(it), style = Ba.type.caption, color = c.text3) }
        Text(why, style = Ba.type.body, color = c.text1)
    }
}
