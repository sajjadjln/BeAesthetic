package app.lantern

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * The Bazaar build is Persian only (D207). Read every string as an English phone would see it: if any
 * still has Latin letters, an English copy shipped (a name missing from values-fa/, or text not translated).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BazaarStringsTest {

    /** Names that stay as they are in Persian text. */
    private val allowed = listOf("Syncthing")
    private val placeholder = Regex("%(\\d+\\$)?[sdf]")
    private val latin = Regex("[A-Za-z]")

    private val english: Context = ApplicationProvider.getApplicationContext<Context>().let { app ->
        app.createConfigurationContext(Configuration(app.resources.configuration).apply { setLocale(Locale.ENGLISH) })
    }

    private fun hasEnglish(text: String): Boolean =
        latin.containsMatchIn(allowed.fold(text.replace(placeholder, "")) { t, word -> t.replace(word, "") })

    @Test
    fun theLanguageIsFixedToPersian() {
        assertEquals("fa", BuildConfig.ONLY_LANGUAGE)
    }

    @Test
    fun noStringIsInEnglish() {
        val res = english.resources
        val strings = R.string::class.java.fields.map { it.name to res.getString(it.getInt(null)) }
        val arrays = R.array::class.java.fields.flatMap { f -> res.getStringArray(f.getInt(null)).map { f.name to it } }
        val found = (strings + arrays).filter { (_, text) -> hasEnglish(text) }
        assertTrue(found.joinToString("\n") { (name, text) -> "$name: $text" }, found.isEmpty())
    }

    @Test
    fun theLauncherSaysFanus() {
        assertEquals("فانوس", english.getString(R.string.app_name))
    }
}
