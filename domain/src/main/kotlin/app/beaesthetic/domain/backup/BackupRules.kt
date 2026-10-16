package app.beaesthetic.domain.backup

import app.beaesthetic.domain.time.Jalali
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Why a backup was made (shown in the list). */
enum class BackupReason { MANUAL, AUTO, BEFORE_RESTORE }

/** What Today and Settings say about backups (SPEC §8): nothing, no folder yet, too old, or the last try failed. */
enum class BackupNotice { NONE, NO_FOLDER, OVERDUE, FAILED }

/** The pure rules of slice 6 (ARCHITECTURE §9, D39, D132–D134). */
object BackupRules {

    /** The folder keeps this many backups (D39). */
    const val KEEP = 8

    /** Older than this, Settings and Today say so (SPEC §8). */
    const val OVERDUE_DAYS = 10L

    /** The automatic safety net: a backup on app open once this many days have passed (ARCHITECTURE §9). */
    const val AUTO_DAYS = 7L

    const val PREFIX = "beaesthetic-"
    const val EXTENSION = ".zip"
    const val PARTIAL = ".partial"

    /**
     * `beaesthetic-1405-07-22-211904.zip`: the Jalali date and the local time to the second, ASCII digits, so
     * names sort by time, never collide, and read easily on the PC.
     */
    fun fileName(local: LocalDateTime): String {
        val j = Jalali.fromGregorian(local.toLocalDate())
        return PREFIX + "%04d-%02d-%02d-%02d%02d%02d".format(java.util.Locale.ROOT, j.year, j.month, j.day, local.hour, local.minute, local.second) + EXTENSION
    }

    fun isBackupName(name: String): Boolean = name.startsWith(PREFIX) && name.endsWith(EXTENSION)

    /** Which files to delete so only the newest [KEEP] backups stay. Other files in the folder are never touched. */
    fun toDelete(names: Collection<String>, keep: Int = KEEP): List<String> =
        names.filter(::isBackupName).sortedDescending().drop(keep)

    /** Leftover `.partial` files from a backup that was cut off: never valid, always removed. */
    fun isLeftover(name: String): Boolean = name.startsWith(PREFIX) && name.endsWith(EXTENSION + PARTIAL)

    /**
     * The automatic backup on app open (D133): when switched on and a folder is chosen, if there is no backup
     * yet or the last one is [AUTO_DAYS] or more days old. The weekly one runs when the Friday ritual ends (D140).
     */
    fun autoDue(enabled: Boolean, hasFolder: Boolean, lastOk: LocalDateTime?, now: LocalDateTime): Boolean {
        if (!enabled || !hasFolder) return false
        if (lastOk == null) return true
        return Duration.between(lastOk, now).toDays() >= AUTO_DAYS
    }

    /** The notice for Today and Settings. A failed last try comes first: it means the folder needs a look. */
    fun notice(hasFolder: Boolean, lastOk: LocalDate?, lastFailed: Boolean, today: LocalDate): BackupNotice = when {
        !hasFolder -> BackupNotice.NO_FOLDER
        lastFailed -> BackupNotice.FAILED
        lastOk == null || lastOk.plusDays(OVERDUE_DAYS) < today -> BackupNotice.OVERDUE
        else -> BackupNotice.NONE
    }
}
