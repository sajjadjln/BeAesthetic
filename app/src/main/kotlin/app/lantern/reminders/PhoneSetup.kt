package app.lantern.reminders

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Opens the system screens that keep reminders alive (ARCHITECTURE §7.7). MIUI and HyperOS component names
 * differ between versions, so each action tries a list of known intents and ends with the app's own
 * system details page. HyperOS (the Poco X4 5G) is tried first.
 */
object PhoneSetup {

    fun isXiaomi(): Boolean = listOf(Build.MANUFACTURER, Build.BRAND).any {
        it.equals("xiaomi", ignoreCase = true) || it.equals("redmi", ignoreCase = true) || it.equals("poco", ignoreCase = true)
    }

    /**
     * Xiaomi's autostart switch, read through its app-op (10008). Not public API, so it may not work on every
     * MIUI/HyperOS version: null means "can't tell", and the screen then asks the user instead.
     */
    fun autostartState(context: Context): Boolean? {
        if (!isXiaomi()) return null
        return try {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val check = AppOpsManager::class.java.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            when (check.invoke(ops, OP_AUTO_START, Process.myUid(), context.packageName) as Int) {
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> false
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private const val OP_AUTO_START = 10008

    fun isIgnoringBattery(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun openNotifications(activity: Activity) = tryAll(
        activity,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
        details(activity),
    )

    /** The system "Alarms and reminders" switch (Android 12+), then the app's details page. */
    fun openExactAlarms(activity: Activity) = tryAll(
        activity,
        *listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${activity.packageName}".toUri()) else null,
            details(activity),
        ).toTypedArray(),
    )

    fun openAutostart(activity: Activity) = tryAll(
        activity,
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"))
            .putExtra("extra_pkgname", activity.packageName),
        details(activity),
    )

    /**
     * Android's own "let it run in the background" prompt first: it really changes the state, and the screen
     * can read it back. Once that's done, Xiaomi's per-app battery saver ("No restrictions") is next.
     */
    @SuppressLint("BatteryLife") // Sideloaded APK; the reminders are the product (ARCHITECTURE §7.3).
    fun openBattery(activity: Activity) = tryAll(
        activity,
        *listOfNotNull(
            if (!isIgnoringBattery(activity)) Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${activity.packageName}".toUri()) else null,
            if (isXiaomi()) {
                Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                    .putExtra("package_name", activity.packageName)
                    .putExtra("package_label", activity.applicationInfo.loadLabel(activity.packageManager))
            } else {
                null
            },
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            details(activity),
        ).toTypedArray(),
    )

    private fun details(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())

    private fun tryAll(activity: Activity, vararg intents: Intent): Boolean {
        for (intent in intents) {
            try {
                activity.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
