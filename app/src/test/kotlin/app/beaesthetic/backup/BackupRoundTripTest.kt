package app.beaesthetic.backup

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.repo.Seed
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * The slice 6 gate (ARCHITECTURE §9): export, wipe, restore, and every table and photo comes back the same.
 * Runs on the real schema through Robolectric's SQLite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BackupRoundTripTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var dir: File
    private val sql: SupportSQLiteDatabase get() = db.openHelper.writableDatabase
    private val schema get() = sql.version

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "backup-test").apply { deleteRecursively(); mkdirs() }
        db = Room.databaseBuilder(context, AppDatabase::class.java, File(dir, "test.db").path).allowMainThreadQueries().build()
        runBlocking { Seed.ensure(db, context, LocalDate.of(2026, 10, 10), Instant.parse("2026-10-10T06:00:00Z")) }
        fillEveryTable()
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    @Test
    fun `export, wipe and restore bring back every table and photo`() {
        val photos = File(dir, "photos").apply { mkdirs() }
        File(photos, "0-front.jpg").writeBytes(ByteArray(3000) { it.toByte() })
        File(photos, "sets/1/side.jpg").apply { parentFile!!.mkdirs() }.writeBytes(byteArrayOf(1, 2, 3))
        val before = snapshot()
        val photosBefore = photoBytes(photos)

        val zip = File(dir, "backup.zip")
        val manifest = zip.outputStream().use { BackupArchive.write(sql, null, photos, "0.6.0", schema, "MANUAL", Instant.parse("2026-10-14T18:00:00Z"), it) }
        BackupArchive.verify(zip, manifest, schema)
        assertEquals(2, manifest.photos.size)
        assertEquals(before.mapValues { it.value.size }, manifest.counts)

        // Wipe everything, photos too, as if this were a new phone.
        BackupArchive.tables(sql).forEach { sql.execSQL("DELETE FROM `$it`") }
        photos.deleteRecursively()
        assertTrue(snapshot().values.all { it.isEmpty() })

        BackupArchive.restore(sql, zip, photos, schema) { block -> db.runInTransaction(block) }
        assertEquals(before, snapshot())
        assertEquals(photosBefore, photoBytes(photos))
    }

    @Test
    fun `restore keeps this phone's folder, backup state and history`() {
        val zip = File(dir, "backup.zip")
        val manifest = zip.outputStream().use { BackupArchive.write(sql, null, File(dir, "none"), "0.6.0", schema, "MANUAL", Instant.now(), it) }
        assertFalse("backup_record" in manifest.counts)

        sql.execSQL("UPDATE settings SET backupTreeUri = 'content://tree/here', autoBackup = 0, lastBackupAt = 1791900000000, accentColor = 'ORANGE'")
        sql.execSQL("INSERT INTO backup_record (at, fileName, sizeBytes, photoCount, reason, ok) VALUES (1791900000000, 'b.zip', 10, 0, 'MANUAL', 1)")
        BackupArchive.restore(sql, zip, File(dir, "photos"), schema) { block -> db.runInTransaction(block) }

        sql.query("SELECT backupTreeUri, autoBackup, lastBackupAt, accentColor FROM settings").use { c ->
            c.moveToFirst()
            assertEquals("content://tree/here", c.getString(0))
            assertEquals(0, c.getInt(1))
            assertEquals(1791900000000, c.getLong(2))
            assertEquals("BLUE", c.getString(3)) // the backup's own choice comes back
        }
        sql.query("SELECT COUNT(*) FROM backup_record").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
    }

    @Test
    fun `an older backup without newer tables and columns restores with defaults`() {
        val zip = File(dir, "backup.zip")
        zip.outputStream().use { BackupArchive.write(sql, null, File(dir, "none"), "0.6.0", schema, "MANUAL", Instant.now(), it) }
        // Make it look like a v4 backup: no anytimeOpen or autoBackup in settings, no round_number table.
        val old = rewriteData(zip, schemaVersion = 4) { tables ->
            val row = tables.getValue("settings").jsonArray[0].jsonObject - "anytimeOpen" - "autoBackup"
            JsonObject(tables - "round_number" + ("settings" to JsonArray(listOf(JsonObject(row)))))
        }
        sql.execSQL("UPDATE settings SET anytimeOpen = 1")

        BackupArchive.restore(sql, old, File(dir, "photos"), schema) { block -> db.runInTransaction(block) }
        sql.query("SELECT anytimeOpen FROM settings").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        sql.query("SELECT COUNT(*) FROM round_number").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        sql.query("SELECT COUNT(*) FROM task").use { c -> c.moveToFirst(); assertTrue(c.getInt(0) > 0) }
    }

    @Test
    fun `a backup from a newer app or another file is refused before anything changes`() {
        val zip = File(dir, "backup.zip")
        zip.outputStream().use { BackupArchive.write(sql, null, File(dir, "none"), "0.6.0", schema, "MANUAL", Instant.now(), it) }
        try {
            BackupArchive.readManifest(zip, currentSchema = schema - 1)
            fail("newer schema accepted")
        } catch (e: BackupException) {
            assertEquals(BackupException.Kind.NEWER, e.kind)
        }
        val notOurs = File(dir, "photo.zip").apply { outputStream().use { ZipOutputStream(it).use { z -> z.putNextEntry(ZipEntry("a.jpg")); z.write(1); z.closeEntry() } } }
        try {
            BackupArchive.readManifest(notOurs, schema)
            fail("foreign zip accepted")
        } catch (e: BackupException) {
            assertEquals(BackupException.Kind.NOT_A_BACKUP, e.kind)
        }
    }

    /** Every table but the local ones, as rows of column → value, in rowid order. */
    private fun snapshot(): Map<String, List<Map<String, Any?>>> =
        BackupArchive.tables(sql).associateWith { table ->
            sql.query("SELECT * FROM `$table` ORDER BY rowid").use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add((0 until c.columnCount).associate { i ->
                            c.getColumnName(i) to when (c.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> null
                                android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                else -> c.getString(i)
                            }
                        })
                    }
                }
            }
        }

    private fun photoBytes(dir: File): Map<String, List<Byte>> =
        dir.walkTopDown().filter { it.isFile }.associate { it.relativeTo(dir).invariantSeparatorsPath to it.readBytes().toList() }

    /** Writes a copy of [zip] whose data.json tables went through [change], with an older schema number. */
    private fun rewriteData(zip: File, schemaVersion: Int, change: (JsonObject) -> JsonObject): File {
        val out = File(dir, "old.zip")
        ZipFile(zip).use { z ->
            ZipOutputStream(out.outputStream()).use { o ->
                for (entry in z.entries()) {
                    val text = z.getInputStream(entry).readBytes()
                    o.putNextEntry(ZipEntry(entry.name))
                    when (entry.name) {
                        BackupArchive.DATA -> {
                            val root = Json.parseToJsonElement(String(text)).jsonObject
                            o.write(buildJsonObject {
                                put("formatVersion", 1)
                                put("dbSchemaVersion", schemaVersion)
                                put("tables", change(root.getValue("tables").jsonObject))
                            }.toString().toByteArray())
                        }
                        BackupArchive.MANIFEST -> {
                            val m = Json.parseToJsonElement(String(text)).jsonObject
                            o.write(JsonObject(m + ("dbSchemaVersion" to JsonPrimitive(schemaVersion))).toString().toByteArray())
                        }
                        else -> o.write(text)
                    }
                    o.closeEntry()
                }
            }
        }
        return out
    }

    /** Real-looking rows in every table the seed leaves empty, with NULLs, REALs and Persian text. */
    private fun fillEveryTable() {
        sql.execSQL("UPDATE settings SET accentColor = 'BLUE', identitySentence = 'من کسی‌ام که سر قولش می‌مونه'")
        sql.execSQL("INSERT INTO completion (taskId, date, completedAt) VALUES (1, '2026-10-10', 1791660000000), (2, '2026-10-11', 1791746400000)")
        sql.execSQL("INSERT INTO day (date, special, minimumMode, dueCount, doneCount, floorDone) VALUES ('2026-10-10', NULL, 0, 16, 9, 1), ('2026-10-11', 'TRAVEL', 1, 4, 4, 1)")
        sql.execSQL("INSERT INTO letter (writtenOn, writtenAt, opensOn, text, openedAt, isFirst) VALUES ('2026-10-10', 1791660000000, '2027-03-20', 'سلام پیکرِ آینده', NULL, 1)")
        sql.execSQL("INSERT INTO exercise (id, name, kind, harderId) VALUES (1, 'شنا', 'REPS', 2), (2, 'Decline push-up', 'REPS', NULL), (3, 'Wall sit', 'SECONDS', NULL)")
        sql.execSQL("INSERT INTO workout (id, weekday, name) VALUES (1, 2, 'Lower A'), (2, 4, 'Upper B')")
        sql.execSQL("INSERT INTO workout_exercise (workoutId, position, exerciseId, sets, rangeMin, rangeMax, kg, perSide, targetEffort, restSeconds, note) VALUES (1, 0, 1, 4, 8, 15, NULL, 0, 8, 90, '۵ ثانیه پایین'), (2, 0, 3, 3, NULL, NULL, 6.5, 1, NULL, NULL, NULL)")
        sql.execSQL("INSERT INTO session (id, workoutId, date, startedAt, finishedAt) VALUES (1, 1, '2026-10-12', 1791800000000, 1791803000000)")
        sql.execSQL("INSERT INTO set_log (sessionId, exerciseId, setIndex, value, kg, effort, loggedAt) VALUES (1, 1, 0, 12, NULL, 8, 1791800100000), (1, 3, 0, 45, 6.5, 7, 1791800200000)")
        sql.execSQL("INSERT INTO round_number (key, reachedOn, seenAt) VALUES ('workouts:10', '2026-10-12', NULL)")
        sql.execSQL("INSERT INTO event (at, type, ref, valueMinutes) VALUES (1791660000000, 'APP_OPEN', NULL, NULL), (1791660100000, 'BUDDY_LINE', 'TICK:3', 4)")
        sql.execSQL("INSERT INTO reminder_log (key, kind, scheduledAt, firedAt, outcome) VALUES ('block:2026-10-10:MORNING', 'BLOCK', 1791640000000, 1791640003000, 'SHOWN')")
        sql.execSQL("INSERT INTO metric (id, kind, key, name, unit, higherIsBetter, sortOrder, archivedAt) VALUES (1, 'BODY', 'weight', 'وزن', 'KG', 0, 0, NULL), (2, 'STRENGTH', NULL, 'اسکات یک‌پا', 'REPS', 1, 1, 1791900000000)")
        sql.execSQL("INSERT INTO metric_entry (metricId, date, value, guess, note) VALUES (1, '2026-10-09', 77.1, NULL, NULL), (2, '2026-10-09', 6, 5, 'چپ ضعیف‌تر')")
        sql.execSQL("INSERT INTO check_in (idx, date, doneAt) VALUES (0, '2026-10-09', 1791900000000)")
        sql.execSQL("INSERT INTO photo (checkInIdx, pose, fileName, takenAt) VALUES (0, 'FRONT', '0-front.jpg', 1791900000000)")
        sql.execSQL("INSERT INTO milestone (id, title, metricId, target, reachedOn, sortOrder) VALUES (1, '۱۰ بارفیکس', 1, 10.0, NULL, 0), (2, 'دماوند', NULL, NULL, '2026-11-01', 1)")
        val empty = BackupArchive.tables(sql).filter { t -> sql.query("SELECT COUNT(*) FROM `$t`").use { it.moveToFirst(); it.getInt(0) == 0 } }
        assertEquals("every table has rows", emptyList<String>(), empty)
    }
}
