package app.beaesthetic.events

import app.beaesthetic.ProgramClock
import app.beaesthetic.data.db.EventDao
import app.beaesthetic.data.db.EventEntity
import app.beaesthetic.data.db.EventType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Writes the local usage log (D59). Nothing here ever leaves the phone. */
class EventLog(private val dao: EventDao, private val clock: ProgramClock) {

    suspend fun appOpen() = log(EventType.APP_OPEN)

    suspend fun screen(name: String) = log(EventType.SCREEN, ref = name)

    /** Whether [name] was opened since [since] (the dashboard's once-a-day entrance, D76). */
    suspend fun screenSeenSince(name: String, since: java.time.Instant): Boolean = dao.countSince(EventType.SCREEN, name, since) > 0

    suspend fun notificationShown(key: String) = log(EventType.NOTIF_SHOWN, ref = key)

    suspend fun notificationTapped(key: String) = log(EventType.NOTIF_TAP, ref = key)

    /**
     * The first tick of a program date, with the minutes since that day's latest block notification
     * (if one was shown). Written once per day.
     */
    suspend fun firstTickIfNeeded(date: LocalDate, rolloverHour: Int) {
        val ref = date.toString()
        if (dao.count(EventType.FIRST_TICK, ref) > 0) return
        val now = clock.now()
        val dayStart = LocalDateTime.of(date, java.time.LocalTime.of(rolloverHour, 0)).atZone(zoneOf()).toInstant()
        val shown = dao.latest(EventType.NOTIF_SHOWN, dayStart)
        val minutes = shown?.let { Duration.between(it.at, now).toMinutes().toInt() }
        log(EventType.FIRST_TICK, ref = ref, valueMinutes = minutes)
    }

    suspend fun prune() = dao.deleteBefore(clock.now() - Duration.ofDays(180))

    private fun zoneOf(): ZoneId = clock.zone()

    private suspend fun log(type: String, ref: String? = null, valueMinutes: Int? = null) =
        dao.insert(EventEntity(at = clock.now(), type = type, ref = ref, valueMinutes = valueMinutes))
}
