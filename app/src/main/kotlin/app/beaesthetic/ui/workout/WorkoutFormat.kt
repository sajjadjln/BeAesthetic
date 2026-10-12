package app.beaesthetic.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.beaesthetic.R
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.PlanItem

/** «۴ × ۸–۱۲», «۳ × ۳۰–۴۵ ث», «۲ × حداکثر» (screens.html 10). */
@Composable
fun setsChip(item: PlanItem): String {
    val sets = PersianDigits.format(item.sets)
    if (item.isMax) return stringResource(R.string.chip_max, sets)
    val range = stringResource(R.string.chip_range, sets, PersianDigits.format(item.rangeMin!!), PersianDigits.format(item.rangeMax!!))
    return if (item.exercise.kind == ExerciseKind.SECONDS) range + " " + stringResource(R.string.unit_seconds_short) else range
}

/** The rest of a plan row in one line: «۸٫۵ کیلو، هر طرف، سختی ۹، استراحت ۲ دقیقه». Empty when there's nothing. */
@Composable
fun planDetails(item: PlanItem): String = listOfNotNull(
    item.kg?.let { kgText(it) },
    if (item.perSide) stringResource(R.string.ex_per_side) else null,
    item.targetEffort?.let { stringResource(R.string.ex_effort_value, PersianDigits.format(it)) },
    item.restSeconds?.let { stringResource(R.string.ex_rest) + " " + restText(it) },
).joinToString(SEP)

/**
 * «، » between values and parts. Not «·»: next to Persian digits it looks just like «۰», so «۱۰ · ۱۰» read as
 * one number. The right-to-left mark keeps the comma out of the digits' run.
 */
const val SEP = "\u200F، "

@Composable
fun kgText(kg: Double): String = stringResource(R.string.kg_value, PersianDigits.format(kg))

/** «۴۵ ثانیه», «۲ دقیقه», «۹۰ ثانیه». */
@Composable
fun restText(seconds: Int): String =
    if (seconds % 60 == 0) stringResource(R.string.rest_minutes, PersianDigits.format(seconds / 60))
    else stringResource(R.string.rest_seconds, PersianDigits.format(seconds))
