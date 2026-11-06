package app.lantern.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lantern.R
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.theme.Ba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- B5 Licenses

/** One license and the parts under it. [asset] is its full text in `src/main/assets/`. */
internal data class License(val name: String, val asset: String, val parts: List<Part>)

/** One shipped part: a readable name and where it comes from (a Maven group, or the font's project). */
internal data class Part(val name: String, val source: String)

/**
 * What the APK ships, by license (B5, D215). Hand-kept on purpose: a license plugin would add a dependency.
 * Names and license titles live here, not in string resources: they are proper names in Latin letters,
 * and the Bazaar build tests every string resource for Latin letters (D207).
 *
 * To refresh it after a dependency change, run
 *   ./gradlew :app:dependencies --configuration releaseRuntimeClasspath
 * and make sure every group in the output is listed below. Every group there today is Apache 2.0, except
 * `androidx.glance:glance-appwidget-external-protobuf` (a copy of Protocol Buffers, BSD 3-Clause).
 * A library's license is in its `.pom`, in ~/.gradle/caches/modules-2/files-2.1/<group>/<name>/.
 */
internal object Licenses {
    val font = License(
        "SIL Open Font License 1.1",
        "estedad-ofl.txt",
        listOf(Part("Estedad", "github.com/aminabedi68/Estedad")),
    )

    val apache = License(
        "Apache License 2.0",
        "apache-2.0.txt",
        listOf(
            Part("Kotlin", "org.jetbrains.kotlin"),
            Part("Kotlin Coroutines", "org.jetbrains.kotlinx"),
            Part("Kotlin Serialization", "org.jetbrains.kotlinx"),
            Part("JetBrains Annotations", "org.jetbrains"),
            Part("JSpecify", "org.jspecify"),
            Part("Guava ListenableFuture", "com.google.guava"),
            Part("Jetpack Compose (Runtime, UI, Foundation, Animation)", "androidx.compose"),
            Part("Compose Material 3", "androidx.compose.material3"),
            Part("Activity", "androidx.activity"),
            Part("Annotation", "androidx.annotation"),
            Part("Arch Core", "androidx.arch.core"),
            Part("Autofill", "androidx.autofill"),
            Part("Collection", "androidx.collection"),
            Part("Concurrent", "androidx.concurrent"),
            Part("Core", "androidx.core"),
            Part("CustomView", "androidx.customview"),
            Part("DataStore", "androidx.datastore"),
            Part("DocumentFile", "androidx.documentfile"),
            Part("DynamicAnimation", "androidx.dynamicanimation"),
            Part("Emoji2", "androidx.emoji2"),
            Part("Glance", "androidx.glance"),
            Part("Graphics", "androidx.graphics"),
            Part("Interpolator", "androidx.interpolator"),
            Part("Legacy Support", "androidx.legacy"),
            Part("Lifecycle", "androidx.lifecycle"),
            Part("Loader", "androidx.loader"),
            Part("LocalBroadcastManager", "androidx.localbroadcastmanager"),
            Part("Navigation", "androidx.navigation"),
            Part("NavigationEvent", "androidx.navigationevent"),
            Part("Print", "androidx.print"),
            Part("ProfileInstaller", "androidx.profileinstaller"),
            Part("Room", "androidx.room"),
            Part("SavedState", "androidx.savedstate"),
            Part("SQLite", "androidx.sqlite"),
            Part("Startup", "androidx.startup"),
            Part("Tracing", "androidx.tracing"),
            Part("Transition", "androidx.transition"),
            Part("VersionedParcelable", "androidx.versionedparcelable"),
            Part("Window", "androidx.window"),
            Part("WorkManager", "androidx.work"),
        ),
    )

    val protobuf = License(
        "BSD 3-Clause License",
        "protobuf-bsd-3.txt",
        listOf(Part("Protocol Buffers (inside Glance)", "androidx.glance")),
    )

    val libraries = listOf(apache, protobuf)
    val all = listOf(font) + libraries

    fun byAsset(asset: String): License? = all.firstOrNull { it.asset == asset }
}

/** «مجوزها»: the font, the libraries and the character art, each license opening its full text. */
@Composable
fun LicensesScreen(onBack: () -> Unit, onOpenLicense: (String) -> Unit) {
    val c = Ba.colors
    SettingsPage(stringResource(R.string.about_licenses), onBack) {
        Text(stringResource(R.string.licenses_help, stringResource(R.string.developer_name)), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(horizontal = 4.dp))

        SectionLabel(R.string.licenses_font)
        LicenseCard(Licenses.font, onOpenLicense)

        SectionLabel(R.string.licenses_libraries)
        Licenses.libraries.forEach { LicenseCard(it, onOpenLicense) }

        // The character pictures (res/drawable-nodpi/char_*). Not open source: shipped with the
        // artist's permission, which the owner confirms in writing before release (BAZAAR-ROADMAP B8).
        SectionLabel(R.string.licenses_art)
        BaCard {
            Text(stringResource(R.string.licenses_art_name), style = Ba.type.tile, color = c.text1)
            Text(stringResource(R.string.licenses_art_note), style = Ba.type.caption, color = c.text3)
        }
    }
}

@Composable
private fun LicenseCard(license: License, onOpen: (String) -> Unit) {
    val c = Ba.colors
    BaCard(padding = PaddingValues(0.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(license.asset) }.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(license.name, style = Ba.type.tile, color = c.text1)
                Text(stringResource(R.string.licenses_read), style = Ba.type.caption, color = c.accent)
            }
            Icon(BaIcons.ChevronForward, null, tint = c.text3)
        }
        HorizontalDivider(color = c.border)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            license.parts.forEach { part ->
                Column(Modifier.fillMaxWidth()) {
                    Text(part.name, style = Ba.type.body, color = c.text2)
                    Text(part.source, style = Ba.type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = c.text3)
                }
            }
        }
    }
}

/** A license's full text, read from assets. Always left to right: the texts are in English. */
@Composable
fun LicenseTextScreen(asset: String, onBack: () -> Unit) {
    val c = Ba.colors
    val context = LocalContext.current
    var text by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(asset) {
        text = withContext(Dispatchers.IO) { runCatching { context.assets.open(asset).bufferedReader().use { it.readText() } }.getOrNull() }
    }
    SettingsPage(Licenses.byAsset(asset)?.name ?: stringResource(R.string.about_licenses), onBack) {
        val body = text ?: return@SettingsPage
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BaCard {
                SelectionContainer {
                    Text(
                        body,
                        style = Ba.type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                        color = c.text2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

