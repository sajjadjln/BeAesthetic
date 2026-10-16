package app.beaesthetic.ui.progress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.data.repo.Pose
import app.beaesthetic.domain.backup.BackupReason
import app.beaesthetic.domain.progress.CheckIns
import app.beaesthetic.domain.progress.DecimalInput
import app.beaesthetic.domain.progress.Metric
import app.beaesthetic.domain.progress.MetricKind
import app.beaesthetic.domain.progress.Progress
import app.beaesthetic.domain.progress.Reading
import app.beaesthetic.domain.text.PersianDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CheckInStep { WEIGHT, TAPE, TESTS, PHOTOS, DONE }

/** A metric in the flow: its readings before this check-in, and what's typed now (pre-filled, flows C2–C3). */
data class CheckInMetric(val metric: Metric, val earlier: List<Reading>, val value: String, val guess: String) {
    val last: Reading? get() = earlier.maxByOrNull { it.date }
}

data class CheckInUiState(
    val idx: Int,
    /** The date the readings are stored with: Day 0's date for the baseline, else today. */
    val date: LocalDate,
    val steps: List<CheckInStep>,
    val step: CheckInStep,
    val weight: CheckInMetric?,
    val tape: List<CheckInMetric>,
    val tests: List<CheckInMetric>,
    /** This check-in's photos and Day 0's, pose → file name. */
    val photos: Map<Pose, String>,
    val day0Photos: Map<Pose, String>,
    /** Readings of every metric with this check-in's values in, for the done screen. */
    val all: List<Pair<Metric, List<Reading>>>,
    val photoFailed: Boolean = false,
    /** A photo is still being copied in; «بعدی» waits for it so the check-in never closes without it. */
    val savingPhoto: Boolean = false,
    /** Every check-in's photos, for the ghost (last earlier photo of a pose) and the reveal. */
    val photoRows: List<PhotoRow> = emptyList(),
) {
    /** The ghost for [pose]: the latest earlier check-in's photo, with that check-in's index. */
    fun ghost(pose: Pose): Pair<Int, String>? =
        photoRows.filter { it.checkIn.idx < idx }.lastOrNull { pose in it.photos }?.let { it.checkIn.idx to it.photos.getValue(pose) }
}

/** The monthly check-in (flows C1–C3, S2) and the Day 0 baseline (idx 0). One view model per visit. */
class CheckInViewModel(private val container: AppContainer, private val idx: Int) : ViewModel() {

    private val drafts = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val guesses = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val step = MutableStateFlow<CheckInStep?>(null)
    private val photoFailed = MutableStateFlow(false)
    private val savingPhotos = MutableStateFlow(0)
    private val date = MutableStateFlow<LocalDate?>(null)
    private val schedule = MutableStateFlow<List<app.beaesthetic.domain.progress.CheckIn>>(emptyList())

    val state: StateFlow<CheckInUiState?> = combine(
        container.progress.observe(),
        date.filterNotNull(),
        combine(drafts, guesses, step, photoFailed, savingPhotos) { d, g, s, f, n -> Typed(d, g, s, f, n > 0) },
    ) { data, day, (d, g, s, failed, saving) ->
        fun item(m: Metric): CheckInMetric {
            val all = data.readings[m.id].orEmpty()
            val earlier = all.filter { it.date < day }
            val now = all.firstOrNull { it.date == day }
            // Pre-filled with this check-in's value, else the last one; a test's guess with the last result (D73).
            val prefill = now?.value ?: earlier.maxByOrNull { it.date }?.value
            val value = d[m.id] ?: prefill?.let(::plain).orEmpty()
            val guess = g[m.id] ?: (now?.guess ?: earlier.maxByOrNull { it.date }?.value)?.let(::plain).orEmpty()
            return CheckInMetric(m, earlier, if (m.kind == MetricKind.STRENGTH && now == null && d[m.id] == null) "" else value, guess)
        }
        val weight = data.metrics.firstOrNull { it.kind == MetricKind.BODY }?.let(::item)
        val tape = data.metrics.filter { it.kind == MetricKind.TAPE }.map(::item)
        val tests = data.metrics.filter { it.kind == MetricKind.STRENGTH }.map(::item)
        val steps = buildList {
            if (weight != null) add(CheckInStep.WEIGHT)
            if (tape.isNotEmpty()) add(CheckInStep.TAPE)
            if (tests.isNotEmpty()) add(CheckInStep.TESTS)
            add(CheckInStep.PHOTOS)
            add(CheckInStep.DONE)
        }
        CheckInUiState(
            idx = idx,
            date = day,
            steps = steps,
            step = s ?: steps.first(),
            weight = weight,
            tape = tape,
            tests = tests,
            photos = data.photos[idx].orEmpty().mapValues { it.value.fileName },
            day0Photos = data.photos[0].orEmpty().mapValues { it.value.fileName },
            all = data.metrics.map { it to data.readings[it.id].orEmpty() },
            photoFailed = failed,
            savingPhoto = saving,
            photoRows = schedule.value.mapNotNull { c -> data.photos[c.idx]?.takeIf { it.isNotEmpty() }?.let { PhotoRow(c, it.mapValues { (_, p) -> p.fileName }) } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            container.ensureReady()
            val s = container.db.settings().observe().filterNotNull().first()
            val all = CheckIns.schedule(s.calendar())
            schedule.value = all
            date.value = if (idx == 0) all.first().date else container.clock.programDate(s.rolloverHour)
            container.events.screen(if (idx == 0) "checkin-day0" else "checkin")
        }
    }

    fun setValue(metricId: Long, text: String) {
        drafts.value = drafts.value + (metricId to text)
    }

    fun setGuess(metricId: Long, text: String) {
        guesses.value = guesses.value + (metricId to text)
    }

    /** Saves the step's numbers (blank ones are skipped) and moves on. */
    fun next() {
        val s = state.value ?: return
        if (s.step == CheckInStep.PHOTOS && s.savingPhoto) return
        viewModelScope.launch {
            when (s.step) {
                CheckInStep.WEIGHT -> s.weight?.let { save(listOf(it), s.date) }
                CheckInStep.TAPE -> save(s.tape, s.date)
                CheckInStep.TESTS -> save(s.tests, s.date)
                CheckInStep.PHOTOS -> {
                    container.progress.finishCheckIn(idx, s.date, container.clock.now())
                    // A backup runs after every check-in (flows C5), however the done screen is left; no folder means none.
                    container.scope.launch { container.backup.backUp(BackupReason.AUTO) }
                }
                CheckInStep.DONE -> Unit
            }
            val i = s.steps.indexOf(s.step)
            step.value = s.steps.getOrElse(i + 1) { CheckInStep.DONE }
        }
    }

    fun back(): Boolean {
        val s = state.value ?: return false
        val i = s.steps.indexOf(s.step)
        if (i <= 0 || s.step == CheckInStep.DONE) return false
        step.value = s.steps[i - 1]
        return true
    }

    fun setWeight(value: Double) {
        val w = state.value?.weight ?: return
        setValue(w.metric.id, plain(value))
    }

    /**
     * Copies a photo in on the app's scope, not this screen's: closing the check-in right after the last shot
     * must not cancel the save between the file and its row.
     */
    fun savePhoto(pose: Pose, uri: Uri) {
        photoFailed.value = false
        savingPhotos.update { it + 1 }
        container.scope.launch {
            try {
                runCatching { container.progress.savePhoto(idx, pose, uri, container.clock.now()) }
                    .onFailure { photoFailed.value = true }
            } finally {
                savingPhotos.update { it - 1 }
            }
        }
    }

    fun deletePhoto(pose: Pose) {
        viewModelScope.launch { container.progress.deletePhoto(idx, pose) }
    }

    private suspend fun save(items: List<CheckInMetric>, day: LocalDate) {
        for (item in items) {
            val value = DecimalInput.parse(item.value) ?: continue
            val guess = if (item.metric.kind == MetricKind.STRENGTH) DecimalInput.parse(item.guess) else null
            container.progress.record(item.metric.id, day, value, guess)
        }
    }

    private data class Typed(
        val drafts: Map<Long, String>,
        val guesses: Map<Long, String>,
        val step: CheckInStep?,
        val photoFailed: Boolean,
        val saving: Boolean,
    )

    private companion object {
        /** A value as it shows in a field: «۷۶٫۸», «۱۲». [DecimalInput] reads it back. */
        fun plain(v: Double): String = PersianDigits.format(Progress.round(v))
    }
}
