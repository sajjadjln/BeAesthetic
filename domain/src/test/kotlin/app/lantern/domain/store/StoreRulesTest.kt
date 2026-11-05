package app.lantern.domain.store

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoreRulesTest {
    private val today = LocalDate.of(2026, 11, 20)
    private fun days(vararg back: Long) = back.map { today.minusDays(it) }.toSet()

    @Test
    fun `asks on the 7th tick day when yesterday had a tick`() {
        assertTrue(StoreRules.shouldAskRating(today, days(0, 1, 2, 3, 4, 5, 6), answered = false))
    }

    @Test
    fun `not before the 7th tick day`() {
        assertFalse(StoreRules.shouldAskRating(today, days(0, 1, 2, 3, 4, 5), answered = false))
    }

    @Test
    fun `never on a day after a gap`() {
        assertFalse(StoreRules.shouldAskRating(today, days(0, 2, 3, 4, 5, 6, 7, 8), answered = false))
    }

    @Test
    fun `not before today has a tick`() {
        assertFalse(StoreRules.shouldAskRating(today, days(1, 2, 3, 4, 5, 6, 7), answered = false))
    }

    @Test
    fun `tick days need not be in a row`() {
        assertTrue(StoreRules.shouldAskRating(today, days(0, 1, 5, 9, 12, 20, 30), answered = false))
    }

    @Test
    fun `future dates do not count`() {
        val ticks = days(0, 1, 2, 3, 4, 5) + today.plusDays(1)
        assertFalse(StoreRules.shouldAskRating(today, ticks, answered = false))
    }

    @Test
    fun `an answer is honored forever`() {
        assertFalse(StoreRules.shouldAskRating(today, days(*LongArray(60) { it.toLong() }), answered = true))
    }

    @Test
    fun `whats new never on a fresh install`() {
        assertNull(StoreRules.whatsNewFor(908, lastSeen = null, freshInstall = true))
    }

    @Test
    fun `whats new once after an update`() {
        assertEquals(908, StoreRules.whatsNewFor(908, lastSeen = null, freshInstall = false))
        assertEquals(909, StoreRules.whatsNewFor(909, lastSeen = 908, freshInstall = false))
        assertNull(StoreRules.whatsNewFor(909, lastSeen = 909, freshInstall = false))
        assertNull(StoreRules.whatsNewFor(908, lastSeen = 909, freshInstall = false))
    }
}
