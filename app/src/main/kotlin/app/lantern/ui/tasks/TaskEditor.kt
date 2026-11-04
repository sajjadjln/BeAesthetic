package app.lantern.ui.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import app.lantern.ui.components.ChoiceGroup
import app.lantern.ui.components.choiceOf
import app.lantern.ui.components.rememberChoiceHighlight
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.drawTick
import app.lantern.ui.theme.hop
import app.lantern.ui.theme.playHop
import app.lantern.ui.theme.staggerIn
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lantern.R
import app.lantern.domain.tasks.FormError
import app.lantern.domain.tasks.RepeatKind
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskForm
import app.lantern.domain.tasks.TaskForms
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.tasks.When
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.ui.clockText
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.BaSwitch
import app.lantern.ui.components.ErrorLine
import app.lantern.ui.components.InputBox
import app.lantern.ui.components.JalaliCalendar
import app.lantern.ui.components.Labeled
import app.lantern.ui.components.PickPill
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.Stepper
import app.lantern.ui.components.TimePickerDialog
import app.lantern.ui.components.ToggleRow
import app.lantern.ui.components.sheetScroll
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.LocalCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.Px
import kotlinx.coroutines.delay
import java.time.LocalDate

/** What the sheet is editing: a new task (in a part, starting on a repeat kind) or an existing one. */
internal sealed interface Editing {
    data class New(val part: Part, val repeat: RepeatKind = RepeatKind.DAILY) : Editing
    data class Existing(val task: Task) : Editing
}

// ---------------------------------------------------------------- 08 Task editor

/**
 * The task editor (08). It opens simple, a name and «کِی؟» (D128); «تنظیمات بیشتر» opens the full form in
 * place with what was typed. An existing task the simple sheet can't show opens full. [onDelete] null hides «حذف».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    editing: Editing,
    today: LocalDate,
    allTasks: List<Task>,
    onDismiss: () -> Unit,
    onSave: (TaskForm) -> Unit,
    onDelete: ((Task) -> Unit)? = null,
) {
    val c = Ba.colors
    val original = (editing as? Editing.Existing)?.task
    var form by remember(editing) {
        mutableStateOf(original?.let(TaskForms::from) ?: (editing as Editing.New).let { TaskForms.blank(it.part, today, it.repeat) })
    }
    var full by remember(editing) { mutableStateOf(original != null && !TaskForms.fitsSimple(original)) }
    // «تنظیمات بیشتر» tapped: the full form's sections rise in and the sheet scrolls along (S2).
    var openedByTap by remember(editing) { mutableStateOf(false) }
    // Errors show only after the first «ذخیره», never while typing.
    var tried by remember(editing) { mutableStateOf(false) }
    // Each failed «ذخیره» scrolls the first problem into view (errors are in screen order).
    var attempt by remember(editing) { mutableIntStateOf(0) }
    var askDelete by remember { mutableStateOf(false) }
    val errors = TaskForms.errors(form, today, original)
    val first = errors.minByOrNull { it.ordinal }
    val scroll = rememberScrollState()
    // A missing name goes to the very top, so the field shows with its message, and the field nudges (S6).
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(attempt) {
        if (attempt == 0 || first != FormError.NO_TITLE) return@LaunchedEffect
        launch { scroll.animateScrollTo(0) }
        nudge.animateTo(0f, keyframes { durationMillis = 380; -7f at 60; 6f at 140; -4f at 220; 2f at 300 })
    }
    val problem: @Composable (FormError, Int) -> Unit = { error, text ->
        val scrollTo = if (error == first && error != FormError.NO_TITLE) attempt else 0
        AnimatedVisibility(
            visible = tried && error in errors,
            enter = expandVertically(tween(260, easing = BaMotion.Open)) + fadeIn(),
            exit = shrinkVertically(tween(200, easing = BaMotion.Close)) + fadeOut(),
        ) { ErrorLine(text, scrollTo) }
    }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // «ذخیره» makes the character smile, hop and answer for a moment before the sheet closes (S7).
    var saved by remember(editing) { mutableStateOf<TaskForm?>(null) }
    val hopT = remember { Animatable(1f) }
    LaunchedEffect(saved) {
        val done = saved ?: return@LaunchedEffect
        launch { hopT.playHop() }
        delay(900)
        onSave(done)
    }
    // The character pops up to greet once the sheet has risen (S5).
    val greet = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(200)
        greet.animateTo(1f, BaMotion.bouncy())
    }
    val character = LocalCharacter.current
    var formTop by remember { mutableIntStateOf(0) }
    val keepName = with(LocalDensity.current) { 80.dp.roundToPx() }
    LaunchedEffect(full) {
        // The form rises toward the top, with the name still in sight above it.
        if (full && openedByTap) scroll.animateScrollTo((formTop - keepName).coerceAtLeast(0), tween(560, easing = BaMotion.Open))
    }

    // The sheet is its own window, and Material3 picks its system bar icons from contentColor. Without it the
    // default is black, so the sheet counted as light and the phone's navigation bar turned light (user, 2026-11-04).
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.surface2, contentColor = c.text1, shape = Px.Dialog) {
        // Everything scrolls; «ذخیره» stays pinned under it, always in reach.
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth().sheetScroll(scroll).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            // The top (D201): the title, with the character's portrait and its line beside it; in Simple only
            // the title and a plain line.
            val companion = Ba.companion
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (companion) CharacterPortrait(
                    character,
                    if (saved != null) Mood.HAPPY else Mood.CALM,
                    Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            val g = greet.value
                            alpha = g.coerceIn(0f, 1f)
                            scaleX = 0.35f + 0.65f * g
                            scaleY = 0.35f + 0.65f * g
                            translationY = (1 - g) * 26.dp.toPx()
                        }
                        .hop({ hopT.value }, { 8.dp }),
                )
                val (hi, ok) = character.editorLines()
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (original == null) R.string.tasks_new else R.string.editor_edit), style = Ba.type.title, color = c.text1)
                    val line = when {
                        saved != null -> if (companion) ok else R.string.editor_ok_simple
                        original != null -> null
                        else -> if (companion) hi else R.string.editor_hi_simple
                    }
                    // S3: the line crossfades when the character answers.
                    AnimatedContent(line, transitionSpec = { fadeIn(tween(200, delayMillis = 80)).togetherWith(fadeOut(tween(100))) }, label = "sheetLine") { res ->
                        if (res != null) Text(stringResource(res), style = Ba.type.label, color = c.text2)
                    }
                }
            }
            NameField(
                form.title,
                warn = tried && FormError.NO_TITLE in errors,
                modifier = Modifier.graphicsLayer { translationX = nudge.value.dp.toPx() },
                onChange = { form = form.copy(title = it.take(TaskForms.MAX_TITLE)) },
            )
            problem(FormError.NO_TITLE, R.string.error_title)

            AnimatedContent(
                full,
                transitionSpec = {
                    fadeIn(tween(260, delayMillis = 120)).togetherWith(fadeOut(tween(120)))
                        .using(SizeTransform(clip = true) { _, _ -> tween(560, easing = BaMotion.Open) })
                },
                label = "fullForm",
                modifier = Modifier.onGloballyPositioned { formTop = it.positionInParent().y.toInt() },
            ) { isFull ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isFull) {
                        FullForm(form, today, original, allTasks, problem, animateIn = openedByTap) { form = it }
                    } else {
                        SimpleForm(form, today, problem) { form = it }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clip(Px.Small).clickable(role = Role.Button) { openedByTap = true; full = true }.padding(vertical = 10.dp),
                        ) {
                            Text(stringResource(R.string.editor_more), style = Ba.type.label, color = c.glow)
                            Icon(BaIcons.ChevronDown, null, tint = c.glow, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (original != null && onDelete != null) {
                TextButton(onClick = { askDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.delete), style = Ba.type.label, color = c.text2)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                // A hairline over the button while more of the form is hidden under it.
                .drawBehind { if (scroll.canScrollForward) drawRect(c.border, size = Size(size.width, 1.dp.toPx())) }
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        ) {
            SaveButton(
                saved = saved != null,
                onClick = { if (errors.isEmpty()) { if (saved == null) saved = form } else { tried = true; attempt++ } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (askDelete && original != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = c.surface,
            title = { Text(stringResource(R.string.delete_title), style = Ba.type.title, color = c.text1) },
            text = { Text(stringResource(R.string.delete_text), style = Ba.type.body, color = c.text2) },
            confirmButton = { TextButton(onClick = { askDelete = false; onDelete(original) }) { Text(stringResource(R.string.delete_confirm), color = c.text1) } },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text(stringResource(R.string.cancel), color = c.text2) } },
        )
    }
}

/**
 * «ذخیره» (S7): on a good save it bumps, turns green and its label becomes a pixel tick drawn in one pixel
 * at a time while the character answers.
 */
@Composable
private fun SaveButton(saved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val bg by animateColorAsState(if (saved) c.accent else c.glow, tween(250), label = "saveBg")
    val bump = remember { Animatable(1f) }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(saved) {
        if (!saved) return@LaunchedEffect
        launch { bump.snapTo(0.96f); bump.animateTo(1f, BaMotion.bouncy()) }
        delay(120)
        tick.animateTo(1f, tween(260, easing = LinearEasing))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .clip(Px.Card)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        AnimatedContent(
            saved,
            transitionSpec = { fadeIn(tween(150, delayMillis = 100)).togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.7f)) },
            label = "save",
        ) { isSaved ->
            if (isSaved) {
                val pixel = Ba.companion
                Box(Modifier.size(28.dp).drawBehind { drawTick(c.onAccent, tick.value, pixel) })
            } else {
                Text(stringResource(R.string.save), style = Ba.type.subtitle, color = c.onGlow)
            }
        }
    }
}

/** «کِی؟»: هر وقت · امروز · فردا · یه روز… · هر روز, and a line saying which day and part (D128). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimpleForm(form: TaskForm, today: LocalDate, problem: @Composable (FormError, Int) -> Unit, onChange: (TaskForm) -> Unit) {
    val c = Ba.colors
    val chosen = TaskForms.whenOf(form, today)
    // «یه روز…» shows the calendar; it stays open while a day is being picked.
    var calendar by remember { mutableStateOf(chosen == When.DAY) }
    val highlight = rememberChoiceHighlight()
    Labeled(R.string.when_label) {
        ChoiceGroup(highlight, chosen) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            When.entries.forEach { choice ->
                val label = when (choice) {
                    When.ANYTIME -> stringResource(R.string.repeat_anytime)
                    When.TODAY -> stringResource(R.string.today)
                    When.TOMORROW -> stringResource(R.string.tomorrow)
                    When.DAY -> if (chosen == When.DAY) jalaliDayMonth(form.onceDate ?: today) else stringResource(R.string.when_day)
                    When.DAILY -> stringResource(R.string.repeat_daily)
                }
                PickPill(label, selected = chosen == choice, modifier = Modifier.choiceOf(highlight, choice), drawSelection = false) {
                    // «یه روز…» starts on the day after tomorrow, so it never repeats «امروز» or «فردا».
                    val day = if (choice == When.DAY && chosen != When.DAY) today.plusDays(2) else form.onceDate
                    onChange(TaskForms.withWhen(form, choice, today, day))
                    calendar = choice == When.DAY
                }
            }
        }
        }
        val part = partNames()[form.part.ordinal]
        val help = when (chosen) {
            When.ANYTIME -> stringResource(R.string.when_help_anytime)
            When.DAILY -> stringResource(R.string.when_help_dated, stringResource(R.string.repeat_daily), part)
            When.TODAY, When.TOMORROW, When.DAY -> stringResource(R.string.when_help_dated, dayLabel(form.onceDate ?: today, today), part)
            null -> ""
        }
        // S3: the line crossfades when the answer changes.
        AnimatedContent(
            help,
            transitionSpec = {
                (fadeIn(tween(180, delayMillis = 110)) + slideInVertically(tween(180, delayMillis = 110)) { it / 6 })
                    .togetherWith(fadeOut(tween(110)) + slideOutVertically(tween(110)) { -it / 6 })
            },
            label = "help",
        ) { text -> if (text.isNotEmpty()) Text(text, style = Ba.type.caption, color = c.text3) }
        Opens(calendar && chosen == When.DAY, duration = 420) {
            JalaliCalendar(form.onceDate, today, onPick = { onChange(TaskForms.withWhen(form, When.DAY, today, it)) })
        }
        problem(FormError.BAD_DATE, R.string.error_date)
    }
    // «بخش روز» (D201): in the simple sheet too, unless it's «هر وقت».
    if (chosen != When.ANYTIME) {
        val partHighlight = rememberChoiceHighlight()
        Labeled(R.string.editor_part) {
            val names = partNames()
            ChoiceGroup(partHighlight, form.part) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Part.entries.forEach { part ->
                        PickPill(names[part.ordinal], selected = form.part == part, modifier = Modifier.weight(1f).choiceOf(partHighlight, part), drawSelection = false) {
                            onChange(form.copy(part = part))
                        }
                    }
                }
            }
        }
    }
}

/** Every field (D107), with «هر وقت», the calendar and the alarm that follows the repeat (D126–D130). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullForm(
    form: TaskForm,
    today: LocalDate,
    original: Task?,
    allTasks: List<Task>,
    problem: @Composable (FormError, Int) -> Unit,
    animateIn: Boolean,
    onChange: (TaskForm) -> Unit,
) {
    val c = Ba.colors
    val anytime = TaskForms.isAnytime(form)
    var pickTime by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    var order = 0
    // S2: opened by a tap, each section rises in after the one before it.
    val rise = { Modifier.staggerIn(order++, animateIn) }

    Box(rise()) { Labeled(R.string.editor_icon) {
        val names = stringArrayResource(R.array.task_icon_names)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskIcon.entries.forEach { icon ->
                PickCircle(selected = form.icon == icon, description = names[icon.ordinal], onClick = { onChange(form.copy(icon = icon)) }) { tint ->
                    Icon(BaIcons.of(icon), null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
        }
    } }

    if (!anytime) {
        val partHighlight = rememberChoiceHighlight()
        Box(rise()) { Labeled(R.string.editor_part) {
            val names = partNames()
            ChoiceGroup(partHighlight, form.part) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Part.entries.forEach { part ->
                        PickBox(selected = form.part == part, onClick = { onChange(form.copy(part = part)) }, modifier = Modifier.weight(1f).choiceOf(partHighlight, part)) { tint ->
                            Icon(BaIcons.of(part), null, tint = tint, modifier = Modifier.size(22.dp))
                            Text(names[part.ordinal], style = Ba.type.caption, color = tint)
                        }
                    }
                }
            }
        } }
    }

    val repeatHighlight = rememberChoiceHighlight()
    Box(rise()) { Labeled(R.string.editor_repeat) {
        ChoiceGroup(repeatHighlight, form.repeat) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskForms.FULL_KINDS.forEach { kind ->
                val label = when (kind) {
                    RepeatKind.ANYTIME -> stringResource(R.string.repeat_anytime)
                    RepeatKind.DAILY -> stringResource(R.string.repeat_daily)
                    RepeatKind.WEEKDAYS -> stringResource(R.string.repeat_weekdays)
                    RepeatKind.EVERY_N_DAYS -> stringResource(R.string.repeat_every_n, PersianDigits.format(form.everyNDays))
                    RepeatKind.ONCE -> stringResource(R.string.repeat_once)
                    RepeatKind.MIN_DAY_ONLY -> stringResource(R.string.repeat_min_only)
                }
                PickPill(label, selected = form.repeat == kind, modifier = Modifier.choiceOf(repeatHighlight, kind), drawSelection = false) { onChange(form.copy(repeat = kind)) }
            }
        }
        }
        // S2: each repeat's own fields open under the pills and close again.
        Opens(form.repeat == RepeatKind.WEEKDAYS) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayCircles(form.weekdays) { onChange(form.copy(weekdays = it)) }
                problem(FormError.NO_WEEKDAYS, R.string.error_weekdays)
            }
        }
        Opens(form.repeat == RepeatKind.EVERY_N_DAYS) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(
                    value = form.everyNDays,
                    range = TaskForms.MIN_EVERY_N..TaskForms.MAX_EVERY_N,
                    label = { n -> stringResource(R.string.repeat_every_n, PersianDigits.format(n)) },
                ) { onChange(form.copy(everyNDays = it)) }
                Text(stringResource(R.string.editor_every_n_help), style = Ba.type.caption, color = c.text3)
            }
        }
        Opens(form.repeat == RepeatKind.ONCE) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRow(form.onceDate ?: today, today) { calendar = !calendar }
                Opens(calendar, duration = 420) { JalaliCalendar(form.onceDate, today, onPick = { onChange(form.copy(onceDate = it)) }) }
                problem(FormError.BAD_DATE, R.string.error_date)
            }
        }
        Opens(form.repeat == RepeatKind.ANYTIME) {
            Text(stringResource(R.string.when_help_anytime), style = Ba.type.caption, color = c.text3)
        }
    } }

    if (!anytime) {
        Box(rise()) { Labeled(R.string.editor_anchor) {
            InputBox(form.anchor, stringResource(R.string.editor_anchor_hint), onChange = { onChange(form.copy(anchor = it.take(TaskForms.MAX_ANCHOR))) })
            val suggestions = TaskForms.anchorSuggestions(allTasks, form.anchor)
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { a -> PickPill(a, selected = false, small = true) { onChange(form.copy(anchor = a)) } }
                }
            }
            Text(stringResource(R.string.editor_anchor_help), style = Ba.type.caption, color = c.text3)
        } }

        Box(rise()) { AlarmCard(form, original, problem, onTime = { pickTime = true }, onChange = onChange) }
    }

    Box(rise()) {
        ToggleRow(BaIcons.Flag, R.string.editor_important, R.string.editor_important_help, form.important) { onChange(form.copy(important = it)) }
    }
    if (!anytime) {
        val floorForced = form.repeat == RepeatKind.MIN_DAY_ONLY
        Box(rise()) {
            ToggleRow(BaIcons.Leaf, R.string.editor_min_day, R.string.editor_min_day_help, form.inMinimumDay || floorForced, enabled = !floorForced) {
                onChange(form.copy(inMinimumDay = it))
            }
        }
    }

    if (pickTime) {
        TimePickerDialog(form.alarmMinute, onPick = { onChange(form.copy(alarmMinute = it)); pickTime = false }, onDismiss = { pickTime = false })
    }
}

/**
 * «زنگ»: the switch, the time (large) and, only where the alarm uses them, the 7 day circles. A one-off
 * rings «همون روز», «هر N روز» on the days it's due (D130).
 */
@Composable
private fun AlarmCard(form: TaskForm, original: Task?, problem: @Composable (FormError, Int) -> Unit, onTime: () -> Unit, onChange: (TaskForm) -> Unit) {
    val c = Ba.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(BaIcons.Bell, null, tint = c.text2, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.editor_alarm), style = Ba.type.subtitle, color = c.text1)
                Text(stringResource(R.string.editor_alarm_help), style = Ba.type.caption, color = c.text3)
            }
            BaSwitch(form.alarmOn, { on ->
                // First time on: ring on the task's own days.
                val days = if (on && original?.alarm == null && !form.alarmOn) TaskForms.defaultAlarmDays(form) else form.alarmWeekdays
                onChange(form.copy(alarmOn = on, alarmWeekdays = days))
            }, stringResource(R.string.editor_alarm))
        }
        Opens(form.alarmOn) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    clockText(form.alarmMinute),
                    style = Ba.type.headline,
                    color = c.glow,
                    modifier = Modifier.clip(Px.Small).clickable(role = Role.Button, onClick = onTime).padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Box(Modifier.weight(1f))
                when (form.repeat) {
                    RepeatKind.ONCE -> Text(stringResource(R.string.alarm_on_day), style = Ba.type.caption, color = c.text3)
                    RepeatKind.EVERY_N_DAYS -> Text(stringResource(R.string.alarm_when_due), style = Ba.type.caption, color = c.text3)
                    else -> Unit
                }
            }
            if (TaskForms.alarmUsesDays(form.repeat)) {
                DayCircles(form.alarmWeekdays) { onChange(form.copy(alarmWeekdays = it)) }
                problem(FormError.NO_ALARM_DAYS, R.string.error_alarm_days)
            }
        } }
    }
}

/**
 * S2: a part of the form that opens to its height and closes again. The opening is slower for a tall part
 * ([duration]) so it never rushes; closing is quicker.
 */
@Composable
private fun Opens(visible: Boolean, duration: Int = 280, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(duration, easing = BaMotion.Open), expandFrom = Alignment.Top) + fadeIn(tween(duration)),
        exit = shrinkVertically(tween(220, easing = BaMotion.Close), shrinkTowards = Alignment.Top) + fadeOut(tween(160)),
    ) { content() }
}

/** A one-off's day: «جمعه ۸ آبان», tap to open or close the calendar. */
@Composable
private fun DateRow(date: LocalDate, today: LocalDate, onClick: () -> Unit) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Calendar, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(dayLabel(date, today), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.editor_date), style = Ba.type.caption, color = c.text3)
    }
}

@Composable
private fun NameField(value: String, warn: Boolean, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = Ba.colors
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    // S6: amber while a save is waiting for a name; never red. The character's color while typing.
    val edge by animateColorAsState(if (warn) c.amber else if (focused) c.glow else Color.Transparent, tween(200), label = "nameEdge")
    Labeled(R.string.editor_name) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Ba.type.subtitle.copy(fontSize = 17.sp, color = c.text1),
            cursorBrush = SolidColor(c.glow),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(Px.Card).background(Color.White.copy(alpha = 0.06f)).border(2.dp, edge, Px.Card).padding(horizontal = 14.dp, vertical = 13.dp)) {
                    if (value.isEmpty()) Text(stringResource(R.string.editor_name_hint), style = Ba.type.subtitle.copy(fontSize = 17.sp), color = c.text3)
                    inner()
                }
            },
        )
    }
}

/** A round, selectable choice (icons). The chosen one takes the accent. */
@Composable
private fun PickCircle(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable (Color) -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .popOn(selected)
            .size(48.dp)
            .clip(Px.Small)
            .background(if (selected) c.glow.copy(alpha = 0.18f) else c.surface)
            .then(if (selected) Modifier.border(2.dp, c.glow, Px.Small) else Modifier)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    ) { content(if (selected) c.glow else c.text2) }
}

/** A part of the day to pick, in a [ChoiceGroup] whose sliding highlight marks the chosen one (S1). */
@Composable
private fun PickBox(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable (Color) -> Unit) {
    val c = Ba.colors
    val tint by animateColorAsState(if (selected) c.glow else c.text2, tween(200), label = "boxTint")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(Px.Small)
            .background(if (selected) Color.Transparent else c.surface)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
    ) { content(tint) }
}

/** A small bounce when a choice turns on (icons, day circles). */
private fun Modifier.popOn(on: Boolean): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val was = remember { booleanArrayOf(on) }
    LaunchedEffect(on) {
        val turnedOn = on && !was[0]
        was[0] = on
        if (turnedOn) { scale.snapTo(0.75f); scale.animateTo(1f, BaMotion.bouncy()) }
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** 7 day circles, Saturday first (on the right in RTL). */
@Composable
private fun DayCircles(bits: Int, onChange: (Int) -> Unit) {
    val c = Ba.colors
    val letters = stringArrayResource(R.array.weekdays_short)
    val names = stringArrayResource(R.array.weekdays)
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        (0..6).forEach { i ->
            val on = bits and (1 shl i) != 0
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .popOn(on)
                    .size(40.dp)
                    .clip(Px.Small)
                    .background(if (on) c.glow else c.surface)
                    .then(if (on) Modifier else Modifier.border(2.dp, c.border, Px.Small))
                    .toggleable(on, role = Role.Checkbox) { onChange(bits xor (1 shl i)) }
                    .semantics { contentDescription = names[i] },
            ) {
                Text(letters[i], style = Ba.type.label, color = if (on) c.onGlow else c.text2)
            }
        }
    }
}

/** The character's hello on a new task and its answer on «ذخیره» (settings-task.html). */
private fun LanternCharacter.editorLines(): Pair<Int, Int> = when (this) {
    LanternCharacter.KNIGHT -> R.string.editor_hi_knight to R.string.editor_ok_knight
    LanternCharacter.VAMPIRE -> R.string.editor_hi_vampire to R.string.editor_ok_vampire
    LanternCharacter.WIZARD -> R.string.editor_hi_wizard to R.string.editor_ok_wizard
    LanternCharacter.GOTH -> R.string.editor_hi_goth to R.string.editor_ok_goth
}
