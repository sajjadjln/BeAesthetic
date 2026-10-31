package app.lantern.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.R
import app.lantern.domain.score.DayStatus
import app.lantern.domain.score.Days
import app.lantern.domain.score.History
import app.lantern.domain.score.SpecialKind
import app.lantern.domain.score.TaskDay
import app.lantern.domain.tasks.Repeat
import app.lantern.domain.tasks.Task
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Jalali
import app.lantern.domain.time.Weekdays
import app.lantern.ui.clockText
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.TopBar
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.sky
import java.time.LocalDate
import java.time.ZoneId

private val CELL = 13.dp
private val GAP = 3.dp

/** «سابقه» / History (D198): the day grid, then each repeating task with a small strip of its own. */
@Composable
fun HistoryRoute(viewModel: HistoryViewModel, onBack: () -> Unit, onOpenTask: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<LocalDate?>(null) }
    val c = Ba.colors
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        TopBar(stringResource(R.string.history_title), onBack, stringResource(R.string.cd_back))
        val s = state ?: return@Column
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            BaCard {
                Text(stringResource(R.string.history_shown_up, PersianDigits.format(s.shownUp)), style = Ba.type.headline, color = c.glow)
                Text(stringResource(R.string.history_since, jalaliDayMonth(s.installDate)), style = Ba.type.caption, color = c.text2)
            }
            Section(stringResource(R.string.history_days))
            DayGrid(s.columns) { date ->
                val status = Days.status(date, s.today, s.installDate, s.snapshots[date])
                Cell(dayColor(status), status, date == s.today, onClick = { open = date }.takeIf { status is DayStatus.Level || status == DayStatus.Special })
            }
            Legend {
                LegendText(stringResource(R.string.history_less))
                (0..4).forEach { LegendCell(levelColor(it)) }
                LegendText(stringResource(R.string.history_more))
                LegendCell(c.special)
                LegendText(stringResource(R.string.history_special))
                BeforeAppDot()
                LegendText(stringResource(R.string.history_before_app))
            }
            Section(stringResource(R.string.history_repeating))
            if (s.repeating.isEmpty()) Text(stringResource(R.string.history_no_repeating), style = Ba.type.body, color = c.text3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.repeating.forEach { task -> TaskTile(task, s) { onOpenTask(task.id) } }
            }
        }
    }
    val s = state
    val date = open
    if (s != null && date != null) DaySheet(s, date, task = null, viewModel) { open = null }
}

/** One repeating task's own grid, counts and months (D198). */
@Composable
fun TaskHistoryRoute(viewModel: HistoryViewModel, taskId: Long, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<LocalDate?>(null) }
    val c = Ba.colors
    val s = state
    val task = s?.tasks?.get(taskId)
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        TopBar(stringResource(R.string.history_title), onBack, stringResource(R.string.cd_back))
        if (s == null || task == null) return@Column
        val done = s.doneByTask[taskId].orEmpty()
        val months = History.monthCounts(done, s.today, 6, jalali = !PersianDigits.latin)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(task.title, style = Ba.type.headline, color = c.text1)
            Text(repeatText(task), style = Ba.type.caption, color = c.text3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
                Fact(PersianDigits.format(done.size), stringResource(R.string.history_all_times), Modifier.weight(1f))
                Fact(PersianDigits.format(months.last().count), stringResource(R.string.history_this_month), Modifier.weight(1f))
            }
            Section(stringResource(R.string.history_task_days))
            DayGrid(s.columns) { date ->
                val day = History.taskDay(task, date, s.today, s.installDate, done)
                val color = when (day) {
                    TaskDay.DONE -> c.glow
                    TaskDay.GAP -> levelColor(0)
                    TaskDay.NOT_DUE -> Color.White.copy(alpha = 0.04f)
                    TaskDay.BEFORE_APP, TaskDay.FUTURE -> null
                }
                Cell(
                    color,
                    if (day == TaskDay.BEFORE_APP) DayStatus.BeforeApp else null,
                    date == s.today,
                    onClick = { open = date }.takeIf { day == TaskDay.DONE },
                )
            }
            Legend {
                LegendCell(c.glow)
                LegendText(stringResource(R.string.history_done))
                LegendCell(levelColor(0))
                LegendText(stringResource(R.string.history_gap))
                LegendCell(Color.White.copy(alpha = 0.04f), outline = true)
                LegendText(stringResource(R.string.history_not_due))
            }
            Section(stringResource(R.string.history_each_month))
            MonthBars(months.map { monthName(it.start) to it.count })
        }
    }
    if (s != null && task != null) open?.let { DaySheet(s, it, task, viewModel) { open = null } }
}

@Composable
private fun Section(text: String) {
    Text(text, style = Ba.type.label, color = Ba.colors.text3, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}

/**
 * Weeks as columns, Saturday on top, oldest first: the layout direction puts time right → left in Persian.
 * It opens scrolled to today's week; month names sit over the week a month starts in.
 */
@Composable
private fun DayGrid(columns: List<LocalDate>, cell: @Composable (LocalDate) -> Unit) {
    val dows = stringArrayResource(R.array.weekdays_short)
    // Starts at the far end; the state clamps it to the real end once measured.
    val scroll = rememberScrollState(Int.MAX_VALUE)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
            Spacer(Modifier.height(16.dp))
            (0..6).forEach { i ->
                Box(Modifier.height(CELL), contentAlignment = Alignment.Center) {
                    Text(if (i % 2 == 0) dows[i] else "", style = Ba.type.caption.copy(fontSize = Ba.type.caption.fontSize * 0.8f), color = Ba.colors.text3)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP), modifier = Modifier.weight(1f).horizontalScroll(scroll)) {
            columns.forEachIndexed { i, week ->
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    Box(Modifier.height(16.dp).width(CELL)) {
                        monthStartIn(week, first = i == 0)?.let { start ->
                            Text(
                                monthName(start),
                                style = Ba.type.caption.copy(fontSize = Ba.type.caption.fontSize * 0.8f),
                                color = Ba.colors.text2,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                            )
                        }
                    }
                    (0L..6L).forEach { cell(week.plusDays(it)) }
                }
            }
        }
    }
}

/** The day a month starts on within [week] (Jalali in Persian, Gregorian in English); the first column always gets one. */
private fun monthStartIn(week: LocalDate, first: Boolean): LocalDate? {
    val days = (0L..6L).map { week.plusDays(it) }
    val start = days.firstOrNull { d -> if (PersianDigits.latin) d.dayOfMonth == 1 else Jalali.fromGregorian(d).day == 1 }
    return start ?: if (first) week else null
}

@Composable
private fun monthName(date: LocalDate): String = if (PersianDigits.latin) {
    stringArrayResource(R.array.gregorian_months)[date.monthValue - 1]
} else {
    stringArrayResource(R.array.jalali_months)[Jalali.fromGregorian(date).month - 1]
}

/** Level 0 is the gray gap; 1–4 step toward the character's color (D198: done is the theme color here). */
@Composable
private fun levelColor(level: Int): Color {
    val c = Ba.colors
    return if (level <= 0) c.grid[0] else lerp(c.grid[0], c.glow, listOf(0.3f, 0.55f, 0.8f, 1f)[level.coerceAtMost(4) - 1])
}

@Composable
private fun dayColor(status: DayStatus): Color? = when (status) {
    is DayStatus.Level -> levelColor(status.level)
    DayStatus.Special -> Ba.colors.special
    DayStatus.BeforeApp, DayStatus.Future -> null
}

@Composable
private fun Cell(color: Color?, status: DayStatus?, isToday: Boolean, onClick: (() -> Unit)?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(CELL)
            .then(if (isToday) Modifier.border(1.5.dp, Ba.colors.text1, RoundedCornerShape(3.dp)) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        when {
            color != null -> Box(Modifier.size(if (isToday) CELL - 4.dp else CELL).clip(RoundedCornerShape(2.dp)).background(color))
            status == DayStatus.BeforeApp -> BeforeAppDot()
        }
    }
}

/** «قبل از اپ» (D56): a small ring, not a gap. */
@Composable
private fun BeforeAppDot() {
    Box(Modifier.size(6.dp).border(1.dp, Ba.colors.border, CircleShape))
}

@Composable
private fun Legend(content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 10.dp),
    ) { content() }
}

@Composable
private fun LegendCell(color: Color, outline: Boolean = false) {
    Box(
        Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(color)
            .then(if (outline) Modifier.border(1.dp, Ba.colors.border, RoundedCornerShape(2.dp)) else Modifier),
    )
}

@Composable
private fun LegendText(text: String) {
    Text(text, style = Ba.type.caption, color = Ba.colors.text3, modifier = Modifier.padding(horizontal = 2.dp))
}

/** A repeating task with its count and the last 12 weeks as a small strip. Tap → its own grid. */
@Composable
private fun TaskTile(task: Task, s: HistoryState, onClick: () -> Unit) {
    val c = Ba.colors
    val done = s.doneByTask[task.id].orEmpty()
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(BaIcons.of(task.icon), null, tint = c.text2, modifier = Modifier.size(20.dp))
            Text(task.title, style = Ba.type.tile, color = c.text1, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.history_times, PersianDigits.format(done.size)), style = Ba.type.label, color = c.glow)
        }
        Text(repeatText(task), style = Ba.type.caption, color = c.text3)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            s.columns.takeLast(12).forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0L..6L).forEach { d ->
                        val color = when (History.taskDay(task, week.plusDays(d), s.today, s.installDate, done)) {
                            TaskDay.DONE -> c.glow
                            TaskDay.GAP -> c.grid[0]
                            TaskDay.NOT_DUE -> Color.White.copy(alpha = 0.04f)
                            TaskDay.BEFORE_APP, TaskDay.FUTURE -> Color.Transparent
                        }
                        Box(Modifier.size(6.dp).background(color))
                    }
                }
            }
        }
    }
}

@Composable
private fun repeatText(task: Task): String = when (val r = task.repeat) {
    Repeat.Daily -> stringResource(R.string.repeat_daily)
    is Repeat.OnWeekdays -> stringArrayResource(R.array.weekdays).let { names -> (0..6).filter { r.bits and (1 shl it) != 0 }.joinToString("، ") { names[it] } }
    is Repeat.EveryNDays -> stringResource(R.string.repeat_every_n, PersianDigits.format(r.n))
    else -> ""
}

@Composable
private fun Fact(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(Px.Card).background(Ba.colors.surface).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = Ba.type.headline, color = Ba.colors.glow)
        Text(label, style = Ba.type.caption, color = Ba.colors.text2)
    }
}

/** Times done per month, oldest first; an empty month is a short gray bar, never a warning. */
@Composable
private fun MonthBars(months: List<Pair<String, Int>>) {
    val c = Ba.colors
    val max = months.maxOf { it.second }.coerceAtLeast(1)
    val tall: Dp = 64.dp
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        months.forEach { (name, n) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                if (n > 0) Text(PersianDigits.format(n), style = Ba.type.caption, color = c.text2)
                Box(Modifier.fillMaxWidth().height((tall * n / max).coerceAtLeast(3.dp)).background(if (n > 0) c.glow.copy(alpha = 0.75f) else c.grid[0]))
                Text(name, style = Ba.type.caption.copy(fontSize = Ba.type.caption.fontSize * 0.85f), color = c.text3, maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * Tap a square: only what was done that day, with the time (D37). Never what wasn't. For today and
 * yesterday the day can be made special here (sick, travel, off), within the weekly cap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(s: HistoryState, date: LocalDate, task: Task?, viewModel: HistoryViewModel, onDismiss: () -> Unit) {
    val c = Ba.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weekdays = stringArrayResource(R.array.weekdays)
    val done = s.doneByDate[date].orEmpty().filter { task == null || it.taskId == task.id }
    val special = s.specials[date]
    val names = mapOf(
        SpecialKind.SICK.name to stringResource(R.string.special_sick),
        SpecialKind.TRAVEL.name to stringResource(R.string.special_travel),
        SpecialKind.OFF.name to stringResource(R.string.special_off),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.surface2, shape = Px.Dialog) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(weekdays[Weekdays.index(date.dayOfWeek)] + " " + jalaliDayMonth(date), style = Ba.type.title, color = c.glow)
            Text(otherCalendar(date), style = Ba.type.caption, color = c.text3)
            special?.let { Text(names[it] ?: it, style = Ba.type.label, color = c.onSpecialSubtle) }
            val snap = s.snapshots[date]
            if (task == null && done.isNotEmpty() && snap != null && snap.counts.due > 0) {
                Text(stringResource(R.string.history_done_of, PersianDigits.format(snap.counts.done), PersianDigits.format(snap.counts.due)), style = Ba.type.label, color = c.text2)
            }
            if (done.isEmpty()) {
                Text(stringResource(R.string.history_nothing), style = Ba.type.body, color = c.text2, modifier = Modifier.padding(vertical = 8.dp))
            }
            done.forEach { row ->
                val t = s.tasks[row.taskId] ?: return@forEach
                val time = row.completedAt.atZone(ZoneId.systemDefault()).toLocalTime()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    Box(Modifier.size(10.dp).background(c.glow))
                    Text(t.title, style = Ba.type.tile, color = c.text1, modifier = Modifier.weight(1f))
                    Text(clockText(time.hour * 60 + time.minute), style = Ba.type.caption, color = c.text3)
                }
            }
            val editable = task == null && !date.isBefore(s.today.minusDays(1))
            if (editable && viewModel.canMarkSpecial(s, date)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
                    Text(stringResource(R.string.history_set_special), style = Ba.type.label, color = c.text2)
                    SpecialKind.entries.forEach { kind ->
                        val on = special == kind.name
                        Text(
                            names.getValue(kind.name),
                            style = Ba.type.label,
                            color = if (on) c.onSpecialSubtle else c.text2,
                            modifier = Modifier.clip(Px.Small)
                                .background(if (on) c.specialSubtle else c.surface)
                                .clickable(role = Role.Button) { viewModel.setSpecial(date, kind.name, special) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** The second calendar under a date: Gregorian in Persian, Jalali in English (dates rule in CLAUDE.md). */
@Composable
private fun otherCalendar(date: LocalDate): String = if (PersianDigits.latin) {
    val j = Jalali.fromGregorian(date)
    "${j.day} ${stringArrayResource(R.array.jalali_months)[j.month - 1]} ${j.year}"
} else {
    "${date.dayOfMonth} ${stringArrayResource(R.array.gregorian_months)[date.monthValue - 1]} ${date.year}"
}
