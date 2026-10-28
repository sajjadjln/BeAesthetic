package app.lantern.ui.backup

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lantern.AppContainer
import app.lantern.backup.BackupException
import app.lantern.backup.BackupOutcome
import app.lantern.backup.BackupStatus
import app.lantern.backup.RestorePreview
import app.lantern.data.db.BackupRecordEntity
import app.lantern.domain.backup.BackupReason
import app.lantern.domain.backup.BackupRules
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A one-line result under the status card; replaced by the next action. */
enum class BackupMessage { SAVED, FAILED, RESTORED, NOT_OURS, NEWER, DAMAGED, RESTORE_FAILED }

data class BackupUiState(
    val status: BackupStatus,
    val folderLabel: String?,
    val autoBackup: Boolean,
    val running: Boolean,
    val recent: List<BackupRecordEntity>,
    val preview: RestorePreview?,
    val message: BackupMessage?,
)

class BackupViewModel(private val container: AppContainer) : ViewModel() {

    private val backup = container.backup
    private val preview = MutableStateFlow<RestorePreview?>(null)
    private val message = MutableStateFlow<BackupMessage?>(null)

    val state: StateFlow<BackupUiState?> = combine(
        backup.observeStatus(),
        container.db.settings().observe(),
        container.db.backups().observeRecentOk(BackupRules.KEEP),
        backup.running,
        combine(preview, message) { p, m -> p to m },
    ) { status, settings, recent, running, (p, m) ->
        BackupUiState(
            status = status,
            folderLabel = settings?.backupTreeUri?.let { backup.folder.label(it.toUri()) },
            autoBackup = settings?.autoBackup ?: true,
            running = running,
            recent = recent,
            preview = p,
            message = m,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { container.events.screen("backup") }
    }

    /** A folder from the picker: keep its permission, drop the old one, and make the first backup there. */
    fun folderPicked(tree: Uri) {
        viewModelScope.launch {
            val old = container.db.settings().get()?.backupTreeUri
            backup.folder.takePermission(tree)
            if (old != null && old != tree.toString()) backup.folder.releasePermission(old.toUri())
            container.db.settings().setBackupTreeUri(tree.toString())
            backUpNow()
        }
    }

    fun backUpNow() {
        message.value = null
        // The work runs in the app's scope, so leaving the screen never cuts a backup off; the screen only waits.
        val job = container.scope.async { backup.backUp(BackupReason.MANUAL) }
        viewModelScope.launch {
            message.value = when (job.await()) {
                is BackupOutcome.Saved -> BackupMessage.SAVED
                else -> BackupMessage.FAILED
            }
        }
    }

    fun setAuto(on: Boolean) {
        viewModelScope.launch { container.db.settings().setAutoBackup(on) }
    }

    /** A file from the picker: read it and show what's inside (B2). Nothing changes yet. */
    fun filePicked(uri: Uri) {
        message.value = null
        viewModelScope.launch {
            try {
                preview.value = backup.open(uri)
            } catch (e: BackupException) {
                message.value = e.toMessage()
            } catch (e: Exception) {
                message.value = BackupMessage.NOT_OURS
            }
        }
    }

    fun dismissPreview() {
        preview.value = null
    }

    fun confirmRestore() {
        val p = preview.value ?: return
        val job = container.scope.async {
            backup.restore(p)
            container.scheduler.reconcile("restore")
        }
        viewModelScope.launch {
            message.value = try {
                job.await()
                BackupMessage.RESTORED
            } catch (e: BackupException) {
                e.toMessage()
            } catch (e: Exception) {
                BackupMessage.RESTORE_FAILED
            }
            preview.value = null
        }
    }

    private fun BackupException.toMessage() = when (kind) {
        BackupException.Kind.NOT_A_BACKUP -> BackupMessage.NOT_OURS
        BackupException.Kind.NEWER -> BackupMessage.NEWER
        BackupException.Kind.DAMAGED -> BackupMessage.DAMAGED
    }
}
