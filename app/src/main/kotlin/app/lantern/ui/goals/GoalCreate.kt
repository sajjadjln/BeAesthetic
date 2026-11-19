package app.lantern.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.R
import app.lantern.domain.goals.GoalActionDraft
import app.lantern.domain.goals.GoalDraft
import app.lantern.domain.goals.GoalPlans
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.ui.components.*
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.*
import java.time.temporal.ChronoUnit

@Composable
fun GoalCreateRoute(vm: GoalCreateViewModel, onBack: () -> Unit, onPinned: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val update: (GoalDraft) -> Unit = vm::update
    val step by vm.step.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val pinned by vm.savedId.collectAsStateWithLifecycle()
    LaunchedEffect(pinned) { if (pinned != null) onPinned() }
    var discard by rememberSaveable { mutableStateOf(false) }
    val back = { if (!saving) { if (step > 0) vm.previous() else if (draft?.title.isNullOrBlank()) onBack() else discard = true } }
    BackHandler { back() }
    val d = draft ?: return
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding().imePadding()) {
        TopBar(stringResource(R.string.goal_create), back, stringResource(R.string.cd_back))
        if (step > 0) Text(d.title, style = Ba.type.label, color = Ba.colors.text2, modifier = Modifier.padding(horizontal = 24.dp))
        AnimatedContent(step, modifier = Modifier.weight(1f), label = "goalStep") { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GoalQuestion(stringArrayResource(R.array.goal_questions)[current], compact = current in 2..4)
                when (current) {
                    0 -> GoalInput(d.title, stringResource(R.string.goal_title_hint), maxLength = 160) { update(d.copy(title = it)) }
                    1 -> GoalDeadline(d, update)
                    2 -> GoalBaseline(d, update)
                    3 -> GoalPlan(d, update)
                    4 -> GoalSummary(d, update)
                    5 -> {
                        GoalInput(d.why, stringResource(R.string.goal_why_hint), minLines = 4, maxLength = 2000) { update(d.copy(why = it)) }
                        Text(stringResource(R.string.goal_why_help), style = Ba.type.body, color = Ba.colors.text2)
                    }
                }
            }
        }
        if (error) Text(stringResource(R.string.goal_save_error), style = Ba.type.body, color = Ba.colors.amber, modifier = Modifier.padding(horizontal = 24.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(6) { index -> Box(Modifier.weight(1f).height(5.dp).clip(Px.Small).background(if (index <= step) Ba.colors.glow else Ba.colors.surface2)) }
            }
            Text(stringResource(R.string.goal_step_count, PersianDigits.format(step + 1)), style = Ba.type.caption, color = Ba.colors.text3)
            PrimaryButton(
                stringResource(if (saving) R.string.goal_saving else if (step == 5) R.string.goal_pin else R.string.goal_next),
                onClick = { if (step == 5) vm.pin() else vm.next() },
                enabled = !saving && GoalPlans.validStep(d, step), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (discard) AlertDialog(
        onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.goal_discard_title)) },
        text = { Text(stringResource(R.string.goal_discard_body)) },
        confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.goal_discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.goal_keep_writing)) } },
    )
}

@Composable
internal fun GoalQuestion(text: String, compact: Boolean = false) {
    if (Ba.companion) {
        CharacterPortrait(LocalCharacter.current, Mood.CALM, Modifier.size(if (compact) 88.dp else 140.dp))
        PixelDialog(text, Modifier.fillMaxWidth())
    } else Text(text, style = Ba.type.headline, color = Ba.colors.text1, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
}

@Composable
internal fun GoalInput(value: String, hint: String, minLines: Int = 2, maxLength: Int = 300, onChange: (String) -> Unit) {
    val c = Ba.colors
    val text = rememberTextFieldState(initialText = value)
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onChange)
    // The input buffer owns typing and selection. Persist snapshots without feeding each character
    // through a value callback that can restart the IME during a keyboard resize or a slow frame.
    LaunchedEffect(text) {
        snapshotFlow { text.text.toString() }.collect { typed ->
            if (typed != latestValue) latestOnChange(typed)
        }
    }
    BasicTextField(
        state = text, lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = minLines),
        inputTransformation = InputTransformation.maxLength(maxLength),
        textStyle = Ba.type.title.copy(color = c.glow), cursorBrush = SolidColor(c.glow),
        modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface2).padding(16.dp),
        decorator = { field -> Box { if (text.text.isEmpty()) Text(hint, color = c.text3, style = Ba.type.body); field() } },
    )
}

@Composable
private fun GoalDeadline(d: GoalDraft, update: (GoalDraft) -> Unit) {
    val days = ChronoUnit.DAYS.between(d.start, d.finish).toInt()
    var calendar by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.goal_days, PersianDigits.format(days)), style = Ba.type.headline, color = Ba.colors.glow)
    Text(jalaliDayMonth(d.finish), style = Ba.type.title, color = Ba.colors.text1)
    // Square the position: a day near the short end, a year at the far end.
    Slider(
        value = kotlin.math.sqrt(days.coerceIn(1, 365) / 365f),
        onValueChange = { value -> update(d.copy(finish = d.start.plusDays((value * value * 365).toLong().coerceAtLeast(1)))) },
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(thumbColor = Ba.colors.glow, activeTrackColor = Ba.colors.glow),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(21, 90, 180, 365).forEach { length ->
            TextButton(onClick = { update(d.copy(finish = d.start.plusDays(length.toLong()))) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.goal_days, PersianDigits.format(length)), color = Ba.colors.glow)
            }
        }
    }
    SecondaryButton(stringResource(R.string.goal_choose_date), { calendar = !calendar }, Modifier.fillMaxWidth())
    if (calendar) JalaliCalendar(d.finish, d.start, { update(d.copy(finish = it)); calendar = false }, monthsAhead = 120, pickable = d.start.plusDays(1)..d.start.plusYears(10))
}

@Composable
private fun GoalBaseline(d: GoalDraft, update: (GoalDraft) -> Unit) {
    var obstacleChoice by remember { mutableIntStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PickPill(stringResource(R.string.goal_tried_yes), d.triedBefore == true) { update(d.copy(triedBefore = true)) }
        PickPill(stringResource(R.string.goal_tried_no), d.triedBefore == false) { update(d.copy(triedBefore = false)) }
    }
    if (d.triedBefore == true) {
        Text(stringResource(R.string.goal_baseline), style = Ba.type.subtitle, color = Ba.colors.text1)
        Stepper(d.baselineMinutes, 0..10080, { stringResource(R.string.goal_minutes, PersianDigits.format(it)) }) { update(d.copy(baselineMinutes = it)) }
        InputBox(d.baselineMinutes.takeIf { it > 0 }?.let(PersianDigits::format).orEmpty(), stringResource(R.string.goal_minutes_hint), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) {
            val n = PersianDigits.normalize(it).toIntOrNull()
            update(d.copy(baselineMinutes = (n ?: 0).coerceIn(0, 10080)))
        }
        GoalInput(d.triedWhat, stringResource(R.string.goal_tried_what)) { update(d.copy(triedWhat = it)) }
        Text(stringResource(R.string.goal_obstacle), style = Ba.type.subtitle, color = Ba.colors.text1)
        stringArrayResource(R.array.goal_obstacles).forEach { obstacle ->
            PickPill(obstacle, d.obstacle == obstacle, Modifier.fillMaxWidth()) { update(d.copy(obstacle = obstacle)); obstacleChoice++ }
        }
        key(obstacleChoice) { GoalInput(d.obstacle, stringResource(R.string.goal_obstacle_hint)) { update(d.copy(obstacle = it)) } }
    }
}

@Composable
private fun GoalPlan(d: GoalDraft, update: (GoalDraft) -> Unit) {
    val months = GoalPlans.months(d.start, d.finish, d.calendar)
    if (months.isNotEmpty()) {
        Text(stringResource(R.string.goal_backward), style = Ba.type.subtitle, color = Ba.colors.text2)
        months.asReversed().forEach { month ->
            BaCard {
                Text(jalaliDayMonth(month.start) + " – " + jalaliDayMonth(month.end), style = Ba.type.subtitle, color = Ba.colors.glow)
                Text(stringResource(if (month == months.first()) R.string.goal_this_month_required else R.string.goal_later_optional), style = Ba.type.caption, color = Ba.colors.text2)
                Spacer(Modifier.height(8.dp))
                GoalInput(d.milestones[month.start].orEmpty(), stringResource(if (month == months.first()) R.string.goal_milestone_hint else R.string.goal_not_sure)) {
                    update(d.copy(milestones = d.milestones + (month.start to it)))
                }
            }
        }
    }
    Text(stringResource(R.string.goal_this_week), style = Ba.type.title, color = Ba.colors.text1)
    Text(stringResource(R.string.goal_action_days_help), style = Ba.type.caption, color = Ba.colors.text2)
    d.actions.forEachIndexed { index, action -> key(action.id) {
        BaCard {
            GoalInput(action.title, stringResource(R.string.goal_action_hint), maxLength = 40) { text -> update(d.copy(actions = d.actions.toMutableList().also { it[index] = action.copy(title = text) })) }
            Spacer(Modifier.height(8.dp))
            GoalWeekdayPicker(action.weekdays) { bits -> update(d.copy(actions = d.actions.toMutableList().also { it[index] = action.copy(weekdays = bits) })) }
            Spacer(Modifier.height(8.dp))
            val parts = stringArrayResource(R.array.goal_parts)
            Part.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { part ->
                        PickPill(parts[part.ordinal], part == action.part, Modifier.weight(1f), small = true) {
                            update(d.copy(actions = d.actions.toMutableList().also { it[index] = action.copy(part = part) }))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            GoalInput(action.cue, stringResource(R.string.goal_cue_hint), minLines = 1, maxLength = 30) { cue -> update(d.copy(actions = d.actions.toMutableList().also { it[index] = action.copy(cue = cue.take(30)) })) }
            if (d.actions.size > 1) TextButton(onClick = { update(d.copy(actions = d.actions.filterIndexed { n, _ -> n != index })) }) { Text(stringResource(R.string.goal_remove_action), color = Ba.colors.text2) }
        }
    } }
    if (d.actions.size < 3) SecondaryButton(stringResource(R.string.goal_add_action), { update(d.copy(actions = d.actions + GoalActionDraft())) }, Modifier.fillMaxWidth())
}

@Composable
private fun GoalWeekdayPicker(bits: Int, onChange: (Int) -> Unit) {
    val names = stringArrayResource(R.array.weekdays_short)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        names.forEachIndexed { index, name ->
            // Seven full touch targets fit a 360dp screen without shrinking the labels.
            val fullName = stringArrayResource(R.array.weekdays)[index]
            Box(
                Modifier.weight(1f).height(48.dp).clip(Px.Small)
                    .background(if (bits and (1 shl index) != 0) Ba.colors.glow else Ba.colors.surface2)
                    .selectable(bits and (1 shl index) != 0, role = Role.Checkbox, onClick = { onChange(bits xor (1 shl index)) })
                    .semantics { contentDescription = fullName },
                contentAlignment = Alignment.Center,
            ) { Text(name, style = Ba.type.caption, color = if (bits and (1 shl index) != 0) Ba.colors.onGlow else Ba.colors.text2) }
        }
    }
}

@Composable
private fun GoalSummary(d: GoalDraft, update: (GoalDraft) -> Unit) {
    BaCard {
        Text(d.title, style = Ba.type.title, color = Ba.colors.glow)
        Text(stringResource(R.string.goal_until, jalaliDayMonth(d.finish)), style = Ba.type.body, color = Ba.colors.text1)
        Text(stringResource(R.string.goal_minutes, PersianDigits.format(if (d.triedBefore == true) d.baselineMinutes else 0)), style = Ba.type.body, color = Ba.colors.text2)
        d.actions.forEach { action -> Text(action.title, style = Ba.type.body, color = Ba.colors.text1) }
        val range = GoalPlans.range(d.actions, d.flexDays)
        Text(stringResource(R.string.goal_week_range, PersianDigits.format(range.first), PersianDigits.format(range.last)), style = Ba.type.subtitle, color = Ba.colors.glow)
    }
    Text(stringResource(R.string.goal_flex_days), style = Ba.type.subtitle, color = Ba.colors.text1)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        (0..2).forEach { days -> PickPill(PersianDigits.format(days), days == d.flexDays) { update(d.copy(flexDays = days)) } }
    }
    Text(stringResource(R.string.goal_flex_help), style = Ba.type.body, color = Ba.colors.text2)
}
