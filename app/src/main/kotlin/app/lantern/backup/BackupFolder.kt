package app.lantern.backup

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import app.lantern.domain.backup.BackupRules
import java.io.File

/** One file in the backup folder. */
data class FolderFile(val documentId: String, val name: String, val size: Long)

/**
 * The backup folder, chosen once with Android's folder picker (Storage Access Framework); the app keeps a
 * persistable permission. Syncthing-Fork syncs it to the PC (ARCHITECTURE §9).
 */
class BackupFolder(private val resolver: ContentResolver) {

    /** Keeps read and write access to [tree] across restarts. */
    fun takePermission(tree: Uri) {
        resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    /** Lets go of an old folder's permission when another one is chosen. */
    fun releasePermission(tree: Uri) {
        runCatching { resolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
    }

    /** Whether the app can still write to [tree] (the folder may be gone, or the permission dropped). */
    fun canWrite(tree: Uri): Boolean =
        resolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission } && runCatching { list(tree) }.isSuccess

    /** «Syncthing/Peykar»: the path after the storage name, else the folder's own name. */
    fun label(tree: Uri): String {
        val id = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return tree.toString()
        return id.substringAfter(':', id).ifBlank { id }
    }

    fun list(tree: Uri): List<FolderFile> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE)
        return resolver.query(children, columns, null, null, null)?.use { c ->
            buildList { while (c.moveToNext()) add(FolderFile(c.getString(0), c.getString(1), if (c.isNull(2)) -1 else c.getLong(2))) }
        } ?: error("folder can't be read")
    }

    /**
     * Copies [file] into the folder as `[name].partial`, checks its size, then renames it to [name]: a
     * half-written file never looks like a backup (D39).
     */
    fun write(tree: Uri, file: File, name: String) {
        val dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val partial = DocumentsContract.createDocument(resolver, dir, "application/octet-stream", name + BackupRules.PARTIAL)
            ?: error("can't create a file in the folder")
        try {
            resolver.openOutputStream(partial, "w")?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: error("can't write")
            val written = list(tree).firstOrNull { it.name == name + BackupRules.PARTIAL }?.size
            if (written != null && written >= 0 && written != file.length()) error("size $written ≠ ${file.length()}")
            DocumentsContract.renameDocument(resolver, partial, name) ?: error("can't rename")
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, partial) }
            throw e
        }
    }

    /** Deletes backups beyond the newest [BackupRules.KEEP] and any leftover `.partial` files. */
    fun prune(tree: Uri) {
        val files = list(tree)
        val names = BackupRules.toDelete(files.map { it.name }).toSet()
        files.filter { it.name in names || BackupRules.isLeftover(it.name) }.forEach { f ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, f.documentId)) }
        }
    }
}
