package app.beaesthetic.domain.reminders

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderHealthTest {

    private val now = Instant.parse("2026-10-14T12:00:00Z")
    private fun row(id: Long, minutesAgo: Long, firedAfter: Long?, outcome: Outcome, kind: AlarmKind = AlarmKind.BLOCK): LogRow {
        val at = now - Duration.ofMinutes(minutesAgo)
        return LogRow(id, kind, at, firedAfter?.let { at + Duration.ofMinutes(it) }, outcome)
    }

    @Test
    fun `late, missed and blocked`() {
        val rows = listOf(
            row(1, 120, 0, Outcome.SHOWN),
            row(2, 120, 12, Outcome.SHOWN), // late
            row(3, 120, null, Outcome.SCHEDULED), // missed
            row(4, 120, 1, Outcome.NOTIFICATIONS_OFF), // blocked
            row(5, 120, null, Outcome.CANCELLED),
            row(6, 120, null, Outcome.DEVICE_OFF),
            row(7, 5, null, Outcome.SCHEDULED), // inside the grace period
            row(8, 120, null, Outcome.SCHEDULED, AlarmKind.TEST),
            row(9, 60 * 24 * 8, null, Outcome.SCHEDULED), // older than the window
            row(10, 120, 9, Outcome.SKIPPED_EMPTY), // fired on time, nothing to show
        )
        val health = ReminderHealth.check(rows, now)
        assertEquals(5, health.checked)
        assertEquals(listOf(2L), health.late.map { it.id })
        assertEquals(listOf(3L), health.missed.map { it.id })
        assertEquals(listOf(4L), health.blocked.map { it.id })
        assertEquals(2, health.onTime)
    }

    @Test
    fun `alarms while the phone was off`() {
        val rows = listOf(
            row(1, 300, null, Outcome.SCHEDULED), // before the phone went off: a real miss
            row(2, 100, null, Outcome.SCHEDULED), // while off
            row(3, 100, 0, Outcome.SHOWN),
        )
        val off = ReminderHealth.deviceOff(rows, lastAlive = now - Duration.ofMinutes(200), bootAt = now - Duration.ofMinutes(10))
        assertEquals(listOf(2L), off.map { it.id })
        assertTrue(ReminderHealth.check(rows.filterNot { it.id == 1L }, now).late.isEmpty())
    }

    @Test
    fun `a problem already seen stays quiet until a new one`() {
        val missed = row(1, 300, null, Outcome.SCHEDULED)
        val late = row(2, 60, 15, Outcome.SHOWN)
        val health = ReminderHealth.check(listOf(missed, late), now)
        assertEquals(Attention.BLOCKED, ReminderHealth.attention(health, seenAt = null))
        // Seen after the miss but before the late one: only the late one is news.
        assertEquals(Attention.LATE, ReminderHealth.attention(health, seenAt = now - Duration.ofMinutes(120)))
        // Seen after both (e.g. a test arrived): quiet, although both are still in the week's log.
        assertEquals(Attention.NONE, ReminderHealth.attention(health, seenAt = now - Duration.ofMinutes(1)))
        // Notifications or exact alarms off always count.
        assertEquals(Attention.BLOCKED, ReminderHealth.attention(health, seenAt = now, systemOk = false))
    }
}
