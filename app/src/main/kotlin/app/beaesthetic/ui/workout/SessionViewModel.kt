package app.beaesthetic.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.data.repo.toDomain
import app.beaesthetic.domain.buddy.BuddyPicker
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.domain.workout.Exercise
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.Ladders
import app.beaesthetic.domain.workout.LoggedSet
import app.beaesthetic.domain.workout.Offer
import app.beaesthetic.domain.workout.PlanItem
import app.beaesthetic.domain.workout.Progression
import app.beaesthetic.domain.workout.RoundNumbers
import app.beaesthetic.domain.workout.SetDraft
import app.beaesthetic.domain.workout.WorkoutPlan
import app.beaesthetic.domain.workout.WorkoutRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One circle of the set row. [value] is null for a set not logged yet. */
data class SetCircle(val value: Int?, val record: Boolean, val current: Boolean)

/** A finished move in the summary: its name and the values logged, «۱۲، ۱۲، ۱۰». */
data class SummaryLine(val name: String, val values: List<Int>, val kind: ExerciseKind)

/** «پله‌ی ۲ از ۴، بعدی: الماسی» under the move's name (D70). [next] is null on the top rung. */
data class LadderLine(val rung: Int, val of: Int, val next: String?)

/** An offer in the summary (D120) and whether «بله» was tapped. */
data class OfferUi(val offer: Offer, val moveName: String, val accepted: Boolean)

/** A round number crossed today (D121), with the move's name for reps and seconds. */
data class RoundUi(val crossing: RoundNumbers.Crossing, val name: String?)

sealed interface SessionUi {
    data object Loading : SessionUi

    /** The day has no plan (D100): the screen sends the user to the plan editor. */
    data class NoPlan(val outfit: Outfit) : SessionUi

    data class Logging(
        val date: LocalDate,
        val plan: WorkoutPlan,
        val exerciseIndex: Int,
        val item: PlanItem,
        val setIndex: Int,
        val circles: List<SetCircle>,
        val draft: SetDraft,
        /** Last time's sets of this move («قبلی ۱۲، ۱۲، ۱۰»). */
        val previous: List<LoggedSet>,
        /** Per move: every planned set logged (the dashes at the top). */
        val exercisesDone: List<Boolean>,
        val anyLogged: Boolean,
        val ladder: LadderLine?,
        /** The current set was logged before: ✓ corrects it. */
        val correcting: Boolean,
    ) : SessionUi

    data class Summary(
        val outfit: Outfit,
        val allDone: Boolean,
        val sets: Int,
        val records: Int,
        val lines: List<SummaryLine>,
        val lineIndex: Int,
        val offers: List<OfferUi>,
        val rounds: List<RoundUi>,
    ) : SessionUi
}

/** Where the user put the session; null fields follow the plan (the first open move, its next set). */
private data class Cursor(val exercise: Int, val set: Int? = null)

private data class Setup(val date: LocalDate, val weekday: Int, val outfit: Outfit)

private data class Ui(val cursor: Cursor?, val draft: SetDraft?, val summary: Boolean, val offers: OfferAnswers)

/** Offers answered in this summary, by move: «بله» or «بعداً». */
private data class OfferAnswers(val accepted: Set<Long> = emptySet(), val later: Set<Long> = emptySet())

/** Moves, ladder links and today's round numbers. */
private data class Library(val exercises: Map<Long, Exercise>, val links: Map<Long, Long>, val rounds: List<RoundNumbers.Crossing>)

/** The workout session (screens.html 04, D103, D115, D116). Every set is saved the moment ✓ is tapped. */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(private val container: AppContainer, private val summaryLines: Int) : ViewModel() {

    private val setup = MutableStateFlow<Setup?>(null)
    private val sessionId = MutableStateFlow<Long?>(null)
    private val cursor = MutableStateFlow<Cursor?>(null)
    private val draft = MutableStateFlow<SetDraft?>(null)
    private val summary = MutableStateFlow(false)

    private val _records = MutableSharedFlow<Pair<Long, Int>>(extraBufferCapacity = 1)

    /** A set that just became a record (exerciseId, setIndex): the screen bursts its circle and vibrates. */
    val records: SharedFlow<Pair<Long, Int>> = _records

    private val answers = MutableStateFlow(OfferAnswers())

    /** The day's plan as it was when the session opened: accepting an offer changes the plan, not this session. */
    private val plan: Flow<WorkoutPlan> = setup.filterNotNull().flatMapLatest { s ->
        container.workouts.observePlans().map { it[s.weekday] }.take(1)
    }

    private val library: Flow<Library> = setup.filterNotNull().flatMapLatest { s ->
        combine(
            container.workouts.observeExercises(),
            container.workouts.observeHarderLinks(),
            container.workouts.observeRoundNumbersOn(s.date),
        ) { ex, links, rounds -> Library(ex.associateBy { it.id }, links, rounds) }
    }

    /** Every set of the plan's moves, all time, in the order they were logged. */
    private val sets: StateFlow<List<LoggedSet>> = plan
        .map { p -> p.items.map { it.exercise.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(emptyList()) else container.workouts.observeSets(ids).map { rows -> rows.map { it.toDomain() } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<SessionUi> = combine(
        setup.filterNotNull(),
        plan,
        sets,
        combine(sessionId, library) { sid, lib -> sid to lib },
        combine(cursor, draft, summary, answers) { c, d, s, a -> Ui(c, d, s, a) },
    ) { s, p, all, (sid, lib), ui -> toUi(s, p, all, sid, lib, ui) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUi.Loading)

    init {
        viewModelScope.launch {
            container.ensureReady()
            val settings = container.db.settings().get() ?: return@launch
            val date = container.clock.programDate(settings.rolloverHour)
            val weekday = Weekdays.index(date.dayOfWeek)
            sessionId.value = container.workouts.existingSession(weekday, date)
            val chapter = Chapters.at(ProgramCalendar(settings.programStartDate, settings.programEndDate, settings.rolloverHour), date)
            setup.value = Setup(date, weekday, BuddyPicker.outfit(chapter?.number ?: 1))
            container.events.screen("workout")
        }
    }

    private fun toUi(s: Setup, plan: WorkoutPlan, all: List<LoggedSet>, sid: Long?, lib: Library, ui: Ui): SessionUi {
        val items = plan.items
        if (items.isEmpty()) return SessionUi.NoPlan(s.outfit)
        val here = if (sid == null) emptyList() else all.filter { it.sessionId == sid }
        val before = all.filter { it.sessionId != sid }
        val records = WorkoutRules.records(here, before)
        val open = WorkoutRules.firstOpen(items, here)

        if (ui.summary || (ui.cursor == null && open == null && here.isNotEmpty())) {
            val lines = items.mapNotNull { item ->
                val mine = here.filter { it.exerciseId == item.exercise.id }.sortedBy { it.setIndex }
                if (mine.isEmpty()) null else SummaryLine(item.exercise.name, mine.map { it.value }, item.exercise.kind)
            }
            val offers = Progression.offers(items, here) { id -> lib.links[id]?.let(lib.exercises::get) }
                .filter { it.exerciseId !in ui.offers.later }
                .map { o -> OfferUi(o, items.first { it.exercise.id == o.exerciseId }.exercise.name, o.exerciseId in ui.offers.accepted) }
            return SessionUi.Summary(
                s.outfit, open == null, here.size, records.size, lines, s.date.dayOfYear % summaryLines.coerceAtLeast(1),
                offers, rounds(items, here, lib),
            )
        }

        val ex = (ui.cursor?.exercise ?: open ?: 0).coerceIn(items.indices)
        val item = items[ex]
        val mine = here.filter { it.exerciseId == item.exercise.id }
        // A move with every set logged shows its last set, ready to be corrected.
        val setIndex = (ui.cursor?.set ?: WorkoutRules.nextSetIndex(item, here)).coerceAtMost(item.sets - 1)
        val existing = mine.firstOrNull { it.setIndex == setIndex }
        val previous = WorkoutRules.previous(all, item.exercise.id, sid ?: NO_SESSION)
        val draft = ui.draft
            ?: existing?.let { SetDraft(it.value, if (item.kg == null) null else it.kg, it.effort) }
            ?: WorkoutRules.prefill(item, previous, mine, setIndex)
        val circles = (0 until item.sets).map { i ->
            val logged = mine.firstOrNull { it.setIndex == i }
            SetCircle(if (i == setIndex) draft.value else logged?.value, (item.exercise.id to i) in records, i == setIndex)
        }
        val counts = here.groupingBy { it.exerciseId }.eachCount()
        return SessionUi.Logging(
            date = s.date,
            plan = plan,
            exerciseIndex = ex,
            item = item,
            setIndex = setIndex,
            circles = circles,
            draft = draft,
            previous = previous,
            exercisesDone = items.map { (counts[it.exercise.id] ?: 0) >= it.sets },
            anyLogged = here.isNotEmpty(),
            ladder = ladderLine(item.exercise.id, lib),
            correcting = existing != null,
        )
    }

    private fun ladderLine(id: Long, lib: Library): LadderLine? {
        val chain = Ladders.chain(id, lib.links)
        if (chain.size < 2) return null
        val at = chain.indexOf(id)
        return LadderLine(at + 1, chain.size, chain.getOrNull(at + 1)?.let { lib.exercises[it]?.name })
    }

    /**
     * Today's round numbers that belong to this session: workouts, and reps or seconds of a ladder this session
     * logged. Named after the move logged today on that ladder. Only the highest of each total.
     */
    private fun rounds(items: List<PlanItem>, here: List<LoggedSet>, lib: Library): List<RoundUi> {
        if (here.isEmpty()) return emptyList()
        val loggedMoves = items.filter { item -> here.any { it.exerciseId == item.exercise.id } }
        val byLadder = loggedMoves.associateBy { Ladders.root(it.exercise.id, lib.links) }
        return RoundNumbers.highestEach(lib.rounds)
            .filter { it.kind == RoundNumbers.Kind.WORKOUTS || (it.ladder != null && it.ladder in byLadder) }
            .map { RoundUi(it, it.ladder?.let { l -> byLadder[l]?.exercise?.name }) }
    }

    /** «بله»: the plan changes for next time (D120). */
    fun acceptOffer(offer: Offer) {
        val setup = setup.value ?: return
        answers.value = answers.value.copy(accepted = answers.value.accepted + offer.exerciseId)
        viewModelScope.launch { container.workouts.accept(setup.weekday, offer) }
    }

    /** «بعداً»: hidden for now; it comes again the next time the top is reached. */
    fun laterOffer(offer: Offer) {
        answers.value = answers.value.copy(later = answers.value.later + offer.exerciseId)
    }

    private fun logging(): SessionUi.Logging? = state.value as? SessionUi.Logging

    fun stepValue(up: Boolean) {
        val s = logging() ?: return
        val step = WorkoutRules.step(s.item.exercise.kind)
        draft.value = s.draft.copy(value = (s.draft.value + if (up) step else -step).coerceIn(0, MAX_VALUE))
    }

    fun stepKg(up: Boolean) {
        val s = logging() ?: return
        val kg = s.draft.kg ?: return
        draft.value = s.draft.copy(kg = WorkoutRules.stepKg(kg, up))
    }

    fun setEffort(effort: Int) {
        val s = logging() ?: return
        draft.value = s.draft.copy(effort = effort.coerceIn(WorkoutRules.MIN_EFFORT, WorkoutRules.MAX_EFFORT))
    }

    fun selectExercise(index: Int) {
        cursor.value = Cursor(index)
        draft.value = null
    }

    fun selectSet(index: Int) {
        val s = logging() ?: return
        cursor.value = Cursor(s.exerciseIndex, index)
        draft.value = null
    }

    /** ✓: saves the set, ticks the workout task (D115), then moves to the next set or move. */
    fun log() {
        val s = logging() ?: return
        val setup = setup.value ?: return
        val d = s.draft
        viewModelScope.launch {
            val all = sets.value
            val sid = sessionId.value
            val earlier = all.filter { it.exerciseId == s.item.exercise.id && !(it.sessionId == sid && it.setIndex == s.setIndex) }
            val record = WorkoutRules.isRecord(d.value, d.kg, earlier)
            val settings = container.db.settings().get()
            val id = container.workouts.logSet(setup.weekday, setup.date, sid, s.item.exercise.id, s.setIndex, d.value, d.kg, d.effort, container.clock.now())
            sessionId.value = id
            if (settings != null) container.events.firstTickIfNeeded(setup.date, settings.rolloverHour)
            container.cancelEmptyBlocks(setup.date)
            if (record) _records.tryEmit(s.item.exercise.id to s.setIndex)
            advance(s, record)
        }
    }

    private suspend fun advance(s: SessionUi.Logging, record: Boolean) {
        draft.value = null
        if (s.correcting) {
            // A correction stays on its set, so several sets can be fixed in a row.
            cursor.value = Cursor(s.exerciseIndex, s.setIndex)
            return
        }
        val doneHere = s.circles.count { it.value != null && !it.current } + 1
        if (doneHere < s.item.sets) {
            // The next open set of this move.
            cursor.value = Cursor(s.exerciseIndex)
            return
        }
        // The move is done: let the last circle (and a «رکورد») show for a moment, then go on.
        cursor.value = Cursor(s.exerciseIndex, s.setIndex)
        delay(if (record) RECORD_PAUSE_MS else DONE_PAUSE_MS)
        val now = logging() ?: return
        val items = s.plan.items
        val next = (1 until items.size).map { (s.exerciseIndex + it) % items.size }.firstOrNull { !now.exercisesDone[it] }
        if (next == null) {
            cursor.value = null
            summary.value = true
            sessionId.value?.let { container.workouts.finish(it, container.clock.now()) }
        } else {
            cursor.value = Cursor(next)
        }
    }

    /** «پایان»: the summary, with what was logged so far. */
    fun showSummary() {
        summary.value = true
        viewModelScope.launch { sessionId.value?.let { container.workouts.finish(it, container.clock.now()) } }
    }

    /** «ادامه‌ی تمرین» from the summary. */
    fun resume() {
        val p = (state.value as? SessionUi.Summary) ?: return
        summary.value = false
        // Everything logged: back on the first move, to look or correct. Otherwise the first open move.
        cursor.value = if (p.allDone) Cursor(0) else null
        draft.value = null
    }

    private companion object {
        const val NO_SESSION = -1L
        const val MAX_VALUE = 999
        const val RECORD_PAUSE_MS = 1_200L
        const val DONE_PAUSE_MS = 450L
    }
}

/** The values of a move, «۱۲، ۱۲، ۱۰». */
fun valuesLine(values: List<Int>): String = values.joinToString(SEP) { PersianDigits.format(it) }
