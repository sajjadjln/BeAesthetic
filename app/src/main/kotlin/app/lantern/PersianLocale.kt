package app.lantern

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * For now the app runs in Persian, right to left, whatever the phone's language. The strings exist in
 * English too (D174); the «زبان» switch in Settings (D176, L7) will choose between them.
 */
object PersianLocale {
    val LOCALE: Locale = Locale.forLanguageTag("fa-IR")

    /**
     * Only the resources' configuration changes. `Locale.setDefault` is left alone on purpose: libraries
     * that format numbers with the default locale (SQL, logs) would get Persian digits.
     */
    fun wrap(base: Context): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(LOCALE)
        config.setLayoutDirection(LOCALE)
        return base.createConfigurationContext(config)
    }
}
