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
import app.lantern.domain.store.StoreRules
import app.lantern.domain.store.UpdateCall
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
    private val updateSheet = "bazaar://details/modal?id=$STORE_ID".toUri()
    private val web = "https://cafebazaar.ir/app/$STORE_ID".toUri()

    /** The app's page in Bazaar; the website if Bazaar isn't installed; a calm note if nothing opens. */
    fun openPage(context: Context) = open(context, Intent(Intent.ACTION_VIEW, details))

    /**
     * «بررسی به‌روزرسانی» and Today's update line: Bazaar's update sheet over this app when a [newer] version is
     * known and Bazaar has the sheet ([StoreRules.useUpdateSheet]); otherwise the app's page.
     */
    fun openUpdate(context: Context, newer: Long?) {
        if (StoreRules.useUpdateSheet(bazaarVersionCode(context), newer)) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, updateSheet).setPackage(BAZAAR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        openPage(context)
    }

    /** The installed Bazaar app's own version code, or null if it isn't installed. */
    private fun bazaarVersionCode(context: Context): Long? = try {
        val info = context.packageManager.getPackageInfo(BAZAAR, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    } catch (_: Exception) {
        null
    }

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

    /**
     * What Bazaar's update service answers for this app, or null (no Bazaar, no answer). Bazaar before 24.7.0
     * has only `getVersionCode` (the newer code or −1); newer ones `getRemoteVersionCode` (the store's code).
     * [StoreRules.updateCall] picks, as BazaarUpdater does.
     */
    private suspend fun askBazaar(context: Context): Long? {
        val app = context.applicationContext
        val call = StoreRules.updateCall(bazaarVersionCode(app) ?: return null)
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        // A binder call: off the main thread.
                        Thread {
                            val code = try {
                                val service = IUpdateCheckService.Stub.asInterface(binder)
                                when (call) {
                                    UpdateCall.REMOTE_VERSION_CODE -> service?.getRemoteVersionCode(app.packageName)
                                    UpdateCall.VERSION_CODE -> service?.getVersionCode(app.packageName)
                                }
                            } catch (_: Exception) { null }
                            finish(code)
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
        StoreRules.newerVersion(askBazaar(context), installed = mine)
    }
}
