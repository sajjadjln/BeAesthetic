package app.lantern.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import app.lantern.R
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Jalali
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

