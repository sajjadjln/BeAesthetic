package app.beaesthetic.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskForm
import app.beaesthetic.domain.tasks.TaskForms
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The tasks list (screens.html 07): active tasks and every completion date (one-offs done before today hide). */
data class TasksUiState(
    val today: LocalDate,
    val tasks: List<Task>,
    val completions: Map<Long, List<LocalDate>>,
)

class TasksViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<TasksUiState?> = combine(
        container.tasks.observeTasks(),
        container.db.settings().observe().filterNotNull(),
    ) { (tasks, completions), settings ->
        TasksUiState(container.clock.programDate(settings.rolloverHour), tasks, completions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves the editor's form (a new task when [original] is null) and re-plans reminders (D107, D108). */
    fun save(form: TaskForm, original: Task?) {
        val s = state.value ?: return
        val task = TaskForms.toTask(form, original, s.today, TaskForms.sortOrderFor(form, original, s.tasks))
        viewModelScope.launch {
            container.tasks.save(task, s.today, container.clock.now())
            container.scheduler.reconcile("task-edit")
        }
    }

    /** «حذف» archives (D109). */
    fun delete(task: Task) {
        val s = state.value ?: return
        viewModelScope.launch {
            container.tasks.archive(task.id, s.today, container.clock.now())
            container.scheduler.reconcile("task-delete")
        }
    }
}
