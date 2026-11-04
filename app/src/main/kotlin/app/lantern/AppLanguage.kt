package app.lantern

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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

    /** A store build with one language (D207: Bazaar is Persian only); null in the personal build. */
    private val only: Choice? = BuildConfig.ONLY_LANGUAGE?.let { Choice.valueOf(it.uppercase()) }

    fun choice(context: Context): Choice =
        only ?: runCatching { Choice.valueOf(prefs(context).getString(KEY, null) ?: "") }.getOrDefault(Choice.SYSTEM)

    fun set(context: Context, choice: Choice) {
        prefs(context).edit().putString(KEY, choice.name).apply()
        syncLauncher(context)
    }

    /**
     * The launcher shows the label of whichever launcher alias is on (D194), and launchers read labels in
     * the phone's language only. So the alias follows the choice: the phone's language, or a fixed
     * «فانوس» / "Lantern". The new one is turned on before the old ones go off, so there is always an entry.
     * Some launchers move the icon once when this happens. A one-language build has only the one alias.
     */
    fun syncLauncher(context: Context) {
        if (only != null) return
        val pm = context.packageManager
        val want = when (choice(context)) {
            Choice.SYSTEM -> LAUNCHER_SYSTEM
            Choice.FA -> LAUNCHER_FA
            Choice.EN -> LAUNCHER_EN
        }
        fun set(alias: String, on: Boolean) {
            val component = ComponentName(context.packageName, alias)
            // The manifest has the system alias on and the other two off; "default" is used when it matches.
            val value = when {
                on == (alias == LAUNCHER_SYSTEM) -> PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                on -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            if (pm.getComponentEnabledSetting(component) != value) pm.setComponentEnabledSetting(component, value, PackageManager.DONT_KILL_APP)
        }
        set(want, true)
        listOf(LAUNCHER_SYSTEM, LAUNCHER_FA, LAUNCHER_EN).filter { it != want }.forEach { set(it, false) }
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

    private const val LAUNCHER_SYSTEM = "app.lantern.LauncherSystem"
    private const val LAUNCHER_FA = "app.lantern.LauncherFa"
    private const val LAUNCHER_EN = "app.lantern.LauncherEn"

    private val FA: Locale = Locale.forLanguageTag("fa-IR")
    private val EN: Locale = Locale.forLanguageTag("en")
}
