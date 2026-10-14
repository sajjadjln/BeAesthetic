package app.beaesthetic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.beaesthetic.R
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.JalaliMonth
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import java.time.LocalDate

/**
 * One Jalali month at a time (D129): Saturday on the right, the next month to the left, today ringed, past
 * days dimmed and not pickable (an already [selected] past day stays shown). Material's date picker is
 * Gregorian only, hence our own grid. Months run from today's to [monthsAhead] after it.
 */
@Composable
fun JalaliCalendar(selected: LocalDate?, today: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier, monthsAhead: Int = 12) {
    val c = Ba.colors
    val first = JalaliMonth.of(minOf(today, selected ?: today))
    val last = JalaliMonth.of(today).plus(monthsAhead)
    var month by remember { mutableStateOf(JalaliMonth.of(selected ?: today)) }
    val months = stringArrayResource(R.array.jalali_months)
    val weekdayLetters = stringArrayResource(R.array.weekdays_short)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            MonthButton(BaIcons.ChevronBackward, stringResource(R.string.cd_prev_month), enabled = month != first) { month = month.plus(-1) }
            Text(
                months[month.month - 1] + " " + PersianDigits.format(month.year),
                style = Ba.type.subtitle,
                color = c.text1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            MonthButton(BaIcons.ChevronForward, stringResource(R.string.cd_next_month), enabled = month != last) { month = month.plus(1) }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdayLetters.forEach { letter ->
                Text(letter, style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val cells = List(month.leadingBlanks) { null } + (1..month.length).map { month.date(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) { if (date != null) DayCell(date, selected, today, onPick) }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
        if (selected != null) {
            val weekdays = stringArrayResource(R.array.weekdays)
            val gregorian = stringResource(
                R.string.calendar_gregorian,
                PersianDigits.format(selected.dayOfMonth),
                stringArrayResource(R.array.gregorian_months)[selected.monthValue - 1],
            )
            Text(
                stringResource(R.string.calendar_picked, weekdays[Weekdays.index(selected.dayOfWeek)], jalaliDayMonth(selected), gregorian),
                style = Ba.type.caption,
                color = c.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: LocalDate?, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val c = Ba.colors
    val isSelected = date == selected
    val past = date < today
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) c.accent else c.surface2)
            .then(if (date == today && !isSelected) Modifier.border(1.5.dp, c.accent, CircleShape) else Modifier)
            .selectable(isSelected, enabled = !past, role = Role.RadioButton) { onPick(date) },
    ) {
        Text(
            PersianDigits.format(Jalali.fromGregorian(date).day),
            style = Ba.type.label,
            color = when {
                isSelected -> c.onAccent
                past -> c.text3.copy(alpha = 0.5f)
                else -> c.text1
            },
        )
    }
}

@Composable
private fun MonthButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { Icon(icon, null, tint = if (enabled) c.text2 else c.text3.copy(alpha = 0.35f), modifier = Modifier.size(20.dp)) }
}
