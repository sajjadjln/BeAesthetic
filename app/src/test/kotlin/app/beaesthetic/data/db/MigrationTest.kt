package app.beaesthetic.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every migration opens the old schema with real-looking data and checks it afterwards (ARCHITECTURE §8).
 * Never fallbackToDestructiveMigration: data must survive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun `1 to 2 keeps every row and adds letters and the new settings`() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO settings (id, programStartDate, programEndDate, installDate, rolloverHour, specialCapPerWeek, followUpMinutes, ritualTime, " +
                    "alarmMode, accentColor, themeMode, showBuddy, onboardingDone, afterFinishMode, experimentFlags) " +
                    "VALUES (1, '2026-10-09', '2027-03-20', '2026-10-10', 4, 2, 45, 1140, 'ALARM_CLOCK', 'BLUE', 'DARK', 1, 0, 'NONE', 0)",
            )
            db.execSQL(
                "INSERT INTO task (id, title, icon, part, repeat, weekdays, startDate, isImportant, inMinimumDay, action, alarmWeekdays, alarmEnabled, sortOrder, createdAt) " +
                    "VALUES (7, 'مسواک', 'TOOTH', 'NIGHT', 'DAILY', 0, '2026-10-10', 1, 1, 'NONE', 0, 0, 0, 1791600000000)",
            )
            db.execSQL("INSERT INTO completion (taskId, date, completedAt) VALUES (7, '2026-10-10', 1791660000000)")
            db.execSQL("INSERT INTO day (date, minimumMode, dueCount, doneCount, floorDone) VALUES ('2026-10-10', 0, 16, 9, 1)")
        }

        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT accentColor, firstLetterOfferDone, autostartConfirmed, healthSeenAt FROM settings").use { c ->
                c.moveToFirst()
                assertEquals("BLUE", c.getString(0))
                assertEquals(0, c.getInt(1))
                assertEquals(0, c.getInt(2))
                assertEquals(true, c.isNull(3))
            }
            db.query("SELECT title FROM task WHERE id = 7").use { c -> c.moveToFirst(); assertEquals("مسواک", c.getString(0)) }
            db.query("SELECT COUNT(*) FROM completion").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT doneCount FROM day").use { c -> c.moveToFirst(); assertEquals(9, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM letter").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }

    @Test
    fun `2 to 3 keeps every row and adds the empty workout tables`() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO task (id, title, icon, part, repeat, weekdays, startDate, isImportant, inMinimumDay, action, alarmWeekdays, alarmEnabled, sortOrder, createdAt) " +
                    "VALUES (3, 'تمرین', 'DUMBBELL', 'MORNING', 'WEEKDAYS', 85, '2026-10-10', 0, 0, 'WORKOUT', 0, 0, 0, 1791600000000)",
            )
            db.execSQL("INSERT INTO completion (taskId, date, completedAt) VALUES (3, '2026-10-12', 1791800000000)")
            db.execSQL("INSERT INTO letter (id, writtenOn, writtenAt, opensOn, text, isFirst) VALUES (1, '2026-10-11', 1791700000000, '2026-11-22', 'سلام', 1)")
        }

        helper.runMigrationsAndValidate(DB, 3, true).use { db ->
            db.query("SELECT action FROM task WHERE id = 3").use { c -> c.moveToFirst(); assertEquals("WORKOUT", c.getString(0)) }
            db.query("SELECT COUNT(*) FROM completion").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT text FROM letter").use { c -> c.moveToFirst(); assertEquals("سلام", c.getString(0)) }
            listOf("exercise", "workout", "workout_exercise", "session", "set_log").forEach { table ->
                db.query("SELECT COUNT(*) FROM $table").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
            }
            // A set with kg in halves round-trips.
            db.execSQL("INSERT INTO set_log (sessionId, exerciseId, setIndex, value, kg, effort, loggedAt) VALUES (1, 1, 0, 12, 8.5, 8, 1791800000000)")
            db.query("SELECT kg FROM set_log").use { c -> c.moveToFirst(); assertEquals(8.5, c.getDouble(0), 0.0) }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
