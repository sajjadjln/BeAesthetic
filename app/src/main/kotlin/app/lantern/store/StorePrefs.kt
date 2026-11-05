package app.lantern.store

import android.content.Context
import android.content.pm.PackageManager
import app.lantern.domain.store.StoreRules

/**
 * Small app-level state for B4: the version «تازه‌ها» was last shown for, the rating answer and the update
 * line on Today. Preferences, not Room: none of it is the user's data, so it stays out of the backup.
 */
class StorePrefs(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("store", Context.MODE_PRIVATE)

    /** The «تازه‌ها» text to show now, or 0. Stores the current version, so it is shown once. */
    fun takeWhatsNew(): Int {
        val current = versionCode()
        val lastSeen = if (prefs.contains(LAST_SEEN)) prefs.getInt(LAST_SEEN, 0) else null
        val info = try { app.packageManager.getPackageInfo(app.packageName, 0) } catch (_: PackageManager.NameNotFoundException) { null }
        val fresh = info == null || info.firstInstallTime == info.lastUpdateTime
        val show = StoreRules.whatsNewFor(current, lastSeen, fresh)
        if (lastSeen != current) prefs.edit().putInt(LAST_SEEN, current).apply()
        if (show == null) return 0
        return app.resources.getIdentifier("whats_new_$show", "string", app.packageName)
    }

    var rateAnswered: Boolean
        get() = prefs.getBoolean(RATE_ANSWERED, false)
        set(value) = prefs.edit().putBoolean(RATE_ANSWERED, value).apply()

    /** The newest store version whose line on Today was already shown and closed. */
    var updateLineClosedFor: Long
        get() = prefs.getLong(UPDATE_CLOSED, 0)
        set(value) = prefs.edit().putLong(UPDATE_CLOSED, value).apply()

    /** After «پاک کردن همه‌چیز»: like a fresh install of this version. */
    fun reset() {
        prefs.edit().clear().putInt(LAST_SEEN, versionCode()).commit()
    }

    @Suppress("DEPRECATION")
    private fun versionCode(): Int = app.packageManager.getPackageInfo(app.packageName, 0).versionCode

    private companion object {
        const val LAST_SEEN = "lastSeenVersion"
        const val RATE_ANSWERED = "rateAnswered"
        const val UPDATE_CLOSED = "updateLineClosedFor"
    }
}
