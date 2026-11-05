package app.lantern.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Room migrations (ARCHITECTURE §8, B3): every schema in `app/schemas` opens with real-looking rows and
 * migrates to the newest with the rows intact. The schemas come from the debug assets; this runs in every
 * `./gradlew check`. Never `fallbackToDestructiveMigration`: data must survive.
 *
 * A new schema version is picked up on its own by [every exported schema migrates to the newest]; a
 * hand-written migration also gets its own test here with the rows it changes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The schema versions exported to `app/schemas`, oldest first. */
    private val versions: List<Int> =
        context.assets.list(AppDatabase::class.java.name).orEmpty().mapNotNull { it.removeSuffix(".json").toIntOrNull() }.sorted()

    private val newest get() = versions.last()

    @Test
    fun `the exported schemas start at 1 and the newest is the database's version`() {
        assertEquals((1..newest).toList(), versions)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            assertEquals(newest, db.openHelper.writableDatabase.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun `1 to 2 keeps every row and adds the character and the layout with their defaults`() {
        helper.createDatabase(DB, 1).use { fillVersion1(it) }

        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT character, todayLayout, accentColor, backupTreeUri, onboardingDone, healthSeenAt FROM settings").use { c ->
                c.moveToFirst()
                assertEquals("KNIGHT", c.getString(0))
                assertEquals("LIST", c.getString(1))
                assertEquals("GREEN", c.getString(2))
                assertEquals("content://tree/docs", c.getString(3))
                assertEquals(1, c.getInt(4))
                assertTrue(c.isNull(5))
            }
            assertRowsOfVersion1(db)
        }
    }

    @Test
    fun `every exported schema migrates to the newest and opens through Room`() {
        for (from in versions.dropLast(1)) {
            val name = "from-$from.db"
            helper.createDatabase(name, from).use { db ->
                // Version 1's rows fit every later schema: changes are additive (ARCHITECTURE §8).
                fillVersion1(db)
            }
            helper.runMigrationsAndValidate(name, newest, true).use { assertRowsOfVersion1(it) }

            // The app's own builder opens it (the migrations it ships, nothing destructive) and reads it back.
            val room = Room.databaseBuilder(context, AppDatabase::class.java, context.getDatabasePath(name).path).allowMainThreadQueries().build()
            helper.closeWhenFinished(room)
            runBlocking {
                val task = room.tasks().active().single()
                assertEquals("مسواک", task.title)
                assertEquals("بعد از صبحانه", task.anchor)
                assertEquals(LocalDate.of(2026, 10, 28), room.days().get(LocalDate.of(2026, 10, 28))?.date)
                assertEquals(1, room.completions().count(task.id, LocalDate.of(2026, 10, 28)))
                val settings = room.settings().get()!!
                assertTrue(settings.onboardingDone)
                assertEquals(8, room.slots().all().size)
            }
        }
    }

    /** Real-looking rows in every table of schema 1, with NULLs and Persian text (as 0.1.0 wrote them). */
    private fun fillVersion1(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO settings (id, programStartDate, programEndDate, installDate, halfwayDate, rolloverHour, specialCapPerWeek, followUpMinutes, ritualTime, " +
                "alarmMode, accentColor, themeMode, showBuddy, backupTreeUri, lastOpenedAt, lastBackupAt, onboardingDone, identitySentence, afterFinishMode, experimentFlags, " +
                "firstLetterOfferDone, cardDismissedOn, healthSeenAt, healthNotifiedOn, autostartConfirmed, recentsLockConfirmed, anytimeOpen, autoBackup) " +
                "VALUES (1, '2026-10-28', '2027-03-20', '2026-10-28', '2026-12-21', 4, 2, 45, 1140, 'ALARM_CLOCK', 'GREEN', 'SYSTEM', 1, 'content://tree/docs', " +
                "1793000000000, 1793000100000, 1, NULL, 'NONE', 0, 0, NULL, NULL, NULL, 1, 0, 1, 1)",
        )
        db.execSQL(
            "INSERT INTO task (id, title, icon, part, repeat, weekdays, everyNDays, onceDate, startDate, isImportant, inMinimumDay, action, alarmTime, alarmWeekdays, " +
                "alarmEnabled, anchor, remindersSilencedAt, fadeOfferedAt, sortOrder, createdAt, archivedAt) VALUES " +
                "(1, 'مسواک', 'TOOTH', 'NIGHT', 'DAILY', 0, NULL, NULL, '2026-10-28', 1, 1, 'NONE', 1380, 127, 1, 'بعد از صبحانه', NULL, NULL, 0, 1793000000000, NULL), " +
                "(2, 'شامپو', 'SHOWER', 'EVENING', 'EVERY_N_DAYS', 0, 3, NULL, '2026-10-28', 0, 0, 'NONE', NULL, 0, 0, NULL, NULL, NULL, 1, 1793000000000, 1793100000000)",
        )
        db.execSQL("INSERT INTO completion (taskId, date, completedAt) VALUES (1, '2026-10-28', 1793050000000), (2, '2026-10-28', 1793050100000)")
        db.execSQL("INSERT INTO day (date, special, minimumMode, dueCount, doneCount, floorDone) VALUES ('2026-10-28', NULL, 0, 2, 2, 1), ('2026-10-29', 'SICK', 1, 1, 0, 0)")
        for ((i, part) in listOf("MORNING", "MIDDAY", "EVENING", "NIGHT").withIndex()) {
            db.execSQL("INSERT INTO reminder_slot (profile, part, time, enabled) VALUES ('NORMAL', '$part', ${420 + i * 240}, 1), ('FRIDAY', '$part', ${480 + i * 240}, ${if (part == "EVENING") 0 else 1})")
        }
        db.execSQL("INSERT INTO reminder_log (key, kind, scheduledAt, firedAt, outcome) VALUES ('block:2026-10-28:NIGHT', 'BLOCK', 1793040000000, 1793040002000, 'SHOWN')")
        db.execSQL("INSERT INTO event (at, type, ref, valueMinutes) VALUES (1793000000000, 'APP_OPEN', NULL, NULL), (1793000005000, 'SCREEN', 'today', NULL)")
        db.execSQL("INSERT INTO backup_record (at, fileName, sizeBytes, photoCount, reason, ok, error) VALUES (1793000100000, 'lantern-1405-08-06-120000.zip', 4096, 0, 'MANUAL', 1, NULL)")
    }

    private fun assertRowsOfVersion1(db: SupportSQLiteDatabase) {
        fun count(table: String) = db.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(
            mapOf("task" to 2, "completion" to 2, "day" to 2, "reminder_slot" to 8, "reminder_log" to 1, "event" to 2, "backup_record" to 1, "settings" to 1),
            listOf("task", "completion", "day", "reminder_slot", "reminder_log", "event", "backup_record", "settings").associateWith(::count),
        )
        db.query("SELECT title, anchor, alarmTime, everyNDays, archivedAt FROM task ORDER BY id").use { c ->
            c.moveToFirst()
            assertEquals("مسواک", c.getString(0))
            assertEquals("بعد از صبحانه", c.getString(1))
            assertEquals(1380, c.getInt(2))
            c.moveToNext()
            assertEquals(3, c.getInt(3))
            assertEquals(1793100000000, c.getLong(4))
        }
        db.query("SELECT special, minimumMode FROM day WHERE date = '2026-10-29'").use { c -> c.moveToFirst(); assertEquals("SICK", c.getString(0)); assertEquals(1, c.getInt(1)) }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
