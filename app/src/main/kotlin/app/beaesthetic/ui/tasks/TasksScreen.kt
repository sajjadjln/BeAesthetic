package app.beaesthetic.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.AppContainer
import app.beaesthetic.R
import app.beaesthetic.domain.tasks.FormError
import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.RepeatKind
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskForm
import app.beaesthetic.domain.tasks.TaskForms
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.ui.clockText
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.BaSwitch
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.components.TimePickerDialog
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.settings.Segmented
import app.beaesthetic.ui.theme.Ba
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What the sheet is editing: a new task (in a part) or an existing one. */
private sealed interface Editing {
    data class New(val part: Part) : Editing
    data class Existing(val task: Task) : Editing
}

// ---------------------------------------------------------------- 07 Tasks

@Composable
fun TasksScreen(container: AppContainer, onBack: () -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    val data by remember { container.tasks.observeTasks() }.collectAsStateWithLifecycle(null)
    val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
    val rollover = settings?.rolloverHour ?: ProgramCalendar.DEFAULT_ROLLOVER_HOUR
    val today = container.clock.programDate(rollover)
    // 0 = all, then one per part.
    var filter by rememberSaveable { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Editing?>(null) }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.tasks_title), onBack, stringResource(R.string.cd_back))
            val (tasks, completions) = data ?: return@Column
            val listed = tasks.filter { TaskForms.isListed(it, completions[it.id].orEmpty(), today) }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            ) {
                if (listed.isEmpty()) {
                    EmptyTasks()
                    return@Column
                }
                val partNames = partNames()
                Segmented(listOf(stringResource(R.string.tasks_all)) + partNames, filter) { filter = it }
                val shown = if (filter == 0) listed else listed.filter { it.part == Part.entries[filter - 1] }
                val (once, repeating) = shown.partition { it.repeat is Repeat.Once }
                if (once.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.tasks_once))
                    TaskCard(once.sortedBy { (it.repeat as Repeat.Once).date }, today) { editing = Editing.Existing(it) }
                }
                val parts = if (filter == 0) Part.entries else listOf(Part.entries[filter - 1])
                parts.forEach { part ->
                    val inPart = repeating.filter { it.part == part }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
                    if (inPart.isEmpty() && filter == 0) return@forEach
                    SectionTitle(partNames[part.ordinal])
                    if (inPart.isEmpty()) {
                        Text(stringResource(R.string.tasks_part_empty), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(horizontal = 4.dp))
                    } else {
                        TaskCard(inPart, today) { editing = Editing.Existing(it) }
                    }
                }
            }
        }
        PrimaryButton(
            stringResource(R.string.tasks_new),
            onClick = { editing = Editing.New(if (filter == 0) Part.MORNING else Part.entries[filter - 1]) },
            icon = BaIcons.Plus,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }

    editing?.let { e ->
        val all = data?.first.orEmpty()
        TaskEditorSheet(
            editing = e,
            today = today,
            allTasks = all,
            onDismiss = { editing = null },
            onSave = { form ->
                val original = (e as? Editing.Existing)?.task
                val task = TaskForms.toTask(form, original, today, TaskForms.sortOrderFor(form, original, all))
                editing = null
                scope.launch {
                    container.tasks.save(task, today, container.clock.now())
                    container.scheduler.reconcile("task-edit")
                }
            },
            onDelete = { task ->
                editing = null
                scope.launch {
                    container.tasks.archive(task.id, today, container.clock.now())
                    container.scheduler.reconcile("task-delete")
                }
            },
        )
    }
}

@Composable
private fun partNames(): List<String> =
    listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night).map { stringResource(it) }

@Composable
private fun EmptyTasks() {
    val c = Ba.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
    ) {
        Icon(BaIcons.List, null, tint = c.text3, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.tasks_empty_title), style = Ba.type.title, color = c.text1)
        Text(stringResource(R.string.tasks_empty_text), style = Ba.type.body, color = c.text2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun TaskCard(tasks: List<Task>, today: LocalDate, onOpen: (Task) -> Unit) {
    val c = Ba.colors
    BaCard(padding = PaddingValues(0.dp)) {
        tasks.forEachIndexed { i, task ->
            if (i > 0) HorizontalDivider(color = c.border)
            ListRow(onClick = { onOpen(task) }) {
                Icon(BaIcons.of(task.icon), null, tint = c.text2, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = Ba.type.subtitle, color = c.text1)
                    Text(rowCaption(task, today), style = Ba.type.caption, color = c.text3)
                }
                // Markers (screens.html 07): «مهم», leaf (in the floor), bell (own alarm), repeat (every N days).
                if (task.isImportant) Chip(stringResource(R.string.tag_important), ChipKind.AMBER)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.inMinimumDay) Icon(BaIcons.Leaf, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    if (task.alarm?.enabled == true) Icon(BaIcons.Bell, null, tint = c.text2, modifier = Modifier.size(16.dp))
                    if (task.repeat is Repeat.EveryNDays) Icon(BaIcons.Repeat, null, tint = c.text2, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** «هر روز», «شنبه، دوشنبه», «هر ۳ روز», «پنجشنبه · ۲۴ مهر» (one-offs show the part), plus the alarm time. */
@Composable
private fun rowCaption(task: Task, today: LocalDate): String {
    val weekdays = stringArrayResource(R.array.weekdays)
    val repeat = when (val r = task.repeat) {
        Repeat.Daily -> stringResource(R.string.repeat_daily)
        is Repeat.OnWeekdays -> weekdayList(r.bits, weekdays)
        is Repeat.EveryNDays -> stringResource(R.string.repeat_every_n, PersianDigits.format(r.n))
        is Repeat.Once -> stringResource(R.string.row_once, dayLabel(r.date, today), partNames()[task.part.ordinal])
        Repeat.MinDayOnly -> stringResource(R.string.repeat_min_only)
    }
    val alarm = task.alarm?.takeIf { it.enabled } ?: return repeat
    return stringResource(R.string.row_with_alarm, repeat, clockText(alarm.minuteOfDay))
}

private fun weekdayList(bits: Int, names: Array<String>): String =
    (0..6).filter { bits and (1 shl it) != 0 }.joinToString("، ") { names[it] }

/** «امروز», «فردا», else «پنجشنبه ۲۴ مهر». */
@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.plusDays(1) -> stringResource(R.string.tomorrow)
    else -> stringArrayResource(R.array.weekdays)[Weekdays.index(date.dayOfWeek)] + " " + jalaliDayMonth(date)
}

// ---------------------------------------------------------------- 08 Task editor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorSheet(
    editing: Editing,
    today: LocalDate,
    allTasks: List<Task>,
    onDismiss: () -> Unit,
    onSave: (TaskForm) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val c = Ba.colors
    val original = (editing as? Editing.Existing)?.task
    var form by remember(editing) {
        mutableStateOf(original?.let(TaskForms::from) ?: TaskForms.blank((editing as Editing.New).part, today))
    }
    // Errors show only after the first «ذخیره», never while typing.
    var tried by remember(editing) { mutableStateOf(false) }
    // Each failed «ذخیره» scrolls the first problem into view (errors are in screen order).
    var attempt by remember(editing) { mutableStateOf(0) }
    var pickTime by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    val errors = TaskForms.errors(form, today, original)
    val first = errors.minByOrNull { it.ordinal }
    val scroll = rememberScrollState()
    // A missing name goes to the very top, so the field shows with its message.
    LaunchedEffect(attempt) { if (attempt > 0 && first == FormError.NO_TITLE) scroll.animateScrollTo(0) }
    @Composable fun Problem(error: FormError, text: Int) {
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
            Problem(FormError.NO_TITLE, R.string.error_title)

            Labeled(R.string.editor_icon) {
                val names = stringArrayResource(R.array.task_icon_names)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskIcon.entries.forEach { icon ->
                        PickCircle(selected = form.icon == icon, description = names[icon.ordinal], onClick = { form = form.copy(icon = icon) }) { tint ->
                            Icon(BaIcons.of(icon), null, tint = tint, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            Labeled(R.string.editor_part) {
                val names = partNames()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Part.entries.forEach { part ->
                        PickBox(selected = form.part == part, onClick = { form = form.copy(part = part) }, modifier = Modifier.weight(1f)) { tint ->
                            Icon(BaIcons.of(part), null, tint = tint, modifier = Modifier.size(22.dp))
                            Text(names[part.ordinal], style = Ba.type.caption, color = tint)
                        }
                    }
                }
            }

            Labeled(R.string.editor_repeat) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatKind.entries.forEach { kind ->
                        val label = when (kind) {
                            RepeatKind.DAILY -> stringResource(R.string.repeat_daily)
                            RepeatKind.WEEKDAYS -> stringResource(R.string.repeat_weekdays)
                            RepeatKind.EVERY_N_DAYS -> stringResource(R.string.repeat_every_n, PersianDigits.format(form.everyNDays))
                            RepeatKind.ONCE -> stringResource(R.string.repeat_once)
                            RepeatKind.MIN_DAY_ONLY -> stringResource(R.string.repeat_min_only)
                        }
                        PickPill(label, selected = form.repeat == kind) { form = form.copy(repeat = kind) }
                    }
                }
                when (form.repeat) {
                    RepeatKind.WEEKDAYS -> {
                        DayCircles(form.weekdays) { form = form.copy(weekdays = it) }
                        Problem(FormError.NO_WEEKDAYS, R.string.error_weekdays)
                    }
                    RepeatKind.EVERY_N_DAYS -> {
                        Stepper(
                            value = form.everyNDays,
                            range = TaskForms.MIN_EVERY_N..TaskForms.MAX_EVERY_N,
                            label = stringResource(R.string.repeat_every_n, PersianDigits.format(form.everyNDays)),
                        ) { form = form.copy(everyNDays = it) }
                        Text(stringResource(R.string.editor_every_n_help), style = Ba.type.caption, color = c.text3)
                    }
                    RepeatKind.ONCE -> {
                        Text(stringResource(R.string.editor_once_day), style = Ba.type.label, color = c.text2)
                        DayStrip(form.onceDate, today) { form = form.copy(onceDate = it) }
                        Problem(FormError.BAD_DATE, R.string.error_date)
                    }
                    else -> Unit
                }
            }

            Labeled(R.string.editor_anchor) {
                InputBox(form.anchor, stringResource(R.string.editor_anchor_hint), onChange = { form = form.copy(anchor = it.take(TaskForms.MAX_ANCHOR)) })
                val suggestions = TaskForms.anchorSuggestions(allTasks, form.anchor)
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { a -> PickPill(a, selected = false, small = true) { form = form.copy(anchor = a) } }
                    }
                }
                Text(stringResource(R.string.editor_anchor_help), style = Ba.type.caption, color = c.text3)
            }

            // «زنگ» card: switch, the time (large) and the 7 day circles.
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
                        form = form.copy(alarmOn = on, alarmWeekdays = days)
                    }, stringResource(R.string.editor_alarm))
                }
                if (form.alarmOn) {
                    Text(
                        clockText(form.alarmMinute),
                        style = Ba.type.headline,
                        color = c.text1,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { pickTime = true }.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    DayCircles(form.alarmWeekdays) { form = form.copy(alarmWeekdays = it) }
                    Problem(FormError.NO_ALARM_DAYS, R.string.error_alarm_days)
                }
            }

            ToggleRow(BaIcons.Flag, R.string.editor_important, R.string.editor_important_help, form.important) { form = form.copy(important = it) }
            val floorForced = form.repeat == RepeatKind.MIN_DAY_ONLY
            ToggleRow(BaIcons.Leaf, R.string.editor_min_day, R.string.editor_min_day_help, form.inMinimumDay || floorForced, enabled = !floorForced) {
                form = form.copy(inMinimumDay = it)
            }

            PrimaryButton(
                stringResource(R.string.save),
                onClick = { if (errors.isEmpty()) onSave(form) else { tried = true; attempt++ } },
                modifier = Modifier.fillMaxWidth(),
            )
            if (original != null) {
                TextButton(onClick = { askDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.delete), style = Ba.type.label, color = c.text2)
                }
            }
        }
    }

    if (pickTime) {
        TimePickerDialog(form.alarmMinute, onPick = { form = form.copy(alarmMinute = it); pickTime = false }, onDismiss = { pickTime = false })
    }
    if (askDelete && original != null) {
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

@Composable
private fun Labeled(label: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = Ba.type.label, color = Ba.colors.text2)
        content()
    }
}

/**
 * A form problem, in neutral words and colors: nothing in the app is ever red. A [scrollTo] above 0 brings
 * it into view; a new value (another failed save) does it again.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ErrorLine(text: Int, scrollTo: Int = 0) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(scrollTo) { if (scrollTo > 0) requester.bringIntoView() }
    Text(stringResource(text), style = Ba.type.caption, color = Ba.colors.amber, modifier = Modifier.bringIntoViewRequester(requester))
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

@Composable
private fun InputBox(value: String, hint: String, onChange: (String) -> Unit) {
    val c = Ba.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = Ba.type.body.copy(color = c.text1),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (value.isEmpty()) Text(hint, style = Ba.type.body, color = c.text3)
                inner()
            }
        },
    )
}

/** A round, selectable choice (icons). The chosen one takes the accent. */
@Composable
private fun PickCircle(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable (androidx.compose.ui.graphics.Color) -> Unit) {
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
private fun PickBox(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable (androidx.compose.ui.graphics.Color) -> Unit) {
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

@Composable
private fun PickPill(text: String, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(if (small) 34.dp else 40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.accentSubtle else c.surface2)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text, style = if (small) Ba.type.caption else Ba.type.label, color = if (selected) c.onAccentSubtle else c.text1)
    }
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

@Composable
private fun Stepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepButton(BaIcons.Minus, stringResource(R.string.cd_less), enabled = value > range.first) { onChange(value - 1) }
        Text(label, style = Ba.type.subtitle, color = c.text1)
        StepButton(BaIcons.Plus, stringResource(R.string.cd_more), enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { Icon(icon, null, tint = if (enabled) c.text1 else c.text3, modifier = Modifier.size(20.dp)) }
}

/** The next 4 weeks as pills (a one-off task is close by); an edited task's past date stays at the start. */
@Composable
private fun DayStrip(selected: LocalDate?, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val days = remember(today, selected) {
        val ahead = (0L until 28L).map { today.plusDays(it) }
        if (selected != null && selected !in ahead) listOf(selected) + ahead else ahead
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(days) { d -> PickPill(dayLabel(d, today), selected = d == selected) { onPick(d) } }
    }
}

@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, help: Int, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = Ba.type.subtitle, color = if (enabled) c.text1 else c.text3)
            Text(stringResource(help), style = Ba.type.caption, color = c.text3)
        }
        if (enabled) BaSwitch(checked, onChange, stringResource(title)) else BaSwitch(checked, {}, stringResource(title))
    }
}
