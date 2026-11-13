package app.lantern.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.lantern.AppContainer
import app.lantern.R
import app.lantern.domain.goals.ActionDraft
import app.lantern.domain.goals.GoalDraft
import app.lantern.domain.goals.GoalRules
import app.lantern.domain.goals.GoalTimeline
import app.lantern.domain.goals.StopReason
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.domain.time.ProgramCalendar
import app.lantern.domain.time.Weekdays
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.EndLayout
import app.lantern.ui.components.InputBox
import app.lantern.ui.components.JalaliCalendar
import app.lantern.ui.components.PickPill
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.Stepper
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LocalCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.sky
import app.lantern.ui.goalDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val STEPS = 6

/**
 * The goal flow (goal-step1…6, D221), in onboarding's layout: back and the progress blocks on top, the
 * companion and its line (in Simple, the line as a title), the step, the buttons at the bottom.
 * [ideaId] starts from an idea slip; its title is filled in and pinning turns it into this goal.
 */
@Composable
fun GoalFlowScreen(container: AppContainer, ideaId: Long?, onClose: () -> Unit, onPinned: (Long) -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    val today = remember { container.clock.programDate(ProgramCalendar.DEFAULT_ROLLOVER_HOUR) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val cal = monthCalendar()
    var draft by remember { mutableStateOf(GoalDraft()) }
    LaunchedEffect(ideaId) {
        val idea = if (ideaId != null) container.goals.get(ideaId) else null
        // A goal set aside keeps its why and flex days for when it comes back.
        if (idea != null) draft = draft.copy(title = idea.title, why = idea.why.orEmpty(), flexDays = idea.flexDays)
    }

    val back: () -> Unit = { if (step == 0) onClose() else step -= 1 }
    BackHandler { back() }

    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = back),
            ) {
                Icon(
                    if (step == 0) BaIcons.X else BaIcons.Back,
                    stringResource(if (step == 0) R.string.goal_flow_close else R.string.cd_back),
                    tint = c.text1,
                    modifier = Modifier.size(24.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                repeat(STEPS) { i ->
                    val fill by animateColorAsState(if (i <= step) c.glow else c.surface2, tween(BaMotion.MEDIUM), label = "goalStep")
                    Box(Modifier.weight(1f).height(6.dp).background(fill))
                }
            }
        }
        AnimatedContent(
            step,
            transitionSpec = {
                val dir = if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                val spec = tween<IntOffset>(BaMotion.MEDIUM, easing = BaMotion.Standard)
                (slideIntoContainer(dir, spec) + fadeIn(tween(BaMotion.MEDIUM))) togetherWith
                    (slideOutOfContainer(dir, spec) { it / 3 } + fadeOut(tween(BaMotion.SHORT)))
            },
            label = "goalFlow",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { st ->
            val next = { step = (step + 1).coerceAtMost(STEPS - 1) }
            EndLayout(
                buttons = {
                    when (st) {
                        0 -> {
                            PrimaryButton(stringResource(R.string.goal_next), { next() }, Modifier.fillMaxWidth(), enabled = draft.step1Ok)
                            Text(
                                stringResource(R.string.goal_s1_idea),
                                style = Ba.type.label,
                                color = if (draft.step1Ok) c.text2 else c.text3,
                                modifier = Modifier.clip(Px.Small)
                                    .clickable(enabled = draft.step1Ok, role = Role.Button) {
                                        scope.launch {
                                            container.goals.saveIdea(draft.title, ideaId, today, container.clock.now())
                                            onClose()
                                        }
                                    }
                                    .padding(8.dp),
                            )
                        }
                        1 -> PrimaryButton(stringResource(R.string.goal_next), { draft = draft.withMonthsFor(today, cal); next() }, Modifier.fillMaxWidth(), enabled = draft.step2Ok(today))
                        2 -> PrimaryButton(stringResource(R.string.goal_next), { next() }, Modifier.fillMaxWidth(), enabled = draft.step3Ok)
                        3 -> PrimaryButton(stringResource(R.string.goal_next), { next() }, Modifier.fillMaxWidth(), enabled = draft.step4Ok)
                        4 -> PrimaryButton(stringResource(R.string.goal_next), { next() }, Modifier.fillMaxWidth())
                        else -> PrimaryButton(
                            stringResource(R.string.goal_s6_pin),
                            {
                                scope.launch {
                                    val id = container.goals.pin(draft, ideaId, today, container.clock.now())
                                    container.scheduler.reconcile("goal")
                                    onPinned(id)
                                }
                            },
                            Modifier.fillMaxWidth(),
                            enabled = draft.ready(today),
                            icon = BaIcons.Flag,
                        )
                    }
                },
            ) {
                when (st) {
                    0 -> StepGoal(draft) { draft = it }
                    1 -> StepWhen(draft, today) { draft = it }
                    2 -> StepBefore(draft) { draft = it }
                    3 -> StepWeek(draft, today) { draft = it }
                    4 -> StepSummary(draft, today) { draft = it }
                    else -> StepWhy(draft) { draft = it }
                }
            }
        }
    }
}

/** The companion and its line; in Simple the line is the step's title (D200). */
@Composable
internal fun Say(text: String, mood: Mood = Mood.CALM) {
    if (Ba.companion) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CharacterPortrait(LocalCharacter.current, mood, Modifier.size(76.dp))
            PixelDialog(text, Modifier.weight(1f))
        }
    } else {
        Text(text, style = Ba.type.title, color = Ba.colors.text1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** The goal's title above each step after ①, so the plan never loses its subject. */
@Composable
private fun GoalTitle(title: String) {
    Text(title, style = Ba.type.subtitle, color = Ba.colors.glow, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun StepGoal(d: GoalDraft, onChange: (GoalDraft) -> Unit) {
    Say(stringResource(if (d.step1Ok) R.string.goal_s1_say_ok else R.string.goal_s1_say), if (d.step1Ok) Mood.HAPPY else Mood.CALM)
    InputBox(d.title, stringResource(R.string.goal_s1_hint)) { onChange(d.copy(title = it)) }
}

@Composable
private fun StepWhen(d: GoalDraft, today: LocalDate, onChange: (GoalDraft) -> Unit) {
    val c = Ba.colors
    val days = d.endDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }
    var position by rememberSaveable { mutableFloatStateOf(GoalTimeline.toPosition(days ?: 91).toFloat()) }
    var calendar by rememberSaveable { mutableStateOf(days != null && days > GoalTimeline.YEAR) }
    val pick = GoalTimeline.pick(position.toInt(), today)
    val line = when {
        calendar -> R.string.goal_s2_calendar_say
        d.endDate == null -> R.string.goal_s2_say
        pick.days < 14 -> R.string.goal_s2_short
        pick.days <= 42 -> R.string.goal_s2_weeks
        pick.marker == GoalTimeline.Mark.NOWRUZ -> R.string.goal_s2_nowruz
        pick.days < GoalTimeline.YEAR -> R.string.goal_s2_long
        else -> R.string.goal_s2_year
    }
    GoalTitle(d.title)
    Say(stringResource(line), if (d.endDate != null) Mood.HAPPY else Mood.CALM)
    if (calendar) {
        JalaliCalendar(d.endDate, today, onPick = { onChange(d.copy(endDate = it)) }, monthsAhead = 36, pickable = today.plusDays(1)..LocalDate.MAX)
        d.endDate?.let { Text(stringResource(R.string.goal_ends, goalDate(it)), style = Ba.type.body, color = c.text2) }
        return
    }
    Text(
        if (d.endDate == null) "—" else lengthText(pick),
        style = Ba.type.headline,
        color = if (d.endDate == null) c.text3 else c.text1,
    )
    if (d.endDate != null) Text(goalDate(today.plusDays(pick.days.toLong())), style = Ba.type.body, color = c.text2)
    Slider(
        value = position,
        onValueChange = {
            position = it
            onChange(d.copy(endDate = today.plusDays(GoalTimeline.pick(it.toInt(), today).days.toLong())))
        },
        valueRange = 0f..GoalTimeline.MAX.toFloat(),
        colors = SliderDefaults.colors(thumbColor = c.glow, activeTrackColor = c.glow, inactiveTrackColor = c.surface2),
        modifier = Modifier.fillMaxWidth(),
    )
    // Each marker sits under its own spot on the track (the slider's thumb leaves 10dp at each end).
    val marks = GoalTimeline.markers(today)
    androidx.compose.ui.layout.Layout(
        content = {
            marks.forEach { m ->
                val on = pick.marker == m.mark && d.endDate != null
                Text(
                    markText(m.mark),
                    style = Ba.type.caption.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                    color = if (on) c.glow else c.text3,
                    modifier = Modifier.clip(Px.Small).clickable(role = Role.Button) {
                        position = GoalTimeline.toPosition(m.days).toFloat()
                        onChange(d.copy(endDate = today.plusDays(m.days.toLong())))
                    }.padding(4.dp),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val inset = 10.dp.roundToPx()
        val track = constraints.maxWidth - 2 * inset
        layout(constraints.maxWidth, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { i, pl ->
                val x = inset + track * GoalTimeline.toPosition(marks[i].days) / GoalTimeline.MAX - pl.width / 2
                pl.placeRelative(x.coerceIn(0, constraints.maxWidth - pl.width), 0)
            }
        }
    }
    AnimatedVisibility(pick.days >= GoalTimeline.YEAR && d.endDate != null) {
        Text(
            stringResource(R.string.goal_s2_more),
            style = Ba.type.label,
            color = c.glow,
            modifier = Modifier.clip(Px.Small).clickable(role = Role.Button) { calendar = true }.padding(8.dp),
        )
    }
}

@Composable
private fun lengthText(p: GoalTimeline.Pick): String = when {
    p.marker == GoalTimeline.Mark.NOWRUZ -> stringResource(R.string.goal_len_nowruz)
    else -> when (val l = GoalTimeline.length(p.days)) {
        is GoalTimeline.Length.Days -> stringResource(R.string.goal_len_days, PersianDigits.format(l.n))
        is GoalTimeline.Length.Weeks -> stringResource(R.string.goal_len_weeks, PersianDigits.format(l.n))
        is GoalTimeline.Length.Months -> stringResource(R.string.goal_len_months, PersianDigits.format(l.n))
        GoalTimeline.Length.Year -> stringResource(R.string.goal_len_year)
    }
}

@Composable
private fun markText(m: GoalTimeline.Mark): String = stringResource(
    when (m) {
        GoalTimeline.Mark.MONTHS_3 -> R.string.goal_mark_3
        GoalTimeline.Mark.NOWRUZ -> R.string.goal_mark_nowruz
        GoalTimeline.Mark.MONTHS_6 -> R.string.goal_mark_6
        GoalTimeline.Mark.MONTHS_9 -> R.string.goal_mark_9
        GoalTimeline.Mark.YEAR -> R.string.goal_mark_year
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepBefore(d: GoalDraft, onChange: (GoalDraft) -> Unit) {
    val c = Ba.colors
    val line = when {
        d.step3Ok -> if (d.triedBefore == true) R.string.goal_s3_ok_yes else R.string.goal_s3_ok_no
        d.triedBefore == true && !d.timeTouched -> R.string.goal_s3_time
        d.triedBefore == true && d.triedWhat.isBlank() -> R.string.goal_s3_what
        d.triedBefore == true -> R.string.goal_s3_why
        else -> R.string.goal_s3_say
    }
    GoalTitle(d.title)
    Say(stringResource(line), if (d.step3Ok) Mood.HAPPY else Mood.CALM)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        PickPill(stringResource(R.string.goal_s3_yes), d.triedBefore == true, Modifier.weight(1f)) { onChange(d.copy(triedBefore = true)) }
        PickPill(stringResource(R.string.goal_s3_no), d.triedBefore == false, Modifier.weight(1f)) { onChange(d.copy(triedBefore = false)) }
    }
    if (d.triedBefore != true) return
    Label(R.string.goal_s3_time)
    // Steps of 15 minutes, up to 10 hours a week.
    Stepper(d.minutesPerWeek / 15, 0..40, label = { weekTime(it * 15) }) { onChange(d.copy(minutesPerWeek = it * 15, timeTouched = true)) }
    if (!d.timeTouched) return
    Label(R.string.goal_s3_what)
    InputBox(d.triedWhat, stringResource(R.string.goal_s3_what_hint)) { onChange(d.copy(triedWhat = it)) }
    if (d.triedWhat.isBlank()) return
    Label(R.string.goal_s3_why)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StopReason.entries.forEach { r -> PickPill(reasonText(r), d.stopReason == r, small = true) { onChange(d.copy(stopReason = r)) } }
    }
    if (d.stopReason == StopReason.OWN) InputBox(d.ownReason, stringResource(R.string.goal_s3_own_hint)) { onChange(d.copy(ownReason = it)) }
}

@Composable
internal fun reasonText(r: StopReason): String = stringResource(
    when (r) {
        StopReason.BORED -> R.string.goal_reason_bored
        StopReason.NO_CHANGE -> R.string.goal_reason_no_change
        StopReason.ROUTINE -> R.string.goal_reason_routine
        StopReason.NO_TIME -> R.string.goal_reason_no_time
        StopReason.OWN -> R.string.goal_reason_own
    },
)

@Composable
internal fun weekTime(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.goal_none_week)
    minutes % 60 == 0 -> stringResource(R.string.goal_hours_week, PersianDigits.format(minutes / 60))
    else -> stringResource(R.string.goal_minutes_week, PersianDigits.format(minutes))
}

@Composable
private fun Label(text: Int) {
    Text(stringResource(text), style = Ba.type.label, color = Ba.colors.text2, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepWeek(d: GoalDraft, today: LocalDate, onChange: (GoalDraft) -> Unit) {
    val c = Ba.colors
    val cal = monthCalendar()
    val months = remember(d.endDate, cal) { d.endDate?.let { GoalRules.months(today, it, cal) }.orEmpty() }
    val names = monthNames()
    val first = d.months.firstOrNull().orEmpty()
    GoalTitle(d.title)
    Say(stringResource(if (first.isBlank()) R.string.goal_s4_say else R.string.goal_s4_say_week), if (first.isBlank()) Mood.CALM else Mood.HAPPY)
    Label(R.string.goal_s4_months)
    // Last month first (goal-step4): planning backward from the end.
    months.indices.reversed().forEach { i ->
        val tag = when (i) {
            0 -> stringResource(R.string.goal_s4_this_month)
            months.lastIndex -> stringResource(R.string.goal_s4_last_month)
            else -> null
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                names[months[i].month - 1] + (tag?.let { " · $it" } ?: ""),
                style = Ba.type.caption.copy(fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal),
                color = if (i == 0) c.glow else c.text3,
            )
            InputBox(d.months.getOrElse(i) { "" }, stringResource(if (i == 0) R.string.goal_s4_need else R.string.goal_s4_unknown)) { v ->
                onChange(d.copy(months = d.months.toMutableList().also { it[i] = v }))
            }
        }
    }
    Label(R.string.goal_s4_week)
    val days = stringArrayResource(R.array.weekdays_short)
    d.actions.forEachIndexed { i, a ->
        fun set(x: ActionDraft) = onChange(d.copy(actions = d.actions.toMutableList().also { it[i] = x }))
        BaCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        InputBox(a.title, stringResource(if (i == 0) R.string.goal_s4_action_hint else R.string.goal_s4_action_more_hint)) { set(a.copy(title = it)) }
                    }
                    if (d.actions.size > 1) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) {
                                onChange(d.copy(actions = d.actions.toMutableList().also { it.removeAt(i) }))
                            },
                        ) { Icon(BaIcons.X, stringResource(R.string.goal_s4_remove), tint = c.text3, modifier = Modifier.size(18.dp)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    days.forEachIndexed { k, label ->
                        val on = a.weekdays and (1 shl k) != 0
                        PickPill(label, on, Modifier.weight(1f), small = true) { set(a.copy(weekdays = a.weekdays xor (1 shl k))) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Part.entries.forEach { p ->
                        val on = a.part == p
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).clip(Px.Small)
                                .background(if (on) c.glow.copy(alpha = 0.18f) else c.surface2)
                                .then(if (on) Modifier.border(2.dp, c.glow, Px.Small) else Modifier)
                                .clickable(role = Role.RadioButton) { set(a.copy(part = p)) },
                        ) { Icon(BaIcons.of(p), partName(p), tint = if (on) c.glow else c.text3, modifier = Modifier.size(18.dp)) }
                    }
                    val n = Integer.bitCount(a.weekdays and Weekdays.ALL)
                    if (n > 0) Text(stringResource(R.string.goal_s4_times, PersianDigits.format(n)), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
    if (d.actions.size < GoalRules.MAX_ACTIONS) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clip(Px.Small).clickable(role = Role.Button) { onChange(d.copy(actions = d.actions + ActionDraft())) }.padding(8.dp),
        ) {
            Icon(BaIcons.Plus, null, tint = c.glow, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.goal_s4_add), style = Ba.type.label, color = c.glow)
        }
    } else {
        Text(stringResource(R.string.goal_s4_three), style = Ba.type.caption, color = c.text3)
    }
}

@Composable
internal fun partName(p: Part): String = stringResource(
    when (p) {
        Part.MORNING -> R.string.part_morning
        Part.MIDDAY -> R.string.part_midday
        Part.EVENING -> R.string.part_evening
        Part.NIGHT -> R.string.part_night
    },
)

@Composable
private fun StepSummary(d: GoalDraft, today: LocalDate, onChange: (GoalDraft) -> Unit) {
    val c = Ba.colors
    GoalTitle(d.title)
    Say(stringResource(R.string.goal_s5_say), Mood.HAPPY)
    BaCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            d.endDate?.let { end ->
                SummaryRow(R.string.goal_s5_by, stringResource(R.string.goal_s5_by_value, goalDate(end), PersianDigits.format(GoalRules.daysLeft(end, today))))
            }
            SummaryRow(R.string.goal_s5_now, if (d.triedBefore == true) weekTime(d.minutesPerWeek) else stringResource(R.string.goal_s5_first))
            val (low, high) = d.range()
            SummaryRow(
                R.string.goal_s5_each_week,
                d.actions.joinToString(" · ") { it.title.trim() } + "\n" +
                    stringResource(R.string.goal_s5_range, PersianDigits.format(low), PersianDigits.format(high)),
            )
        }
    }
    Text(stringResource(R.string.goal_s5_flex), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        (0..GoalRules.MAX_FLEX).forEach { n ->
            PickPill(PersianDigits.format(n), d.flexDays == n, Modifier.weight(1f)) { onChange(d.copy(flexDays = n)) }
        }
    }
    Text(stringResource(R.string.goal_s5_flex_help), style = Ba.type.caption, color = c.text3, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SummaryRow(label: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(label), style = Ba.type.caption, color = Ba.colors.text3)
        Text(value, style = Ba.type.tile, color = Ba.colors.text1)
    }
}

@Composable
private fun StepWhy(d: GoalDraft, onChange: (GoalDraft) -> Unit) {
    GoalTitle(d.title)
    Say(stringResource(R.string.goal_s6_say), if (d.step6Ok) Mood.HAPPY else Mood.CALM)
    NoteBox(d.why, stringResource(R.string.goal_s6_hint)) { onChange(d.copy(why = it)) }
    Text(stringResource(R.string.goal_s6_help), style = Ba.type.caption, color = Ba.colors.text3, modifier = Modifier.fillMaxWidth())
}

/** A few lines of free text, in the app's input look (InputBox is one line). */
@Composable
internal fun NoteBox(value: String, hint: String, onChange: (String) -> Unit) {
    val c = Ba.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = Ba.type.body.copy(color = c.text1),
        cursorBrush = SolidColor(c.glow),
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(Px.Card).background(c.surface2).padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (value.isEmpty()) Text(hint, style = Ba.type.body, color = c.text3)
                inner()
            }
        },
    )
}

/** Goal months follow the language: Jalali in Persian, Gregorian in English (D176). */
@Composable
internal fun monthCalendar(): app.lantern.domain.goals.MonthCalendar =
    if (app.lantern.domain.text.PersianDigits.latin) app.lantern.domain.goals.MonthCalendar.GREGORIAN else app.lantern.domain.goals.MonthCalendar.JALALI

@Composable
internal fun monthNames(): Array<String> = androidx.compose.ui.res.stringArrayResource(
    if (app.lantern.domain.text.PersianDigits.latin) R.array.gregorian_months else R.array.jalali_months,
)
