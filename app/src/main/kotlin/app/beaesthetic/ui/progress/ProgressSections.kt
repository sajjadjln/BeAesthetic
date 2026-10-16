package app.beaesthetic.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.beaesthetic.R
import app.beaesthetic.domain.progress.DecimalInput
import app.beaesthetic.domain.progress.LadderView
import app.beaesthetic.domain.progress.Metric
import app.beaesthetic.domain.progress.MilestoneView
import app.beaesthetic.domain.progress.Milestones
import app.beaesthetic.domain.progress.ProgramLine
import app.beaesthetic.domain.progress.Totals
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.ErrorLine
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.Labeled
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.PickPill
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import java.time.LocalDate

/** D51: a thin line from Day 0 to the finish, chapter starts as ticks, today as an accent dot. RTL runs right → left. */
@Composable
fun ProgramLineCard(line: ProgramLine) {
    val c = Ba.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.line_title), style = Ba.type.label, color = c.text2, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.dash_day_of, PersianDigits.format(line.dayNumber), PersianDigits.format(line.totalDays)), style = Ba.type.caption, color = c.text3)
        }
        Canvas(
            Modifier.fillMaxWidth().height(24.dp).padding(top = 8.dp)
                .semantics { contentDescription = "${line.dayNumber} / ${line.totalDays}" },
        ) {
            val y = size.height / 2
            fun x(f: Float) = if (rtl) size.width * (1 - f) else size.width * f
            drawLine(c.border, Offset(0f, y), Offset(size.width, y), 4.dp.toPx(), StrokeCap.Round)
            drawLine(c.accent, Offset(x(0f), y), Offset(x(line.today), y), 4.dp.toPx(), StrokeCap.Round)
            line.chapterTicks.forEach { drawLine(c.text3, Offset(x(it), y - 6.dp.toPx()), Offset(x(it), y + 6.dp.toPx()), 1.5.dp.toPx()) }
            drawCircle(c.accent, 6.dp.toPx(), Offset(x(line.today), y))
        }
        Row {
            Text(stringResource(R.string.progress_day0_title), style = Ba.type.caption, color = c.text3, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.line_end), style = Ba.type.caption, color = c.text3)
        }
    }
}

/** D71: big numbers that only go up. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TotalsCard(totals: Totals) {
    val c = Ba.colors
    if (totals.workouts == 0 && totals.daysShownUp == 0) return
    SectionTitle(stringResource(R.string.totals_title))
    BaCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Big(PersianDigits.grouped(totals.daysShownUp), stringResource(R.string.dash_shown_up))
            Big(PersianDigits.grouped(totals.workouts), stringResource(R.string.totals_workouts))
            totals.perLadder.take(4).forEach { t ->
                val value = if (t.kind == ExerciseKind.SECONDS) PersianDigits.grouped(t.total / 60) else PersianDigits.grouped(t.total)
                val unit = if (t.kind == ExerciseKind.SECONDS) stringResource(R.string.totals_minutes, t.name) else t.name
                Big(value, unit)
            }
        }
    }
}

@Composable
private fun Big(value: String, label: String) {
    Column {
        Text(value, style = Ba.type.headline, color = Ba.colors.text1)
        Text(label, style = Ba.type.caption, color = Ba.colors.text2, maxLines = 1)
    }
}

/** D70: one row per ladder, rungs as small steps: reached = accent, current = outlined, the rest = text-3. */
@Composable
fun LaddersCard(ladders: List<LadderView>) {
    val c = Ba.colors
    if (ladders.isEmpty()) return
    SectionTitle(stringResource(R.string.ladders_title))
    BaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ladders.forEach { l ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        val name = l.rungs[l.current ?: l.reached ?: 0].name
                        Text(name, style = Ba.type.subtitle, color = c.text1, maxLines = 1)
                        Text(
                            stringResource(R.string.ladder_rung, PersianDigits.format((l.current ?: l.reached ?: 0) + 1), PersianDigits.format(l.rungs.size)),
                            style = Ba.type.caption, color = c.text3,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        l.rungs.indices.forEach { i ->
                            val reached = l.reached.let { r -> r != null && i <= r }
                            Box(
                                Modifier.width(14.dp).height((10 + i * 6).dp).clip(RoundedCornerShape(3.dp))
                                    .background(if (reached) c.accent else c.surface2)
                                    .then(if (i == l.current) Modifier.border(1.5.dp, c.accent, RoundedCornerShape(3.dp)) else Modifier),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** D65: reached ones in the accent with the date, open ones with plain progress. No badges. */
@Composable
fun MilestonesCard(milestones: List<MilestoneView>, metrics: List<Metric>, onToggle: (MilestoneView) -> Unit, onEdit: (MilestoneView?) -> Unit) {
    val c = Ba.colors
    SectionTitle(stringResource(R.string.milestones_title))
    BaCard(padding = PaddingValues(vertical = 4.dp)) {
        milestones.forEach { v ->
            ListRow(onClick = { onEdit(v) }) {
                val done = v.reachedOn != null
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(if (done) c.accent else c.surface2)
                        .clickable(enabled = v.milestone.metricId == null) { onToggle(v) },
                ) { if (done) Icon(BaIcons.Check, null, tint = c.onAccent, modifier = Modifier.size(16.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(v.milestone.title, style = Ba.type.subtitle, color = if (done) c.onAccentSubtle else c.text1)
                    val metric = metrics.firstOrNull { it.id == v.milestone.metricId }
                    val sub = when {
                        done -> jalaliDayMonth(v.reachedOn!!)
                        metric != null && v.milestone.target != null ->
                            stringResource(R.string.milestone_progress, v.best?.let { PersianDigits.format(it) } ?: "—", valueText(metric.unit, v.milestone.target!!))
                        else -> null
                    }
                    sub?.let { Text(it, style = Ba.type.caption, color = c.text3) }
                }
            }
        }
        if (milestones.size < Milestones.MAX) {
            ListRow(onClick = { onEdit(null) }) {
                Icon(BaIcons.Plus, null, tint = c.text2, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.milestone_add), style = Ba.type.body, color = c.text2)
            }
        }
    }
}

/** A new milestone or an edit: a title and, optionally, a metric with a target (then it ticks itself). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MilestoneSheet(
    editing: MilestoneView?,
    metrics: List<Metric>,
    onSave: (String, Long?, Double?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val c = Ba.colors
    val m = editing?.milestone
    var title by remember { mutableStateOf(m?.title ?: "") }
    var metricId by remember { mutableStateOf(m?.metricId) }
    var target by remember { mutableStateOf(m?.target?.let { PersianDigits.format(it) } ?: "") }
    var problem by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(stringResource(if (m == null) R.string.milestone_new else R.string.metric_form_edit), style = Ba.type.title, color = c.text1)
            if (problem > 0) ErrorLine(R.string.milestone_title_empty, problem)
            Labeled(R.string.milestone_title_label) { InputBox(title, stringResource(R.string.milestone_title_hint), onChange = { title = it.take(60) }) }
            Labeled(R.string.milestone_metric) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickPill(stringResource(R.string.milestone_by_hand), metricId == null) { metricId = null }
                    metrics.forEach { metric -> PickPill(metric.name, metricId == metric.id) { metricId = metric.id } }
                }
            }
            val metric = metrics.firstOrNull { it.id == metricId }
            if (metric != null) {
                Labeled(R.string.milestone_target) {
                    InputBox(target, stringResource(unitName(metric.unit)), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), onChange = { target = it })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(stringResource(R.string.save), modifier = Modifier.weight(1f), onClick = {
                    val t = if (metric != null) DecimalInput.parse(target) else null
                    if (title.isBlank() || (metric != null && t == null)) problem++ else onSave(title, metric?.id, t)
                })
                onDelete?.let { SecondaryButton(stringResource(R.string.delete), onClick = it) }
            }
        }
    }
}

/** Sessions done before the app, or not logged: past plan days from Day 0 (workout backfill). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackfillSheet(days: List<LocalDate>, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(stringResource(R.string.backfill_title), style = Ba.type.title, color = c.text1)
            Text(stringResource(R.string.backfill_help), style = Ba.type.caption, color = c.text2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { d -> PickPill(weekdays[Weekdays.index(d.dayOfWeek)] + " " + jalaliDayMonth(d), false) { onPick(d) } }
            }
        }
    }
}
