package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.ProgramCalendar
import java.time.DayOfWeek
import java.time.LocalDate

enum class MetricKind { BODY, TAPE, STRENGTH }

enum class MetricUnit { KG, CM, REPS, SECONDS }

/** Something measured over the program (SPEC M5, M7): weight, a tape site or a strength test (D106). */
data class Metric(
    val id: Long,
    val kind: MetricKind,
    /** A [Suggestions] key for the suggested ones (where-to-measure help, the weight card); null for the user's own. */
    val key: String?,
    val name: String,
    val unit: MetricUnit,
    val higherIsBetter: Boolean,
)

/** One reading. Day 0 readings carry the Day 0 date. [guess] = the strength-test guess (D73). */
data class Reading(val date: LocalDate, val value: Double, val guess: Double? = null)

/** How a change is shown. Never a "worse" color: a change the wrong way is just neutral (no shame). */
enum class Trend { BETTER, NEUTRAL }

/** First and latest reading of a metric (DESIGN 03: value, delta chip, starting value). */
data class SinceDay0(val start: Reading, val latest: Reading, val delta: Double, val trend: Trend)

enum class GuessOutcome {
    /** No guess, so no comparison (D73). */
    NONE,

    /** «از حدست بهتر». */
    BEAT,

    /** Both numbers, neutral. */
    SHOWN,
}

object Progress {

    /**
     * Since the first reading. Weight is never better or worse (DESIGN C2: weight isn't scored), and with
     * one reading there is no change yet. Readings may come in any order.
     */
    fun sinceDay0(metric: Metric, readings: List<Reading>): SinceDay0? {
        if (readings.isEmpty()) return null
        val sorted = readings.sortedBy { it.date }
        val start = sorted.first()
        val latest = sorted.last()
        val delta = round(latest.value - start.value)
        val better = delta != 0.0 && (delta > 0) == metric.higherIsBetter && metric.kind != MetricKind.BODY
        return SinceDay0(start, latest, delta, if (better) Trend.BETTER else Trend.NEUTRAL)
    }

    /** The best reading so far, or null with none. */
    fun best(metric: Metric, readings: List<Reading>): Reading? =
        if (metric.higherIsBetter) readings.maxByOrNull { it.value } else readings.minByOrNull { it.value }

    /** A strength test beat every earlier reading (D54 for tests). The first reading is never a record. */
    fun isRecord(metric: Metric, value: Double, earlier: List<Reading>): Boolean {
        if (metric.kind != MetricKind.STRENGTH || earlier.isEmpty()) return false
        val best = best(metric, earlier)!!.value
        return if (metric.higherIsBetter) value > best else value < best
    }

    /** D73 / SPEC §6.7: beating the guess is «از حدست بهتر»; anything else shows both numbers, neutral. */
    fun guess(metric: Metric, guess: Double?, result: Double): GuessOutcome = when {
        guess == null -> GuessOutcome.NONE
        if (metric.higherIsBetter) result > guess else result < guess -> GuessOutcome.BEAT
        else -> GuessOutcome.SHOWN
    }

    /**
     * The weekly weigh-in (Q4 of slice 7): on Friday, when weight is tracked and nothing was logged this
     * program week (Saturday to Friday). A check-in asks for weight itself, so it replaces the row.
     */
    fun weighInDue(today: LocalDate, tracksWeight: Boolean, weightDates: List<LocalDate>, checkInOpen: Boolean): Boolean {
        if (!tracksWeight || checkInOpen || today.dayOfWeek != DayOfWeek.FRIDAY) return false
        val weekStart = ProgramCalendar.weekStart(today)
        return weightDates.none { it in weekStart..today }
    }

    /** Values are kept to 0.1 (kg, cm) or whole numbers; rounding hides floating-point noise in deltas. */
    fun round(value: Double): Double = Math.round(value * 10) / 10.0
}

/** A metric the app suggests (D100: nothing is seeded; each is one tap to add). Names live in string resources. */
data class SuggestedMetric(val key: String, val kind: MetricKind, val unit: MetricUnit, val higherIsBetter: Boolean)

object Suggestions {
    const val WEIGHT = "weight"

    /** Weight, the 5 tape sites (D85), then the plan's tests (D106) in the test order (D85). */
    val all: List<SuggestedMetric> = listOf(
        SuggestedMetric(WEIGHT, MetricKind.BODY, MetricUnit.KG, higherIsBetter = false),
        SuggestedMetric("waist", MetricKind.TAPE, MetricUnit.CM, higherIsBetter = false),
        SuggestedMetric("chest", MetricKind.TAPE, MetricUnit.CM, higherIsBetter = true),
        SuggestedMetric("arm", MetricKind.TAPE, MetricUnit.CM, higherIsBetter = true),
        SuggestedMetric("thigh", MetricKind.TAPE, MetricUnit.CM, higherIsBetter = true),
        SuggestedMetric("shoulders", MetricKind.TAPE, MetricUnit.CM, higherIsBetter = true),
        SuggestedMetric("pullups", MetricKind.STRENGTH, MetricUnit.REPS, higherIsBetter = true),
        SuggestedMetric("pushups", MetricKind.STRENGTH, MetricUnit.REPS, higherIsBetter = true),
        SuggestedMetric("plank", MetricKind.STRENGTH, MetricUnit.SECONDS, higherIsBetter = true),
        SuggestedMetric("dead_hang", MetricKind.STRENGTH, MetricUnit.SECONDS, higherIsBetter = true),
        SuggestedMetric("toe_touch", MetricKind.STRENGTH, MetricUnit.CM, higherIsBetter = false),
        SuggestedMetric("knee_wall_left", MetricKind.STRENGTH, MetricUnit.CM, higherIsBetter = true),
        SuggestedMetric("knee_wall_right", MetricKind.STRENGTH, MetricUnit.CM, higherIsBetter = true),
    )

    fun byKey(key: String?): SuggestedMetric? = all.firstOrNull { it.key == key }

    /** The ones not added yet, in suggestion order. */
    fun remaining(added: Collection<String?>): List<SuggestedMetric> = all.filter { it.key !in added }
}

object DecimalInput {
    /**
     * Reads «۷۶٫۸», «76.8», «76,8» or «۷۶/۸» (a common Persian keyboard slip). Null for anything else,
     * a negative number, or more than one decimal place's worth of junk.
     */
    fun parse(text: String): Double? {
        val normalized = app.beaesthetic.domain.text.PersianDigits.normalize(text.trim())
            .replace('٫', '.').replace(',', '.').replace('/', '.').replace('،', '.')
        if (normalized.isEmpty() || normalized.count { it == '.' } > 1) return null
        if (!normalized.all { it.isDigit() || it == '.' }) return null
        return normalized.toDoubleOrNull()?.let(Progress::round)
    }
}
