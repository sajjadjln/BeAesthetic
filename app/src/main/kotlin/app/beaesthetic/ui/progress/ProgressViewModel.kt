package app.beaesthetic.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.data.repo.Pose
import app.beaesthetic.domain.progress.CheckIn
import app.beaesthetic.domain.progress.CheckIns
import app.beaesthetic.domain.progress.Metric
import app.beaesthetic.domain.progress.MetricKind
import app.beaesthetic.domain.progress.MetricUnit
import app.beaesthetic.domain.progress.Progress
import app.beaesthetic.domain.progress.Reading
import app.beaesthetic.domain.progress.SinceDay0
import app.beaesthetic.domain.progress.SuggestedMetric
import app.beaesthetic.domain.progress.Suggestions
import app.beaesthetic.domain.time.ProgramCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A metric with its readings, for a stat card and its sheet. */
data class MetricCard(val metric: Metric, val readings: List<Reading>, val since: SinceDay0?, val best: Reading?)

/** One check-in's photos: pose → file name. */
data class PhotoRow(val checkIn: CheckIn, val photos: Map<Pose, String>)

data class ProgressUiState(
    val today: LocalDate,
    val day0: LocalDate,
    val hasMetrics: Boolean,
    /** Day 0's check-in is done (or nothing is tracked yet, so there's nothing to ask). */
    val day0Done: Boolean,
    /** The next check-in to do, and whether it's open today. */
    val next: CheckIn?,
    val nextOpen: Boolean,
    val isFinish: Boolean,
    val weight: MetricCard?,
    /** Tape sites, then strength tests. */
    val others: List<MetricCard>,
    val photoRows: List<PhotoRow>,
    val checkInCount: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val container: AppContainer) : ViewModel() {

    private val settings = container.db.settings().observe().filterNotNull()

    val state: StateFlow<ProgressUiState?> = combine(settings, container.progress.observe(), container.clock.minuteTicks()) { s, data, _ ->
        val today = container.clock.programDate(s.rolloverHour)
        val schedule = CheckIns.schedule(s.calendar())
        val done = data.checkIns.keys
        val open = CheckIns.open(today, schedule, done)
        val next = CheckIns.next(today, schedule, done)
        val cards = data.metrics.map { m ->
            val readings = data.readings[m.id].orEmpty()
            MetricCard(m, readings, Progress.sinceDay0(m, readings), Progress.best(m, readings))
        }
        ProgressUiState(
            today = today,
            day0 = s.programStartDate,
            hasMetrics = data.metrics.isNotEmpty(),
            day0Done = 0 in done,
            next = next,
            nextOpen = open != null,
            isFinish = next?.idx == schedule.last().idx,
            weight = cards.firstOrNull { it.metric.kind == MetricKind.BODY },
            others = cards.filter { it.metric.kind != MetricKind.BODY }.sortedBy { it.metric.kind.ordinal },
            photoRows = schedule.mapNotNull { c -> data.photos[c.idx]?.takeIf { it.isNotEmpty() }?.let { PhotoRow(c, it.mapValues { (_, p) -> p.fileName }) } },
            checkInCount = schedule.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            container.ensureReady()
            container.events.screen(SCREEN)
        }
    }

    fun record(metricId: Long, date: LocalDate, value: Double) {
        viewModelScope.launch { container.progress.record(metricId, date, value) }
    }

    fun deleteReading(metricId: Long, date: LocalDate) {
        viewModelScope.launch { container.progress.deleteReading(metricId, date) }
    }

    // The metrics screen (D100: suggested ones are one tap; nothing is seeded).

    fun addSuggested(suggested: SuggestedMetric, name: String) {
        viewModelScope.launch { container.progress.addSuggested(suggested, name) }
    }

    fun saveMetric(id: Long?, kind: MetricKind, name: String, unit: MetricUnit, higherIsBetter: Boolean) {
        viewModelScope.launch { container.progress.save(id, kind, name.trim(), unit, higherIsBetter) }
    }

    fun removeMetric(id: Long) {
        viewModelScope.launch { container.progress.remove(id, container.clock.now()) }
    }

    fun remainingSuggestions(): List<SuggestedMetric> =
        Suggestions.remaining(state.value?.let { s -> (listOfNotNull(s.weight) + s.others).map { it.metric.key } }.orEmpty())

    private companion object {
        const val SCREEN = "progress"
    }
}

fun SettingsEntity.calendar() = ProgramCalendar(programStartDate, programEndDate, rolloverHour)
