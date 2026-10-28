package app.lantern.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** What a backup holds, read from `manifest.json` before anything changes (flows B2). */
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val dbSchemaVersion: Int,
    val createdAt: Instant,
    val reason: String,
    /** Rows per table. */
    val counts: Map<String, Int>,
    /** Photo paths under `photos/` and their sizes in bytes. */
    val photos: Map<String, Long>,
)

class BackupException(val kind: Kind, message: String, cause: Throwable? = null) : Exception(message, cause) {
    enum class Kind { NOT_A_BACKUP, NEWER, DAMAGED }
}

/**
 * The backup zip (ARCHITECTURE §9, docs/BACKUP_FORMAT.md): `manifest.json`, `data.json` (every table, row
 * by row: the source of truth for restore), `photos/` and `db/lantern.db` (a raw copy, a fallback only).
 *
 * It only needs the open database and the photos folder, so the round trip runs in a unit test. Schema
 * changes stay additive (new tables, new columns with defaults), so an older `data.json` inserts into the
 * current tables as it is (D132).
 */
object BackupArchive {

    const val FORMAT_VERSION = 1
    const val MANIFEST = "manifest.json"
    const val DATA = "data.json"
    const val PHOTOS = "photos/"
    const val RAW_DB = "db/lantern.db"

    /** Kept on this phone only: a restore never replaces them (D132). */
    val LOCAL_TABLES = setOf("backup_record")

    /** Settings that belong to this phone (the folder and its permission, the backup state): kept through a restore. */
    val LOCAL_SETTINGS = listOf("backupTreeUri", "lastBackupAt", "autoBackup")

    private val json = Json { prettyPrint = false }

    /** Every table a backup carries, in name order. */
    fun tables(db: SupportSQLiteDatabase): List<String> =
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }.filterNot { it.startsWith("sqlite_") || it == "android_metadata" || it == "room_master_table" || it in LOCAL_TABLES }

    private fun columns(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val name = c.getColumnIndexOrThrow("name")
            buildList { while (c.moveToNext()) add(c.getString(name)) }
        }

    /**
     * Writes the whole backup to [out]. The tables are read in one transaction, so the copy is consistent.
     * [dbFile] is the raw database for the fallback copy (checkpointed first); null leaves it out.
     */
    fun write(
        db: SupportSQLiteDatabase,
        dbFile: File?,
        photosDir: File,
        appVersion: String,
        schemaVersion: Int,
        reason: String,
        now: Instant,
        out: OutputStream,
    ): BackupManifest {
        val counts = linkedMapOf<String, Int>()
        val data: JsonObject
        db.beginTransaction()
        try {
            data = buildJsonObject {
                put("formatVersion", FORMAT_VERSION)
                put("dbSchemaVersion", schemaVersion)
                put("tables", buildJsonObject {
                    for (table in tables(db)) {
                        val rows = db.query("SELECT * FROM `$table` ORDER BY rowid").use(::rowsOf)
                        counts[table] = rows.size
                        put(table, rows)
                    }
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        val photos = photoFiles(photosDir).associate { (path, file) -> path to file.length() }
        val manifest = BackupManifest(FORMAT_VERSION, appVersion, schemaVersion, now, reason, counts, photos)

        if (dbFile != null) db.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(json.encodeToString(JsonObject.serializer(), manifestJson(manifest)).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATA))
            zip.write(json.encodeToString(JsonObject.serializer(), data).toByteArray())
            zip.closeEntry()
            for ((path, file) in photoFiles(photosDir)) {
                zip.putNextEntry(ZipEntry(PHOTOS + path))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            if (dbFile != null && dbFile.exists()) {
                zip.putNextEntry(ZipEntry(RAW_DB))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return manifest
    }

    /** Reads and checks the manifest of [zip]. Throws [BackupException] for a file that isn't ours or is newer. */
    fun readManifest(zip: File, currentSchema: Int): BackupManifest {
        val manifest = try {
            ZipFile(zip).use { z ->
                val entry = z.getEntry(MANIFEST) ?: throw BackupException(BackupException.Kind.NOT_A_BACKUP, "no manifest")
                parseManifest(json.parseToJsonElement(z.getInputStream(entry).bufferedReader().readText()).jsonObject)
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupException.Kind.NOT_A_BACKUP, "unreadable: ${e.message}", e)
        }
        if (manifest.formatVersion > FORMAT_VERSION || manifest.dbSchemaVersion > currentSchema) {
            throw BackupException(BackupException.Kind.NEWER, "format ${manifest.formatVersion}, schema ${manifest.dbSchemaVersion}")
        }
        return manifest
    }

    /** Re-opens a written zip and checks it against [expected]: every table's rows and every photo's size. */
    fun verify(zip: File, expected: BackupManifest, currentSchema: Int) {
        val manifest = readManifest(zip, currentSchema)
        val tables = readTables(zip)
        val counts = tables.mapValues { it.value.size }
        if (manifest.counts != expected.counts || counts != expected.counts) {
            throw BackupException(BackupException.Kind.DAMAGED, "counts differ")
        }
        ZipFile(zip).use { z ->
            for ((path, size) in expected.photos) {
                val entry = z.getEntry(PHOTOS + path) ?: throw BackupException(BackupException.Kind.DAMAGED, "missing photo $path")
                if (entry.size != size) throw BackupException(BackupException.Kind.DAMAGED, "photo size $path")
            }
        }
    }

    /**
     * Replaces every table with the backup's rows in one transaction, keeping [LOCAL_TABLES] and
     * [LOCAL_SETTINGS], then swaps in the photos. A table the backup doesn't have (an older app) ends up
     * empty; a column it doesn't have takes its default. If anything fails, nothing has changed.
     */
    fun restore(db: SupportSQLiteDatabase, zip: File, photosDir: File, currentSchema: Int, inTransaction: (() -> Unit) -> Unit) {
        readManifest(zip, currentSchema)
        val backup = readTables(zip)
        if ("settings" !in backup) throw BackupException(BackupException.Kind.NOT_A_BACKUP, "no settings")

        // Photos go to a side folder first; they replace the current ones only after the data is in.
        val staged = File(photosDir.parentFile, photosDir.name + ".restore")
        staged.deleteRecursively()
        staged.mkdirs()
        try {
            ZipFile(zip).use { z ->
                for (entry in z.entries()) {
                    if (!entry.name.startsWith(PHOTOS) || entry.isDirectory) continue
                    val target = File(staged, entry.name.removePrefix(PHOTOS))
                    // Never write outside the folder ("zip slip").
                    if (!target.canonicalPath.startsWith(staged.canonicalPath + File.separator)) throw BackupException(BackupException.Kind.DAMAGED, "bad path ${entry.name}")
                    target.parentFile?.mkdirs()
                    z.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                }
            }

            val local = db.query("SELECT ${LOCAL_SETTINGS.joinToString { "`$it`" }} FROM settings WHERE id = 1").use { c ->
                if (c.moveToFirst()) LOCAL_SETTINGS.indices.map { valueOf(c, it) } else null
            }
            inTransaction {
                for (table in tables(db)) {
                    db.execSQL("DELETE FROM `$table`")
                    val known = columns(db, table).toSet()
                    for (row in backup[table].orEmpty()) {
                        val values = ContentValues()
                        for ((column, value) in row.jsonObject) if (column in known) values.putJson(column, value)
                        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
                    }
                }
                if (local != null) {
                    val values = ContentValues()
                    LOCAL_SETTINGS.forEachIndexed { i, column -> values.putJson(column, local[i]) }
                    db.update("settings", SQLiteDatabase.CONFLICT_ABORT, values, "id = 1", null)
                }
            }
        } catch (e: Exception) {
            staged.deleteRecursively()
            throw e
        }
        // The data is in: swap the photo folders. The old one goes only after the new one is in place.
        val old = File(photosDir.parentFile, photosDir.name + ".old")
        old.deleteRecursively()
        if (photosDir.exists() && !photosDir.renameTo(old)) photosDir.deleteRecursively()
        if (!staged.renameTo(photosDir)) staged.copyRecursively(photosDir, overwrite = true)
        old.deleteRecursively()
        staged.deleteRecursively()
    }

    /** `data.json`'s tables as row arrays. */
    fun readTables(zip: File): Map<String, JsonArray> = try {
        ZipFile(zip).use { z ->
            val entry = z.getEntry(DATA) ?: throw BackupException(BackupException.Kind.NOT_A_BACKUP, "no data")
            val root = json.parseToJsonElement(z.getInputStream(entry).bufferedReader().readText()).jsonObject
            root.getValue("tables").jsonObject.mapValues { it.value.jsonArray }
        }
    } catch (e: BackupException) {
        throw e
    } catch (e: Exception) {
        throw BackupException(BackupException.Kind.DAMAGED, "data: ${e.message}", e)
    }

    /** Every file under [dir], as (path inside `photos/`, file), in name order. */
    private fun photoFiles(dir: File): List<Pair<String, File>> =
        if (!dir.isDirectory) emptyList() else dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath to it }.sortedBy { it.first }.toList()

    private fun rowsOf(c: Cursor): JsonArray = buildJsonArray {
        while (c.moveToNext()) {
            add(buildJsonObject { for (i in 0 until c.columnCount) put(c.getColumnName(i), valueOf(c, i)) })
        }
    }

    private fun valueOf(c: Cursor, i: Int): JsonElement = when (c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> JsonNull
        Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(c.getLong(i))
        Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(c.getDouble(i))
        Cursor.FIELD_TYPE_STRING -> JsonPrimitive(c.getString(i))
        else -> throw IllegalStateException("BLOB in column ${c.getColumnName(i)}: not part of the format")
    }

    private fun ContentValues.putJson(column: String, value: JsonElement) {
        when {
            value is JsonNull -> putNull(column)
            value !is JsonPrimitive -> throw BackupException(BackupException.Kind.DAMAGED, "bad value in $column")
            value.isString -> put(column, value.content)
            else -> {
                val long = value.longOrNull
                val double = value.doubleOrNull
                when {
                    long != null -> put(column, long)
                    double != null -> put(column, double)
                    else -> throw BackupException(BackupException.Kind.DAMAGED, "bad value in $column")
                }
            }
        }
    }

    private fun manifestJson(m: BackupManifest) = buildJsonObject {
        put("formatVersion", m.formatVersion)
        put("appVersion", m.appVersion)
        put("dbSchemaVersion", m.dbSchemaVersion)
        put("createdAt", m.createdAt.toString())
        put("reason", m.reason)
        put("counts", buildJsonObject { m.counts.forEach { (k, v) -> put(k, v) } })
        put("photos", buildJsonObject { m.photos.forEach { (k, v) -> put(k, v) } })
    }

    private fun parseManifest(o: JsonObject) = BackupManifest(
        formatVersion = o.getValue("formatVersion").jsonPrimitive.int,
        appVersion = o.getValue("appVersion").jsonPrimitive.content,
        dbSchemaVersion = o.getValue("dbSchemaVersion").jsonPrimitive.int,
        createdAt = Instant.parse(o.getValue("createdAt").jsonPrimitive.content),
        reason = o["reason"]?.jsonPrimitive?.content ?: "",
        counts = o.getValue("counts").jsonObject.mapValues { it.value.jsonPrimitive.int },
        photos = o["photos"]?.jsonObject?.mapValues { it.value.jsonPrimitive.long }.orEmpty(),
    )
}
