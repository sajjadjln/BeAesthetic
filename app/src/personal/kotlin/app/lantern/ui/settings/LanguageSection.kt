package app.lantern.ui.settings

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lantern.AppContainer
import app.lantern.AppLanguage
import app.lantern.R

/** «زبان» (D176): like the phone, Persian or English. The screen is redrawn in the new language. */
@Composable
internal fun LanguageSection(container: AppContainer) {
    val context = LocalContext.current
    SectionLabel(R.string.settings_language)
    val language = remember { AppLanguage.choice(context) }
    Segmented(
        listOf(stringResource(R.string.lang_system), stringResource(R.string.lang_fa), stringResource(R.string.lang_en)),
        AppLanguage.Choice.entries.indexOf(language),
    ) { i ->
        val pick = AppLanguage.Choice.entries[i]
        if (pick != language) {
            AppLanguage.set(context, pick)
            container.notifier.createChannels() // The channel names in the new language.
            (context as? Activity)?.recreate()
        }
    }
}
