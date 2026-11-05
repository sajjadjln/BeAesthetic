package app.lantern.store

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.core.net.toUri
import app.lantern.R
import com.farsitel.bazaar.IUpdateCheckService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Cafe Bazaar (B4, D214). The update check binds to the Bazaar app's own service, the same one the
 * BazaarUpdater library uses, so this app needs no INTERNET. Store id is fixed: the listing is `app.lantern`.
 */
object BazaarStore {
    private const val STORE_ID = "app.lantern"
    private const val BAZAAR = "com.farsitel.bazaar"
    private const val BIND = "com.farsitel.bazaar.service.UpdateCheckService.BIND"
    private val details = "bazaar://details?id=$STORE_ID".toUri()
    private val web = "https://cafebazaar.ir/app/$STORE_ID".toUri()

    /** The app's page in Bazaar; the website if Bazaar isn't installed; a calm note if nothing opens. */
    fun openPage(context: Context) = open(context, Intent(Intent.ACTION_VIEW, details))

    /** Bazaar's rating screen for the app. */
    fun openRating(context: Context) = open(context, Intent(Intent.ACTION_EDIT, details))

    private fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.setPackage(BAZAAR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, web).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.store_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    /** The newest version code Bazaar knows for this app, or null (no Bazaar, an old Bazaar, no answer). */
    suspend fun remoteVersionCode(context: Context): Long? {
        val app = context.applicationContext
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        // A binder call: off the main thread.
                        Thread {
                            val code = try {
                                IUpdateCheckService.Stub.asInterface(binder)?.getRemoteVersionCode(app.packageName)
                            } catch (_: Exception) { null }
                            finish(code?.takeIf { it > 0 })
                        }.start()
                    }
                    override fun onServiceDisconnected(name: ComponentName?) = finish(null)
                    override fun onBindingDied(name: ComponentName?) = finish(null)
                    override fun onNullBinding(name: ComponentName?) = finish(null)
                    @Synchronized fun finish(value: Long?) {
                        try { app.unbindService(this) } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(value)
                    }
                }
                val bound = try {
                    app.bindService(Intent(BIND).setPackage(BAZAAR), connection, Context.BIND_AUTO_CREATE)
                } catch (_: SecurityException) { false }
                if (!bound) {
                    try { app.unbindService(connection) } catch (_: Exception) {}
                    cont.resume(null)
                }
                cont.invokeOnCancellation { try { app.unbindService(connection) } catch (_: Exception) {} }
            }
        }
    }

    /** A newer version code than this build, or null. */
    suspend fun newerVersion(context: Context): Long? = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        val mine = context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        remoteVersionCode(context)?.takeIf { it > mine }
    }
}
