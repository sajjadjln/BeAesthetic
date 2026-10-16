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

    @Test
    fun `3 to 4 keeps the plan and the log and adds ladder links and round numbers`() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL("INSERT INTO exercise (id, name, kind) VALUES (1, 'شنا', 'REPS')")
            db.execSQL("INSERT INTO workout (id, weekday, name) VALUES (1, 2, 'Lower A')")
            db.execSQL("INSERT INTO workout_exercise (workoutId, position, exerciseId, sets, rangeMin, rangeMax, perSide) VALUES (1, 0, 1, 4, 8, 15, 0)")
            db.execSQL("INSERT INTO session (id, workoutId, date, startedAt) VALUES (1, 1, '2026-10-12', 1791800000000)")
            db.execSQL("INSERT INTO set_log (sessionId, exerciseId, setIndex, value, effort, loggedAt) VALUES (1, 1, 0, 12, 8, 1791800000000)")
        }

        helper.runMigrationsAndValidate(DB, 4, true).use { db ->
            db.query("SELECT name, harderId FROM exercise").use { c -> c.moveToFirst(); assertEquals("شنا", c.getString(0)); assertEquals(true, c.isNull(1)) }
            db.query("SELECT rangeMax FROM workout_exercise").use { c -> c.moveToFirst(); assertEquals(15, c.getInt(0)) }
            db.query("SELECT value FROM set_log").use { c -> c.moveToFirst(); assertEquals(12, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM round_number").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }

    @Test
    fun `4 to 5 keeps settings and tasks and adds the anytime card state`() {
        helper.createDatabase(DB, 4).use { db ->
            db.execSQL(
                "INSERT INTO settings (id, programStartDate, programEndDate, installDate, rolloverHour, specialCapPerWeek, followUpMinutes, ritualTime, " +
                    "alarmMode, accentColor, themeMode, showBuddy, onboardingDone, afterFinishMode, experimentFlags, firstLetterOfferDone, autostartConfirmed, recentsLockConfirmed) " +
                    "VALUES (1, '2026-10-09', '2027-03-20', '2026-10-10', 4, 2, 45, 1140, 'ALARM_CLOCK', 'TEAL', 'DARK', 1, 0, 'NONE', 0, 1, 1, 0)",
            )
            db.execSQL(
                "INSERT INTO task (id, title, icon, part, repeat, weekdays, startDate, isImportant, inMinimumDay, action, alarmWeekdays, alarmEnabled, sortOrder, createdAt) " +
                    "VALUES (3, 'تمرین', 'DUMBBELL', 'MORNING', 'WEEKDAYS', 85, '2026-10-10', 0, 0, 'WORKOUT', 0, 0, 0, 1791600000000)",
            )
        }

        helper.runMigrationsAndValidate(DB, 5, true).use { db ->
            db.query("SELECT accentColor, autostartConfirmed, anytimeOpen FROM settings").use { c ->
                c.moveToFirst()
                assertEquals("TEAL", c.getString(0))
                assertEquals(1, c.getInt(1))
                assertEquals(0, c.getInt(2))
            }
            db.query("SELECT action, weekdays FROM task WHERE id = 3").use { c -> c.moveToFirst(); assertEquals("WORKOUT", c.getString(0)); assertEquals(85, c.getInt(1)) }
        }
    }

    @Test
    fun `5 to 6 keeps settings and adds the backup history, auto backup on`() {
        helper.createDatabase(DB, 5).use { db ->
            db.execSQL(
                "INSERT INTO settings (id, programStartDate, programEndDate, installDate, rolloverHour, specialCapPerWeek, followUpMinutes, ritualTime, " +
                    "alarmMode, accentColor, themeMode, showBuddy, onboardingDone, afterFinishMode, experimentFlags, firstLetterOfferDone, autostartConfirmed, recentsLockConfirmed, anytimeOpen) " +
                    "VALUES (1, '2026-10-09', '2027-03-20', '2026-10-10', 4, 2, 45, 1140, 'ALARM_CLOCK', 'GREEN', 'SYSTEM', 1, 0, 'NONE', 0, 1, 1, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO task (id, title, icon, part, repeat, weekdays, startDate, isImportant, inMinimumDay, action, alarmWeekdays, alarmEnabled, sortOrder, createdAt) " +
                    "VALUES (21, 'جو دوسر', 'CHECK', 'MORNING', 'ANYTIME', 0, '2026-10-14', 0, 0, 'NONE', 0, 0, 0, 1791950000000)",
            )
        }

        helper.runMigrationsAndValidate(DB, 6, true).use { db ->
            db.query("SELECT anytimeOpen, autoBackup, backupTreeUri FROM settings").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
                assertEquals(1, c.getInt(1))
                assertEquals(true, c.isNull(2))
            }
            db.query("SELECT repeat FROM task WHERE id = 21").use { c -> c.moveToFirst(); assertEquals("ANYTIME", c.getString(0)) }
            db.query("SELECT COUNT(*) FROM backup_record").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }

    @Test
    fun `6 to 7 keeps the workout log and settings and adds the empty progress tables`() {
        helper.createDatabase(DB, 6).use { db ->
            db.execSQL(
                "INSERT INTO settings (id, programStartDate, programEndDate, installDate, rolloverHour, specialCapPerWeek, followUpMinutes, ritualTime, " +
                    "alarmMode, accentColor, themeMode, showBuddy, onboardingDone, afterFinishMode, experimentFlags, firstLetterOfferDone, autostartConfirmed, recentsLockConfirmed, anytimeOpen, autoBackup) " +
                    "VALUES (1, '2026-10-09', '2027-03-20', '2026-10-10', 4, 2, 45, 1140, 'ALARM_CLOCK', 'GREEN', 'SYSTEM', 1, 0, 'NONE', 0, 1, 1, 1, 0, 1)",
            )
            db.execSQL("INSERT INTO exercise (id, name, kind) VALUES (1, 'Pull-up', 'REPS')")
            db.execSQL("INSERT INTO workout (id, weekday, name) VALUES (1, 0, 'Upper A')")
            db.execSQL("INSERT INTO session (id, workoutId, date, startedAt) VALUES (1, 1, '2026-10-10', 1791660000000)")
            db.execSQL("INSERT INTO set_log (sessionId, exerciseId, setIndex, value, effort, loggedAt) VALUES (1, 1, 0, 8, 8, 1791660000000)")
        }

        helper.runMigrationsAndValidate(DB, 7, true).use { db ->
            db.query("SELECT value FROM set_log").use { c -> c.moveToFirst(); assertEquals(8, c.getInt(0)) }
            db.query("SELECT autoBackup FROM settings").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            for (table in listOf("metric", "metric_entry", "check_in", "photo")) {
                db.query("SELECT COUNT(*) FROM $table").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
            }
            // One reading a day per metric: the same day again replaces it.
            db.execSQL("INSERT INTO metric (id, kind, name, unit, higherIsBetter, sortOrder) VALUES (1, 'BODY', 'وزن', 'KG', 0, 0)")
            db.execSQL("INSERT INTO metric_entry (metricId, date, value) VALUES (1, '2026-10-09', 77.1)")
            db.execSQL("INSERT OR REPLACE INTO metric_entry (metricId, date, value) VALUES (1, '2026-10-09', 76.9)")
            db.query("SELECT COUNT(*), MAX(value) FROM metric_entry").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
                assertEquals(76.9, c.getDouble(1), 0.0001)
            }
        }
    }

    @Test
    fun `7 to 8 keeps the readings and adds the empty milestone table`() {
        helper.createDatabase(DB, 7).use { db ->
            db.execSQL("INSERT INTO metric (id, kind, name, unit, higherIsBetter, sortOrder) VALUES (1, 'STRENGTH', 'بارفیکس', 'REPS', 1, 0)")
            db.execSQL("INSERT INTO metric_entry (metricId, date, value, guess) VALUES (1, '2026-10-09', 8, 7)")
            db.execSQL("INSERT INTO photo (checkInIdx, pose, fileName, takenAt) VALUES (0, 'FRONT', '0-front.jpg', 1791900000000)")
        }
        helper.runMigrationsAndValidate(DB, 8, true).use { db ->
            db.query("SELECT value, guess FROM metric_entry").use { c -> c.moveToFirst(); assertEquals(8.0, c.getDouble(0), 0.0); assertEquals(7.0, c.getDouble(1), 0.0) }
            db.query("SELECT fileName FROM photo").use { c -> c.moveToFirst(); assertEquals("0-front.jpg", c.getString(0)) }
            db.query("SELECT COUNT(*) FROM milestone").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }

    @Test
    fun `8 to 9 keeps days and settings and adds the empty week plan and snack tables`() {
        helper.createDatabase(DB, 8).use { db ->
            db.execSQL("INSERT INTO day (date, special, minimumMode, dueCount, doneCount, floorDone) VALUES ('2026-10-15', 'OFF', 0, 4, 2, 0)")
            db.execSQL("INSERT INTO milestone (id, title, sortOrder) VALUES (1, '۱۰ بارفیکس', 0)")
        }
        helper.runMigrationsAndValidate(DB, 9, true).use { db ->
            db.query("SELECT special, doneCount FROM day").use { c -> c.moveToFirst(); assertEquals("OFF", c.getString(0)); assertEquals(2, c.getInt(1)) }
            db.query("SELECT title FROM milestone").use { c -> c.moveToFirst(); assertEquals("۱۰ بارفیکس", c.getString(0)) }
            db.query("SELECT COUNT(*) FROM week_plan").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM snack_note").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }

    @Test
    fun `9 to 10 keeps the ritual weeks and adds the empty recap table`() {
        helper.createDatabase(DB, 9).use { db ->
            db.execSQL("INSERT INTO week_plan (weekStart, ritualDoneAt) VALUES ('2026-10-10', 1791990000000)")
        }
        helper.runMigrationsAndValidate(DB, 10, true).use { db ->
            db.query("SELECT weekStart FROM week_plan").use { c -> c.moveToFirst(); assertEquals("2026-10-10", c.getString(0)) }
            db.query("SELECT COUNT(*) FROM recap").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
