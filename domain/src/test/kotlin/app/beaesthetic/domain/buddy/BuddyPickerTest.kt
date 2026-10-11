package app.beaesthetic.domain.buddy

import app.beaesthetic.domain.time.Part
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuddyPickerTest {

    private val base = TodayState(
        minimumMode = false, special = false, comebackFirstTick = false,
        done = 3, total = 12, selectedPartDone = false, currentPart = Part.MIDDAY, nightDone = false,
    )

    @Test
    fun `pose follows the day`() {
        assertEquals(BuddyChoice(Pose.THUMBS, Moment.TICK), BuddyPicker.today(base))
        assertEquals(Pose.STRETCH, BuddyPicker.today(base.copy(done = 0, currentPart = Part.MORNING)).pose)
        assertEquals(Pose.WAVE, BuddyPicker.today(base.copy(done = 0, currentPart = Part.EVENING)).pose)
        assertEquals(Pose.CHEER, BuddyPicker.today(base.copy(selectedPartDone = true)).pose)
        assertEquals(Pose.FLEX, BuddyPicker.today(base.copy(done = 12, selectedPartDone = true)).pose)
        assertEquals(Pose.SLEEP, BuddyPicker.today(base.copy(currentPart = Part.NIGHT, nightDone = true, selectedPartDone = true)).pose)
        assertEquals(Pose.HUG, BuddyPicker.today(base.copy(done = 1, comebackFirstTick = true)).pose)
        assertEquals(BuddyChoice(Pose.TEA, Moment.MINIMUM), BuddyPicker.today(base.copy(minimumMode = true, done = 12)))
        assertEquals(BuddyChoice(Pose.TEA, Moment.SPECIAL), BuddyPicker.today(base.copy(special = true)))
    }

    @Test
    fun `no line comes back within 7 days while another is left`() {
        val today = LocalDate.of(2026, 11, 1)
        assertEquals(0, BuddyPicker.line(3, emptyMap(), today))
        assertEquals(1, BuddyPicker.line(3, mapOf(0 to today), today))
        assertEquals(2, BuddyPicker.line(3, mapOf(0 to today.minusDays(1), 1 to today), today))
        // All shown recently: the least recent comes back.
        assertEquals(1, BuddyPicker.line(3, mapOf(0 to today.minusDays(1), 1 to today.minusDays(3), 2 to today), today))
        // Shown 7 days ago is allowed again.
        assertEquals(0, BuddyPicker.line(2, mapOf(0 to today.minusDays(7), 1 to today), today))
    }

    @Test
    fun `simulated weeks never repeat a line within 7 days when the pool allows it`() {
        val shown = mutableMapOf<Int, LocalDate>()
        var day = LocalDate.of(2026, 10, 10)
        val history = mutableListOf<Pair<LocalDate, Int>>()
        repeat(60) {
            val i = BuddyPicker.line(8, shown, day)
            history += day to i
            shown[i] = day
            day = day.plusDays(1)
        }
        history.forEach { (d, i) ->
            assertTrue(history.none { (d2, i2) -> i2 == i && d2 > d && d2 < d.plusDays(7) }, "line $i repeated within 7 days of $d")
        }
    }

    @Test
    fun `outfit per chapter`() {
        assertEquals(listOf(Outfit.NONE, Outfit.BAND, Outfit.WINTER, Outfit.SHADES, Outfit.SPROUT), (1..5).map(BuddyPicker::outfit))
    }
}
