package app.beaesthetic.domain.progress

import app.beaesthetic.domain.time.ProgramCalendar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckInsTest {

    private val calendar = ProgramCalendar(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 3, 20))
    private val schedule = CheckIns.schedule(calendar)
    private fun d(m: Int, day: Int) = LocalDate.of(if (m >= 10) 2026 else 2027, m, day)

    @Test
    fun `seven check-ins - day 0, the friday nearest each 17th, the finish`() {
        assertEquals(
            listOf(d(10, 9), d(11, 6), d(12, 11), d(1, 8), d(2, 5), d(3, 5), d(3, 20)),
            schedule.map { it.date },
        )
        assertEquals((0..6).toList(), schedule.map { it.idx })
    }

    @Test
    fun `the friday nearest the 17th can be before or after it`() {
        assertEquals(d(11, 6), CheckIns.fridayNear17(1405, 8)) // 17 Aban = Sun 8 Nov
        assertEquals(d(12, 11), CheckIns.fridayNear17(1405, 9)) // 17 Azar = Tue 8 Dec
        assertEquals(d(2, 5), CheckIns.fridayNear17(1405, 11)) // 17 Bahman = Sat 6 Feb
    }

    @Test
    fun `a check-in is open from its date for a week, until done`() {
        assertNull(CheckIns.open(d(11, 5), schedule, emptySet()))
        assertEquals(1, CheckIns.open(d(11, 6), schedule, emptySet())?.idx)
        assertEquals(1, CheckIns.open(d(11, 12), schedule, emptySet())?.idx)
        assertNull(CheckIns.open(d(11, 13), schedule, emptySet()))
        assertNull(CheckIns.open(d(11, 7), schedule, setOf(1)))
    }

    @Test
    fun `day 0 is never offered on today`() {
        assertNull(CheckIns.open(d(10, 9), schedule, emptySet()))
        assertNull(CheckIns.open(d(10, 15), schedule, emptySet()))
    }

    @Test
    fun `the esfand window closes when the finish starts`() {
        assertEquals(5, CheckIns.open(d(3, 11), schedule, emptySet())?.idx)
        assertEquals(6, CheckIns.open(d(3, 20), schedule, emptySet())?.idx)
    }

    @Test
    fun `next is the open one, else the first ahead`() {
        assertEquals(1, CheckIns.next(d(10, 15), schedule, emptySet())?.idx)
        assertEquals(22, CheckIns.daysUntil(d(10, 15), CheckIns.next(d(10, 15), schedule, emptySet())!!))
        assertEquals(1, CheckIns.next(d(11, 8), schedule, emptySet())?.idx)
        assertEquals(2, CheckIns.next(d(11, 8), schedule, setOf(1))?.idx)
        assertNull(CheckIns.next(d(3, 21), schedule, setOf(6)))
    }

    @Test
    fun `nearest check-in to a date`() {
        assertEquals(0, CheckIns.nearest(d(10, 12), schedule).idx)
        assertEquals(1, CheckIns.nearest(d(11, 1), schedule).idx)
    }
}

class ProgressTest {

    private val waist = Metric(1, MetricKind.TAPE, "waist", "کمر", MetricUnit.CM, higherIsBetter = false)
    private val weight = Metric(2, MetricKind.BODY, "weight", "وزن", MetricUnit.KG, higherIsBetter = false)
    private val pullups = Metric(3, MetricKind.STRENGTH, "pullups", "بارفیکس", MetricUnit.REPS, higherIsBetter = true)
    private val toe = Metric(4, MetricKind.STRENGTH, "toe_touch", "فاصله‌ی انگشت", MetricUnit.CM, higherIsBetter = false)
    private fun r(day: Int, value: Double, guess: Double? = null) = Reading(LocalDate.of(2026, 10, day), value, guess)

    @Test
    fun `since day 0 uses the first and the latest reading by date`() {
        val since = Progress.sinceDay0(waist, listOf(r(20, 86.0), r(9, 88.0), r(15, 87.5)))!!
        assertEquals(88.0, since.start.value)
        assertEquals(86.0, since.latest.value)
        assertEquals(-2.0, since.delta)
        assertEquals(Trend.BETTER, since.trend)
    }

    @Test
    fun `a change the wrong way is neutral, never worse`() {
        assertEquals(Trend.NEUTRAL, Progress.sinceDay0(waist, listOf(r(9, 86.0), r(20, 87.0)))!!.trend)
        assertEquals(Trend.NEUTRAL, Progress.sinceDay0(pullups, listOf(r(9, 8.0), r(20, 8.0)))!!.trend)
    }

    @Test
    fun `weight is never scored`() {
        val since = Progress.sinceDay0(weight, listOf(r(9, 77.1), r(20, 75.5)))!!
        assertEquals(-1.6, since.delta)
        assertEquals(Trend.NEUTRAL, since.trend)
    }

    @Test
    fun `no readings, no card`() {
        assertNull(Progress.sinceDay0(waist, emptyList()))
    }

    @Test
    fun `records follow the direction and need an earlier reading`() {
        assertFalse(Progress.isRecord(pullups, 8.0, emptyList()))
        assertTrue(Progress.isRecord(pullups, 9.0, listOf(r(9, 8.0))))
        assertFalse(Progress.isRecord(pullups, 8.0, listOf(r(9, 8.0))))
        assertTrue(Progress.isRecord(toe, 4.0, listOf(r(9, 6.0))))
        assertFalse(Progress.isRecord(waist, 80.0, listOf(r(9, 88.0))))
    }

    @Test
    fun `guess outcome`() {
        assertEquals(GuessOutcome.NONE, Progress.guess(pullups, null, 10.0))
        assertEquals(GuessOutcome.BEAT, Progress.guess(pullups, 8.0, 10.0))
        assertEquals(GuessOutcome.SHOWN, Progress.guess(pullups, 8.0, 8.0))
        assertEquals(GuessOutcome.SHOWN, Progress.guess(pullups, 12.0, 10.0))
        assertEquals(GuessOutcome.BEAT, Progress.guess(toe, 6.0, 4.0))
    }

    @Test
    fun `weigh-in is due on friday when this week has no weight`() {
        val friday = LocalDate.of(2026, 10, 16)
        assertTrue(Progress.weighInDue(friday, true, listOf(LocalDate.of(2026, 10, 9)), checkInOpen = false))
        assertFalse(Progress.weighInDue(friday, true, listOf(LocalDate.of(2026, 10, 10)), checkInOpen = false))
        assertFalse(Progress.weighInDue(friday.minusDays(1), true, emptyList(), checkInOpen = false))
        assertFalse(Progress.weighInDue(friday, false, emptyList(), checkInOpen = false))
        assertFalse(Progress.weighInDue(friday, true, emptyList(), checkInOpen = true))
    }

    @Test
    fun `suggestions left after some are added`() {
        val left = Suggestions.remaining(listOf("weight", "waist", null))
        assertEquals("chest", left.first().key)
        assertEquals(Suggestions.all.size - 2, left.size)
    }

    @Test
    fun `decimal input reads persian and latin`() {
        assertEquals(76.8, DecimalInput.parse("۷۶٫۸"))
        assertEquals(76.8, DecimalInput.parse("76.8"))
        assertEquals(76.8, DecimalInput.parse(" 76,8 "))
        assertEquals(76.8, DecimalInput.parse("۷۶/۸"))
        assertEquals(12.0, DecimalInput.parse("۱۲"))
        assertNull(DecimalInput.parse(""))
        assertNull(DecimalInput.parse("1.2.3"))
        assertNull(DecimalInput.parse("-3"))
        assertNull(DecimalInput.parse("abc"))
        assertEquals("۷۶٫۸", DecimalInput.display("76.8"))
        assertEquals(76.8, DecimalInput.parse(DecimalInput.display("76,8")))
    }
}
