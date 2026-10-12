package app.beaesthetic.domain.reminders

import app.beaesthetic.domain.time.Part
import java.time.Duration
import java.time.Instant

/** What happened to one planned alarm (the `reminder_log` outcome). */
enum class Outcome { SCHEDULED, SHOWN, SKIPPED_EMPTY, SKIPPED_LATE, NOTIFICATIONS_OFF, CANCELLED, DEVICE_OFF }

data class LogRow(val id: Long, val kind: AlarmKind, val scheduledAt: Instant, val firedAt: Instant?, val outcome: Outcome, val key: String = "") {
    /** The part of the day of a block or follow-up ("block:2026-10-14:NIGHT"). */
    val part: Part?
        get() = key.split(":").getOrNull(2)?.let { p -> Part.entries.firstOrNull { it.name == p } }
}

/**
 * Reminder health over a window (ARCHITECTURE §7.5). A row more than [GRACE] past its time with no fire is
 * missed; a fire more than [GRACE] late is late; a fire the phone wouldn't show is blocked.
 */
data class Health(val checked: Int, val late: List<LogRow>, val missed: List<LogRow>, val blocked: List<LogRow>) {
    val onTime: Int get() = checked - late.size - missed.size - blocked.size
    val ok: Boolean get() = late.isEmpty() && missed.isEmpty() && blocked.isEmpty()
}

/** What the health line and Today's banner say (D96). */
enum class Attention { NONE, LATE, BLOCKED }

object ReminderHealth {
    val GRACE: Duration = Duration.ofMinutes(10)
    val WINDOW: Duration = Duration.ofDays(7)

    fun check(rows: List<LogRow>, now: Instant, since: Instant = now - WINDOW): Health {
        val due = rows.filter {
            it.kind != AlarmKind.TEST &&
                it.outcome != Outcome.CANCELLED && it.outcome != Outcome.DEVICE_OFF &&
                it.scheduledAt >= since && it.scheduledAt <= now - GRACE
        }
        val missed = due.filter { it.firedAt == null }
        val fired = due.filter { it.firedAt != null }
        val blocked = fired.filter { it.outcome == Outcome.NOTIFICATIONS_OFF }
        val late = fired.filter { it.outcome != Outcome.NOTIFICATIONS_OFF && Duration.between(it.scheduledAt, it.firedAt) > GRACE }
        return Health(due.size, late, missed, blocked)
    }

    /**
     * Whether reminders need a look. Problems before [seenAt] were already seen (the user opened «گوشی», or a
     * test notification arrived) and are quiet until something new happens. Notifications or exact alarms
     * being off ([systemOk] false) always count. One answer for Today's banner and every health line, so
     * they never disagree.
     */
    fun attention(health: Health, seenAt: Instant?, systemOk: Boolean = true): Attention {
        fun fresh(row: LogRow) = seenAt == null || row.scheduledAt > seenAt
        return when {
            !systemOk || (health.missed + health.blocked).any(::fresh) -> Attention.BLOCKED
            health.late.any(::fresh) -> Attention.LATE
            else -> Attention.NONE
        }
    }

    /**
     * Alarms that fell while the phone was off: after the last sign of life before boot and before the boot.
     * They are marked DEVICE_OFF and never count as missed.
     */
    fun deviceOff(rows: List<LogRow>, lastAlive: Instant?, bootAt: Instant): List<LogRow> = rows.filter {
        it.firedAt == null && it.outcome == Outcome.SCHEDULED &&
            it.scheduledAt < bootAt && (lastAlive == null || it.scheduledAt > lastAlive)
    }
}
