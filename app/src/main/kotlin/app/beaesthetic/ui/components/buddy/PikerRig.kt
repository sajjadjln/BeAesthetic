package app.beaesthetic.ui.components.buddy

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.buddy.Pose
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * «پیکر» (D82): a port of the mockups' rig, `docs/design/piker.js`. The same simple shapes are drawn four
 * times, like the logo (D81): drop shadow, white sticker border, ink outline, body color. The body takes
 * the user's accent; ink, paper and props are fixed. Coordinates are the rig's own (viewBox -8 -2 124 102).
 */
internal object PikerRig {
    private val INK = Color(0xFF0E1110)
    private val PAPER = Color(0xFFF7F8F7)
    private val WHITE = Color.White
    private const val BORDER = 5.8f
    private const val OUTLINE = 2.2f

    const val VIEW_X = -8f
    const val VIEW_Y = -2f
    const val VIEW_W = 124f
    const val VIEW_H = 102f

    // ---------- primitives ----------
    sealed interface Shape
    data class Circle(val cx: Float, val cy: Float, val r: Float) : Shape
    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : Shape
    data class RoundRect(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float, val rotate: Triple<Float, Float, Float>? = null) : Shape
    data class Seg(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val width: Float) : Shape
    data class P(val d: String) : Shape

    private val paths = HashMap<String, Path>()
    private fun path(d: String): Path = paths.getOrPut(d) { PathParser().parsePathString(d).toPath() }

    /** One shape in [color]; with a border [b], also stroked 2·b wide, so it grows outward. */
    private fun DrawScope.prim(s: Shape, color: Color, b: Float) {
        val stroke = if (b > 0) Stroke(width = 2 * b, join = StrokeJoin.Round) else null
        when (s) {
            is Circle -> {
                drawCircle(color, s.r, Offset(s.cx, s.cy))
                stroke?.let { drawCircle(color, s.r, Offset(s.cx, s.cy), style = it) }
            }
            is Ellipse -> {
                val tl = Offset(s.cx - s.rx, s.cy - s.ry)
                val size = Size(2 * s.rx, 2 * s.ry)
                drawOval(color, tl, size)
                stroke?.let { drawOval(color, tl, size, style = it) }
            }
            is RoundRect -> {
                val draw: DrawScope.() -> Unit = {
                    drawRoundRect(color, Offset(s.x, s.y), Size(s.w, s.h), CornerRadius(s.rx))
                    stroke?.let { drawRoundRect(color, Offset(s.x, s.y), Size(s.w, s.h), CornerRadius(s.rx), style = it) }
                }
                val r = s.rotate
                if (r != null) rotate(r.first, Offset(r.second, r.third), draw) else draw()
            }
            is Seg -> drawLine(color, Offset(s.x1, s.y1), Offset(s.x2, s.y2), strokeWidth = s.width + 2 * b, cap = StrokeCap.Round)
            is P -> {
                drawPath(path(s.d), color)
                stroke?.let { drawPath(path(s.d), color, style = it) }
            }
        }
    }

    private fun DrawScope.all(list: List<Shape>, color: Color, b: Float) = list.forEach { prim(it, color, b) }

    /** A prop or outfit part: ink outline then its fill. */
    private fun DrawScope.outline(list: List<Shape>, fill: Color, w: Float = 1.3f) = list.forEach {
        prim(it, INK, w)
        prim(it, fill, 0f)
    }

    private fun DrawScope.strokeD(d: String, color: Color, width: Float, alpha: Float = 1f, cap: StrokeCap = StrokeCap.Round, join: StrokeJoin = StrokeJoin.Round) =
        drawPath(path(d), color, alpha = alpha, style = Stroke(width = width, cap = cap, join = join))

    private fun DrawScope.fillD(d: String, color: Color) = drawPath(path(d), color, style = Fill)

    private fun DrawScope.star(x: Float, y: Float, r: Float) =
        fillD("M$x ${y - r}Q$x $y ${x + r} ${y}Q$x $y $x ${y + r}Q$x $y ${x - r} ${y}Q$x $y $x ${y - r}Z", WHITE)

    // ---------- arms ----------
    private enum class Hand { FIST, OPEN, THUMB, POINT }
    private data class ArmSpec(val ex: Float, val ey: Float, val hx: Float, val hy: Float, val hand: Hand = Hand.FIST, val bi: Float = .4f, val front: Boolean = false)
    private class Arm(val upper: List<Shape>, val lower: List<Shape>, val shine: Offset)

    private fun unit(x: Float, y: Float): Pair<Float, Float> {
        val l = hypot(x, y).takeIf { it != 0f } ?: 1f
        return x / l to y / l
    }

    private fun rot(v: Pair<Float, Float>, deg: Float): Pair<Float, Float> {
        val a = Math.toRadians(deg.toDouble())
        return (v.first * cos(a) - v.second * sin(a)).toFloat() to (v.first * sin(a) + v.second * cos(a)).toFloat()
    }

    private fun arm(left: Boolean, a: ArmSpec): Arm {
        val sx = if (left) 37f else 71f
        val sy = 52f
        val d = unit(a.ex - sx, a.ey - sy)
        val f = unit(a.hx - a.ex, a.hy - a.ey)
        var p = -d.second to d.first
        if (p.first * f.first + p.second * f.second < 0) p = d.second to -d.first
        val mx = (sx + a.ex) / 2
        val my = (sy + a.ey) / 2
        val off = 2.4f + 4.3f * a.bi
        val br = 5.8f + 2.4f * a.bi
        val bx = mx + p.first * off
        val by = my + p.second * off
        val hcx = a.hx - f.first * 3.5f
        val hcy = a.hy - f.second * 3.5f
        val upper = listOf(Seg(sx, sy, a.ex, a.ey, 10f), Circle(bx, by, br))
        val lower = mutableListOf<Shape>(Seg(a.ex, a.ey, hcx, hcy, 9f))
        lower += Circle(a.hx, a.hy, if (a.hand == Hand.OPEN) 6.2f else 6.4f)
        when (a.hand) {
            Hand.OPEN -> {
                listOf(-38f, 0f, 38f).forEach { g ->
                    val v = rot(f, g)
                    lower += Circle(a.hx + v.first * 6, a.hy + v.second * 6, 2.5f)
                }
                val t = rot(f, if (left) -95f else 95f)
                lower += Circle(a.hx + t.first * 5.6f, a.hy + t.second * 5.6f, 2.5f)
            }
            Hand.THUMB -> lower += Seg(a.hx + .5f, a.hy - 3, a.hx + 1.2f, a.hy - 11, 4.6f)
            Hand.POINT -> lower += Seg(a.hx, a.hy, a.hx + f.first * 10, a.hy + f.second * 10, 4.2f)
            Hand.FIST -> Unit
        }
        return Arm(upper, lower, Offset(bx - 2.9f, by - 3.3f))
    }

    // ---------- body ----------
    private val TRAPS = P("M46 44h16l5 6.5H41z")
    private val TORSO = P("M35 51C35 45.5 73 45.5 73 51C76 57 72 66 66 73C63.5 77 62 81 61 84L47 84C46 81 44.5 77 42 73C36 66 32 57 35 51Z")
    private fun body(hx: Float, hy: Float) = listOf(TRAPS, TORSO, Circle(38f, 51f, 7.5f), Circle(70f, 51f, 7.5f), Circle(54 + hx, 34 + hy, 10.5f))

    // ---------- faces ----------
    private enum class Eyes { OPEN, WIDE, HAPPY, CLOSED, WINK, UP }
    private enum class Brows(val d: String) {
        DET("M47.6 30.3l4.2 1.2M60.4 30.3l-4.2 1.2"),
        UP("M47.8 29.9Q50 28.1 52.2 29.4M60.2 29.9Q58 28.1 55.8 29.4"),
        SOFT("M48 30.6Q50 29.8 52 30.4M60 30.6Q58 29.8 56 30.4"),
        QUIRK("M47.8 30.6Q50 30.2 52.1 30.9M60.3 29.2Q58 27.6 55.8 28.9"),
    }
    private enum class Mouth { GRIN, OPEN, SMILE, O, YAWN, HMM }
    private data class Face(val eyes: Eyes, val brows: Brows, val mouth: Mouth)

    private fun DrawScope.face(f: Face, hx: Float, hy: Float) = translate(hx, hy) {
        when (f.eyes) {
            Eyes.OPEN -> { prim(Ellipse(50.2f, 34f, 1.5f, 1.9f), INK, 0f); prim(Ellipse(57.8f, 34f, 1.5f, 1.9f), INK, 0f) }
            Eyes.WIDE -> {
                drawCircle(INK, 2.3f, Offset(50f, 34f)); drawCircle(INK, 2.3f, Offset(58f, 34f))
                drawCircle(WHITE, .75f, Offset(50.8f, 33.1f)); drawCircle(WHITE, .75f, Offset(58.8f, 33.1f))
            }
            Eyes.HAPPY -> strokeD("M48.5 34.8Q50.2 31.9 51.9 34.8M56.1 34.8Q57.8 31.9 59.5 34.8", INK, 1.7f)
            Eyes.CLOSED -> strokeD("M48.5 33.6Q50.2 35.8 51.9 33.6M56.1 33.6Q57.8 35.8 59.5 33.6", INK, 1.7f)
            Eyes.WINK -> { prim(Ellipse(50.2f, 34f, 1.5f, 1.9f), INK, 0f); strokeD("M56.1 34.8Q57.8 31.9 59.5 34.8", INK, 1.7f) }
            Eyes.UP -> { prim(Ellipse(51f, 32.9f, 1.5f, 1.9f), INK, 0f); prim(Ellipse(58.6f, 32.9f, 1.5f, 1.9f), INK, 0f) }
        }
        strokeD(f.brows.d, INK, 1.7f)
        when (f.mouth) {
            Mouth.GRIN -> strokeD("M49.6 38.2Q54 42.4 58.4 38.2", INK, 1.7f)
            Mouth.OPEN -> {
                val d = "M49.3 37.8Q54 45.6 58.7 37.8Z"
                fillD(d, INK); strokeD(d, INK, 1f)
                fillD("M51.6 41.4Q54 40 56.4 41.4Q54 43.2 51.6 41.4Z", Color(0xFFF08FA8))
            }
            Mouth.SMILE -> strokeD("M51.2 38.7Q54 40.9 56.8 38.7", INK, 1.7f)
            Mouth.O -> prim(Ellipse(54f, 39.6f, 1.7f, 2.1f), INK, 0f)
            Mouth.YAWN -> prim(Ellipse(54f, 39.8f, 2.5f, 3.3f), INK, 0f)
            Mouth.HMM -> strokeD("M51.4 39.4Q54 40.3 56.8 38.6", INK, 1.7f)
        }
    }

    // ---------- props ----------
    private class Art(val sil: List<Shape>, val behind: Boolean = false, val draw: DrawScope.(Color) -> Unit)

    private fun prop(name: String): Art? = when (name) {
        "mug" -> {
            val cup = listOf(RoundRect(45.5f, 53.5f, 17f, 17f, 3.6f))
            Art(cup) {
                outline(cup, Color(0xFFEFE7DA))
                drawRect(Color(0xFFC9B9A2), Offset(45.5f, 57f), Size(17f, 2.6f))
                strokeD("M51 51q-1.6-2 0-4t0-4M57 51q-1.6-2 0-4t0-4", WHITE, 1.5f, alpha = .85f)
            }
        }
        "letter" -> {
            val env = listOf(RoundRect(40.5f, 56f, 27f, 18f, 2.2f))
            Art(env) { seal ->
                outline(env, Color(0xFFF4EFE6))
                strokeD("M41.3 57.2L54 66.2L66.7 57.2", INK, 1.3f)
                drawCircle(seal, 2.8f, Offset(54f, 66f))
                drawCircle(INK, 2.8f, Offset(54f, 66f), style = Stroke(1.1f))
            }
        }
        "book" -> {
            val pages = listOf(P("M40 58Q47 55 54 58.5Q61 55 68 58V73Q61 70.5 54 73.5Q47 70.5 40 73Z"))
            Art(pages) {
                val cover = "M38.6 59.5V75Q47 72 54 75.6Q61 72 69.4 75V59.5"
                fillD(cover, Color(0xFF6B4A33)); strokeD(cover, INK, 1.3f)
                outline(pages, Color(0xFFF4EFE6))
                strokeD("M54 58.5V73.5M43 61.3q4-1.2 8 0M43 64.3q4-1.2 8 0M43 67.3q4-1.2 8 0M57 61.3q4-1.2 8 0M57 64.3q4-1.2 8 0M57 67.3q4-1.2 8 0", INK, .9f, alpha = .45f)
            }
        }
        "phone" -> {
            val r = Triple(10f, 86f, 23.5f)
            val ph = listOf(RoundRect(79.5f, 13f, 13.5f, 21f, 3f, r))
            Art(ph) {
                outline(ph, Color(0xFF1F2624))
                prim(RoundRect(81.3f, 15f, 9.9f, 15.5f, 1.8f, r), Color(0xFF3B4643), 0f)
                rotate(10f, Offset(86f, 23.5f)) { drawCircle(Color(0xFF8D9692), .9f, Offset(87.3f, 31.8f)) }
            }
        }
        "dumbbell" -> {
            val plates = listOf(RoundRect(64f, 24.5f, 6f, 16f, 2f), RoundRect(88f, 24.5f, 6f, 16f, 2f))
            Art(plates + Seg(67f, 32.5f, 91f, 32.5f, 3.2f), behind = true) {
                strokeD("M67 32.5H91", INK, 4.8f)
                strokeD("M67 32.5H91", Color(0xFF8D9692), 2.2f)
                outline(plates, Color(0xFF2E3532))
                strokeD("M66 27v6M90 27v6", WHITE, 1.2f, alpha = .35f)
            }
        }
        "sabzeh" -> {
            val pot = listOf(P("M45 63H63L60.5 75H47.5Z"))
            val blades = "M47.5 63.5Q46 56 43.5 52M50 63.5Q49.5 55 48.5 49.5M52.5 63.5Q53 54 52.5 47.5M55 63.5Q56 54.5 57.5 48.5M57.5 63.5Q59.5 56 62 51.5M60.5 63.5Q62.5 58.5 65 56"
            Art(pot + Ellipse(54f, 56f, 10.5f, 8f)) {
                strokeD(blades, INK, 3.6f)
                strokeD(blades, Color(0xFF9BE36F), 1.8f)
                outline(pot, Color(0xFFD9A066))
                strokeD("M45.6 66.5H62.4", Color(0xFFF4EFE6), 2f, cap = StrokeCap.Butt)
            }
        }
        "bowl" -> {
            val g = listOf(Circle(90f, 74f, 10.5f))
            Art(g) {
                outline(g, Color(0xFFCFEFFA))
                fillD("M80.4 72.5Q90 69.5 99.6 72.5A10.5 10.5 0 0 1 80.4 72.5Z", Color(0xFF86CDEB))
                val fish = "M86 77.5q3-3.4 6.5 0q-3.5 3.4-6.5 0zM92.5 77.5l3-2.4v4.8z"
                fillD(fish, Color(0xFFF28B3C)); strokeD(fish, INK, .9f)
                drawCircle(INK, .6f, Offset(88.4f, 76.8f))
                strokeD("M83.5 68.5q2-2.5 5-3", WHITE, 1.4f)
            }
        }
        else -> null
    }

    // ---------- outfits (one per chapter, D83) ----------
    private fun outfit(o: Outfit): Art? = when (o) {
        Outfit.NONE -> null
        Outfit.BAND -> {
            val band = listOf(P("M43.3 30.6Q54 24.6 64.7 30.6L64.4 26.6Q54 20.6 43.6 26.6Z"))
            Art(band) { seal ->
                strokeD("M44.2 28.5L37.8 31.6M44.2 28.5L38.6 25.2", INK, 4.2f)
                strokeD("M44.2 28.5L37.8 31.6M44.2 28.5L38.6 25.2", PAPER, 2f)
                outline(band, PAPER)
                strokeD("M44 28.4Q54 22.6 64 28.4", seal, 1.3f, cap = StrokeCap.Butt)
            }
        }
        Outfit.WINTER -> {
            val hat = listOf(P("M43 31.5C42.6 17.5 65.4 17.5 65 31.5Z"))
            val brim = listOf(RoundRect(42f, 28f, 24f, 5.4f, 2.6f))
            val pom = listOf(Circle(54f, 18.8f, 3.4f))
            val scarf = listOf(P("M43.5 43Q54 49.5 64.5 43L65.6 47.6Q54 54.5 42.4 47.6Z"), P("M56.5 48.5L59 62.5L64.2 61.4L61.4 47.6Z"))
            Art(hat + brim + pom + scarf) {
                val wool = Color(0xFFEDE6D6)
                outline(scarf, wool)
                strokeD("M57.6 52.5l4-.8M58.3 56.5l4-.8M43.6 45.6Q54 51.3 64.4 45.6", INK, 1.2f, alpha = .45f, cap = StrokeCap.Butt)
                outline(hat, wool)
                outline(pom, wool)
                outline(brim, Color(0xFFD9CFBC))
                strokeD("M47 24.5v3M50.5 22v5.5M54 21.3v6.2M57.5 22v5.5M61 24.5v3", INK, 1f, alpha = .3f, cap = StrokeCap.Butt)
            }
        }
        Outfit.SHADES -> Art(emptyList()) {
            strokeD("M53.6 33.2h1M45.6 32.8l-1.6-.8M62.4 32.8l1.6-.8", INK, 1.5f)
            outline(listOf(RoundRect(46.2f, 31.6f, 7.6f, 5.2f, 2.2f), RoundRect(54.2f, 31.6f, 7.6f, 5.2f, 2.2f)), Color(0xFF151918), .9f)
            strokeD("M47.6 33.2l2.2-1M55.6 33.2l2.2-1", WHITE, 1f, alpha = .7f)
        }
        Outfit.SPROUT -> {
            val leaves = listOf(P("M54 24.5Q47 23.5 46 17.5Q52.5 17.5 54 24.5Z"), P("M54 24.5Q60 22 61.5 15.5Q55 16.5 54 24.5Z"))
            Art(leaves) {
                strokeD("M54 25.5v-3", INK, 2.4f)
                outline(leaves, Color(0xFFB6EE7E))
            }
        }
    }

    // ---------- poses ----------
    private data class PoseSpec(
        val l: ArmSpec,
        val r: ArmSpec,
        val face: Face,
        val prop: String? = null,
        val sparks: Boolean = false,
        val rot: Float = -8f,
        val hx: Float = 0f,
        val hy: Float = 0f,
        val zzz: Boolean = false,
    )

    private val SIDE_L = ArmSpec(21.5f, 62f, 15.5f, 75.5f)
    private val FLEX_L = ArmSpec(25.5f, 52.5f, 29f, 32f, bi = 1f)
    private val FLEX_R = ArmSpec(82.5f, 52.5f, 79f, 32f, bi = 1f)

    private fun spec(pose: Pose): PoseSpec = when (pose) {
        Pose.FLEX -> PoseSpec(FLEX_L, FLEX_R, Face(Eyes.OPEN, Brows.DET, Mouth.GRIN), sparks = true)
        Pose.WAVE -> PoseSpec(SIDE_L, ArmSpec(85f, 44f, 87f, 23.5f, Hand.OPEN, .5f), Face(Eyes.HAPPY, Brows.UP, Mouth.OPEN), rot = -4f)
        Pose.CHEER -> PoseSpec(ArmSpec(26f, 37f, 21f, 18.5f, bi = .55f), ArmSpec(82f, 37f, 87f, 18.5f, bi = .55f), Face(Eyes.HAPPY, Brows.UP, Mouth.OPEN), sparks = true, rot = 0f, hy = -1f)
        Pose.THUMBS -> PoseSpec(SIDE_L, ArmSpec(86f, 60f, 82f, 44f, Hand.THUMB, .7f), Face(Eyes.WINK, Brows.QUIRK, Mouth.GRIN), rot = -6f)
        Pose.POINT -> PoseSpec(SIDE_L, ArmSpec(84f, 61f, 94f, 71f, Hand.POINT, .35f), Face(Eyes.OPEN, Brows.UP, Mouth.GRIN), rot = -6f, hx = 1f)
        Pose.STRETCH -> PoseSpec(ArmSpec(27.5f, 32f, 46f, 16.5f, bi = .5f), ArmSpec(80.5f, 32f, 62f, 16.5f, bi = .5f), Face(Eyes.CLOSED, Brows.SOFT, Mouth.YAWN), rot = 3f, hy = 1f)
        Pose.TEA -> PoseSpec(ArmSpec(31f, 73f, 46f, 70f, bi = .2f, front = true), ArmSpec(77f, 73f, 62f, 70f, bi = .2f, front = true), Face(Eyes.HAPPY, Brows.SOFT, Mouth.SMILE), prop = "mug", rot = -3f)
        Pose.SLEEP -> PoseSpec(ArmSpec(30f, 67f, 57f, 67.5f, bi = .2f, front = true), ArmSpec(78f, 64f, 51f, 62.5f, bi = .2f, front = true), Face(Eyes.CLOSED, Brows.SOFT, Mouth.SMILE), rot = 6f, hx = 1.5f, hy = 1f, zzz = true)
        Pose.THINK -> PoseSpec(SIDE_L, ArmSpec(80f, 67f, 59.5f, 49f, bi = .35f, front = true), Face(Eyes.UP, Brows.QUIRK, Mouth.HMM), rot = -5f, hx = -2f, hy = -1.5f)
        Pose.SELFIE -> PoseSpec(FLEX_L, ArmSpec(87f, 52f, 87f, 36f, bi = .55f, front = true), Face(Eyes.WINK, Brows.DET, Mouth.GRIN), prop = "phone", rot = -6f)
        Pose.HUG -> PoseSpec(ArmSpec(22f, 58f, 12f, 45f, Hand.OPEN, .45f), ArmSpec(86f, 58f, 96f, 45f, Hand.OPEN, .45f), Face(Eyes.HAPPY, Brows.UP, Mouth.OPEN), rot = 0f)
        Pose.LETTER -> PoseSpec(ArmSpec(29f, 66f, 42.5f, 64.5f, bi = .2f, front = true), ArmSpec(79f, 66f, 65.5f, 64.5f, bi = .2f, front = true), Face(Eyes.WIDE, Brows.UP, Mouth.OPEN), prop = "letter", rot = -4f)
        Pose.READ -> PoseSpec(ArmSpec(29f, 68f, 41f, 67f, bi = .2f, front = true), ArmSpec(79f, 68f, 67f, 67f, bi = .2f, front = true), Face(Eyes.CLOSED, Brows.UP, Mouth.SMILE), prop = "book", rot = -3f, hy = 1f)
        Pose.WOW -> PoseSpec(ArmSpec(26f, 64f, 38.5f, 44f, Hand.OPEN, .4f, front = true), ArmSpec(82f, 64f, 69.5f, 44f, Hand.OPEN, .4f, front = true), Face(Eyes.WIDE, Brows.UP, Mouth.O), sparks = true, rot = 0f)
        Pose.CURL -> PoseSpec(SIDE_L, ArmSpec(82.5f, 52.5f, 79f, 32f, bi = 1f), Face(Eyes.OPEN, Brows.DET, Mouth.GRIN), prop = "dumbbell", rot = -6f)
        Pose.SABZEH -> PoseSpec(ArmSpec(29f, 68f, 45f, 69f, bi = .2f, front = true), ArmSpec(79f, 68f, 63f, 69f, bi = .2f, front = true), Face(Eyes.HAPPY, Brows.UP, Mouth.OPEN), prop = "sabzeh", rot = -4f)
        Pose.FINISH -> PoseSpec(FLEX_L, FLEX_R, Face(Eyes.HAPPY, Brows.UP, Mouth.OPEN), prop = "bowl", sparks = true, rot = -6f)
    }

    private fun DrawScope.shine(cx: Float, cy: Float, rx: Float, ry: Float) =
        rotate(-30f, Offset(cx, cy)) { drawOval(WHITE.copy(alpha = .38f), Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry)) }

    /** Draws [pose] into the current scope, already scaled to the rig's coordinates. */
    fun DrawScope.drawPiker(pose: Pose, color: Color, outfitChoice: Outfit, sparks: Boolean = true) {
        val p = spec(pose)
        val aL = arm(true, p.l)
        val aR = arm(false, p.r)
        val back = body(p.hx, p.hy).toMutableList()
        val front = mutableListOf<List<Shape>>()
        for ((s, a) in listOf(p.l to aL, p.r to aR)) {
            back += a.upper
            if (s.front) front += a.lower else back += a.lower
        }
        val prop = p.prop?.let(::prop)
        val outfit = outfit(outfitChoice)
        val sil = back + (prop?.sil ?: emptyList()) + (outfit?.sil ?: emptyList()) + front.flatten()

        rotate(p.rot, Offset(54f, 54f)) {
            // 1. Drop shadow: the silhouette as one layer at 42% (group opacity, so overlaps don't darken).
            drawContext.canvas.saveLayer(Rect(VIEW_X, VIEW_Y, VIEW_X + VIEW_W, VIEW_Y + VIEW_H), Paint().apply { alpha = .42f })
            translate(1.6f, 2.4f) { all(sil, Color.Black, BORDER) }
            drawContext.canvas.restore()
            // 2. White sticker border.
            all(sil, PAPER, BORDER)
            if (prop?.behind == true) prop.draw(this, color)
            // 3–4. Ink outline, then the body in the accent.
            all(back, INK, OUTLINE)
            all(back, color, 0f)
            shine(49.5f + p.hx, 28.5f + p.hy, 3.6f, 2.1f)
            shine(44f, 55f, 3f, 1.6f)
            shine(aL.shine.x, aL.shine.y, 2.7f, 1.6f)
            shine(aR.shine.x, aR.shine.y, 2.7f, 1.6f)
            strokeD("M45.5 58.5Q54 63.5 62.5 58.5M54 64.5v8M49.5 67q2 1.2 3 0M55.5 67q1 1.2 3 0", INK, 1.7f, alpha = .5f)
            face(p.face, p.hx, p.hy)
            if (prop != null && !prop.behind) prop.draw(this, color)
            outfit?.draw?.invoke(this, color)
            front.forEach { f ->
                all(f, INK, OUTLINE)
                all(f, color, 0f)
            }
            if (p.sparks && sparks) {
                star(38f, 15f, 3.4f); star(72f, 13f, 2.6f); star(94f, 64f, 2.2f)
            }
            if (p.zzz) {
                strokeD("M70 22h5l-5 6h5", WHITE, 1.8f)
                strokeD("M79 12h7l-7 8h7", WHITE, 1.8f)
            }
        }
    }

    /** Maps the rig's viewBox onto a canvas of [size], keeping the aspect ratio, centered. */
    fun DrawScope.inViewBox(block: DrawScope.() -> Unit) {
        val scale = minOf(size.width / VIEW_W, size.height / VIEW_H)
        val dx = (size.width - VIEW_W * scale) / 2
        val dy = (size.height - VIEW_H * scale) / 2
        withTransform({
            translate(dx, dy)
            scale(scale, scale, Offset.Zero)
            translate(-VIEW_X, -VIEW_Y)
        }) { block() }
    }
}
