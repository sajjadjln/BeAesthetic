package app.lantern

import app.lantern.domain.time.ProgramCalendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The one source of "now" (ARCHITECTURE §4). The debug menu can shift it with [offset] to check
 * time-dependent screens without waiting. The zone is read on every call, so a time-zone change is
 * picked up at once.
 */
class ProgramClock {
    @Volatile var offset: Duration = Duration.ZERO

    fun now(): Instant = Instant.now() + offset

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun localNow(): LocalDateTime = LocalDateTime.ofInstant(now(), zone())

    fun programDate(rolloverHour: Int): LocalDate = ProgramCalendar.programDate(localNow(), rolloverHour)

    /** Emits the current time now and then at the start of every minute; enough for the rollover and part changes. */
    fun minuteTicks(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = localNow()
            emit(now)
            delay(60_000L - (now.second * 1000L + now.nano / 1_000_000))
        }
    }
}
