package app.beaesthetic.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.domain.score.Dashboard
import app.beaesthetic.domain.score.DayCounts
import app.beaesthetic.domain.score.DaySnapshot
import app.beaesthetic.domain.score.DayStatus
import app.beaesthetic.domain.score.SpecialKind
import app.beaesthetic.domain.score.WeekRow
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.PickPill
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.ui.theme.BaColors
import app.beaesthetic.ui.theme.PeykarTheme
import app.beaesthetic.ui.theme.ThemeMode
import app.beaesthetic.ui.workout.SEP
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DashboardActions(
    val select: (LocalDate) -> Unit = {},
    val closeDetail: () -> Unit = {},
    val setSpecial: (SpecialKind?) -> Unit = {},
    val toggleYesterday: (Long) -> Unit = {},
)

@Composable
fun DashboardRoute(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    DashboardScreen(state, detail, DashboardActions(viewModel::select, viewModel::closeDetail, viewModel::setSpecial, viewModel::toggleYesterday))
}

// ---------------------------------------------------------------- 02 Dashboard

@Composable
fun DashboardScreen(state: DashboardUiState?, detail: DayDetail?, actions: DashboardActions) {
    val c = Ba.colors
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        val s = state ?: return@Box
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column {
                Text(stringResource(R.string.dash_title), style = Ba.type.headline, color = c.text1)
                val year = PersianDigits.format(Jalali.fromGregorian(s.calendar.endDate).year)
                Text(
                    stringResource(R.string.dash_range, jalaliDayMonth(s.calendar.day0), jalaliDayMonth(s.calendar.endDate), year),
                    style = Ba.type.label,
                    color = c.text2,
                )
            }
            // «روز حضور» and «روز ویژه». No weekly average (DESIGN 02).
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(
                    stringResource(R.string.dash_shown_up_value, PersianDigits.format(s.summary.shownUp), PersianDigits.format(s.summary.counted)),
                    stringResource(R.string.dash_shown_up),
                    Modifier.weight(1f),
                )
                Stat(PersianDigits.format(s.summary.special), stringResource(R.string.dash_special), Modifier.weight(1f), special = true)
            }
            BaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dash_whole), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.dash_day_of, PersianDigits.format(s.dayNumber), PersianDigits.format(s.totalDays)),
                        style = Ba.type.caption,
                        color = c.text3,
                    )
                }
                Grid(s, actions.select, Modifier.padding(top = 12.dp))
                Legend(Modifier.padding(top = 12.dp))
            }
            SectionTitle(stringResource(R.string.dash_weeks))
            BaCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    s.weeks.forEach { WeekLine(it, s.today, actions.select) }
                }
            }
        }
    }
    detail?.let { DaySheet(it, actions) }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier, special: Boolean = false) {
    val c = Ba.colors
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp)) {
        Text(value, style = Ba.type.headline, color = if (special) c.onSpecialSubtle else c.text1)
        Text(label, style = Ba.type.caption, color = c.text2)
    }
}

/**
 * «کل مسیر»: one column per week, the oldest on the right (time runs right → left in RTL), Saturday on top.
 * Drawn on a Canvas, which isn't mirrored, so x is counted from the right edge. The first open of the day
 * fills the squares in week by week (~0.4 s) and today's ring glows once (D76).
 */
@Composable
internal fun Grid(s: DashboardUiState, onTap: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val cols = s.columns.size
    val fill = remember { Animatable(if (s.animate) 0f else 1f) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(s.animate) {
        if (s.animate) {
            fill.animateTo(1f, tween(400, easing = LinearEasing))
            glow.animateTo(1f, tween(300))
            glow.animateTo(0f, tween(500))
        }
    }
    val months = stringArrayResource(R.array.jalali_months)
    val description = stringResource(R.string.dash_whole) + SEP +
        stringResource(R.string.dash_day_of, PersianDigits.format(s.dayNumber), PersianDigits.format(s.totalDays))
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 3.dp
        val cell: Dp = (maxWidth - gap * (cols - 1)) / cols
        val height = cell * 7 + gap * 6
        Column {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .semantics { contentDescription = description }
                    .pointerInput(s.columns) {
                        detectTapGestures { p ->
                            val step = (cell + gap).toPx()
                            val col = ((size.width - p.x) / step).toInt()
                            val row = (p.y / step).toInt()
                            val day = s.columns.getOrNull(col)?.first?.plusDays(row.toLong()) ?: return@detectTapGestures
                            if (row in 0..6 && s.columns[col].second[row] != null) onTap(day)
                        }
                    },
            ) {
                val step = (cell + gap).toPx()
                val side = cell.toPx()
                s.columns.forEachIndexed { col, (start, days) ->
                    // Columns appear right → left as the fill runs.
                    val alpha = (fill.value * cols - col).coerceIn(0f, 1f)
                    if (alpha <= 0f) return@forEachIndexed
                    val x = size.width - (col + 1) * side - col * gap.toPx()
                    days.forEachIndexed { row, status ->
                        status ?: return@forEachIndexed
                        val topLeft = Offset(x, row * step)
                        square(status, topLeft, side, c, alpha)
                        if (start.plusDays(row.toLong()) == s.today) {
                            drawRoundRect(
                                c.accent.copy(alpha = alpha),
                                topLeft - Offset(2.dp.toPx(), 2.dp.toPx()),
                                Size(side + 4.dp.toPx(), side + 4.dp.toPx()),
                                CornerRadius(4.dp.toPx()),
                                style = Stroke((1.5f + 2f * glow.value).dp.toPx()),
                            )
                        }
                    }
                }
            }
            // Jalali month labels under the column where each month starts.
            Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(18.dp)) {
                var last = -1
                s.columns.forEachIndexed { col, (start, days) ->
                    val first = days.indexOfFirst { it != null }.takeIf { it >= 0 } ?: return@forEachIndexed
                    val month = Jalali.fromGregorian(start.plusDays(first.toLong())).month
                    if (month != last) {
                        last = month
                        Text(
                            months[month - 1],
                            style = Ba.type.caption,
                            color = c.text3,
                            maxLines = 1,
                            // offset follows the layout direction: in RTL it moves left from the right edge.
                            modifier = Modifier.offset(x = (cell + gap) * col),
                        )
                    }
                }
            }
        }
    }
}

internal fun DrawScope.square(status: DayStatus, topLeft: Offset, side: Float, c: BaColors, alpha: Float) {
    val radius = CornerRadius(side * 0.22f)
    when (status) {
        is DayStatus.Level -> drawRoundRect(c.grid[status.level.coerceIn(0, 4)].copy(alpha = alpha), topLeft, Size(side, side), radius)
        DayStatus.Special -> drawRoundRect(c.special.copy(alpha = alpha), topLeft, Size(side, side), radius)
        DayStatus.BeforeApp -> {
            val w = 1.dp.toPx()
            drawRoundRect(
                c.border.copy(alpha = alpha),
                topLeft + Offset(w / 2, w / 2),
                Size(side - w, side - w),
                radius,
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))),
            )
        }
        DayStatus.Future -> {
            val w = 1.dp.toPx()
            drawRoundRect(c.border.copy(alpha = alpha * 0.6f), topLeft + Offset(w / 2, w / 2), Size(side - w, side - w), radius, style = Stroke(w))
        }
    }
}

/** less → more, special, «قبل از اپ» (D56). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val c = Ba.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.dash_less), style = Ba.type.caption, color = c.text3)
        (0..4).forEach { LegendSquare(DayStatus.Level(it)) }
        Text(stringResource(R.string.dash_more), style = Ba.type.caption, color = c.text3)
        Box(Modifier.size(10.dp))
        LegendSquare(DayStatus.Special)
        Text(stringResource(R.string.dash_legend_special), style = Ba.type.caption, color = c.text3)
        Box(Modifier.size(10.dp))
        LegendSquare(DayStatus.BeforeApp)
        Text(stringResource(R.string.dash_before_app), style = Ba.type.caption, color = c.text3)
    }
}

@Composable
private fun LegendSquare(status: DayStatus) {
    val c = Ba.colors
    Canvas(Modifier.size(12.dp)) { square(status, Offset.Zero, size.minDimension, c, 1f) }
}

/** «هفته ۵» with its dates, 7 squares, and the score chip («جاری» while it runs; accent from 80 %). */
@Composable
private fun WeekLine(week: WeekRow, today: LocalDate, onTap: (LocalDate) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                if (week.number == 0) stringResource(R.string.dash_week0) else stringResource(R.string.dash_week, PersianDigits.format(week.number)),
                style = Ba.type.label,
                color = c.text1,
            )
            Text(
                stringResource(R.string.dash_week_range, jalaliDayMonth(week.start), jalaliDayMonth(week.start.plusDays(6))),
                style = Ba.type.caption,
                color = c.text3,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.days.forEachIndexed { i, status ->
                val date = week.start.plusDays(i.toLong())
                Box(
                    Modifier
                        .size(18.dp)
                        .then(if (date == today) Modifier.border(1.5.dp, c.accent, RoundedCornerShape(5.dp)) else Modifier)
                        .pointerInput(date) { detectTapGestures { if (status != null) onTap(date) } },
                ) {
                    if (status != null) Canvas(Modifier.size(18.dp).padding(2.dp)) { square(status, Offset.Zero, size.minDimension, c, 1f) }
                }
            }
        }
        val score = week.score
        when {
            week.current -> Chip(stringResource(R.string.dash_current), ChipKind.NEUTRAL)
            score == null -> Chip(stringResource(R.string.none_dash), ChipKind.NEUTRAL)
            else -> Chip(stringResource(R.string.dash_score, PersianDigits.format(score)), if (score >= 80) ChipKind.ACCENT else ChipKind.NEUTRAL)
        }
    }
}

// ---------------------------------------------------------------- D1 Day sheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DaySheet(d: DayDetail, actions: DashboardActions) {
    val c = Ba.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingYesterday by remember(d.date) { mutableStateOf(false) }
    val weekdays = stringArrayResource(R.array.weekdays)
    val gregorianMonths = stringArrayResource(R.array.gregorian_months)
    ModalBottomSheet(onDismissRequest = actions.closeDetail, sheetState = sheet, containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            // «دیروز · دوشنبه ۱۸ آبان», then «۹ نوامبر ۲۰۲۶ · هفته ۵».
            val relative = when (d.date) {
                d.today -> stringResource(R.string.today) + " · "
                d.today.minusDays(1) -> stringResource(R.string.yesterday) + " · "
                else -> ""
            }
            Text(relative + weekdays[Weekdays.index(d.date.dayOfWeek)] + " " + jalaliDayMonth(d.date), style = Ba.type.title, color = c.text1)
            val gregorian = PersianDigits.format(d.date.dayOfMonth) + " " + gregorianMonths[d.date.monthValue - 1] + " " + PersianDigits.format(d.date.year)
            Text(gregorian + SEP + stringResource(R.string.detail_week, PersianDigits.format(d.week)), style = Ba.type.caption, color = c.text3)

            if (d.status == DayStatus.BeforeApp) {
                Text(stringResource(R.string.detail_before_app), style = Ba.type.body, color = c.text2)
                return@Column
            }

            // The level as words and a number, so the grid isn't color-only (D68).
            // A gap is just «چیزی ثبت نشد», in a neutral color: no «۰ از ۱۵», which would read as a list of misses.
            val (levelLine, levelColor) = when (val st = d.status) {
                is DayStatus.Level -> if (st.level == 0) {
                    stringResource(R.string.level_gap) to c.text2
                } else {
                    val counts = d.counts
                    val level = stringResource(R.string.level_words, PersianDigits.format(st.level))
                    val line = if (counts != null && counts.due > 0) {
                        stringResource(R.string.detail_counts, PersianDigits.format(counts.done), PersianDigits.format(counts.due)) + SEP + level
                    } else {
                        level
                    }
                    line to c.onAccentSubtle
                }
                DayStatus.Special -> stringResource(R.string.special_day_kind, d.special?.let { specialName(it) } ?: stringResource(R.string.dash_legend_special)) to c.onSpecialSubtle
                else -> "" to c.text2
            }
            if (levelLine.isNotEmpty()) Text(levelLine, style = Ba.type.subtitle, color = levelColor)
            val parts = listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night).map { stringResource(it) }

            // Only what was done (ARCHITECTURE §8): never a list of what wasn't.
            if (d.done.isNotEmpty() || d.records.isNotEmpty()) {
                Text(stringResource(R.string.detail_done), style = Ba.type.label, color = c.text2)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val repeated = d.done.groupingBy { it.title }.eachCount().filterValues { it > 1 }.keys
                    d.done.forEach { Chip(if (it.title in repeated) it.title + " " + parts[it.part.ordinal] else it.title, ChipKind.ACCENT) }
                    d.records.forEach { Chip(stringResource(R.string.detail_record, it), ChipKind.ACCENT) }
                }
            }

            if (d.editable) {
                Text(stringResource(R.string.detail_special_q), style = Ba.type.label, color = c.text2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecialKind.entries.forEach { kind ->
                        val on = d.special == kind
                        val enabled = on || d.special != null || d.canMarkSpecial
                        PickPill(specialName(kind), selected = on) { if (enabled) actions.setSpecial(kind) }
                    }
                }
                if (d.special == null && !d.canMarkSpecial) {
                    Text(stringResource(R.string.detail_special_full), style = Ba.type.caption, color = c.amber)
                }
            }

            if (d.date == d.today.minusDays(1) && d.yesterdayItems.isNotEmpty()) {
                if (!editingYesterday) {
                    TextButton(onClick = { editingYesterday = true }) {
                        Text(stringResource(R.string.detail_edit_yesterday), style = Ba.type.label, color = c.accent)
                    }
                } else {
                    // D124: yesterday's due tasks as chips to tap on or off, without saying which were missed.
                    Text(stringResource(R.string.detail_yesterday_help), style = Ba.type.caption, color = c.text3)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // A name used in more than one part («مسواک» morning and night) gets its part.
                        val repeated = d.yesterdayItems.groupingBy { it.task.title }.eachCount().filterValues { it > 1 }.keys
                        d.yesterdayItems.forEach { item ->
                            val t = item.task
                            PickPill(if (t.title in repeated) t.title + " " + parts[t.part.ordinal] else t.title, selected = item.done, small = true) {
                                actions.toggleYesterday(t.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun specialName(kind: SpecialKind): String = stringResource(
    when (kind) {
        SpecialKind.SICK -> R.string.special_sick
        SpecialKind.TRAVEL -> R.string.special_travel
        SpecialKind.OFF -> R.string.special_off
    },
)

@Preview(locale = "fa", showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun DashboardPreview() {
    val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    val install = LocalDate.of(2026, 10, 10)
    val today = LocalDate.of(2026, 11, 9)
    val snapshots = (0L until ChronoUnit.DAYS.between(install, today)).associate { i ->
        install.plusDays(i) to DaySnapshot(DayCounts(10, ((i * 7) % 11).toInt().coerceAtMost(10), false), special = i == 12L)
    }
    val state = DashboardUiState(
        today, calendar, Dashboard.summary(today, install, calendar, snapshots),
        Dashboard.dayOf(calendar, today).first, Dashboard.dayOf(calendar, today).second,
        Dashboard.columns(calendar).map { start -> start to (0L..6L).map { Dashboard.cell(start.plusDays(it), calendar, today, install, snapshots) } },
        Dashboard.weeks(calendar, today, install, snapshots), animate = false,
    )
    PeykarTheme(mode = ThemeMode.DARK) { DashboardScreen(state, null, DashboardActions()) }
}
