package app.beaesthetic.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.data.db.SnackNoteEntity
import app.beaesthetic.data.db.WeekPlanEntity
import app.beaesthetic.domain.backup.BackupReason
import app.beaesthetic.domain.buddy.BuddyPicker
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.score.Dashboard
import app.beaesthetic.domain.score.DayScore
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.score.SpecialKind
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.week.Ritual
import app.beaesthetic.domain.week.WeekReview
import app.beaesthetic.domain.week.WeekWins
import app.beaesthetic.domain.week.Win
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The open ritual's review (DESIGN 05): which week, its wins, and the line «پیکر» cheers. */
data class ReviewUi(val weekStart: LocalDate, val number: Int, val wins: WeekWins, val lineIndex: Int, val outfit: Outfit)

/** A day chip in «روزهای ویژه». Past days of the plan week are shown but can't change (D17). */
data class SpecialChip(val date: LocalDate, val kind: SpecialKind?, val editable: Boolean, val isToday: Boolean)

data class SnackRow(val date: LocalDate, val text: String)

data class WeekUiState(
    val today: LocalDate,
    val review: ReviewUi?,
    val identity: String?,
    val planWeek: LocalDate,
    val planWeekNumber: Int,
    /** The plan week starts after the finish line: the last ritual closes the program, it starts nothing. */
    val planAfterFinish: Boolean,
    val specials: List<SpecialChip>,
    val reserves: Int,
    val cap: Int,
    val snacks: List<SnackRow>,
    val ritualMinute: Int,
    /** Set right after «شروع هفته N», until the screen is left. */
    val justStarted: Int?,
    val hasBackupFolder: Boolean,
)

private data class Base(val s: SettingsEntity, val today: LocalDate, val snapshots: Map<LocalDate, DaySnapshot>, val done: Set<LocalDate>)

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(private val container: AppContainer, private val linePool: (Win) -> Int) : ViewModel() {

    private val justStarted = MutableStateFlow<Int?>(null)

    private val today = combine(container.clock.minuteTicks(), container.db.settings().observe().filterNotNull()) { local, s ->
        ProgramCalendar.programDate(local, s.rolloverHour)
    }.distinctUntilChanged()

    private val base = combine(today, container.db.settings().observe().filterNotNull()) { t, s -> t to s }
        .flatMapLatest { (t, s) ->
            combine(
                container.tasks.observeSnapshots(s.programStartDate.minusDays(7), s.programEndDate.plusDays(7)),
                container.tasks.observeDay(t),
                container.db.weeks().observeDone(),
            ) { stored, day, done ->
                val live = DaySnapshot(DayScore.counts(day.items), stored[t]?.special ?: false)
                Base(s, t, stored + (t to live), done.toSet())
            }
        }

    /** The review only reads the logs when a ritual is open. */
    private val review = base.flatMapLatest { b ->
        val calendar = b.s.calendar()
        val week = Ritual.open(b.today, calendar, b.done) ?: return@flatMapLatest flowOf(null)
        container.tasks.observeTasks().combine(container.workouts.observeAllSets()) { t, sets -> t to sets }.mapLatest { (t, sets) ->
            val (tasks, completions) = t
            val days = (0L..6L).map { week.plusDays(it) }.filter { it <= b.today }
            val records = days.sumOf { container.workouts.recordsOn(it).size }
            val workouts = sets.filter { it.date in days }.map { it.sessionId }.distinct().size
            val from = maxOf(b.s.installDate, calendar.day0)
            val routines = WeekReview.routines(tasks, completions, from, week, b.today)
            val wins = WeekReview.wins(week, b.today, b.s.installDate, calendar, b.snapshots, routines, records, workouts)
            // One line per week, picked by the week, so it doesn't change while the screen is open.
            val pool = linePool(wins.headline).coerceAtLeast(1)
            ReviewUi(week, calendar.weekNumber(week), wins, (week.toEpochDay() % pool).toInt(), BuddyPicker.outfit(Chapters.at(calendar, b.today)?.number ?: 1))
        }
    }

    private val plan = base.flatMapLatest { b ->
        val week = Ritual.planWeek(b.today)
        combine(
            container.db.days().observeRange(week, week.plusDays(6)),
            container.db.weeks().observeSnacks(week, week.plusDays(6)),
        ) { days, snacks ->
            val kinds = days.associate { it.date to it.special?.let { k -> runCatching { SpecialKind.valueOf(k) }.getOrNull() } }
            val notes = snacks.associate { it.date to it.text }
            Triple(
                week,
                (0L..6L).map { week.plusDays(it) }.map { SpecialChip(it, kinds[it], it >= b.today, it == b.today) },
                (0L..6L).map { week.plusDays(it) }.map { SnackRow(it, notes[it].orEmpty()) },
            )
        }
    }

    val state: StateFlow<WeekUiState?> = combine(base, review, plan, justStarted) { b, r, (week, chips, snacks), started ->
        val calendar = b.s.calendar()
        WeekUiState(
            today = b.today,
            review = r,
            identity = b.s.identitySentence?.takeIf { it.isNotBlank() },
            planWeek = week,
            planWeekNumber = calendar.weekNumber(week),
            planAfterFinish = week > calendar.endDate,
            specials = chips,
            reserves = WeekReview.reserves(week, b.snapshots, b.s.specialCapPerWeek),
            cap = b.s.specialCapPerWeek,
            snacks = snacks,
            ritualMinute = b.s.ritualTime,
            justStarted = started,
            hasBackupFolder = b.s.backupTreeUri != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            container.ensureReady()
            container.events.screen("week")
        }
    }

    /** A chip tap: a kind marks the day (within the week's cap), null clears it. */
    fun setSpecial(date: LocalDate, kind: SpecialKind?) {
        val s = state.value ?: return
        val chip = s.specials.firstOrNull { it.date == date } ?: return
        if (!chip.editable) return
        viewModelScope.launch {
            val settings = container.db.settings().get() ?: return@launch
            if (kind != null && chip.kind == null) {
                val snapshots = container.tasks.observeSnapshots(s.planWeek, s.planWeek.plusDays(6)).first()
                if (!Dashboard.canMarkSpecial(date, snapshots, settings.specialCapPerWeek)) return@launch
            }
            container.tasks.setSpecial(date, kind?.name)
        }
    }

    /** A snack note; an empty one is removed. */
    fun setSnack(date: LocalDate, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) container.db.weeks().deleteSnack(date) else container.db.weeks().upsertSnack(SnackNoteEntity(date, text))
        }
    }

    /** The identity sentence (D65), written once in the first ritual and editable here. */
    fun setIdentity(text: String) {
        viewModelScope.launch {
            val s = container.db.settings().get() ?: return@launch
            container.db.settings().upsert(s.copy(identitySentence = text.trim().ifEmpty { null }))
        }
    }

    /**
     * «شروع هفته N»: the ritual is done. The weekly backup runs in the app's scope (leaving the screen
     * doesn't cut it off; no folder means none), and the Friday reminder is taken off.
     */
    fun startWeek() {
        val s = state.value ?: return
        val r = s.review ?: return
        viewModelScope.launch {
            container.db.weeks().upsert(WeekPlanEntity(r.weekStart, container.clock.now()))
            justStarted.value = s.planWeekNumber
            container.scope.launch { container.backup.backUp(BackupReason.AUTO) }
            container.scheduler.reconcile("ritual")
        }
    }

    fun clearJustStarted() {
        justStarted.value = null
    }
}

private fun SettingsEntity.calendar() = ProgramCalendar(programStartDate, programEndDate, rolloverHour)
