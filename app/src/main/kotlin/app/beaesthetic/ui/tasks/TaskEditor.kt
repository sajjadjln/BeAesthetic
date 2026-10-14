package app.beaesthetic.ui.tasks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.beaesthetic.R
import app.beaesthetic.domain.tasks.FormError
import app.beaesthetic.domain.tasks.RepeatKind
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskForm
import app.beaesthetic.domain.tasks.TaskForms
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.tasks.When
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Part
import app.beaesthetic.ui.clockText
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.BaSwitch
import app.beaesthetic.ui.components.ErrorLine
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.JalaliCalendar
import app.beaesthetic.ui.components.Labeled
import app.beaesthetic.ui.components.PickPill
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.Stepper
import app.beaesthetic.ui.components.TimePickerDialog
import app.beaesthetic.ui.components.ToggleRow
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
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
    // Errors show only after the first «ذخیره», never while typing.
    var tried by remember(editing) { mutableStateOf(false) }
    // Each failed «ذخیره» scrolls the first problem into view (errors are in screen order).
    var attempt by remember(editing) { mutableIntStateOf(0) }
    var askDelete by remember { mutableStateOf(false) }
    val errors = TaskForms.errors(form, today, original)
    val first = errors.minByOrNull { it.ordinal }
    val scroll = rememberScrollState()
    // A missing name goes to the very top, so the field shows with its message.
    LaunchedEffect(attempt) { if (attempt > 0 && first == FormError.NO_TITLE) scroll.animateScrollTo(0) }
    val problem: @Composable (FormError, Int) -> Unit = { error, text ->
        val scrollTo = if (error == first && error != FormError.NO_TITLE) attempt else 0
        if (tried && error in errors) ErrorLine(text, scrollTo)
    }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(stringResource(if (original == null) R.string.editor_new else R.string.editor_edit), style = Ba.type.title, color = c.text1)
            NameField(form.title, onChange = { form = form.copy(title = it.take(TaskForms.MAX_TITLE)) })
            problem(FormError.NO_TITLE, R.string.error_title)

            if (full) {
                FullForm(form, today, original, allTasks, problem) { form = it }
            } else {
                SimpleForm(form, today, problem) { form = it }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { full = true }.padding(vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.editor_more), style = Ba.type.label, color = c.text2)
                    Icon(BaIcons.ChevronDown, null, tint = c.text2, modifier = Modifier.size(18.dp))
                }
            }

            PrimaryButton(
                stringResource(R.string.save),
                onClick = { if (errors.isEmpty()) onSave(form) else { tried = true; attempt++ } },
                modifier = Modifier.fillMaxWidth(),
            )
            if (original != null && onDelete != null) {
                TextButton(onClick = { askDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.delete), style = Ba.type.label, color = c.text2)
                }
            }
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

/** «کِی؟»: هر وقت · امروز · فردا · یه روز… · هر روز, and a line saying which day and part (D128). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimpleForm(form: TaskForm, today: LocalDate, problem: @Composable (FormError, Int) -> Unit, onChange: (TaskForm) -> Unit) {
    val c = Ba.colors
    val chosen = TaskForms.whenOf(form, today)
    // «یه روز…» shows the calendar; it stays open while a day is being picked.
    var calendar by remember { mutableStateOf(chosen == When.DAY) }
    Labeled(R.string.when_label) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            When.entries.forEach { choice ->
                val label = when (choice) {
                    When.ANYTIME -> stringResource(R.string.repeat_anytime)
                    When.TODAY -> stringResource(R.string.today)
                    When.TOMORROW -> stringResource(R.string.tomorrow)
                    When.DAY -> if (chosen == When.DAY) jalaliDayMonth(form.onceDate ?: today) else stringResource(R.string.when_day)
                    When.DAILY -> stringResource(R.string.repeat_daily)
                }
                PickPill(label, selected = chosen == choice) {
                    // «یه روز…» starts on the day after tomorrow, so it never repeats «امروز» or «فردا».
                    val day = if (choice == When.DAY && chosen != When.DAY) today.plusDays(2) else form.onceDate
                    onChange(TaskForms.withWhen(form, choice, today, day))
                    calendar = choice == When.DAY
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
        if (help.isNotEmpty()) Text(help, style = Ba.type.caption, color = c.text3)
        if (calendar && chosen == When.DAY) {
            JalaliCalendar(form.onceDate, today, onPick = { onChange(TaskForms.withWhen(form, When.DAY, today, it)) })
        }
        problem(FormError.BAD_DATE, R.string.error_date)
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
    onChange: (TaskForm) -> Unit,
) {
    val c = Ba.colors
    val anytime = TaskForms.isAnytime(form)
    var pickTime by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }

    Labeled(R.string.editor_icon) {
        val names = stringArrayResource(R.array.task_icon_names)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskIcon.entries.forEach { icon ->
                PickCircle(selected = form.icon == icon, description = names[icon.ordinal], onClick = { onChange(form.copy(icon = icon)) }) { tint ->
                    Icon(BaIcons.of(icon), null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    if (!anytime) {
        Labeled(R.string.editor_part) {
            val names = partNames()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Part.entries.forEach { part ->
                    PickBox(selected = form.part == part, onClick = { onChange(form.copy(part = part)) }, modifier = Modifier.weight(1f)) { tint ->
                        Icon(BaIcons.of(part), null, tint = tint, modifier = Modifier.size(22.dp))
                        Text(names[part.ordinal], style = Ba.type.caption, color = tint)
                    }
                }
            }
        }
    }

    Labeled(R.string.editor_repeat) {
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
                PickPill(label, selected = form.repeat == kind) { onChange(form.copy(repeat = kind)) }
            }
        }
        when (form.repeat) {
            RepeatKind.WEEKDAYS -> {
                DayCircles(form.weekdays) { onChange(form.copy(weekdays = it)) }
                problem(FormError.NO_WEEKDAYS, R.string.error_weekdays)
            }
            RepeatKind.EVERY_N_DAYS -> {
                Stepper(
                    value = form.everyNDays,
                    range = TaskForms.MIN_EVERY_N..TaskForms.MAX_EVERY_N,
                    label = stringResource(R.string.repeat_every_n, PersianDigits.format(form.everyNDays)),
                ) { onChange(form.copy(everyNDays = it)) }
                Text(stringResource(R.string.editor_every_n_help), style = Ba.type.caption, color = c.text3)
            }
            RepeatKind.ONCE -> {
                DateRow(form.onceDate ?: today, today) { calendar = !calendar }
                if (calendar) JalaliCalendar(form.onceDate, today, onPick = { onChange(form.copy(onceDate = it)) })
                problem(FormError.BAD_DATE, R.string.error_date)
            }
            RepeatKind.ANYTIME -> Text(stringResource(R.string.when_help_anytime), style = Ba.type.caption, color = c.text3)
            else -> Unit
        }
    }

    if (!anytime) {
        Labeled(R.string.editor_anchor) {
            InputBox(form.anchor, stringResource(R.string.editor_anchor_hint), onChange = { onChange(form.copy(anchor = it.take(TaskForms.MAX_ANCHOR))) })
            val suggestions = TaskForms.anchorSuggestions(allTasks, form.anchor)
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { a -> PickPill(a, selected = false, small = true) { onChange(form.copy(anchor = a)) } }
                }
            }
            Text(stringResource(R.string.editor_anchor_help), style = Ba.type.caption, color = c.text3)
        }

        AlarmCard(form, original, problem, onTime = { pickTime = true }, onChange = onChange)
    }

    ToggleRow(BaIcons.Flag, R.string.editor_important, R.string.editor_important_help, form.important) { onChange(form.copy(important = it)) }
    if (!anytime) {
        val floorForced = form.repeat == RepeatKind.MIN_DAY_ONLY
        ToggleRow(BaIcons.Leaf, R.string.editor_min_day, R.string.editor_min_day_help, form.inMinimumDay || floorForced, enabled = !floorForced) {
            onChange(form.copy(inMinimumDay = it))
        }
        ToggleRow(BaIcons.Dumbbell, R.string.editor_workout, R.string.editor_workout_help, form.opensWorkout) { onChange(form.copy(opensWorkout = it)) }
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(16.dp),
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
        if (form.alarmOn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    clockText(form.alarmMinute),
                    style = Ba.type.headline,
                    color = c.text1,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onTime).padding(horizontal = 8.dp, vertical = 2.dp),
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
        }
    }
}

/** A one-off's day: «جمعه ۸ آبان», tap to open or close the calendar. */
@Composable
private fun DateRow(date: LocalDate, today: LocalDate, onClick: () -> Unit) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Calendar, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(dayLabel(date, today), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.editor_date), style = Ba.type.caption, color = c.text3)
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    val c = Ba.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = Ba.type.headline.copy(color = c.text1),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(stringResource(R.string.editor_name), style = Ba.type.headline, color = c.text3)
                inner()
            }
        },
    )
}

/** A round, selectable choice (icons). The chosen one takes the accent. */
@Composable
private fun PickCircle(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable (Color) -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) c.accentSubtle else c.surface2)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    ) { content(if (selected) c.onAccentSubtle else c.text2) }
}

@Composable
private fun PickBox(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable (Color) -> Unit) {
    val c = Ba.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.accentSubtle else c.surface2)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
    ) { content(if (selected) c.onAccentSubtle else c.text2) }
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (on) c.accent else c.surface)
                    .then(if (on) Modifier else Modifier.border(1.5.dp, c.border, CircleShape))
                    .toggleable(on, role = Role.Checkbox) { onChange(bits xor (1 shl i)) }
                    .semantics { contentDescription = names[i] },
            ) {
                Text(letters[i], style = Ba.type.label, color = if (on) c.onAccent else c.text2)
            }
        }
    }
}
