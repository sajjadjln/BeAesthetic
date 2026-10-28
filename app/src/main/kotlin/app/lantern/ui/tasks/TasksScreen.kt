package app.lantern.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.R
import app.lantern.domain.tasks.Repeat
import app.lantern.domain.tasks.RepeatKind
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskAction
import app.lantern.domain.tasks.TaskForm
import app.lantern.domain.tasks.TaskForms
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.domain.time.Weekdays
import app.lantern.ui.clockText
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.Chip
import app.lantern.ui.components.ChipKind
import app.lantern.ui.components.ListRow
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.SectionTitle
import app.lantern.ui.components.TopBar
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.settings.Segmented
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.LanternTheme
import java.time.LocalDate

// ---------------------------------------------------------------- 07 Tasks

@Composable
fun TasksRoute(viewModel: TasksViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TasksScreen(state, onBack, onSave = viewModel::save, onDelete = viewModel::delete)
}

/** Stateless: the list and the editor sheet. Only the filter and the open sheet are local UI state. */
@Composable
fun TasksScreen(state: TasksUiState?, onBack: () -> Unit, onSave: (TaskForm, Task?) -> Unit, onDelete: (Task) -> Unit) {
    val c = Ba.colors
    // 0 = all, then one per part.
    var filter by rememberSaveable { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Editing?>(null) }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.tasks_title), onBack, stringResource(R.string.cd_back))
            val (today, tasks, completions) = state ?: return@Column
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
                // «هر وقت» has no part, so a part filter hides it (D127).
                val (anytime, dated) = listed.partition { it.repeat == Repeat.Anytime }
                if (filter == 0 && anytime.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.tasks_anytime))
                    TaskCard(anytime.sortedWith(compareBy({ it.sortOrder }, { it.id })), today) { editing = Editing.Existing(it) }
                }
                val shown = if (filter == 0) dated else dated.filter { it.part == Part.entries[filter - 1] }
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
            // From «همه» a new task starts as «هر وقت»; from a part's filter, as a daily task in that part (D128).
            onClick = { editing = if (filter == 0) Editing.New(Part.MORNING, RepeatKind.ANYTIME) else Editing.New(Part.entries[filter - 1]) },
            icon = BaIcons.Plus,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }

    val s = state ?: return
    editing?.let { e ->
        TaskEditorSheet(
            editing = e,
            today = s.today,
            allTasks = s.tasks,
            onDismiss = { editing = null },
            onSave = { form ->
                editing = null
                onSave(form, (e as? Editing.Existing)?.task)
            },
            onDelete = { task ->
                editing = null
                onDelete(task)
            },
        )
    }
}

@Composable
internal fun partNames(): List<String> =
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
                    rowCaption(task, today).takeIf { it.isNotEmpty() }?.let { Text(it, style = Ba.type.caption, color = c.text3) }
                }
                // Markers (screens.html 07): «مهم», leaf (in the floor), bell (own alarm), repeat (every N days).
                if (task.isImportant) Chip(stringResource(R.string.tag_important), ChipKind.AMBER)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.inMinimumDay) Icon(BaIcons.Leaf, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    if (task.alarm?.enabled == true) Icon(BaIcons.Bell, null, tint = c.text2, modifier = Modifier.size(16.dp))
                    if (task.repeat is Repeat.EveryNDays) Icon(BaIcons.Repeat, null, tint = c.text2, modifier = Modifier.size(16.dp))
                    if (task.action == TaskAction.WORKOUT) Icon(BaIcons.Dumbbell, null, tint = c.text2, modifier = Modifier.size(16.dp))
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
        Repeat.Anytime -> return ""
    }
    val alarm = task.alarm?.takeIf { it.enabled } ?: return repeat
    return stringResource(R.string.row_with_alarm, repeat, clockText(alarm.minuteOfDay))
}

private fun weekdayList(bits: Int, names: Array<String>): String =
    (0..6).filter { bits and (1 shl it) != 0 }.joinToString("، ") { names[it] }

/** «امروز», «فردا», else «پنجشنبه ۲۴ مهر». */
@Composable
internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.plusDays(1) -> stringResource(R.string.tomorrow)
    else -> stringArrayResource(R.array.weekdays)[Weekdays.index(date.dayOfWeek)] + " " + jalaliDayMonth(date)
}
