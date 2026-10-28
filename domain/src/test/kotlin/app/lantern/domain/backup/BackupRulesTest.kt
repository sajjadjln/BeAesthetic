package app.lantern.domain.backup

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupRulesTest {

    @Test
    fun `file names carry the Jalali date and sort by time`() {
        assertEquals("lantern-1405-07-22-211904.zip", BackupRules.fileName(LocalDateTime.of(2026, 10, 14, 21, 19, 4)))
        assertEquals("lantern-1405-12-29-070500.zip", BackupRules.fileName(LocalDateTime.of(2027, 3, 20, 7, 5)))
        val a = BackupRules.fileName(LocalDateTime.of(2026, 12, 31, 23, 0)) // 10 Dey
        val b = BackupRules.fileName(LocalDateTime.of(2027, 1, 1, 8, 0)) // 11 Dey
        assertTrue(a < b)
    }

    @Test
    fun `only the newest eight backups stay, other files are never touched`() {
        val names = (1..10).map { "lantern-1405-08-%02d-1200.zip".format(it) } + listOf("notes.txt", "lantern-1405-08-11-1200.zip.partial")
        val delete = BackupRules.toDelete(names)
        assertEquals(listOf("lantern-1405-08-02-1200.zip", "lantern-1405-08-01-1200.zip"), delete)
        assertTrue(BackupRules.isLeftover("lantern-1405-08-11-1200.zip.partial"))
        assertFalse(BackupRules.isLeftover("lantern-1405-08-11-1200.zip"))
    }

    @Test
    fun `the automatic backup runs after a week or when there is none, not just because it's Friday`() {
        val wednesday = LocalDateTime.of(2026, 10, 14, 9, 0)
        val friday = LocalDateTime.of(2026, 10, 16, 9, 0)
        assertTrue(BackupRules.autoDue(true, true, null, wednesday))
        assertFalse(BackupRules.autoDue(true, true, wednesday.minusDays(6), wednesday))
        assertTrue(BackupRules.autoDue(true, true, wednesday.minusDays(7), wednesday))
        assertFalse(BackupRules.autoDue(true, true, wednesday, friday))
        assertFalse(BackupRules.autoDue(true, true, friday.minusHours(1), friday))
        assertFalse(BackupRules.autoDue(false, true, null, friday))
        assertFalse(BackupRules.autoDue(true, false, null, friday))
    }

    @Test
    fun `the notice asks for a folder first, then a failed try, then age`() {
        val today = LocalDate.of(2026, 10, 30)
        assertEquals(BackupNotice.NO_FOLDER, BackupRules.notice(false, today, false, today))
        assertEquals(BackupNotice.FAILED, BackupRules.notice(true, today, true, today))
        assertEquals(BackupNotice.NONE, BackupRules.notice(true, today.minusDays(10), false, today))
        assertEquals(BackupNotice.OVERDUE, BackupRules.notice(true, today.minusDays(11), false, today))
        assertEquals(BackupNotice.OVERDUE, BackupRules.notice(true, null, false, today))
    }
}
