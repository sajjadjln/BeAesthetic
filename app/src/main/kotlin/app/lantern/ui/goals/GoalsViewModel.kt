package app.lantern.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lantern.AppContainer
import app.lantern.data.db.GoalEntity
import app.lantern.data.db.GoalMilestoneEntity
import app.lantern.data.repo.toDomain
import app.lantern.domain.goals.GoalCalendar
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.goals.GoalDraft
import app.lantern.domain.goals.GoalPlans
import app.lantern.domain.goals.GoalStatus
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskForm
import app.lantern.domain.time.ProgramCalendar
import app.lantern.widget.TodayWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** All wall progress comes from the same completion rows as Today. Nothing is duplicated. */
data class GoalNote(
    val goal: GoalEntity,
    val milestones: List<GoalMilestoneEntity>,
    val actions: List<Task>,
    val completions: Map<Long, List<LocalDate>>,
    val activeActionIds: Set<Long>,
) {
    val activeActions get() = actions.filter { it.id in activeActionIds }
    val taskIds get() = actions.map { it.id }.toSet()
    fun sessions(from: LocalDate, to: LocalDate) = GoalPlans.sessions(taskIds, completions, from, to)
    fun weekCount(today: LocalDate): Int = sessions(ProgramCalendar.weekStart(today), today)
    fun month(today: LocalDate) = milestones.firstOrNull { today in it.startDate..it.endDate }
        ?: if (today > goal.finishDate) milestones.lastOrNull() else milestones.firstOrNull()
    fun progress(today: LocalDate): Float {
        val month = month(today)
        val from = month?.startDate ?: goal.startDate
        val to = month?.endDate ?: goal.finishDate
        val planned = GoalPlans.planned(actions, from, to)
        return if (planned == 0) 0f else (sessions(from, minOf(today, to)).toFloat() / planned).coerceIn(0f, 1f)
    }
}

data class GoalsUiState(val today: LocalDate, val notes: List<GoalNote>)

class GoalsViewModel(private val container: AppContainer) : ViewModel() {
    private val date = combine(container.clock.minuteTicks(), container.db.settings().observe().filterNotNull()) { local, settings ->
        ProgramCalendar.programDate(local, settings.rolloverHour)
    }
    private val rows = combine(container.db.goals().observeAll(), container.db.goals().observeMilestones(), container.db.tasks().observeAll(), container.db.completions().observeAll()) { goals, months, tasks, completions ->
        val done = completions.groupBy({ it.taskId }, { it.date })
        goals.map { g -> GoalNote(g, months.filter { it.goalId == g.id }, tasks.filter { it.goalId == g.id }.map { it.toDomain() }, done, tasks.filter { it.goalId == g.id && it.archivedAt == null }.map { it.id }.toSet()) }
    }
    val state = combine(date, rows, ::GoalsUiState).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val problem = kotlinx.coroutines.flow.MutableStateFlow(false)
    val error = problem.asStateFlow()

    fun dismissError() { problem.value = false }
    fun whyOffered(id: Long) = change { container.db.goals().setWhyOffered(id, container.clock.now()) }
    fun status(id: Long, status: GoalStatus) = change {
        val today = state.value?.today ?: return@change
        container.goals.setStatus(id, status, today, container.clock.now())
        refresh()
    }
    fun milestone(id: Long, reached: Boolean) = change { container.db.goals().setReached(id, if (reached) container.clock.now() else null) }
    fun statement(id: Long, text: String) = change { container.db.goals().setStatement(id, text.trim()) }
    fun saveAction(form: TaskForm, task: Task) = change {
        val today = state.value?.today ?: return@change
        container.tasks.saveForm(form, task, today, container.clock.now())
        refresh()
    }
    fun tick(taskId: Long) = change {
        val today = state.value?.today ?: return@change
        container.tasks.toggle(taskId, today, container.clock.now())
        container.cancelEmptyBlocks(today)
        container.scheduler.reconcile("goal-tick")
    }
    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { problem.value = true }
        }
    }
    private suspend fun refresh() {
        container.scheduler.reconcile("goal-change")
        TodayWidget.refresh(container.app)
    }
}

class GoalCreateViewModel(private val container: AppContainer, private val saved: SavedStateHandle) : ViewModel() {
    val draft = saved.getStateFlow<GoalDraft?>("draft", null)
    val step = saved.getStateFlow("step", 0)
    val savedId = saved.getStateFlow<Long?>("pinned", null)
    private val working = kotlinx.coroutines.flow.MutableStateFlow(false)
    val saving: kotlinx.coroutines.flow.StateFlow<Boolean> = working
    private val problem = kotlinx.coroutines.flow.MutableStateFlow(false)
    val error: kotlinx.coroutines.flow.StateFlow<Boolean> = problem

    init {
        viewModelScope.launch {
            container.ensureReady()
            if (draft.value == null) {
                val settings = checkNotNull(container.db.settings().get())
                saved["draft"] = GoalDraft(start = container.clock.programDate(settings.rolloverHour), calendar = if (PersianDigits.latin) GoalCalendar.GREGORIAN else GoalCalendar.JALALI)
            }
        }
    }
    fun update(value: GoalDraft) { if (!working.value) { saved["draft"] = value; problem.value = false } }
    fun next() { if (draft.value?.let { GoalPlans.validStep(it, step.value) } == true) saved["step"] = (step.value + 1).coerceAtMost(5) }
    fun previous() { if (!working.value) saved["step"] = (step.value - 1).coerceAtLeast(0) }
    fun pin() {
        val value = draft.value ?: return
        if (working.value || savedId.value != null || !GoalPlans.valid(value)) return
        working.value = true
        problem.value = false
        viewModelScope.launch {
            try {
                val settings = checkNotNull(container.db.settings().get())
                val today = container.clock.programDate(settings.rolloverHour)
                val id = container.goals.pin(value, today, container.clock.now())
                // Once committed, never offer a second pin because a reminder/widget refresh failed.
                saved.set<Long?>("pinned", id)
                container.scope.launch {
                    runCatching { container.scheduler.reconcile("goal-pin"); TodayWidget.refresh(container.app) }
                }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { problem.value = true }
            finally { working.value = false }
        }
    }
}
