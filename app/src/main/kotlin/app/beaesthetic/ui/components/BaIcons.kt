package app.beaesthetic.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Part

/**
 * The line icons of the mockups (`docs/design/screens.html`, 24×24, stroke 1.8, round caps and joins),
 * as Compose vectors. Paths are copied from the mockup's sprite; circles and rects are written as paths.
 */
object BaIcons {
    val Check = stroke("M20 6 9 17l-5-5")
    val Gear = stroke(
        circle(12f, 12f, 3f),
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
    )
    val Leaf = stroke(
        "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z",
        "M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12",
    )
    val Sunrise = stroke("M12 2v6M4.93 10.93l1.41 1.41M2 18h2M20 18h2M19.07 10.93l-1.41 1.41M22 22H2M16 18a4 4 0 0 0-8 0M8 6l4-4 4 4")
    val Sun = stroke(
        circle(12f, 12f, 4f),
        "M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41",
    )
    val Sunset = stroke("M12 10V2M4.93 10.93l1.41 1.41M2 18h2M20 18h2M19.07 10.93l-1.41 1.41M22 22H2M16 18a4 4 0 0 0-8 0M16 6l-4 4-4-4")
    val Moon = stroke("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    val Drop = stroke("M12 2.7s6 6.3 6 11.3a6 6 0 0 1-12 0c0-5 6-11.3 6-11.3z", "M9.5 14.5a2.5 2.5 0 0 0 2.5 2.5")
    val Tooth = stroke("M7 3C5 3 3.5 4.8 3.5 7.2c0 3 1.5 4.3 2 7 .5 3 1 7 2.8 7 1.6 0 1.6-4.5 3.7-4.5s2.1 4.5 3.7 4.5c1.8 0 2.3-4 2.8-7 .5-2.7 2-4 2-7C20.5 4.8 19 3 17 3c-2 0-3 1-5 1S9 3 7 3z")
    val Pill = stroke("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7")
    val Egg = stroke("M12 22c-4.4 0-7-3-7-7.5C5 9 8.5 2 12 2s7 7 7 12.5c0 4.5-2.6 7.5-7 7.5z")
    val Dumbbell = stroke("M6 7v10M18 7v10M3 9.5v5M21 9.5v5M6 12h12")
    val Scissors = stroke(circle(6f, 6f, 3f), circle(6f, 18f, 3f), "M20 4 8.12 15.88M14.47 14.48 20 20M8.12 8.12 12 12")
    val Apple = stroke("M12 7c-2-1.5-7-1.5-7 4.5S8 21 10 21c1 0 1.3-.5 2-.5s1 .5 2 .5c2 0 5-3.5 5-9.5S14 5.5 12 7z", "M12 7c0-2 1-3.5 3-4")
    val Shower = stroke("M4 21V9a5 5 0 0 1 9-3", "M10 8h10", "M12 12v1M15 12v1M18 12v1M13.5 16v1M16.5 16v1M15 20v1")
    val Mountain = stroke("m8 3 4 8 5-5 5 15H2L8 3z")
    /** A bathroom scale: not in the mockup sprite; drawn in the same style. */
    val Scale = stroke(roundRect(3f, 3f, 18f, 18f, 4f), "M8 10a4 4 0 0 1 8 0", "M12 10l1.6-2.2")
    val Play = filled("M8 5.5v13l11-6.5z")
    /** Points "back": drawn left, mirrored to point right in RTL. */
    val Back = stroke("M19 12H5M12 19l-7-7 7-7", autoMirror = true)
    val X = stroke("M18 6 6 18M6 6l12 12")
    val Bell = stroke("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val List = stroke("M9 6h11M9 12h11M9 18h11", "m3 6 1.5 1.5L7 5M3 12l1.5 1.5L7 11M3 18l1.5 1.5L7 17")
    val Cloud = stroke("M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z")
    val Palette = stroke(
        circle(13.5f, 6.5f, 1.5f), circle(17.5f, 10.5f, 1.5f), circle(8.5f, 7.5f, 1.5f), circle(6.5f, 12.5f, 1.5f),
        "M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.9 0 1.7-.8 1.7-1.7 0-.4-.2-.8-.4-1.1-.3-.3-.4-.7-.4-1.1 0-.9.8-1.7 1.7-1.7h2c3 0 5.5-2.5 5.5-5.5C22 6 17.5 2 12 2z",
    )
    val Phone = stroke(roundRect(6f, 2f, 12f, 20f, 2f), "M11 18h2")
    val Shield = stroke("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z", "m9 12 2 2 4-4")
    val Lock = stroke(roundRect(4f, 11f, 16f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4")
    val Mail = stroke(roundRect(3f, 5f, 18f, 14f, 2f), "m3 7 9 6 9-6")
    val Repeat = stroke("m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3")
    val Plus = stroke("M12 5v14M5 12h14")
    val Minus = stroke("M5 12h14")
    val Flag = stroke("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z", "M4 22v-7")
    val CheckCircle = stroke(circle(12f, 12f, 9f), "m8.5 12.5 2.5 2.5 5-5.5")
    val Grid = stroke(roundRect(4f, 4f, 7f, 7f, 1.5f), roundRect(13f, 4f, 7f, 7f, 1.5f), roundRect(4f, 13f, 7f, 7f, 1.5f), roundRect(13f, 13f, 7f, 7f, 1.5f))
    val Grip = stroke(circle(9f, 6f, 1f), circle(15f, 6f, 1f), circle(9f, 12f, 1f), circle(15f, 12f, 1f), circle(9f, 18f, 1f), circle(15f, 18f, 1f))
    val Pen = stroke("M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    val Calendar = stroke(roundRect(3f, 4f, 18f, 18f, 3f), "M16 2v4M8 2v4M3 10h18")
    val ChevronDown = stroke("m6 9 6 6 6-6")
    /** Points "forward" (later in time): right in LTR, left in RTL (time flows right to left). */
    val ChevronForward = stroke("m9 18 6-6-6-6", autoMirror = true)
    val ChevronBackward = stroke("m15 18-6-6 6-6", autoMirror = true)

    fun of(icon: TaskIcon): ImageVector = when (icon) {
        TaskIcon.DROP -> Drop
        TaskIcon.TOOTH -> Tooth
        TaskIcon.PILL -> Pill
        TaskIcon.EGG -> Egg
        TaskIcon.DUMBBELL -> Dumbbell
        TaskIcon.MOUNTAIN -> Mountain
        TaskIcon.SCALE -> Scale
        TaskIcon.SHOWER -> Shower
        TaskIcon.SCISSORS -> Scissors
        TaskIcon.APPLE -> Apple
        TaskIcon.SUN -> Sun
        TaskIcon.CHECK -> CheckCircle
    }

    fun of(part: Part): ImageVector = when (part) {
        Part.MORNING -> Sunrise
        Part.MIDDAY -> Sun
        Part.EVENING -> Sunset
        Part.NIGHT -> Moon
    }

    private fun stroke(vararg paths: String, autoMirror: Boolean = false): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = autoMirror).apply {
            for (d in paths) {
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(d: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            .build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"
}
