package app.lantern

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import app.lantern.domain.text.PersianDigits
import java.util.Locale

/**
 * The app's language (D174, D176): the phone's, or Persian or English picked in Settings. Persian is right
 * to left with Persian digits; English is left to right with Latin digits. Kept in plain preferences, not
 * the database, because it's needed before anything else starts (attachBaseContext).
 */
object AppLanguage {
    enum class Choice { SYSTEM, FA, EN }

    private const val PREFS = "lantern_prefs"
    private const val KEY = "language"

    fun choice(context: Context): Choice =
        runCatching { Choice.valueOf(prefs(context).getString(KEY, null) ?: "") }.getOrDefault(Choice.SYSTEM)

    fun set(context: Context, choice: Choice) {
        prefs(context).edit().putString(KEY, choice.name).apply()
    }

    /** Persian when picked, or when the phone is in Persian; English otherwise. */
    fun locale(context: Context): Locale = when (choice(context)) {
        Choice.FA -> FA
        Choice.EN -> EN
        Choice.SYSTEM -> if (Resources.getSystem().configuration.locales[0].language == "fa") FA else EN
    }

    /**
     * Only the resources' configuration changes. `Locale.setDefault` is left alone on purpose: libraries
     * that format numbers with the default locale (SQL, logs) would get Persian digits. Display digits
     * follow the language through [PersianDigits.latin].
     */
    fun wrap(base: Context): Context {
        val locale = locale(base)
        PersianDigits.latin = locale.language != "fa"
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val FA: Locale = Locale.forLanguageTag("fa-IR")
    private val EN: Locale = Locale.forLanguageTag("en")
}
