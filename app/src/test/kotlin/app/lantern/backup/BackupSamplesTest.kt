package app.lantern.backup

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import app.lantern.data.db.AppDatabase
import app.lantern.data.repo.Seed
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * A backup from every shipped version restores into the newest schema with its data intact (B3). The
 * samples live in `src/test/resources/backups/` (their README says where each one comes from). When a
 * new backup format or database schema ships, this test fails until a sample for it is added.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BackupSamplesTest {

    /** One sample per shipped (format, schema). */
    private data class Sample(val file: String, val appVersion: String, val format: Int, val schema: Int)

    private val samples = listOf(
        Sample("lantern-0.1.0-schema1.zip", "0.1.0", format = 1, schema = 1),
        Sample("lantern-0.9.4-schema2.zip", "0.9.4", format = 1, schema = 2),
        Sample("lantern-goals-schema3.zip", "goals-dev", format = 1, schema = 3),
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "samples-test").apply { deleteRecursively(); mkdirs() }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `there is a sample for every backup format and every database schema`() {
        val db = openFresh("count.db")
        val newest = try { db.openHelper.writableDatabase.version } finally { db.close() }
        assertEquals((1..BackupArchive.FORMAT_VERSION).toSet(), samples.map { it.format }.toSet())
        assertEquals((1..newest).toSet(), samples.map { it.schema }.toSet())
    }

    @Test
    fun `every sample restores into the newest schema with its rows intact`() {
        for (sample in samples) {
            val zip = File(dir, sample.file).apply { writeBytes(resource(sample.file)) }
            val db = openFresh(sample.file + ".db")
            try {
                val sql = db.openHelper.writableDatabase
                val schema = sql.version
                // This phone already has its own backup folder; a restore must keep it (D132).
                sql.execSQL("UPDATE settings SET backupTreeUri = 'content://tree/this-phone', lastBackupAt = 1795000000000")

                val manifest = BackupArchive.readManifest(zip, schema)
                assertEquals(sample.file, sample.appVersion, manifest.appVersion)
                assertEquals(sample.file, sample.format, manifest.formatVersion)
                assertEquals(sample.file, sample.schema, manifest.dbSchemaVersion)

                BackupArchive.restore(sql, zip, File(dir, "photos"), schema) { block -> db.runInTransaction(block) }

                val backup = BackupArchive.readTables(zip)
                assertEquals(sample.file, manifest.counts, backup.mapValues { it.value.size })
                for ((table, rows) in backup) {
                    val skip = if (table == "settings") BackupArchive.LOCAL_SETTINGS.toSet() else emptySet()
                    val expected = rows.map { row -> row.jsonObject.filterKeys { it !in skip } }
                    val columns = expected.firstOrNull()?.keys?.toList() ?: continue
                    assertEquals("${sample.file}: $table", expected, rowsOf(sql, table, columns))
                }

                // What the backup can't know comes from the newest schema's defaults or from this phone.
                sql.query("SELECT backupTreeUri, lastBackupAt, character, todayLayout FROM settings").use { c ->
                    c.moveToFirst()
                    assertEquals("content://tree/this-phone", c.getString(0))
                    assertEquals(1795000000000, c.getLong(1))
                    val settings = backup.getValue("settings")[0].jsonObject
                    assertEquals(settings["character"]?.let { (it as JsonPrimitive).content } ?: "KNIGHT", c.getString(2))
                    assertEquals(settings["todayLayout"]?.let { (it as JsonPrimitive).content } ?: "LIST", c.getString(3))
                }

                // And the app reads it through Room, as it will on the phone.
                runBlocking {
                    val task = db.tasks().active().single()
                    assertEquals("Water", task.title)
                    assertEquals(470, task.alarmTime)
                    assertEquals(1, db.completions().count(task.id, LocalDate.of(2026, 11, 5)))
                    val settings = db.settings().get()!!
                    assertTrue(settings.onboardingDone)
                    assertEquals(8, db.slots().all().size)
                }
            } finally {
                db.close()
            }
        }
    }

    private fun openFresh(name: String): AppDatabase {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, File(dir, name).path).allowMainThreadQueries().build()
        runBlocking { Seed.ensure(db, LocalDate.of(2026, 11, 5), Instant.parse("2026-11-05T06:00:00Z")) }
        return db
    }

    private fun resource(name: String): ByteArray =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("backups/$name")) { "missing sample backups/$name" }.use { it.readBytes() }

    /** [table]'s rows, as the backup writes them: only [columns], in rowid order. */
    private fun rowsOf(sql: SupportSQLiteDatabase, table: String, columns: List<String>): List<JsonObject> =
        sql.query("SELECT ${columns.joinToString { "`$it`" }} FROM `$table` ORDER BY rowid").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(JsonObject(columns.indices.associate { i -> columns[i] to value(c, i) }))
                }
            }
        }

    private fun value(c: android.database.Cursor, i: Int): JsonElement = when (c.getType(i)) {
        android.database.Cursor.FIELD_TYPE_NULL -> JsonNull
        android.database.Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(c.getLong(i))
        android.database.Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(c.getDouble(i))
        else -> JsonPrimitive(c.getString(i))
    }
}
