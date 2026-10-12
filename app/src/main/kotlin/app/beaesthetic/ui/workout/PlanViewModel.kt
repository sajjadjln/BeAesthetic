package app.beaesthetic.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.domain.workout.Exercise
import app.beaesthetic.domain.workout.PlanItem
import app.beaesthetic.domain.workout.PlanItemForm
import app.beaesthetic.domain.workout.WorkoutPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The plan editor (screens.html 10): the week's plans, every move and the ladder links (D119). */
data class PlanUiState(
    val plans: List<WorkoutPlan>,
    val exercises: List<Exercise>,
    val links: Map<Long, Long>,
    /** The day the editor opens on: today (Saturday = 0). */
    val todayWeekday: Int,
)

class PlanViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<PlanUiState?> = combine(
        container.workouts.observePlans(),
        container.workouts.observeExercises(),
        container.workouts.observeHarderLinks(),
        container.db.settings().observe().filterNotNull(),
    ) { plans, exercises, links, settings ->
        PlanUiState(plans, exercises, links, Weekdays.index(container.clock.programDate(settings.rolloverHour).dayOfWeek))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setName(weekday: Int, name: String) {
        viewModelScope.launch { container.workouts.setName(weekday, name) }
    }

    fun saveItem(weekday: Int, form: PlanItemForm, original: PlanItem?) {
        viewModelScope.launch { container.workouts.saveItem(weekday, form, original) }
    }

    fun removeItem(weekday: Int, item: PlanItem) {
        viewModelScope.launch { container.workouts.removeItem(weekday, item.exercise.id) }
    }

    fun move(weekday: Int, from: Int, to: Int) {
        viewModelScope.launch { container.workouts.move(weekday, from, to) }
    }
}
