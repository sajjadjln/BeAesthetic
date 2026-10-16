package app.beaesthetic.ui.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.domain.score.SpecialKind
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.domain.week.Win
import app.beaesthetic.ui.clockText
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.components.buddy.Piker
import app.beaesthetic.ui.dashboard.square
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import java.time.LocalDate

/** The line pools «پیکر» cheers from at the bottom of the review card (D82). */
fun Win.linesRes(): Int = when (this) {
    Win.RECORDS -> R.array.week_win_records
    Win.COMEBACKS -> R.array.week_win_comebacks
    Win.ROUTINES -> R.array.week_win_routines
    Win.WORKOUTS -> R.array.week_win_workouts
    Win.SHOWED_UP -> R.array.week_win_showed_up
}

@Composable
fun WeekRoute(vm: WeekViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { vm.clearJustStarted() } }
    WeekScreen(state, vm)
}

/**
 * 05 هفته (DESIGN 05, M8, D140). On Friday (or the Saturday after) the review comes first: the week's
 * squares, the score, wins only, the identity sentence and «شروع هفته N» right under it (the short path,
 * D62). The plan below is optional. On other days the tab is just this week's plan.
 */
@Composable
private fun WeekScreen(state: WeekUiState?, vm: WeekViewModel) {
    val c = Ba.colors
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        val s = state ?: return@Box
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column {
                Text(stringResource(R.string.week_title), style = Ba.type.headline, color = c.text1)
                Text(
                    stringResource(R.string.week_range, jalaliDayMonth(s.planWeek), jalaliDayMonth(s.planWeek.plusDays(6))),
                    style = Ba.type.label,
                    color = c.text2,
                )
            }
            s.justStarted?.let { StartedCard(it, s.hasBackupFolder) }
            val review = s.review
            if (review != null) {
                ReviewCard(review, s.today)
                Identity(s.identity, ask = true, onSave = vm::setIdentity)
                StartButton(s, vm::startWeek)
                if (!s.planAfterFinish) SectionTitle(stringResource(R.string.week_plan_optional, PersianDigits.format(s.planWeekNumber)))
            } else {
                if (s.identity != null) Identity(s.identity, ask = false, onSave = vm::setIdentity)
                if (s.justStarted == null) {
                    Text(stringResource(R.string.week_next_ritual, clockText(s.ritualMinute)), style = Ba.type.caption, color = c.text3)
                }
                if (!s.planAfterFinish) SectionTitle(stringResource(R.string.week_plan_title, PersianDigits.format(s.planWeekNumber)))
            }
            if (!s.planAfterFinish) {
                Specials(s, vm::setSpecial)
                Snacks(s, vm::setSnack)
                if (review != null) StartButton(s, vm::startWeek)
            }
        }
    }
}

@Composable
private fun StartButton(s: WeekUiState, onClick: () -> Unit) {
    val text = if (s.planAfterFinish) stringResource(R.string.week_close_last) else stringResource(R.string.week_start, PersianDigits.format(s.planWeekNumber))
    PrimaryButton(text, onClick, Modifier.fillMaxWidth(), icon = BaIcons.Play)
}

@Composable
private fun StartedCard(number: Int, hasFolder: Boolean) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accentSubtle).padding(16.dp),
    ) {
        Icon(BaIcons.CheckCircle, contentDescription = null, tint = c.onAccentSubtle, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.week_done_title, PersianDigits.format(number)), style = Ba.type.subtitle, color = c.onAccentSubtle)
            Text(
                stringResource(if (hasFolder) R.string.week_done_body else R.string.week_done_body_no_folder),
                style = Ba.type.caption,
                color = c.onAccentSubtle,
            )
        }
    }
}

/** The week's 7 squares, the score chip, the wins as chips, the best routines, and «پیکر» cheering one line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCard(r: ReviewUi, today: LocalDate) {
    val c = Ba.colors
    val w = r.wins
    BaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.week_review_title, PersianDigits.format(r.number)), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
            w.score?.let { Chip(stringResource(R.string.dash_score, PersianDigits.format(it)), if (it >= 80) ChipKind.ACCENT else ChipKind.NEUTRAL) }
        }
        Text(
            stringResource(R.string.week_range, jalaliDayMonth(r.weekStart), jalaliDayMonth(r.weekStart.plusDays(6))),
            style = Ba.type.caption,
            color = c.text3,
        )
        val short = stringArrayResource(R.array.weekdays_short)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
            w.days.forEachIndexed { i, status ->
                val date = r.weekStart.plusDays(i.toLong())
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.size(30.dp).then(if (date == today) Modifier.border(1.5.dp, c.accent, RoundedCornerShape(8.dp)) else Modifier).padding(3.dp),
                    ) {
                        if (status != null) Canvas(Modifier.fillMaxSize()) { square(status, Offset.Zero, size.minDimension, c, 1f) }
                    }
                    Text(short[i], style = Ba.type.caption, color = c.text3)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            if (w.shownUp > 0) Chip(stringResource(R.string.week_shown_up, PersianDigits.format(w.shownUp)), ChipKind.ACCENT, icon = BaIcons.Check)
            if (w.workouts > 0) Chip(stringResource(R.string.week_workouts, PersianDigits.format(w.workouts)), ChipKind.ACCENT, icon = BaIcons.Dumbbell)
            if (w.records > 0) Chip(stringResource(R.string.week_records, PersianDigits.format(w.records)), ChipKind.ACCENT, icon = BaIcons.Trend)
            if (w.comebacks > 0) Chip(stringResource(R.string.week_comebacks, PersianDigits.format(w.comebacks)), ChipKind.ACCENT, icon = BaIcons.Repeat)
        }
        if (w.bestRoutines.isNotEmpty()) {
            Text(stringResource(R.string.week_best_routines), style = Ba.type.label, color = c.text2, modifier = Modifier.padding(top = 14.dp))
            w.bestRoutines.forEach { routine ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                    Text(routine.title, style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
                    Text(
                        PersianDigits.format(routine.done) + " / " + PersianDigits.format(routine.due),
                        style = Ba.type.caption,
                        color = c.text3,
                    )
                }
            }
        }
        val lines = stringArrayResource(w.headline.linesRes())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 14.dp)) {
            Piker(Pose.CHEER, c.accent, r.outfit, Modifier.width(64.dp))
            Text(lines[r.lineIndex.coerceIn(0, lines.lastIndex)], style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
        }
    }
}

/** The identity sentence (D65): asked once in the first ritual, then shown under the card, editable. */
@Composable
private fun Identity(sentence: String?, ask: Boolean, onSave: (String) -> Unit) {
    val c = Ba.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    if (sentence != null && !editing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).clickable { editing = true }.padding(16.dp),
        ) {
            Text(sentence, style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
            Icon(BaIcons.Pen, contentDescription = stringResource(R.string.week_identity_edit), tint = c.text3, modifier = Modifier.size(18.dp))
        }
        return
    }
    if (!ask && !editing) return
    var text by rememberSaveable { mutableStateOf(sentence.orEmpty()) }
    BaCard {
        Text(stringResource(R.string.week_identity_title), style = Ba.type.subtitle, color = c.text1)
        Text(stringResource(R.string.week_identity_ask), style = Ba.type.caption, color = c.text2, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        InputBox(text, stringResource(R.string.week_identity_hint), onDone = { onSave(text); editing = false }) { text = it }
        SecondaryButton(
            stringResource(R.string.save),
            onClick = { onSave(text); editing = false },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            height = 44.dp,
        )
    }
}

/** «روزهای ویژه»: 7 day chips with «۲ روز ذخیره» (D65). A full week refuses the next one. */
@Composable
private fun Specials(s: WeekUiState, onSet: (LocalDate, SpecialKind?) -> Unit) {
    val c = Ba.colors
    BaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.week_special_days), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
            Chip(
                if (s.reserves > 0) stringResource(R.string.week_reserves, PersianDigits.format(s.reserves)) else stringResource(R.string.week_reserves_none),
                ChipKind.SPECIAL,
            )
        }
        Text(stringResource(R.string.week_special_help), style = Ba.type.caption, color = c.text2, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
            s.specials.forEach { chip -> DayChip(chip, canAdd = s.reserves > 0, onSet = onSet, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayChip(chip: SpecialChip, canAdd: Boolean, onSet: (LocalDate, SpecialKind?) -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val short = stringArrayResource(R.array.weekdays_short)
    val kinds = listOf(SpecialKind.TRAVEL to R.string.special_travel, SpecialKind.OFF to R.string.special_off, SpecialKind.SICK to R.string.special_sick)
    var menu by remember { mutableStateOf(false) }
    val marked = chip.kind != null
    val enabled = chip.editable && (marked || canAdd)
    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (marked) c.specialSubtle else c.surface2)
                .then(if (chip.isToday) Modifier.border(1.5.dp, c.accent, RoundedCornerShape(12.dp)) else Modifier)
                .clickable(enabled = enabled, role = Role.Button) { if (marked) onSet(chip.date, null) else menu = true }
                .padding(vertical = 8.dp),
        ) {
            val alpha = if (chip.editable) 1f else 0.45f
            Text(short[Weekdays.index(chip.date.dayOfWeek)], style = Ba.type.caption, color = (if (marked) c.onSpecialSubtle else c.text2).copy(alpha = alpha))
            Text(
                PersianDigits.format(Jalali.fromGregorian(chip.date).day),
                style = Ba.type.label,
                color = (if (marked) c.onSpecialSubtle else c.text1).copy(alpha = alpha),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            kinds.forEach { (kind, label) ->
                DropdownMenuItem(text = { Text(stringResource(label), style = Ba.type.body) }, onClick = { menu = false; onSet(chip.date, kind) })
            }
        }
    }
}

/** «میان‌وعده‌ها»: one row per day; closed rows show the day and a dot (filled = has a note). */
@Composable
private fun Snacks(s: WeekUiState, onSet: (LocalDate, String) -> Unit) {
    val c = Ba.colors
    val days = stringArrayResource(R.array.weekdays)
    BaCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
        Text(stringResource(R.string.week_snacks), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        s.snacks.forEach { row ->
            var open by rememberSaveable(row.date) { mutableStateOf(false) }
            // Seeded once: the database echo of each keystroke must not move the cursor.
            var text by rememberSaveable(row.date) { mutableStateOf(row.text) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    stringResource(R.string.week_day_label, days[Weekdays.index(row.date.dayOfWeek)], jalaliDayMonth(row.date)),
                    style = Ba.type.body,
                    color = c.text1,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(10.dp).clip(CircleShape)
                        .then(if (text.isNotBlank()) Modifier.background(c.accent) else Modifier.border(1.5.dp, c.border, CircleShape)),
                )
                Icon(
                    if (open) BaIcons.ChevronDown else BaIcons.ChevronForward,
                    contentDescription = null,
                    tint = c.text3,
                    modifier = Modifier.padding(start = 10.dp).size(18.dp),
                )
            }
            AnimatedVisibility(open) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it; onSet(row.date, it) },
                    textStyle = Ba.type.body.copy(color = c.text1),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(14.dp)) {
                            if (text.isEmpty()) Text(stringResource(R.string.week_snack_hint), style = Ba.type.body, color = c.text3)
                            inner()
                        }
                    },
                )
            }
        }
    }
}
