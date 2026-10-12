package app.beaesthetic.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.domain.score.Dashboard
import app.beaesthetic.domain.score.DayCounts
import app.beaesthetic.domain.score.DayScore
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.score.DayStatus
import app.beaesthetic.domain.score.Days
import app.beaesthetic.domain.score.SpecialKind
import app.beaesthetic.domain.score.Summary
import app.beaesthetic.domain.score.WeekRow
import app.beaesthetic.domain.tasks.DayItem
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.time.ProgramCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DashboardUiState(
    val today: LocalDate,
    val calendar: ProgramCalendar,
    val summary: Summary,
    /** «روز ۳۱ از ۱۶۲». */
    val dayNumber: Int,
    val totalDays: Int,
    /** The grid: one column per week (oldest first), 7 squares each, Saturday first; null = outside the program. */
    val columns: List<Pair<LocalDate, List<DayStatus?>>>,
    val weeks: List<WeekRow>,
    /** The squares fill in week by week, once a day (D76). */
    val animate: Boolean,
)

/** The day sheet (flows.html D1). Only what was done is listed, never what wasn't. */
data class DayDetail(
    val date: LocalDate,
    val today: LocalDate,
    val week: Int,
    val status: DayStatus,
    val counts: DayCounts?,
    val special: SpecialKind?,
    val done: List<Task>,
    val records: List<String>,
    /** Today and yesterday (SPEC §6.1): special day and, for yesterday, its ticks. */
    val editable: Boolean,
    val canMarkSpecial: Boolean,
    /** Yesterday's due items, for «ویرایش تیک‌های دیروز» (D124). Empty for other days. */
    val yesterdayItems: List<DayItem>,
)

private data class Base(val settings: SettingsEntity, val today: LocalDate, val snapshots: Map<LocalDate, DaySnapshot>)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val animate = MutableStateFlow<Boolean?>(null)
    private val selected = MutableStateFlow<LocalDate?>(null)

    private val settings = container.db.settings().observe().filterNotNull()

    /** Stored days since Day 0, with today's counts live (it may not be stored yet). */
    private val base = settings.flatMapLatest { s ->
        val today = container.clock.programDate(s.rolloverHour)
        val calendar = s.calendar()
        combine(
            container.tasks.observeSnapshots(calendar.day0, calendar.endDate),
            container.tasks.observeDay(today),
        ) { stored, day ->
            val live = DaySnapshot(DayScore.counts(day.items), stored[today]?.special ?: false)
            Base(s, today, stored + (today to live))
        }
    }

    val state: StateFlow<DashboardUiState?> = combine(base, animate.filterNotNull()) { b, anim ->
        val calendar = b.settings.calendar()
        val install = b.settings.installDate
        val (dayNumber, total) = Dashboard.dayOf(calendar, b.today)
        DashboardUiState(
            today = b.today,
            calendar = calendar,
            summary = Dashboard.summary(b.today, install, calendar, b.snapshots),
            dayNumber = dayNumber,
            totalDays = total,
            columns = Dashboard.columns(calendar).map { start ->
                start to (0L..6L).map { Dashboard.cell(start.plusDays(it), calendar, b.today, install, b.snapshots) }
            },
            weeks = Dashboard.weeks(calendar, b.today, install, b.snapshots),
            animate = anim,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val detail: StateFlow<DayDetail?> = selected.flatMapLatest { date ->
        if (date == null) flowOf(null) else detailFlow(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            container.ensureReady()
            val s = container.db.settings().get()
            val startOfDay = (s?.let { container.clock.programDate(it.rolloverHour) } ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant()
            animate.value = !container.events.screenSeenSince(SCREEN, startOfDay)
            container.events.screen(SCREEN)
        }
    }

    private fun detailFlow(date: LocalDate) = base.flatMapLatest { b ->
        val yesterday = b.today.minusDays(1)
        val editable = date == b.today || date == yesterday
        combine(
            container.tasks.observeDoneOn(date),
            container.tasks.observeSpecial(date),
            if (date == yesterday) container.tasks.observeDay(date).map { it.items.filterNot(DayItem::minDayOnly) } else flowOf(emptyList()),
            flowOf(Unit).map { container.workouts.recordsOn(date) },
        ) { done, special, items, records ->
            val calendar = b.settings.calendar()
            DayDetail(
                date = date,
                today = b.today,
                week = calendar.weekNumber(date),
                status = Days.status(date, b.today, b.settings.installDate, b.snapshots[date]),
                counts = b.snapshots[date]?.counts,
                special = special?.let { runCatching { SpecialKind.valueOf(it) }.getOrNull() },
                done = done,
                records = records,
                editable = editable,
                canMarkSpecial = Dashboard.canMarkSpecial(date, b.snapshots, b.settings.specialCapPerWeek),
                yesterdayItems = items,
            )
        }
    }.distinctUntilChanged()

    /** A tapped square. Future days have nothing to show. */
    fun select(date: LocalDate) {
        val today = state.value?.today ?: return
        if (date <= today) selected.value = date
    }

    fun closeDetail() {
        selected.value = null
    }

    /** «روز ویژه بود؟» (today and yesterday): picking the same kind again clears it. */
    fun setSpecial(kind: SpecialKind?) {
        val d = detail.value ?: return
        if (!d.editable) return
        if (kind != null && d.special == null && !d.canMarkSpecial) return
        val value = if (kind == d.special) null else kind
        viewModelScope.launch { container.tasks.setSpecial(d.date, value?.name) }
    }

    /** A yesterday chip (D124): ticks or unticks the task for yesterday. */
    fun toggleYesterday(taskId: Long) {
        val d = detail.value ?: return
        if (d.date != d.today.minusDays(1)) return
        viewModelScope.launch { container.tasks.toggle(taskId, d.date, container.clock.now()) }
    }

    private companion object {
        const val SCREEN = "dashboard"
    }
}

private fun SettingsEntity.calendar() = ProgramCalendar(programStartDate, programEndDate, rolloverHour)
