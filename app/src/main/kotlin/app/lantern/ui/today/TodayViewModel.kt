package app.lantern.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lantern.AppContainer
import app.lantern.backup.BackupStatus
import app.lantern.domain.backup.BackupNotice
import app.lantern.data.repo.DayView
import app.lantern.domain.reminders.Attention
import app.lantern.domain.reminders.PartOfDay
import app.lantern.domain.reminders.ReminderHealth
import app.lantern.domain.reminders.ReminderSlot
import app.lantern.domain.tasks.DayItem
import app.lantern.domain.tasks.Task
import app.lantern.domain.tasks.TaskForm
import app.lantern.domain.tasks.TaskIcon
import app.lantern.domain.time.Jalali
import app.lantern.domain.time.JalaliDate
import app.lantern.domain.time.Part
import app.lantern.domain.time.ProgramCalendar
import app.lantern.reminders.AlarmScheduler.Companion.toLogRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class TileUi(
    val taskId: Long,
    val title: String,
    val icon: TaskIcon,
    val done: Boolean,
    val important: Boolean,
    /** An "every N days" or one-off task that is due: «موعدش رسیده». */
    val due: Boolean,
    val part: Part = Part.MORNING,
)

/** One «هر وقت» item (D127). A done one stays, ticked, until the day ends. */
data class AnytimeUi(val taskId: Long, val title: String, val done: Boolean, val important: Boolean)

enum class HealthBanner { LATE, BLOCKED }

data class TodayUiState(
    val date: LocalDate,
    val jalali: JalaliDate,
    val selectedPart: Part,
    val minimumMode: Boolean,
    val tiles: Map<Part, List<TileUi>>,
    val floorTiles: List<TileUi>,
    val completeParts: Set<Part>,
    /** The ring: today's visible tasks, or the floor in minimum-day mode. */
    val done: Int,
    val total: Int,
    val banner: HealthBanner?,
    val anytime: List<AnytimeUi> = emptyList(),
    val anytimeOpen: Boolean = false,
    /** The backup row under the tiles (SPEC §8); shown only when no reminder banner is. */
    val backup: BackupStatus? = null,
) {
    val dayComplete: Boolean get() = total > 0 && done == total
}

/** Where "today" is: the date (after the 04:00 rollover) and the current part of the day. Changes at most once a minute. */
private data class Now(val date: LocalDate, val part: Part)

/** Settings Today reads, without the fields that change on every app open (so nothing re-subscribes). */
private data class TodaySettings(val healthSeenAt: Instant?, val anytimeOpen: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val container: AppContainer) : ViewModel() {

    /** The part the user picked; cleared when the real part of the day moves on. */
    private val pickedPart = MutableStateFlow<Part?>(null)

    /** Notification and exact-alarm permissions, re-read when the screen resumes. */
    private val systemOk = MutableStateFlow(true)

    private val settings = container.db.settings().observe().filterNotNull()

    private val now: Flow<Now> = combine(
        container.clock.minuteTicks(),
        settings,
        container.db.slots().observeAll(),
    ) { local, s, slots ->
        val domainSlots = slots.map { ReminderSlot(it.profile, it.part, it.time, it.enabled) }
        Now(
            date = ProgramCalendar.programDate(local, s.rolloverHour),
            part = PartOfDay.current(local, domainSlots, s.rolloverHour),
        )
    }
        .distinctUntilChanged()
        .onEach { pickedPart.value = null }

    private val todaySettings = settings.map { TodaySettings(it.healthSeenAt, it.anytimeOpen) }.distinctUntilChanged()

    private val day: Flow<Pair<Now, DayView>> = now.flatMapLatest { n -> container.tasks.observeDay(n.date).map { n to it } }

    private val health = container.db.reminderLog()
        .observeSince(Instant.now() - ReminderHealth.WINDOW)
        .map { rows -> ReminderHealth.check(rows.map { it.toLogRow() }, container.clock.now()) }

    val state: StateFlow<TodayUiState?> = combine(day, todaySettings, pickedPart, combine(health, container.backup.observeStatus()) { h, b -> h to b }, systemOk) { (n, d), s, picked, (h, backup), sysOk ->
        val banner = when (ReminderHealth.attention(h, s.healthSeenAt, sysOk)) {
            Attention.BLOCKED -> HealthBanner.BLOCKED
            Attention.LATE -> HealthBanner.LATE
            Attention.NONE -> null
        }
        toState(n, d, s, picked ?: n.part, banner).copy(backup = backup.takeIf { banner == null && it.notice != BackupNotice.NONE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { container.ensureReady() }
    }

    fun selectPart(part: Part) {
        pickedPart.value = part
    }

    fun toggle(taskId: Long) {
        val current = state.value ?: return
        viewModelScope.launch {
            val settings = container.db.settings().get() ?: return@launch
            val nowDone = container.tasks.toggle(taskId, current.date, container.clock.now())
            if (nowDone) container.events.firstTickIfNeeded(current.date, settings.rolloverHour)
            container.cancelEmptyBlocks(current.date)
        }
    }

    /** Opens or closes the «هر وقت» card; remembered (D127). */
    fun setAnytimeOpen(open: Boolean) {
        viewModelScope.launch { container.db.settings().setAnytimeOpen(open) }
    }

    /** Every active task, for the editor's suggestions when a task is added from Today. */
    val editorTasks: StateFlow<List<Task>> = container.tasks.observeTasks()
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds a task from Today (D128) and re-plans reminders. */
    fun saveTask(form: TaskForm) {
        val current = state.value ?: return
        viewModelScope.launch {
            container.tasks.saveForm(form, null, current.date, container.clock.now())
            container.scheduler.reconcile("task-edit")
        }
    }

    fun toggleMinimumMode() {
        val current = state.value ?: return
        viewModelScope.launch {
            container.tasks.setMinimumMode(current.date, !current.minimumMode)
            container.cancelEmptyBlocks(current.date)
        }
    }

    fun refreshSystemState() {
        systemOk.value = container.notifier.canNotify() && container.scheduler.canScheduleExact()
    }

    fun screenShown() {
        viewModelScope.launch { container.events.screen("today") }
    }

    private fun toState(n: Now, day: DayView, s: TodaySettings, selected: Part, banner: HealthBanner?): TodayUiState {
        val visible = day.items.filterNot { it.minDayOnly }
        val floor = day.items.filter { it.inFloor }
        val ring = if (day.minimumMode) floor else visible
        return TodayUiState(
            date = n.date,
            jalali = Jalali.fromGregorian(n.date),
            selectedPart = selected,
            minimumMode = day.minimumMode,
            tiles = visible.groupBy { it.task.part }.mapValues { (_, items) -> items.map { tile(it) } },
            floorTiles = floor.map { tile(it) },
            completeParts = visible.groupBy { it.task.part }.filterValues { list -> list.all(DayItem::done) }.keys,
            done = ring.count { it.done },
            total = ring.size,
            banner = banner,
            anytime = day.anytime.map { AnytimeUi(it.task.id, it.task.title, it.done, it.task.isImportant) },
            anytimeOpen = s.anytimeOpen,
        )
    }

    private fun tile(item: DayItem) = TileUi(
        item.task.id, item.task.title, item.task.icon, item.done, item.task.isImportant, due = item.dueSince != null, part = item.task.part,
    )
}
