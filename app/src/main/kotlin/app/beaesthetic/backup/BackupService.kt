package app.beaesthetic.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.beaesthetic.App
import app.beaesthetic.BuildConfig
import app.beaesthetic.ProgramClock
import app.beaesthetic.data.db.AppDatabase
import app.beaesthetic.data.db.BackupRecordEntity
import app.beaesthetic.domain.backup.BackupReason
import app.beaesthetic.domain.backup.BackupRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** A backup that was written, or why not. */
sealed interface BackupOutcome {
    data class Saved(val fileName: String, val sizeBytes: Long, val photoCount: Int) : BackupOutcome
    data object NoFolder : BackupOutcome
    data class Failed(val error: String) : BackupOutcome
}

/** A picked file, read and checked, shown before anything changes (flows B2). */
data class RestorePreview(val file: File, val displayName: String, val manifest: BackupManifest)

/**
 * Backups and restore (ARCHITECTURE §9, slice 6). One at a time: a backup and a restore never overlap.
 * Every try is written to `backup_record`, so Settings and Today can tell when the folder needs a look.
 */
class BackupService(private val context: Context, private val db: AppDatabase, private val clock: ProgramClock) {

    val folder = BackupFolder(context.contentResolver)
    private val mutex = Mutex()
    private val _running = MutableStateFlow(false)

    /** True while a backup or a restore runs. */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    val photosDir: File get() = File(context.filesDir, "photos")
    private val dbFile: File get() = context.getDatabasePath(AppDatabase.NAME)
    private val schema: Int get() = db.openHelper.readableDatabase.version

    suspend fun backUp(reason: BackupReason): BackupOutcome = withContext(Dispatchers.IO) {
        mutex.withLock { busy { backUpLocked(reason) } }
    }

    private suspend fun backUpLocked(reason: BackupReason): BackupOutcome {
        val tree = db.settings().get()?.backupTreeUri?.let(Uri::parse) ?: return BackupOutcome.NoFolder
        val name = BackupRules.fileName(clock.localNow())
        val temp = File(context.cacheDir, "backup/$name")
        return try {
            temp.parentFile?.mkdirs()
            val manifest = writeTo(temp, reason)
            BackupArchive.verify(temp, manifest, schema)
            folder.write(tree, temp, name)
            folder.prune(tree)
            val now = clock.now()
            db.backups().insert(BackupRecordEntity(at = now, fileName = name, sizeBytes = temp.length(), photoCount = manifest.photos.size, reason = reason.name, ok = true))
            db.settings().setLastBackupAt(now)
            BackupOutcome.Saved(name, temp.length(), manifest.photos.size)
        } catch (e: Exception) {
            val error = (e.message ?: e.javaClass.simpleName).take(200)
            db.backups().insert(BackupRecordEntity(at = clock.now(), fileName = name, sizeBytes = 0, photoCount = 0, reason = reason.name, ok = false, error = error))
            BackupOutcome.Failed(error)
        } finally {
            temp.delete()
        }
    }

    private fun writeTo(file: File, reason: BackupReason): BackupManifest =
        file.outputStream().use { out ->
            BackupArchive.write(db.openHelper.writableDatabase, dbFile, photosDir, BuildConfig.VERSION_NAME, schema, reason.name, clock.now(), out)
        }

    /** Copies the picked file into the cache and reads its manifest. Throws [BackupException] if it isn't usable. */
    suspend fun open(uri: Uri): RestorePreview = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        val file = File(dir, "incoming.zip")
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            ?: throw BackupException(BackupException.Kind.NOT_A_BACKUP, "can't open")
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: file.name
        RestorePreview(file, name, BackupArchive.readManifest(file, schema))
    }

    /**
     * Restores [preview] (D39): first a backup of the current data (into the folder, or, with no usable
     * folder, into private storage), then everything replaced in one transaction. If the safety backup
     * fails, nothing is replaced. The caller re-plans reminders.
     */
    suspend fun restore(preview: RestorePreview) = withContext(Dispatchers.IO) {
        mutex.withLock {
            busy {
                val tree = db.settings().get()?.backupTreeUri?.let(Uri::parse)
                if (tree != null && folder.canWrite(tree)) {
                    val safety = backUpLocked(BackupReason.BEFORE_RESTORE)
                    if (safety !is BackupOutcome.Saved) error("safety backup: $safety")
                } else {
                    privateSafetyCopy()
                }
                BackupArchive.restore(db.openHelper.writableDatabase, preview.file, photosDir, schema) { block -> db.runInTransaction(block) }
                preview.file.delete()
            }
        }
    }

    /** With no folder, the safety copy stays in the app's own storage; the newest two are kept. */
    private fun privateSafetyCopy() {
        val dir = File(context.filesDir, "safety").apply { mkdirs() }
        val file = File(dir, BackupRules.fileName(clock.localNow()))
        val manifest = writeTo(file, BackupReason.BEFORE_RESTORE)
        BackupArchive.verify(file, manifest, schema)
        dir.listFiles().orEmpty().sortedByDescending { it.name }.drop(2).forEach { it.delete() }
    }

    /** The backup status for Today, Settings and the backup screen. Days are counted in calendar days. */
    fun observeStatus(): Flow<BackupStatus> =
        combine(db.settings().observe().filterNotNull(), db.backups().observeLast(), db.backups().observeLastOk()) { s, latest, ok ->
            val today = clock.localNow().toLocalDate()
            val okDate = ok?.at?.atZone(clock.zone())?.toLocalDate()
            BackupStatus(
                notice = BackupRules.notice(s.backupTreeUri != null, okDate, latest?.ok == false, today),
                hasFolder = s.backupTreeUri != null,
                lastOk = ok,
                daysSince = okDate?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) },
            )
        }.distinctUntilChanged()

    /** On app open (D133): queues the automatic backup when it's due. */
    suspend fun autoIfDue() {
        if (autoDue()) BackupWorker.enqueue(context, BackupReason.AUTO)
    }

    /**
     * The automatic backup itself. It checks again once it has the lock: a manual backup that was running
     * when it was queued (choosing the folder makes one) already counts.
     */
    suspend fun autoBackUp(): BackupOutcome? = withContext(Dispatchers.IO) {
        mutex.withLock { if (autoDue()) busy { backUpLocked(BackupReason.AUTO) } else null }
    }

    private suspend fun autoDue(): Boolean {
        val s = db.settings().get() ?: return false
        val last = db.backups().lastOk()?.at?.let { LocalDateTime.ofInstant(it, clock.zone()) }
        return BackupRules.autoDue(s.autoBackup, s.backupTreeUri != null, last, clock.localNow())
    }

    private suspend fun <T> busy(block: suspend () -> T): T {
        _running.value = true
        try {
            return block()
        } finally {
            _running.value = false
        }
    }
}

/** Runs a backup as a WorkManager job, so leaving the screen or the app doesn't cut it off (ARCHITECTURE §9). */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as App).container
        container.ensureReady()
        val reason = runCatching { BackupReason.valueOf(inputData.getString(KEY_REASON).orEmpty()) }.getOrDefault(BackupReason.AUTO)
        // A failure is recorded and shown in Settings and on Today; retrying blindly wouldn't fix a lost folder.
        if (reason == BackupReason.AUTO) container.backup.autoBackUp() else container.backup.backUp(reason)
        return Result.success()
    }

    companion object {
        private const val NAME = "backup"
        private const val KEY_REASON = "reason"

        fun enqueue(context: Context, reason: BackupReason) {
            val request = OneTimeWorkRequestBuilder<BackupWorker>().setInputData(workDataOf(KEY_REASON to reason.name)).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
