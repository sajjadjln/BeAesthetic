package app.lantern.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lantern.AppContainer
import app.lantern.data.db.CompletionEntity
import app.lantern.data.repo.toDomain
import app.lantern.domain.score.Dashboard
import app.lantern.domain.score.DayCounts
import app.lantern.domain.score.DaySnapshot
import app.lantern.domain.score.Days
import app.lantern.domain.score.History
import app.lantern.domain.tasks.Task
import app.lantern.domain.time.ProgramCalendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Everything History draws (D198), all from stored rows: the day snapshots for the grid, the completions for
 * the day sheet and the task grids. [tasks] keeps archived ones so old days keep their names.
 */
data class HistoryState(
    val today: LocalDate,
    val installDate: LocalDate,
    val columns: List<LocalDate>,
    val snapshots: Map<LocalDate, DaySnapshot>,
    /** The stored special kind per day (SICK, TRAVEL, OFF). */
    val specials: Map<LocalDate, String>,
    val tasks: Map<Long, Task>,
    /** Active repeating tasks, in Today's order. */
    val repeating: List<Task>,
    val doneByTask: Map<Long, Set<LocalDate>>,
    val doneByDate: Map<LocalDate, List<CompletionEntity>>,
    val shownUp: Int,
    val specialCap: Int,
)

class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    private val db = container.db

    val state: StateFlow<HistoryState?> = combine(
        db.settings().observe().filterNotNull(),
        db.tasks().observeAll(),
        db.completions().observeAll(),
        db.days().observeRange(LocalDate.of(2000, 1, 1), LocalDate.of(9999, 12, 31)),
        container.clock.minuteTicks(),
    ) { settings, taskRows, completions, days, now ->
        val today = ProgramCalendar.programDate(now, settings.rolloverHour)
        val snapshots = days.associate { it.date to DaySnapshot(DayCounts(it.dueCount, it.doneCount, it.floorDone), special = it.special != null) }
        val tasks = taskRows.associate { it.id to it.toDomain() }
        HistoryState(
            today = today,
            installDate = settings.installDate,
            columns = History.columns(settings.installDate, today),
            snapshots = snapshots,
            specials = days.mapNotNull { d -> d.special?.let { d.date to it } }.toMap(),
            tasks = tasks,
            repeating = taskRows.filter { it.archivedAt == null }.map { tasks.getValue(it.id) }.filter(History::isRepeating)
                .sortedWith(compareBy({ it.part }, { it.sortOrder }, { it.id })),
            doneByTask = completions.groupBy({ it.taskId }, { it.date }).mapValues { it.value.toSet() },
            doneByDate = completions.groupBy { it.date }.mapValues { (_, rows) -> rows.sortedBy { it.completedAt } },
            shownUp = Days.shownUp(snapshots, today, settings.installDate),
            specialCap = settings.specialCapPerWeek,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Today and yesterday only (D17), within the weekly cap; picking the same kind again clears it (D125). */
    fun canMarkSpecial(s: HistoryState, date: LocalDate): Boolean = Dashboard.canMarkSpecial(date, s.snapshots, s.specialCap)

    fun setSpecial(date: LocalDate, kind: String, current: String?) {
        viewModelScope.launch { container.tasks.setSpecial(date, if (kind == current) null else kind) }
    }
}
