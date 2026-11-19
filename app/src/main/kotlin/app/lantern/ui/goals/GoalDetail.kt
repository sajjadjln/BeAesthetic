package app.lantern.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.R
import app.lantern.domain.goals.GoalStatus
import app.lantern.domain.tasks.DueRules
import app.lantern.domain.tasks.Task
import app.lantern.domain.text.PersianDigits
import app.lantern.ui.components.*
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.tasks.TaskEditorSheet
import app.lantern.ui.tasks.Editing
import app.lantern.ui.theme.*
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
fun GoalDetailRoute(vm: GoalsViewModel, id: Long, onBack: () -> Unit, onToday: (() -> Unit)? = null) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return
    val note = s.notes.firstOrNull { it.goal.id == id }
    val error by vm.error.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var why by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var milestoneId by rememberSaveable { mutableStateOf<Long?>(null) }
    var statement by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        TopBar(note?.goal?.title ?: stringResource(R.string.tab_goals), onBack, stringResource(R.string.cd_back))
        if (note == null) { Text(stringResource(R.string.goal_unavailable), color = Ba.colors.text2, modifier = Modifier.padding(24.dp)); return@Column }
        val g = note.goal
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.goal_until, jalaliDayMonth(g.finishDate)), style = Ba.type.subtitle, color = Ba.colors.glow)
            Text(stringResource(R.string.goal_remaining, PersianDigits.format(ChronoUnit.DAYS.between(s.today, g.finishDate).coerceAtLeast(0))), style = Ba.type.body, color = Ba.colors.text2)
            Text(stringResource(R.string.goal_flex_count, PersianDigits.format(g.flexDays)), style = Ba.type.label, color = Ba.colors.text2)
            BaCard {
                SectionTitle(stringResource(R.string.goal_this_week))
                Text(stringResource(R.string.goal_week_progress, PersianDigits.format(note.weekCount(s.today)), PersianDigits.format(g.weeklyLow), PersianDigits.format(g.weeklyHigh)), style = Ba.type.title, color = Ba.colors.glow)
                GoalWeekStrip(note, s.today)
                note.activeActions.forEach { task ->
                    val doneDates = note.completions[task.id].orEmpty()
                    val item = DueRules.itemFor(s.today, task, doneDates)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).clickable { editing = task }.padding(vertical = 12.dp)) {
                            Text(task.title, style = Ba.type.subtitle, color = Ba.colors.text1)
                            task.anchor?.let { Text(it, style = Ba.type.caption, color = Ba.colors.text2) }
                            Text(stringResource(R.string.goal_edit_action), style = Ba.type.caption, color = Ba.colors.glow)
                        }
                        if (g.status == GoalStatus.ACTIVE.name && item != null) {
                            Checkbox(checked = item.done, onCheckedChange = { vm.tick(task.id) }, colors = CheckboxDefaults.colors(checkedColor = Ba.colors.accent))
                        }
                    }
                }
            }
            if (note.milestones.isNotEmpty()) {
                SectionTitle(stringResource(R.string.goal_path))
                note.milestones.forEach { month ->
                    val current = s.today in month.startDate..month.endDate
                    BaCard {
                        Text(jalaliDayMonth(month.startDate) + " – " + jalaliDayMonth(month.endDate), style = Ba.type.label, color = if (current) Ba.colors.glow else Ba.colors.text2)
                        Row(Modifier.fillMaxWidth()) {
                            Text(month.statement.ifBlank { stringResource(R.string.goal_not_sure) }, style = Ba.type.body, color = Ba.colors.text1, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                            Checkbox(month.reachedAt != null, { vm.milestone(month.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Ba.colors.accent))
                        }
                        if (current) GoalProgress(note.progress(s.today))
                        TextButton(onClick = { milestoneId = month.id; statement = month.statement }) { Text(stringResource(R.string.goal_edit_milestone), color = Ba.colors.glow) }
                    }
                }
            }
            // Always in the page, never behind the reminder dialog (D217).
            Column(Modifier.fillMaxWidth().clip(Px.Card).background(Ba.colors.glow.copy(alpha = 0.10f)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.goal_why_title), style = Ba.type.subtitle, color = Ba.colors.glow)
                Text(g.why, style = Ba.type.body, color = Ba.colors.text1)
                TextButton(onClick = { why = true }) { Text(stringResource(R.string.goal_read_why), color = Ba.colors.glow) }
            }
            if (g.triedBefore) BaCard {
                SectionTitle(stringResource(R.string.goal_baseline))
                Text(stringResource(R.string.goal_minutes, PersianDigits.format(g.baselineMinutes)), style = Ba.type.body, color = Ba.colors.text2)
                Text(g.triedWhat, style = Ba.type.body, color = Ba.colors.text1)
                Text(g.obstacle, style = Ba.type.body, color = Ba.colors.text2)
            }
            if (g.status == GoalStatus.ACTIVE.name) {
                PrimaryButton(stringResource(R.string.goal_reached), { confirm = GoalStatus.ACHIEVED.name }, Modifier.fillMaxWidth())
                SecondaryButton(stringResource(R.string.goal_set_aside), { confirm = GoalStatus.SET_ASIDE.name }, Modifier.fillMaxWidth())
            } else {
                Text(stringResource(if (g.status == GoalStatus.ACHIEVED.name) R.string.goal_achieved else R.string.goal_aside), style = Ba.type.title, color = Ba.colors.accent)
                SecondaryButton(stringResource(R.string.goal_resume), { vm.status(id, GoalStatus.ACTIVE) }, Modifier.fillMaxWidth())
            }
        }
    }
    if (error) AlertDialog(onDismissRequest = vm::dismissError, text = { Text(stringResource(R.string.goal_save_error)) }, confirmButton = { TextButton(onClick = vm::dismissError) { Text(stringResource(R.string.goal_close)) } })
    if (note != null && why) GoalWhyDialog(note, onDismiss = { why = false }, onStep = { why = false; if (onToday != null) onToday() else scope.launch { scroll.animateScrollTo(0) } })
    confirm?.let { status ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(if (status == GoalStatus.ACHIEVED.name) R.string.goal_reached else R.string.goal_set_aside)) },
            text = { Text(stringResource(R.string.goal_status_help)) },
            confirmButton = { TextButton(onClick = { vm.status(id, GoalStatus.valueOf(status)); confirm = null }) { Text(stringResource(R.string.goal_confirm)) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.goal_cancel)) } },
        )
    }
    milestoneId?.let { monthId ->
        AlertDialog(
            onDismissRequest = { milestoneId = null }, title = { Text(stringResource(R.string.goal_edit_milestone)) },
            text = { GoalInput(statement, stringResource(R.string.goal_milestone_hint)) { statement = it } },
            confirmButton = { TextButton(onClick = { vm.statement(monthId, statement); milestoneId = null }, enabled = statement.isNotBlank()) { Text(stringResource(R.string.goal_confirm)) } },
            dismissButton = { TextButton(onClick = { milestoneId = null }) { Text(stringResource(R.string.goal_cancel)) } },
        )
    }
    editing?.let { task ->
        TaskEditorSheet(editing = Editing.Existing(task), today = s.today, allTasks = note?.actions.orEmpty(), onDismiss = { editing = null }, onSave = { form -> vm.saveAction(form, task); editing = null })
    }
}
