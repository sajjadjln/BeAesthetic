package app.beaesthetic.ui.recap

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.beaesthetic.AppContainer
import app.beaesthetic.R
import app.beaesthetic.data.db.RecapEntity
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.data.repo.Pose as PhotoPose
import app.beaesthetic.domain.buddy.BuddyPicker
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.domain.progress.AfterFinish
import app.beaesthetic.domain.progress.Recaps
import app.beaesthetic.domain.time.ProgramDates
import androidx.room.withTransaction
import app.beaesthetic.domain.progress.Trend
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.EndLayout
import app.beaesthetic.ui.components.buddy.Piker
import app.beaesthetic.ui.dashboard.DashboardUiState
import app.beaesthetic.ui.dashboard.DashboardViewModel
import app.beaesthetic.ui.dashboard.Grid
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.progress.MetricCard
import app.beaesthetic.ui.progress.PhotoThumb
import app.beaesthetic.ui.progress.ProgressUiState
import app.beaesthetic.ui.progress.ProgressViewModel
import app.beaesthetic.ui.progress.deltaText
import app.beaesthetic.ui.progress.valueText
import app.beaesthetic.ui.theme.Ba
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

/** What only the recap needs beyond Progress and the dashboard: records so far, the sentence, the next chapter. */
data class RecapExtra(
    val records: Int,
    val identity: String?,
    val nextChapterMonth: Int?,
    val weeksLeft: Int,
    val outfitChapter: Int,
    /** The finish card (F1): the finish date, the program's weeks and days. */
    val endDate: java.time.LocalDate,
    val totalWeeks: Int,
    val totalDays: Int,
    val nowruzNext: Boolean,
    val chosen: AfterFinish,
)

class RecapViewModel(private val container: AppContainer, private val finish: Boolean) : ViewModel() {

    val extra: StateFlow<RecapExtra?> = combine(
        container.db.settings().observe().filterNotNull(),
        container.workouts.observeAllSets(),
        container.clock.minuteTicks(),
    ) { s, sets, local ->
        val calendar = ProgramCalendar(s.programStartDate, s.programEndDate, s.rolloverHour)
        val today = ProgramCalendar.programDate(local, s.rolloverHour)
        val next = Chapters.at(calendar, maxOf(today, Recaps.halfway(s.halfwayDate, calendar)).plusDays(1))
        RecapExtra(
            records = Recaps.records(sets, today),
            identity = s.identitySentence?.takeIf { it.isNotBlank() },
            nextChapterMonth = next?.months?.firstOrNull(),
            weeksLeft = ((ChronoUnit.DAYS.between(today, calendar.endDate) + 6) / 7).toInt().coerceAtLeast(0),
            outfitChapter = next?.number ?: 3,
            endDate = calendar.endDate,
            totalWeeks = calendar.totalWeeks,
            totalDays = ChronoUnit.DAYS.between(calendar.day0, calendar.endDate).toInt(),
            nowruzNext = app.beaesthetic.domain.time.Jalali.fromGregorian(calendar.endDate.plusDays(1)).let { it.month == 1 && it.day == 1 },
            chosen = runCatching { AfterFinish.valueOf(s.afterFinishMode) }.getOrDefault(AfterFinish.NONE),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            container.ensureReady()
            container.db.weeks().markRecapSeen(RecapEntity(if (finish) Recaps.FINISH else Recaps.HALFWAY, container.clock.now()))
            container.events.screen("recap")
        }
    }

    /** «ادامه» (D61): routines go on, no chapters or week count. */
    fun keepGoing(onDone: () -> Unit) {
        viewModelScope.launch {
            container.db.settings().setAfterFinishMode(AfterFinish.MAINTENANCE.name)
            onDone()
        }
    }

    /**
     * «برنامه‌ی تازه» (D61, D145): a backup first, then new dates (today or the day after the finish, to the
     * next Nowruz eve). Tasks, the workout plan, metrics, milestones and history stay; the old check-ins and
     * photos move up out of the new schedule's way; both recaps and the first letter come back.
     */
    fun startNewProgram(onDone: () -> Unit) {
        container.scope.launch {
            container.backup.backUp(app.beaesthetic.domain.backup.BackupReason.AUTO)
            val s = container.db.settings().get() ?: return@launch
            val today = container.clock.programDate(s.rolloverHour)
            val day0 = ProgramDates.newDay0(today, s.programEndDate)
            val end = ProgramDates.defaultEnd(day0)
            container.db.withTransaction {
                val offset = Recaps.archiveOffset(container.db.progress().maxCheckInIdx())
                if (offset > 0) {
                    container.db.progress().shiftCheckIns(offset)
                    container.db.progress().shiftPhotos(offset)
                }
                container.db.weeks().clearRecaps()
                container.db.settings().upsert(
                    s.copy(
                        programStartDate = day0,
                        programEndDate = end,
                        halfwayDate = ProgramDates.yaldaWithin(day0, end),
                        afterFinishMode = AfterFinish.NONE.name,
                        firstLetterOfferDone = false,
                        cardDismissedOn = null,
                    ),
                )
            }
            container.scheduler.reconcile("new-program")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onDone() }
        }
    }
}

/**
 * The halfway recap (flows R1–R3 + «نیمه‌ی دوم»): story cards with a progress bar on top; a tap anywhere
 * moves on, ✕ closes. Wins only: days shown up, Day 0 → now, totals, milestones reached, photos, the sentence.
 */
@Composable
fun RecapRoute(
    recap: RecapViewModel,
    progress: ProgressViewModel,
    dashboard: DashboardViewModel,
    photos: PhotoStore,
    onClose: () -> Unit,
    finish: Boolean = false,
    startAtChoice: Boolean = false,
) {
    val extra by recap.extra.collectAsStateWithLifecycle()
    val p by progress.state.collectAsStateWithLifecycle()
    val d by dashboard.state.collectAsStateWithLifecycle()
    val c = Ba.colors
    // The finish adds the finish-line card and the choice (F1, D61); the choice can be opened on its own.
    val count = if (finish) 5 else 4
    var card by rememberSaveable { mutableIntStateOf(if (finish && startAtChoice) count - 1 else 0) }
    val lastIsChoice = finish
    Box(
        Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (card < count - 1) card++ else if (!lastIsChoice) onClose() },
    ) {
        val e = extra
        val ps = p
        val ds = d
        if (e == null || ps == null || ds == null) return@Box
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(count) { i ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= card) c.accent else c.surface2))
                }
                Icon(
                    BaIcons.X,
                    contentDescription = stringResource(R.string.recap_close),
                    tint = c.text2,
                    modifier = Modifier.padding(start = 8.dp).size(28.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(4.dp),
                )
            }
            AnimatedContent(card, label = "recap", modifier = Modifier.weight(1f).fillMaxWidth()) { i ->
                // The last halfway card ends the recap: its button sits at the bottom (D147).
                val closing = !finish && i == count - 1
                EndLayout(
                    horizontalPadding = 0.dp,
                    buttons = if (closing) { -> PrimaryButton(stringResource(R.string.recap_r4_done), onClose, Modifier.fillMaxWidth()) } else null,
                ) {
                    when {
                        i == 0 -> ShownUp(ds, finish)
                        i == 1 -> Strength(ps, e)
                        i == 2 -> PhotosAndSentence(ps, e, photos)
                        !finish -> SecondHalf(e)
                        i == 3 -> FinishLine(e, ds)
                        else -> Choice(e, onKeepGoing = { recap.keepGoing(onClose) }, onNewProgram = { recap.startNewProgram(onClose) })
                    }
                }
            }
            if (card < count - 1) {
                Text(stringResource(R.string.recap_tap_next), style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ShownUp(d: DashboardUiState, finish: Boolean) {
    val c = Ba.colors
    Text(stringResource(R.string.recap_r1_title), style = Ba.type.headline, color = c.text1)
    Grid(d, onTap = {}, modifier = Modifier.fillMaxWidth())
    Text(
        stringResource(if (finish) R.string.recap_f_r1_body else R.string.recap_r1_body, PersianDigits.format(d.summary.shownUp)),
        style = Ba.type.title,
        color = c.text1,
        textAlign = TextAlign.Center,
    )
    if (d.summary.special > 0) {
        Text(stringResource(R.string.recap_r1_special, PersianDigits.format(d.summary.special)), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Strength(p: ProgressUiState, e: RecapExtra) {
    val c = Ba.colors
    Text(stringResource(R.string.recap_r2_title), style = Ba.type.headline, color = c.text1)
    val cards: List<MetricCard> = (listOfNotNull(p.weight) + p.others).filter { it.since != null && it.since.start.date != it.since.latest.date }
    // Better changes first; the rest stay neutral, never "worse" (no shame).
    cards.sortedBy { if (it.since?.trend == Trend.BETTER) 0 else 1 }.take(5).forEach { card ->
        val since = card.since ?: return@forEach
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(14.dp)) {
            Text(card.metric.name, style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
            Text(valueText(card.metric.unit, since.start.value) + "  ←  " + valueText(card.metric.unit, since.latest.value), style = Ba.type.label, color = c.text1)
            Chip(deltaText(since.delta), if (since.trend == Trend.BETTER) ChipKind.ACCENT else ChipKind.NEUTRAL, Modifier.padding(start = 8.dp))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (p.totals.workouts > 0) Big(PersianDigits.grouped(p.totals.workouts), stringResource(R.string.totals_workouts))
        if (e.records > 0) Big(PersianDigits.grouped(e.records), stringResource(R.string.recap_records))
        p.totals.perLadder.take(3).forEach { t ->
            val value = if (t.kind == ExerciseKind.SECONDS) PersianDigits.grouped(t.total / 60) else PersianDigits.grouped(t.total)
            Big(value, if (t.kind == ExerciseKind.SECONDS) stringResource(R.string.totals_minutes, t.name) else t.name)
        }
    }
    p.milestones.filter { it.reachedOn != null }.forEach { m ->
        Chip(stringResource(R.string.recap_reached, m.milestone.title), ChipKind.ACCENT, icon = BaIcons.Flag)
    }
    if (cards.isEmpty() && p.totals.workouts == 0) {
        Text(stringResource(R.string.recap_r2_empty), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Big(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Ba.type.headline, color = Ba.colors.text1)
        Text(label, style = Ba.type.caption, color = Ba.colors.text2, maxLines = 1)
    }
}

@Composable
private fun PhotosAndSentence(p: ProgressUiState, e: RecapExtra, photos: PhotoStore) {
    val c = Ba.colors
    Text(stringResource(R.string.recap_r3_title), style = Ba.type.headline, color = c.text1)
    val withFront = p.photoRows.filter { it.photos[PhotoPose.FRONT] != null }.sortedBy { it.checkIn.idx }
    val first = withFront.firstOrNull()
    val last = withFront.lastOrNull()?.takeIf { it != first }
    if (first != null && last != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(first, last).forEach { row ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    PhotoThumb(photos, row.photos[PhotoPose.FRONT], jalaliDayMonth(row.checkIn.date), Modifier.fillMaxWidth().aspectRatio(3f / 4f), maxEdge = 900)
                    Text(jalaliDayMonth(row.checkIn.date), style = Ba.type.caption, color = c.text2, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    } else {
        Text(stringResource(R.string.recap_r3_no_photos), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
    }
    e.identity?.let {
        Text("«$it»", style = Ba.type.title, color = c.text1, textAlign = TextAlign.Center)
        Text(stringResource(R.string.recap_r3_own), style = Ba.type.caption, color = c.text3)
    }
}

/** F1: the finish line. The whole program line, «پیکر» with the sprout. No confetti. */
@Composable
private fun FinishLine(e: RecapExtra, d: DashboardUiState) {
    val c = Ba.colors
    val j = app.beaesthetic.domain.time.Jalali.fromGregorian(e.endDate)
    Text(stringResource(R.string.recap_f_date, jalaliDayMonth(e.endDate), PersianDigits.format(j.year)), style = Ba.type.label, color = c.text2)
    Piker(Pose.FINISH, c.accent, app.beaesthetic.domain.buddy.Outfit.SPROUT, Modifier.width(170.dp))
    Text(stringResource(R.string.recap_f_weeks, PersianDigits.format(e.totalWeeks)), style = Ba.type.headline, color = c.text1, textAlign = TextAlign.Center)
    Text(stringResource(R.string.recap_f_days, PersianDigits.format(d.summary.shownUp)), style = Ba.type.display, color = c.accent)
    Text(
        stringResource(if (e.nowruzNext) R.string.recap_f_days_body_nowruz else R.string.recap_f_days_body, PersianDigits.format(e.totalDays)),
        style = Ba.type.body,
        color = c.text2,
        textAlign = TextAlign.Center,
    )
}

/** D61: what comes after. Both keep every task, the plan and the history. */
@Composable
private fun Choice(e: RecapExtra, onKeepGoing: () -> Unit, onNewProgram: () -> Unit) {
    val c = Ba.colors
    Text(stringResource(R.string.recap_choice_title), style = Ba.type.headline, color = c.text1)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(onClick = onKeepGoing).padding(16.dp),
    ) {
        Text(stringResource(R.string.recap_choice_keep), style = Ba.type.subtitle, color = c.text1)
        Text(stringResource(R.string.recap_choice_keep_body), style = Ba.type.caption, color = c.text2)
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accentSubtle).clickable(onClick = onNewProgram).padding(16.dp),
    ) {
        Text(stringResource(R.string.recap_choice_new), style = Ba.type.subtitle, color = c.onAccentSubtle)
        Text(stringResource(R.string.recap_choice_new_body), style = Ba.type.caption, color = c.onAccentSubtle)
    }
}

@Composable
private fun SecondHalf(e: RecapExtra) {
    val c = Ba.colors
    val months = stringArrayResource(R.array.jalali_months)
    Piker(Pose.CHEER, c.accent, BuddyPicker.outfit(e.outfitChapter), Modifier.width(170.dp))
    Text(stringResource(R.string.recap_r4_title), style = Ba.type.headline, color = c.text1)
    val month = e.nextChapterMonth?.let { months.getOrNull(it - 1) } ?: months[9]
    Text(stringResource(R.string.recap_r4_body, month, PersianDigits.format(e.weeksLeft)), style = Ba.type.title, color = c.text1, textAlign = TextAlign.Center)
}
