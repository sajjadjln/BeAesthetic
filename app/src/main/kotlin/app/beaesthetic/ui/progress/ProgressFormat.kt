package app.beaesthetic.ui.progress

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.beaesthetic.R
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.data.repo.Pose
import app.beaesthetic.domain.progress.MetricUnit
import app.beaesthetic.domain.progress.Reading
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.theme.Ba
import java.io.File
import kotlin.math.abs

/** «۷۶٫۸ کیلو», «۱۲ تا», «۷۰ ثانیه». */
@Composable
fun valueText(unit: MetricUnit, value: Double): String = stringResource(unitFormat(unit), PersianDigits.format(value))

fun unitFormat(unit: MetricUnit): Int = when (unit) {
    MetricUnit.KG -> R.string.value_kg
    MetricUnit.CM -> R.string.value_cm
    MetricUnit.REPS -> R.string.value_reps
    MetricUnit.SECONDS -> R.string.value_seconds
}

fun unitName(unit: MetricUnit): Int = when (unit) {
    MetricUnit.KG -> R.string.unit_kg
    MetricUnit.CM -> R.string.unit_cm
    MetricUnit.REPS -> R.string.unit_reps
    MetricUnit.SECONDS -> R.string.unit_seconds
}

/** «+۱٫۵», «−۲»: a signed change with the real minus sign. */
fun deltaText(delta: Double): String = when {
    delta > 0 -> "+" + PersianDigits.format(delta)
    delta < 0 -> "−" + PersianDigits.format(abs(delta))
    else -> PersianDigits.format(0)
}

/** A suggested metric's name (D100); the user's own metrics carry their own. */
fun suggestionName(key: String): Int? = when (key) {
    "weight" -> R.string.metric_weight
    "waist" -> R.string.metric_waist
    "chest" -> R.string.metric_chest
    "arm" -> R.string.metric_arm
    "thigh" -> R.string.metric_thigh
    "shoulders" -> R.string.metric_shoulders
    "pullups" -> R.string.metric_pullups
    "pushups" -> R.string.metric_pushups
    "plank" -> R.string.metric_plank
    "dead_hang" -> R.string.metric_dead_hang
    "toe_touch" -> R.string.metric_toe_touch
    "knee_wall_left" -> R.string.metric_knee_wall_left
    "knee_wall_right" -> R.string.metric_knee_wall_right
    else -> null
}

/** Where to measure (flows C3), for the suggested sites and tests. */
fun measureHint(key: String?): Int? = when (key) {
    "waist" -> R.string.hint_waist
    "chest" -> R.string.hint_chest
    "arm" -> R.string.hint_arm
    "thigh" -> R.string.hint_thigh
    "shoulders" -> R.string.hint_shoulders
    "toe_touch" -> R.string.hint_toe_touch
    "knee_wall_left", "knee_wall_right" -> R.string.hint_knee_wall
    else -> null
}

fun poseName(pose: Pose): Int = when (pose) {
    Pose.FRONT -> R.string.pose_front
    Pose.SIDE -> R.string.pose_side
    Pose.BACK -> R.string.pose_back
}

/**
 * A plain line chart (DESIGN 03). Time runs right → left in RTL. [highlightLast] marks the latest point in
 * the accent; the line itself is neutral so a dip never reads as a warning.
 */
@Composable
fun LineChart(readings: List<Reading>, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val c = Ba.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val points = readings.sortedBy { it.date }
    Canvas(modifier) {
        if (points.isEmpty()) return@Canvas
        val first = points.first().date.toEpochDay()
        val span = (points.last().date.toEpochDay() - first).coerceAtLeast(1)
        val min = points.minOf { it.value }
        val max = points.maxOf { it.value }
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val pad = 6.dp.toPx()
        fun at(r: Reading): Offset {
            val fx = if (points.size == 1) 0.5f else (r.date.toEpochDay() - first).toFloat() / span
            val x = pad + (size.width - 2 * pad) * (if (rtl) 1 - fx else fx)
            val fy = if (max == min) 0.5f else ((r.value - min) / range).toFloat()
            val y = size.height - pad - (size.height - 2 * pad) * fy
            return Offset(x, y)
        }
        val path = Path()
        points.forEachIndexed { i, r -> at(r).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
        drawPath(path, c.text3, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { drawCircle(c.text3, 2.5.dp.toPx(), at(it)) }
        if (highlightLast) drawCircle(c.accent, 4.dp.toPx(), at(points.last()))
    }
}

/** A stored photo, loaded small off the main thread; a quiet placeholder while it loads or when it's missing. */
@Composable
fun PhotoThumb(store: PhotoStore, fileName: String?, description: String, modifier: Modifier = Modifier, maxEdge: Int = 480) {
    val c = Ba.colors
    val bitmap by produceState<Bitmap?>(null, fileName, fileStamp(store, fileName)) {
        value = fileName?.let { store.load(it, maxEdge) }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(c.surface2).semantics { contentDescription = description },
    ) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(BaIcons.Image, null, tint = c.text3)
        }
    }
}

/** Changes when the file is replaced (same name, new shot), so the thumbnail reloads. */
private fun fileStamp(store: PhotoStore, name: String?): Long = name?.let { File(store.dir, it).lastModified() } ?: 0L
