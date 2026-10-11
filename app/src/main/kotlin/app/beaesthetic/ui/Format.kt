package app.beaesthetic.ui

import androidx.annotation.ArrayRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import app.beaesthetic.R
import app.beaesthetic.domain.buddy.Moment
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import java.time.LocalDate

/** «۱ آذر»: Jalali day and month, Persian digits. Gregorian comes second only where a full date is shown. */
@Composable
fun jalaliDayMonth(date: LocalDate): String {
    val j = Jalali.fromGregorian(date)
    return stringResource(R.string.date_day_month, PersianDigits.format(j.day), stringArrayResource(R.array.jalali_months)[j.month - 1])
}

/** «HH:mm» in Persian digits. */
fun clockText(minuteOfDay: Int): String =
    PersianDigits.format("%02d:%02d".format(java.util.Locale.ROOT, minuteOfDay / 60, minuteOfDay % 60))

/** The string array that holds each moment's lines (D84). */
@ArrayRes
fun Moment.linesRes(): Int = when (this) {
    Moment.MORNING -> R.array.buddy_morning
    Moment.START -> R.array.buddy_start
    Moment.TICK -> R.array.buddy_tick
    Moment.PART_DONE -> R.array.buddy_part_done
    Moment.DAY_DONE -> R.array.buddy_day_done
    Moment.COMEBACK -> R.array.buddy_comeback
    Moment.MINIMUM -> R.array.buddy_minimum
    Moment.SPECIAL -> R.array.buddy_special
    Moment.NIGHT -> R.array.buddy_night
    Moment.TAP -> R.array.buddy_tap
    Moment.CHAPTER_START -> R.array.chapter_lines
    Moment.LETTER_DAY -> R.array.buddy_letter_day
}
