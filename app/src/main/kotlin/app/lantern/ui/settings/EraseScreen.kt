package app.lantern.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.AppContainer
import app.lantern.R
import app.lantern.ui.backup.BackupViewModel
import app.lantern.ui.backup.MessageLine
import app.lantern.ui.backup.StatusCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.theme.Ba
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Settings → «پاک کردن همه‌چیز» (B3, D213). A backup comes first: the status and a button to make one now
 * (or to pick a folder, which makes one). Then a clear confirmation. Calm words, nothing red. On yes the
 * app goes back to a fresh install and [onErased] opens onboarding.
 */
@Composable
fun EraseScreen(container: AppContainer, viewModel: BackupViewModel, onBack: () -> Unit, onErased: () -> Unit) {
    val c = Ba.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(viewModel::folderPicked) }
    var asking by remember { mutableStateOf(false) }
    var erasing by remember { mutableStateOf(false) }

    SettingsPage(stringResource(R.string.erase_title), onBack) {
        Text(stringResource(R.string.erase_intro), style = Ba.type.body, color = c.text2)
        val s = state ?: return@SettingsPage

        SectionLabel(R.string.erase_backup_first)
        StatusCard(s.status)
        s.message?.let { MessageLine(it) }
        PrimaryButton(
            stringResource(
                when {
                    s.running -> R.string.backup_running
                    s.status.hasFolder -> R.string.backup_now
                    else -> R.string.erase_pick_folder
                },
            ),
            onClick = { if (s.status.hasFolder) viewModel.backUpNow() else pickFolder.launch(null) },
            enabled = !s.running && !erasing,
            icon = BaIcons.Upload,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(R.string.erase_then)
        SecondaryButton(
            stringResource(if (erasing) R.string.erase_running else R.string.erase_button),
            onClick = { if (!erasing && !s.running) asking = true },
            icon = BaIcons.Trash,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }

    if (asking) {
        val backedUpToday = state?.status?.daysSince == 0L
        AlertDialog(
            onDismissRequest = { asking = false },
            containerColor = c.surface,
            title = { Text(stringResource(R.string.erase_confirm_title), style = Ba.type.title, color = c.text1) },
            text = {
                Text(
                    stringResource(R.string.erase_confirm_text) + "\n\n" +
                        stringResource(if (backedUpToday) R.string.erase_confirm_backup_today else R.string.erase_confirm_no_backup_today),
                    style = Ba.type.body,
                    color = c.text2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    erasing = true
                    // The app's scope: leaving the screen never stops an erase halfway.
                    val job = container.scope.async { container.eraseEverything() }
                    scope.launch {
                        // Even if a step failed, start over: what's left is reseeded or harmless.
                        runCatching { job.await() }
                        onErased()
                    }
                }) { Text(stringResource(R.string.erase_confirm_yes), color = c.text1) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(stringResource(R.string.cancel), color = c.text2) } },
        )
    }
}
