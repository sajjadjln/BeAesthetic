package app.beaesthetic.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Check-in photos (ARCHITECTURE §10): copied into `files/photos/`, turned upright, downscaled to 2560 px on
 * the long edge and re-encoded as JPEG 90. Re-encoding drops every EXIF tag, location included. Never
 * written to the shared gallery; photos leave the phone only inside backups.
 */
class PhotoStore(private val context: Context) {

    val dir: File get() = File(context.filesDir, "photos")

    fun file(name: String): File = File(dir, name)

    /** A file the system camera writes into (shared through the FileProvider), cleared on every new shot. */
    fun cameraTarget(): File = File(context.cacheDir, "camera").apply { mkdirs() }.let { File(it, "shot.jpg") }.apply { delete() }

    suspend fun import(source: Uri, name: String): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0) { "not an image" }

        // Decode at the smallest power-of-two size that still covers the target.
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(source)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: error("decode failed")
        val rotation = resolver.openInputStream(source)!!.use { orientation(ExifInterface(it)) }

        val scale = minOf(1f, MAX_EDGE.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        dir.mkdirs()
        val temp = File(dir, "$name.partial")
        temp.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        if (upright !== decoded) upright.recycle()
        decoded.recycle()
        val target = file(name)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        name
    }

    suspend fun delete(name: String) = withContext(Dispatchers.IO) { file(name).delete() }

    /** A small copy for thumbnails and the reveal, or null when the file is gone. */
    suspend fun load(name: String, maxEdge: Int): Bitmap? = withContext(Dispatchers.IO) {
        val f = file(name)
        if (!f.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun orientation(exif: ExifInterface): Int = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private companion object {
        const val MAX_EDGE = 2560
        const val QUALITY = 90
    }
}
