package app.beaesthetic.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.backup.BackupStatus
import app.beaesthetic.data.db.EventEntity
import app.beaesthetic.domain.backup.BackupNotice
import app.beaesthetic.data.db.EventType
import app.beaesthetic.data.db.LetterEntity
import app.beaesthetic.data.db.RoundNumberEntity
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.data.repo.DayView
import app.beaesthetic.data.repo.ProgressData
import app.beaesthetic.domain.progress.CheckIns
import app.beaesthetic.domain.progress.MetricKind
import app.beaesthetic.domain.progress.Progress
import app.beaesthetic.ui.progress.MetricCard
import app.beaesthetic.domain.buddy.BuddyPicker
import app.beaesthetic.domain.buddy.Moment
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.domain.buddy.TodayState
import app.beaesthetic.domain.reminders.Attention
import app.beaesthetic.domain.reminders.PartOfDay
import app.beaesthetic.domain.reminders.ReminderHealth
import app.beaesthetic.domain.reminders.ReminderSlot
import app.beaesthetic.domain.score.DayScore
import app.beaesthetic.domain.score.DayStatus
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.score.Days
import app.beaesthetic.domain.tasks.DayItem
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskForm
import app.beaesthetic.domain.tasks.TaskAction
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Chapter
import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.time.Countdown
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.JalaliDate
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.domain.workout.RoundNumbers
import app.beaesthetic.domain.workout.WorkoutPlan
import app.beaesthetic.reminders.AlarmScheduler.Companion.toLogRow
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
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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

/** «تمرین امروز» above the part switcher, or the rest-day row (D126). */
sealed interface WorkoutCardUi {
    /** Today's plan. [taskId] is the linked «کار تمرین» due today, if any: it ticks with the first set. */
    data class Training(val name: String, val moves: Int, val sets: Int, val taskId: Long?, val done: Boolean) : WorkoutCardUi

    /** No plan today: the next one, [weekday] (Saturday = 0) [daysAway] days from today. */
    data class Rest(val name: String, val weekday: Int, val daysAway: Int) : WorkoutCardUi
}

/** One «هر وقت» item (D127). A done one stays, ticked, until the day ends. */
data class AnytimeUi(val taskId: Long, val title: String, val done: Boolean, val important: Boolean)

/** The header's last part: the chapter, or in the last chapter the countdown (D74). */
sealed interface HeaderTail {
    data class InChapter(val monthIndex: Int) : HeaderTail
    data class Counting(val countdown: Countdown) : HeaderTail
}

/**
 * Cards in the «پیکر» row's place (flows L1–L3). A chapter letter opens on the next chapter's first day,
 * so the letter and the chapter-start card come on the same day: both show, the letter first.
 */
sealed interface TodayCard {
    data class ChapterStart(val chapter: Chapter, val letterOpensOn: LocalDate, val outfit: Outfit) : TodayCard
    data class FirstLetter(val opensOn: LocalDate) : TodayCard
    data class LetterArrived(val id: Long, val writtenOn: LocalDate, val text: String, val opened: Boolean, val outfit: Outfit) : TodayCard

    /** «۳۰ روز حضور از روز صفر» (D121), until «دمت گرم». */
    data class DaysShownUp(val days: Int, val outfit: Outfit) : TodayCard
}

/** Slice 7a: today's check-in (flows C1), or the Friday weigh-in row when weight is tracked. */
sealed interface ProgressPrompt {
    data class CheckIn(val idx: Int, val monthIndex: Int) : ProgressPrompt
    data class WeighIn(val weight: MetricCard) : ProgressPrompt
}

data class BuddyUi(val pose: Pose, val outfit: Outfit, val moment: Moment, val lineIndex: Int, val count: Int, val total: Int, val part: Part)

enum class HealthBanner { LATE, BLOCKED }

data class TodayUiState(
    val date: LocalDate,
    val jalali: JalaliDate,
    /** 0 on Day 0, then 1…[totalWeeks]. */
    val week: Int,
    val totalWeeks: Int,
    val headerTail: HeaderTail?,
    val selectedPart: Part,
    val minimumMode: Boolean,
    val tiles: Map<Part, List<TileUi>>,
    val floorTiles: List<TileUi>,
    val completeParts: Set<Part>,
    /** The ring: today's visible tasks, or the floor in minimum-day mode. */
    val done: Int,
    val total: Int,
    val weekDots: List<DayStatus>,
    val todayIndex: Int,
    val cards: List<TodayCard>,
    val buddy: BuddyUi?,
    val banner: HealthBanner?,
    /** Days at level 1 or more since Day 0, today included (D121). */
    val daysShownUp: Int = 0,
    val workout: WorkoutCardUi? = null,
    val anytime: List<AnytimeUi> = emptyList(),
    val anytimeOpen: Boolean = false,
    /** The backup row under the tiles (SPEC §8); shown only when no reminder banner is. */
    val backup: BackupStatus? = null,
    val progress: ProgressPrompt? = null,
) {
    val dayComplete: Boolean get() = total > 0 && done == total
}

/** Where "today" is: the program date and the current part of the day. Changes at most once a minute. */
private data class Now(val date: LocalDate, val part: Part, val calendar: ProgramCalendar)

/** Settings Today reads, without the fields that change on every app open (so nothing re-subscribes). */
private data class TodaySettings(
    val installDate: LocalDate,
    val showBuddy: Boolean,
    val firstLetterOfferDone: Boolean,
    val cardDismissedOn: LocalDate?,
    val healthSeenAt: Instant?,
    val anytimeOpen: Boolean,
)

private data class Inputs(
    val now: Now,
    val day: DayView,
    val snapshots: Map<LocalDate, DaySnapshot>,
    val settings: TodaySettings,
    val letters: List<LetterEntity>,
    val plans: List<WorkoutPlan>,
    /** Every stored day since Day 0, for days shown up (D121). */
    val allDays: Map<LocalDate, DaySnapshot>,
    val unseenDays: List<RoundNumberEntity>,
    val progress: ProgressData,
)

private data class Extra(val plans: List<WorkoutPlan>, val allDays: Map<LocalDate, DaySnapshot>, val unseen: List<RoundNumberEntity>, val progress: ProgressData)

private fun SettingsEntity.calendar() = ProgramCalendar(programStartDate, programEndDate, rolloverHour)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val container: AppContainer,
    /** Number of lines in each moment's pool (string resources). */
    private val poolSize: (Moment) -> Int,
) : ViewModel() {

    /** The part the user picked; cleared when the real part of the day moves on. */
    private val pickedPart = MutableStateFlow<Part?>(null)

    /** A tap on «پیکر»: a new line and a flex or a wink, until the day's state changes (D84). */
    private val taps = MutableStateFlow(0)

    /** Notification and exact-alarm permissions, re-read when the screen resumes. */
    private val systemOk = MutableStateFlow(true)

    private val buddyLine = MutableStateFlow<Pair<Moment, Int>?>(null)

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
            calendar = s.calendar(),
        )
    }
        .distinctUntilChanged()
        .onEach { pickedPart.value = null }

    private val todaySettings = settings
        .map { TodaySettings(it.installDate, it.showBuddy, it.firstLetterOfferDone, it.cardDismissedOn, it.healthSeenAt, it.anytimeOpen) }
        .distinctUntilChanged()

    private val inputs: Flow<Inputs> = now.flatMapLatest { n ->
        val weekStart = ProgramCalendar.weekStart(n.date)
        combine(
            container.tasks.observeDay(n.date),
            container.tasks.observeSnapshots(minOf(weekStart, n.date.minusDays(1)), weekStart.plusDays(6)),
            todaySettings,
            container.db.letters().observeAll(),
            combine(
                container.workouts.observePlans(),
                container.tasks.observeSnapshots(n.calendar.day0, n.date),
                container.db.roundNumbers().observeUnseenDays(),
                container.progress.observe(),
            ) { planned, all, unseen, progress -> Extra(planned, all, unseen, progress) },
        ) { day, snapshots, s, letters, x -> Inputs(n, day, snapshots, s, letters, x.plans, x.allDays, x.unseen, x.progress) }
    }

    private val health = container.db.reminderLog()
        .observeSince(Instant.now() - ReminderHealth.WINDOW)
        .map { rows -> ReminderHealth.check(rows.map { it.toLogRow() }, container.clock.now()) }

    val state: StateFlow<TodayUiState?> = combine(inputs, pickedPart, combine(health, container.backup.observeStatus()) { h, b -> h to b }, systemOk, combine(taps, buddyLine) { t, l -> t to l }) { input, picked, (h, backup), sysOk, (tapCount, line) ->
        val banner = when (ReminderHealth.attention(h, input.settings.healthSeenAt, sysOk)) {
            Attention.BLOCKED -> HealthBanner.BLOCKED
            Attention.LATE -> HealthBanner.LATE
            Attention.NONE -> null
        }
        toState(input, picked ?: input.now.part, banner, tapCount, line).copy(backup = backup.takeIf { banner == null && it.notice != BackupNotice.NONE })
    }
        .onEach(::ensureBuddyLine)
        .onEach(::noteDaysShownUp)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    /** Adds a task from the «هر وقت» card (D128) and re-plans reminders. */
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

    fun tapBuddy() {
        taps.value += 1
    }

    fun dismissCard() {
        val current = state.value ?: return
        viewModelScope.launch {
            val s = container.db.settings().get() ?: return@launch
            when (current.cards.lastOrNull { it !is TodayCard.DaysShownUp }) {
                is TodayCard.FirstLetter -> container.db.settings().upsert(s.copy(firstLetterOfferDone = true))
                else -> container.db.settings().upsert(s.copy(cardDismissedOn = current.date))
            }
        }
    }

    /** «دمت گرم» on a days-shown-up card. */
    fun dismissDaysShownUp() {
        viewModelScope.launch { container.db.roundNumbers().markDaysSeen(container.clock.now()) }
    }

    fun openLetter(id: Long) {
        viewModelScope.launch { container.db.letters().markOpened(id, container.clock.now()) }
    }

    fun refreshSystemState() {
        systemOk.value = container.notifier.canNotify() && container.scheduler.canScheduleExact()
    }

    fun screenShown() {
        viewModelScope.launch { container.events.screen("today") }
    }

    /**
     * Picks today's line for the current moment once and logs it (D84): the same moment keeps its line all
     * day, and a line shown in the last 7 days isn't picked while another is left.
     */
    private fun ensureBuddyLine(state: TodayUiState?) {
        val buddy = state?.buddy ?: return
        if (buddy.moment == Moment.TAP || buddyLine.value?.first == buddy.moment) return
        val moment = buddy.moment
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val since = state.date.minusDays(BuddyPicker.NO_REPEAT_DAYS).atStartOfDay(zone).toInstant()
            val history = container.db.events().ofTypeSince(EventType.BUDDY_LINE, since)
                .mapNotNull { e -> parseLine(e)?.let { (m, i) -> Triple(m, i, e.at.atZone(zone).toLocalDate()) } }
                .filter { it.first == moment }
            val todays = history.lastOrNull { it.third == state.date }
            val index = todays?.second ?: BuddyPicker.line(
                poolSize(moment),
                history.groupBy({ it.second }, { it.third }).mapValues { (_, dates) -> dates.max() },
                state.date,
            ).also { i ->
                container.db.events().insert(EventEntity(at = container.clock.now(), type = EventType.BUDDY_LINE, ref = "${moment.name}:$i"))
            }
            buddyLine.value = moment to index
        }
    }

    private var notedDays = -1

    /** Writes a days-shown-up round number when it's first crossed (D121); the card then shows on Today. */
    private fun noteDaysShownUp(state: TodayUiState?) {
        val days = state?.daysShownUp ?: return
        if (days == notedDays) return
        notedDays = days
        val reached = RoundNumbers.reached(RoundNumbers.Kind.DAYS, null, days)
        if (reached.isEmpty()) return
        viewModelScope.launch { container.db.roundNumbers().insertAll(reached.map { RoundNumberEntity(it.key, state.date) }) }
    }

    private fun parseLine(e: EventEntity): Pair<Moment, Int>? {
        val (m, i) = e.ref?.split(":")?.takeIf { it.size == 2 } ?: return null
        return runCatching { Moment.valueOf(m) to i.toInt() }.getOrNull()
    }

    private fun toState(input: Inputs, selected: Part, banner: HealthBanner?, tapCount: Int, line: Pair<Moment, Int>?): TodayUiState {
        val (n, day, snapshots, s, letters) = input
        val date = n.date
        val calendar = n.calendar
        val chapter = Chapters.at(calendar, date)
        val outfit = BuddyPicker.outfit(chapter?.number ?: 1)

        val visible = day.items.filterNot { it.minDayOnly }
        val floor = day.items.filter { it.inFloor }
        val workout = workoutCard(date, input.plans, visible)
        // The card stands in for the linked task's tile (D126); the task still counts in the ring.
        val workoutTaskId = (workout as? WorkoutCardUi.Training)?.taskId
        val tiles = visible.filter { it.task.id != workoutTaskId }.groupBy { it.task.part }.mapValues { (_, items) -> items.map { tile(it) } }
        val partsDone = visible.groupBy { it.task.part }.filterValues { list -> list.all(DayItem::done) }.keys
        val ring = if (day.minimumMode) floor else visible

        // Today's dot is live; the other days come from their stored snapshots (D37).
        val todayCounts = DayScore.counts(day.items)
        val weekStart = ProgramCalendar.weekStart(date)
        val liveSnapshots = snapshots + (date to DaySnapshot(todayCounts, snapshots[date]?.special ?: false))
        val dots = Days.week(weekStart, date, s.installDate, liveSnapshots)
        val todayStatus = Days.status(date, date, s.installDate, liveSnapshots[date])
        val yesterday = date.minusDays(1)
        val yesterdayStatus = Days.status(yesterday, date, s.installDate, snapshots[yesterday])

        val shownUp = Days.shownUp(input.allDays + (date to liveSnapshots.getValue(date)), date, s.installDate)
        val cards = cards(date, calendar, s, letters, outfit) + input.unseenDays
            .mapNotNull { row -> RoundNumbers.parse(row.key)?.let { TodayCard.DaysShownUp(it.value, outfit) } }
            .sortedByDescending { it.days }
            .take(1)
        val buddy = if (!s.showBuddy || cards.isNotEmpty()) {
            null
        } else {
            val choice = BuddyPicker.today(
                TodayState(
                    minimumMode = day.minimumMode,
                    special = liveSnapshots[date]?.special == true,
                    comebackFirstTick = Days.isComeback(todayStatus, yesterdayStatus) && visible.count { it.done } == 1,
                    done = ring.count { it.done },
                    total = ring.size,
                    selectedPartDone = visible.filter { it.task.part == selected }.let { it.isNotEmpty() && it.all(DayItem::done) },
                    currentPart = n.part,
                    nightDone = visible.filter { it.task.part == Part.NIGHT }.let { it.isNotEmpty() && it.all(DayItem::done) },
                ),
            )
            if (tapCount > 0) {
                BuddyUi(if (tapCount % 2 == 1) Pose.FLEX else Pose.THUMBS, outfit, Moment.TAP, (tapCount - 1) % poolSize(Moment.TAP), 0, 0, selected)
            } else {
                BuddyUi(choice.pose, outfit, choice.moment, line?.takeIf { it.first == choice.moment }?.second ?: 0, ring.count { it.done }, ring.size, selected)
            }
        }

        return TodayUiState(
            date = date,
            jalali = Jalali.fromGregorian(date),
            week = calendar.weekNumber(date),
            totalWeeks = calendar.totalWeeks,
            headerTail = Chapters.countdown(calendar, date)?.let { HeaderTail.Counting(it) }
                ?: chapter?.let { HeaderTail.InChapter(Jalali.fromGregorian(date).month) },
            selectedPart = selected,
            minimumMode = day.minimumMode,
            tiles = tiles,
            floorTiles = floor.map { tile(it) },
            completeParts = partsDone,
            done = ring.count { it.done },
            total = ring.size,
            weekDots = dots,
            todayIndex = ChronoUnit.DAYS.between(weekStart, date).toInt(),
            cards = cards,
            buddy = buddy,
            banner = banner,
            daysShownUp = shownUp,
            workout = workout,
            anytime = day.anytime.map { AnytimeUi(it.task.id, it.task.title, it.done, it.task.isImportant) },
            anytimeOpen = s.anytimeOpen,
            progress = progressPrompt(date, calendar, input.progress),
        )
    }

    /** The check-in on its days (not Day 0, D101), else the weekly weigh-in on Friday. */
    private fun progressPrompt(date: LocalDate, calendar: ProgramCalendar, data: ProgressData): ProgressPrompt? {
        if (data.metrics.isEmpty()) return null
        val open = CheckIns.open(date, CheckIns.schedule(calendar), data.checkIns.keys)
        if (open != null) return ProgressPrompt.CheckIn(open.idx, Jalali.fromGregorian(open.date).month)
        val weight = data.metrics.firstOrNull { it.kind == MetricKind.BODY } ?: return null
        val readings = data.readings[weight.id].orEmpty()
        if (!Progress.weighInDue(date, true, readings.map { it.date }, checkInOpen = false)) return null
        return ProgressPrompt.WeighIn(MetricCard(weight, readings, Progress.sinceDay0(weight, readings), Progress.best(weight, readings)))
    }

    /** The Friday weigh-in from Today. */
    fun recordWeight(metricId: Long, value: Double) {
        val current = state.value ?: return
        viewModelScope.launch { container.progress.record(metricId, current.date, value) }
    }

    /** Today's plan with its linked task, or the next plan on a rest day; null with no plan at all (D126). */
    private fun workoutCard(date: LocalDate, plans: List<WorkoutPlan>, visible: List<DayItem>): WorkoutCardUi? {
        val planned = plans.filter { it.items.isNotEmpty() }.associateBy { it.weekday }
        if (planned.isEmpty()) return null
        val today = Weekdays.index(date.dayOfWeek)
        planned[today]?.let { plan ->
            val linked = visible.firstOrNull { it.task.action == TaskAction.WORKOUT }
            return WorkoutCardUi.Training(plan.name, plan.items.size, plan.items.sumOf { it.sets }, linked?.task?.id, linked?.done == true)
        }
        val ahead = (1..7).first { (today + it) % 7 in planned }
        val next = planned.getValue((today + ahead) % 7)
        return WorkoutCardUi.Rest(next.name, next.weekday, ahead)
    }

    private fun cards(date: LocalDate, calendar: ProgramCalendar, s: TodaySettings, letters: List<LetterEntity>, outfit: Outfit): List<TodayCard> {
        val zone = ZoneId.systemDefault()
        val cards = mutableListOf<TodayCard>()
        // A letter on (or after) its day, until the day it was opened ends.
        letters.filter { it.opensOn <= date && (it.openedAt == null || it.openedAt.atZone(zone).toLocalDate() >= date) }
            .forEach { cards += TodayCard.LetterArrived(it.id, it.writtenOn, it.text, it.openedAt != null, outfit) }
        val chapter = Chapters.at(calendar, date)
        val wroteToday = letters.any { it.writtenOn == date && !it.isFirst }
        if (chapter != null && Chapters.isChapterStart(calendar, date) && s.cardDismissedOn != date && !wroteToday) {
            cards += TodayCard.ChapterStart(chapter, Chapters.letterOpensOn(calendar, date, firstLetter = false), BuddyPicker.outfit(chapter.number))
        } else if (!s.firstLetterOfferDone && letters.none { it.isFirst } && date >= s.installDate) {
            cards += TodayCard.FirstLetter(Chapters.letterOpensOn(calendar, date, firstLetter = true))
        }
        return cards
    }

    private fun tile(item: DayItem) = TileUi(
        item.task.id, item.task.title, item.task.icon, item.done, item.task.isImportant, due = item.dueSince != null, part = item.task.part,
    )
}
