package app.lantern.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lantern.AppContainer
import app.lantern.BuildConfig
import app.lantern.R
import app.lantern.crash.CrashReport
import app.lantern.domain.text.PersianDigits
import app.lantern.ui.clockText
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.jalaliDayMonth
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.Px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

// ---------------------------------------------------------------- B5 About

/**
 * «درباره» (B5): the version, the privacy note, a contact (when set), «مجوزها» and the local crash log
 * (D211, D215). A crash opens its full text with «کپی». Nothing here touches the network.
 */
@Composable
fun AboutScreen(container: AppContainer, onBack: () -> Unit, onOpenCrash: (String) -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    var crashes by remember { mutableStateOf<List<CrashReport>?>(null) }
    var askClear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { crashes = withContext(Dispatchers.IO) { container.crashes.list() } }

    SettingsPage(stringResource(R.string.settings_about), onBack) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).padding(16.dp),
        ) {
            Text(
                stringResource(R.string.settings_version, "\u2066" + PersianDigits.format(BuildConfig.VERSION_NAME) + "\u2069"),
                style = Ba.type.subtitle,
                color = c.text1,
                textAlign = TextAlign.Center,
            )
            // The developer's name, as on the Bazaar listing («عنوان توسعه‌دهنده»).
            Text(stringResource(R.string.about_maker, stringResource(R.string.developer_name)), style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center)
        }

        // The privacy note (D215). Keep it true to the code: no INTERNET permission (the build checks it),
        // no accounts, no SDKs, allowBackup off, backups only to the folder the user picks (SAF). The Bazaar
        // build's update check talks to the Bazaar app on the phone, so "this app sends nothing" stays true.
        SectionLabel(R.string.about_privacy)
        BaCard { Text(stringResource(R.string.about_privacy_text, stringResource(R.string.developer_name)), style = Ba.type.body, color = c.text2) }

        val context = LocalContext.current
        // The contact row shows only once res/values/strings.xml `about_contact` has an address or a link.
        val contact = stringResource(R.string.about_contact).trim()
        if (contact.isNotEmpty()) {
            BaCard(padding = PaddingValues(0.dp)) {
                AboutRow(BaIcons.Mail, stringResource(R.string.about_contact_row), contact) { openContact(context, contact) }
                // The licenses page is hidden for now (D218). To bring it back, add `onOpenLicenses: () -> Unit`
                // to AboutScreen, uncomment these two lines and the routes in MainActivity.kt:
                // HorizontalDivider(color = c.border)
                // AboutRow(BaIcons.Info, stringResource(R.string.about_licenses), null, onOpenLicenses)
            }
        }

        // B4: «امتیاز بده تو بازار», in the Bazaar build only.
        app.lantern.ui.store.AboutStoreRows()

        SectionLabel(R.string.about_crashes)
        Text(stringResource(R.string.about_crashes_help), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(horizontal = 4.dp))
        val list = crashes
        when {
            list == null -> Unit
            list.isEmpty() -> Text(
                stringResource(R.string.about_no_crashes),
                style = Ba.type.body,
                color = c.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).padding(vertical = 20.dp, horizontal = 16.dp),
            )
            else -> {
                BaCard(padding = PaddingValues(0.dp)) {
                    list.forEachIndexed { i, crash ->
                        if (i > 0) HorizontalDivider(color = c.border)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenCrash(crash.name) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(crashTime(crash), style = Ba.type.tile, color = c.text1)
                                // The exception line is code: always left to right.
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        crash.firstLine,
                                        style = Ba.type.caption.copy(fontFamily = FontFamily.Monospace),
                                        color = c.text3,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Icon(BaIcons.ChevronForward, null, tint = c.text3, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                SecondaryButton(stringResource(R.string.about_clear), onClick = { askClear = true }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            containerColor = c.surface,
            title = { Text(stringResource(R.string.about_clear_title), style = Ba.type.title, color = c.text1) },
            text = { Text(stringResource(R.string.about_clear_text), style = Ba.type.body, color = c.text2) },
            confirmButton = {
                TextButton(onClick = {
                    askClear = false
                    scope.launch { crashes = withContext(Dispatchers.IO) { container.crashes.clear(); container.crashes.list() } }
                }) { Text(stringResource(R.string.about_clear_yes), color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { askClear = false }) { Text(stringResource(R.string.cancel), color = c.text2) } },
        )
    }
}

/** One crash in full, with «کپی» (the clipboard; nothing is sent). */
@Composable
fun CrashScreen(container: AppContainer, name: String, onBack: () -> Unit) {
    val c = Ba.colors
    val context = LocalContext.current
    var crash by remember { mutableStateOf<CrashReport?>(null) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(name) { crash = withContext(Dispatchers.IO) { container.crashes.get(name) } }

    SettingsPage(crash?.let { crashTime(it) } ?: stringResource(R.string.crash_title), onBack) {
        val report = crash ?: return@SettingsPage
        SecondaryButton(
            stringResource(if (copied) R.string.crash_copied else R.string.crash_copy),
            onClick = { copyText(context, report.text); copied = true },
            icon = if (copied) BaIcons.Check else null,
            modifier = Modifier.fillMaxWidth(),
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BaCard {
                SelectionContainer {
                    Text(
                        report.text,
                        style = Ba.type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                        color = c.text2,
                        softWrap = false,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

/** A row in the About card: icon, label, an optional left-to-right detail (an address), a chevron. */
@Composable
private fun AboutRow(icon: ImageVector, label: String, detail: String?, onClick: () -> Unit) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(Modifier.size(30.dp).clip(Px.Small).background(c.glow.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.glow, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = Ba.type.tile, color = c.text1)
            if (detail != null) Text("\u2066" + detail + "\u2069", style = Ba.type.caption, color = c.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(BaIcons.ChevronForward, null, tint = c.text3)
    }
}

/** An email opens the mail app (ACTION_SENDTO, so only mail apps answer); a link opens the browser. */
private fun openContact(context: Context, contact: String) {
    val intent = if (contact.startsWith("http://") || contact.startsWith("https://")) {
        Intent(Intent.ACTION_VIEW, contact.toUri())
    } else {
        Intent(Intent.ACTION_SENDTO, ("mailto:" + contact.removePrefix("mailto:")).toUri())
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No mail app or browser: the address is on screen, so there is nothing else to do.
    }
}

@Composable
private fun crashTime(crash: CrashReport): String {
    val local = crash.at.atZone(ZoneId.systemDefault())
    return jalaliDayMonth(local.toLocalDate()) + " · " + clockText(local.hour * 60 + local.minute)
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    // Android 13+ also shows its own note; the button's «کپی شد» covers the older ones.
    clipboard.setPrimaryClip(ClipData.newPlainText("crash", text))
}
