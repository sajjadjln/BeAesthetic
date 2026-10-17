package app.beaesthetic.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.data.repo.Pose
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.progress.CheckIn
import app.beaesthetic.domain.progress.CheckIns
import app.beaesthetic.domain.progress.DecimalInput
import app.beaesthetic.domain.progress.MilestoneView
import app.beaesthetic.domain.progress.Progress
import app.beaesthetic.domain.progress.Reveals
import app.beaesthetic.domain.progress.Trend
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.ErrorLine
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.JalaliCalendar
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.components.StepButton
import app.beaesthetic.ui.components.sheetScroll
import app.beaesthetic.ui.components.buddy.Piker
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.domain.buddy.Pose as BuddyPose
import java.time.LocalDate

@Composable
fun ProgressRoute(
    viewModel: ProgressViewModel,
    photos: PhotoStore,
    onCheckIn: (Int) -> Unit,
    onManage: () -> Unit,
    onBackfill: (LocalDate) -> Unit,
    onReveal: (Int) -> Unit,
    onRecap: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var milestone by remember { mutableStateOf<MilestoneView?>(null) }
    var addingMilestone by remember { mutableStateOf(false) }
    var backfilling by remember { mutableStateOf(false) }
    ProgressScreen(
        state, photos, onCheckIn, onManage, viewModel::record, viewModel::deleteReading,
        extras = ProgressExtras(
            toggleMilestone = viewModel::toggleMilestone,
            editMilestone = { v -> if (v == null) addingMilestone = true else milestone = v },
            backfill = { backfilling = true },
            reveal = onReveal,
            recap = onRecap,
            finish = onFinish,
        ),
    )
    val s = state ?: return
    if (addingMilestone || milestone != null) {
        val editing = milestone
        MilestoneSheet(
            editing, s.metrics,
            onSave = { title, metricId, target ->
                viewModel.saveMilestone(editing?.milestone?.id, title, metricId, target, editing?.milestone?.reachedOn.takeIf { metricId == null })
                milestone = null; addingMilestone = false
            },
            onDelete = editing?.let { e -> { viewModel.deleteMilestone(e.milestone.id); milestone = null } },
            onDismiss = { milestone = null; addingMilestone = false },
        )
    }
    if (backfilling) BackfillSheet(s.backfillDays, onPick = { d -> backfilling = false; onBackfill(d) }, onDismiss = { backfilling = false })
}

data class ProgressExtras(
    val toggleMilestone: (MilestoneView) -> Unit = {},
    val editMilestone: (MilestoneView?) -> Unit = {},
    val backfill: () -> Unit = {},
    val reveal: (Int) -> Unit = {},
    val recap: () -> Unit = {},
    val finish: () -> Unit = {},
)

// ---------------------------------------------------------------- 03 Progress

@Composable
fun ProgressScreen(
    state: ProgressUiState?,
    photos: PhotoStore,
    onCheckIn: (Int) -> Unit,
    onManage: () -> Unit,
    onRecord: (Long, LocalDate, Double) -> Unit,
    onDelete: (Long, LocalDate) -> Unit,
    extras: ProgressExtras = ProgressExtras(),
) {
    val c = Ba.colors
    var openMetric by rememberSaveable { mutableStateOf<Long?>(null) }
    var weighing by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        val s = state ?: return@Box
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(stringResource(R.string.progress_title), style = Ba.type.headline, color = c.text1)
            if (!s.hasMetrics) {
                EmptyCard(onManage)
                return@Column
            }
            if (!s.day0Done) Day0Card(s.day0) { onCheckIn(0) } else s.next?.let { CheckInCard(it, s.today, s.nextOpen, s.isFinish) { onCheckIn(it.idx) } }

            ProgramLineCard(s.line)
            if (s.finishAvailable) {
                ListRow(onClick = extras.finish) {
                    Icon(BaIcons.Flag, null, tint = Ba.colors.accent, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.progress_finish_row), style = Ba.type.body, color = Ba.colors.text1, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    Icon(BaIcons.ChevronForward, null, tint = Ba.colors.text3, modifier = Modifier.size(18.dp))
                }
            }
            if (s.recapAvailable) {
                ListRow(onClick = extras.recap) {
                    Icon(BaIcons.Flag, null, tint = Ba.colors.accent, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.progress_recap_row), style = Ba.type.body, color = Ba.colors.text1, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    Icon(BaIcons.ChevronForward, null, tint = Ba.colors.text3, modifier = Modifier.size(18.dp))
                }
            }

            s.weight?.let { WeightCard(it, onOpen = { openMetric = it.metric.id }, onAdd = { weighing = true }) }

            if (s.others.isNotEmpty()) {
                SectionTitle(stringResource(R.string.progress_since_day0))
                s.others.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { card -> StatCard(card, Modifier.weight(1f)) { openMetric = card.metric.id } }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            TotalsCard(s.totals)
            LaddersCard(s.ladders)
            MilestonesCard(s.milestones, s.metrics, extras.toggleMilestone, extras.editMilestone)

            if (s.photoRows.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    SectionTitle(stringResource(R.string.progress_photos), Modifier.weight(1f))
                    Text(
                        stringResource(R.string.progress_photos_count, PersianDigits.format(s.photoRows.size), PersianDigits.format(s.checkInCount)),
                        style = Ba.type.caption, color = c.text3,
                    )
                }
                BaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val withPhotos = s.photoRows.map { it.checkIn.idx }
                        s.photoRows.reversed().forEach { row ->
                            // Only a row with an earlier set to compare against opens the reveal (not Day 0).
                            val reveal = Reveals.against(row.checkIn.idx, withPhotos) != null
                            PhotoRowView(row, photos, if (reveal) Modifier.clickable { extras.reveal(row.checkIn.idx) } else Modifier)
                        }
                    }
                }
            }

            BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                if (s.backfillDays.isNotEmpty()) {
                    ListRow(onClick = extras.backfill) {
                        Icon(BaIcons.Dumbbell, null, tint = c.text2, modifier = Modifier.size(22.dp))
                        Text(stringResource(R.string.backfill_row), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                        Icon(BaIcons.ChevronForward, null, tint = c.text3, modifier = Modifier.size(20.dp))
                    }
                }
                ListRow(onClick = onManage) {
                    Icon(BaIcons.Ruler, null, tint = c.text2, modifier = Modifier.size(22.dp))
                    Text(stringResource(R.string.progress_manage), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                    Icon(BaIcons.ChevronForward, null, tint = c.text3, modifier = Modifier.size(20.dp))
                }
            }
        }
        val card = (listOfNotNull(s.weight) + s.others).firstOrNull { it.metric.id == openMetric }
        if (card != null) MetricSheet(card, s.today, s.day0, onRecord, onDelete) { openMetric = null }
        val weight = s.weight
        if (weighing && weight != null) {
            WeighSheet(weight, onSave = { v -> onRecord(weight.metric.id, s.today, v); weighing = false }, onDismiss = { weighing = false })
        }
    }
}

@Composable
private fun EmptyCard(onManage: () -> Unit) {
    val c = Ba.colors
    BaCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Piker(BuddyPose.THINK, c.accent, Outfit.NONE, Modifier.width(140.dp))
            Text(stringResource(R.string.progress_empty_title), style = Ba.type.subtitle, color = c.text1)
            Text(stringResource(R.string.progress_empty_text), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
            PrimaryButton(stringResource(R.string.progress_choose), onClick = onManage, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Day 0 isn't entered yet: the numbers and photos can still be added with Day 0's date (baseline entry). */
@Composable
private fun Day0Card(day0: LocalDate, onStart: () -> Unit) {
    val c = Ba.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.specialSubtle).padding(16.dp),
    ) {
        Text(stringResource(R.string.progress_day0_title), style = Ba.type.subtitle, color = c.onSpecialSubtle)
        Text(stringResource(R.string.progress_day0_text, jalaliDayMonth(day0)), style = Ba.type.body, color = c.text1)
        PrimaryButton(stringResource(R.string.progress_day0_button), onClick = onStart, modifier = Modifier.fillMaxWidth(), height = 44.dp)
    }
}

/** «چک‌این ماهانه: جمعه ۱۵ آبان · وزن، متر، تست‌ها، ۳ عکس» and the days left (DESIGN 03). Indigo, like C1. */
@Composable
private fun CheckInCard(next: CheckIn, today: LocalDate, open: Boolean, isFinish: Boolean, onStart: () -> Unit) {
    val c = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    val days = CheckIns.daysUntil(today, next)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.specialSubtle).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val date = weekdays[Weekdays.index(next.date.dayOfWeek)] + " " + jalaliDayMonth(next.date)
            Text(
                stringResource(R.string.checkin_card_title, if (isFinish) stringResource(R.string.checkin_finish_line) + " · " + date else date),
                style = Ba.type.subtitle, color = c.onSpecialSubtle, modifier = Modifier.weight(1f),
            )
            val chip = when {
                days == 0 -> stringResource(R.string.checkin_today)
                days < 0 -> stringResource(R.string.checkin_still_open)
                else -> stringResource(R.string.checkin_days_left, PersianDigits.format(days))
            }
            Chip(chip, ChipKind.SPECIAL)
        }
        Text(stringResource(R.string.checkin_card_parts), style = Ba.type.caption, color = c.text2)
        if (open) PrimaryButton(stringResource(R.string.checkin_start), onClick = onStart, modifier = Modifier.fillMaxWidth(), height = 44.dp, icon = BaIcons.Camera)
    }
}

@Composable
private fun WeightCard(card: MetricCard, onOpen: () -> Unit, onAdd: () -> Unit) {
    val c = Ba.colors
    BaCard(Modifier.clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.metric.name, style = Ba.type.label, color = c.text2)
                Text(
                    card.since?.latest?.let { valueText(card.metric.unit, it.value) } ?: stringResource(R.string.progress_no_reading),
                    style = Ba.type.title, color = c.text1,
                )
                card.since?.takeIf { card.readings.size > 1 }?.let {
                    // Weight is neutral (DESIGN C2): never scored, never colored.
                    Text(
                        deltaText(it.delta) + " " + stringResource(R.string.progress_since_day0),
                        style = Ba.type.caption, color = c.text2,
                    )
                }
            }
            if (card.readings.size > 1) LineChart(card.readings, Modifier.width(120.dp).height(48.dp), highlightLast = false)
        }
        SecondaryButton(stringResource(R.string.progress_add_weight), onClick = onAdd, icon = BaIcons.Plus, height = 40.dp, modifier = Modifier.padding(top = 10.dp).fillMaxWidth())
    }
}

/** A "since Day 0" card: value, delta chip (accent only when it moved the right way), starting value. */
@Composable
private fun StatCard(card: MetricCard, modifier: Modifier, onOpen: () -> Unit) {
    val c = Ba.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(onClick = onOpen).padding(14.dp),
    ) {
        Text(card.metric.name, style = Ba.type.label, color = c.text2, maxLines = 1)
        val since = card.since
        if (since == null) {
            Text(stringResource(R.string.progress_no_reading), style = Ba.type.body, color = c.text3)
            return@Column
        }
        Text(valueText(card.metric.unit, since.latest.value), style = Ba.type.subtitle, color = c.text1)
        if (card.readings.size > 1) {
            Chip(deltaText(since.delta), if (since.trend == Trend.BETTER) ChipKind.ACCENT else ChipKind.NEUTRAL)
            Text(stringResource(R.string.progress_start_value, PersianDigits.format(since.start.value)), style = Ba.type.caption, color = c.text3)
        } else {
            Text(stringResource(R.string.progress_one_reading), style = Ba.type.caption, color = c.text3)
        }
    }
}

@Composable
private fun PhotoRowView(row: PhotoRow, photos: PhotoStore, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val months = stringArrayResource(R.array.jalali_months)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        val label = if (row.checkIn.isDay0) stringResource(R.string.progress_day0_title) else months[Jalali.fromGregorian(row.checkIn.date).month - 1]
        Text(label + " · " + jalaliDayMonth(row.checkIn.date), style = Ba.type.label, color = c.text2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pose.entries.forEach { pose ->
                val name = stringResource(poseName(pose))
                PhotoThumb(photos, row.photos[pose], stringResource(R.string.cd_photo, name, label), Modifier.weight(1f).aspectRatio(3f / 4f))
            }
        }
    }
}

// ---------------------------------------------------------------- sheets

/** A metric's chart and readings; a new reading can take any date from Day 0 to today (baseline entry). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSheet(
    card: MetricCard,
    today: LocalDate,
    day0: LocalDate,
    onRecord: (Long, LocalDate, Double) -> Unit,
    onDelete: (Long, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Ba.colors
    var date by remember { mutableStateOf(today) }
    var picking by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().sheetScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(card.metric.name, style = Ba.type.title, color = c.text1)
            if (card.readings.size > 1) LineChart(card.readings, Modifier.fillMaxWidth().height(120.dp))

            SectionTitle(stringResource(R.string.metric_add_reading))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(jalaliDayMonth(date), onClick = { picking = !picking }, icon = BaIcons.Calendar, height = 44.dp)
                Box(Modifier.weight(1f)) {
                    InputBox(text, stringResource(unitName(card.metric.unit)), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)) { text = it }
                }
                PrimaryButton(stringResource(R.string.save), height = 44.dp, onClick = {
                    val v = DecimalInput.parse(text)
                    if (v == null) bad++ else {
                        onRecord(card.metric.id, date, v)
                        text = ""
                    }
                })
            }
            if (bad > 0) ErrorLine(R.string.reading_value_bad, bad)
            if (picking) JalaliCalendar(date, today, { date = it; picking = false }, pickable = day0..today)

            if (card.readings.isNotEmpty()) {
                SectionTitle(stringResource(R.string.metric_readings))
                val best = card.best
                card.readings.sortedByDescending { it.date }.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(jalaliDayMonth(r.date), style = Ba.type.body, color = c.text2, modifier = Modifier.width(84.dp))
                        Text(valueText(card.metric.unit, r.value), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                        r.guess?.let { Text(stringResource(R.string.reading_guess, PersianDigits.format(it)), style = Ba.type.caption, color = c.text3) }
                        if (card.metric.kind == app.beaesthetic.domain.progress.MetricKind.STRENGTH && card.readings.size > 1 && best == r) {
                            Chip(stringResource(R.string.progress_record), ChipKind.ACCENT)
                        }
                        StepButton(BaIcons.X, stringResource(R.string.delete), enabled = true) { onDelete(card.metric.id, r.date) }
                    }
                }
            }
        }
    }
}

/** The weekly weigh-in: pre-filled with the last weight, ±0.1 per tap (flows C2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighSheet(card: MetricCard, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    val c = Ba.colors
    val last = card.readings.maxByOrNull { it.date }
    var value by remember { mutableStateOf(last?.value ?: 70.0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(stringResource(R.string.weigh_title), style = Ba.type.title, color = c.text1)
            Text(stringResource(R.string.weight_help), style = Ba.type.caption, color = c.text2)
            DecimalStepper(value, card.metric.unit, onChange = { value = it })
            last?.let { Text(stringResource(R.string.weight_prev, PersianDigits.format(it.value), jalaliDayMonth(it.date)), style = Ba.type.caption, color = c.text3) }
            PrimaryButton(stringResource(R.string.save), onClick = { onSave(value) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A big number with − / + (0.1 per tap); tapping the number lets it be typed. */
@Composable
fun DecimalStepper(value: Double, unit: app.beaesthetic.domain.progress.MetricUnit, onChange: (Double) -> Unit, step: Double = 0.1) {
    val c = Ba.colors
    var typing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StepButton(BaIcons.Minus, stringResource(R.string.cd_less), enabled = value - step >= 0) { onChange(Progress.round(value - step)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
            if (typing) {
                InputBox(text, PersianDigits.format(value), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), onDone = {
                    DecimalInput.parse(text)?.let(onChange)
                    typing = false
                }) { t ->
                    text = t
                    DecimalInput.parse(t)?.let(onChange)
                }
            } else {
                Text(
                    PersianDigits.format(value),
                    style = Ba.type.display,
                    color = c.text1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { text = ""; typing = true }
                        .semantics { contentDescription = PersianDigits.format(value) },
                )
            }
            Text(stringResource(unitName(unit)), style = Ba.type.caption, color = c.text3)
        }
        StepButton(BaIcons.Plus, stringResource(R.string.cd_more), enabled = true) { onChange(Progress.round(value + step)) }
    }
}
