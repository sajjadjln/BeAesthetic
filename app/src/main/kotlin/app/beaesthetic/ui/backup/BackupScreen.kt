package app.beaesthetic.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.backup.BackupStatus
import app.beaesthetic.backup.RestorePreview
import app.beaesthetic.data.db.BackupRecordEntity
import app.beaesthetic.domain.backup.BackupNotice
import app.beaesthetic.domain.backup.BackupReason
import app.beaesthetic.domain.backup.BackupRules
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.BaSwitch
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import java.time.Instant
import java.time.ZoneId

@Composable
fun BackupRoute(viewModel: BackupViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(viewModel::folderPicked) }
    // Any file: some file managers give a zip another type, and the manifest check decides anyway.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::filePicked) }
    BackupScreen(
        state,
        onBack = onBack,
        onBackUp = { if (state?.status?.hasFolder == true) viewModel.backUpNow() else pickFolder.launch(null) },
        onPickFolder = { pickFolder.launch(null) },
        onAuto = viewModel::setAuto,
        onRestore = { pickFile.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
    )
    state?.preview?.let { preview ->
        RestoreSheet(preview, running = state?.running == true, onConfirm = viewModel::confirmRestore, onDismiss = viewModel::dismissPreview)
    }
}

/** B1 (flows.html): status first, then the one button, the folder and the switch, the last backups, restore. */
@Composable
fun BackupScreen(
    state: BackupUiState?,
    onBack: () -> Unit,
    onBackUp: () -> Unit,
    onPickFolder: () -> Unit,
    onAuto: (Boolean) -> Unit,
    onRestore: () -> Unit,
) {
    val c = Ba.colors
    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        TopBar(stringResource(R.string.backup_title), onBack, stringResource(R.string.cd_back))
        val s = state ?: return@Column
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            StatusCard(s.status)
            s.message?.let { MessageLine(it) }
            PrimaryButton(
                stringResource(if (s.running) R.string.backup_running else R.string.backup_now),
                onClick = onBackUp,
                enabled = !s.running,
                icon = BaIcons.Upload,
                modifier = Modifier.fillMaxWidth(),
            )
            BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                ListRow(onClick = onPickFolder) {
                    Icon(BaIcons.Folder, null, tint = c.text2, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.backup_folder), style = Ba.type.subtitle, color = c.text1)
                        // A path reads left to right even inside a right-to-left line.
                        Text("\u2066" + (s.folderLabel ?: stringResource(R.string.backup_folder_none)) + "\u2069", style = Ba.type.caption, color = c.text3)
                    }
                    Text(stringResource(if (s.folderLabel == null) R.string.backup_folder_pick else R.string.backup_folder_change), style = Ba.type.label, color = c.accent)
                }
                HorizontalDivider(color = c.border)
                ListRow {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.backup_auto), style = Ba.type.subtitle, color = c.text1)
                        Text(stringResource(R.string.backup_auto_help), style = Ba.type.caption, color = c.text3)
                    }
                    BaSwitch(s.autoBackup, onAuto, stringResource(R.string.backup_auto))
                }
            }
            if (s.recent.isNotEmpty()) {
                SectionTitle(stringResource(R.string.backup_recent, PersianDigits.format(BackupRules.KEEP)))
                BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    s.recent.forEachIndexed { i, record ->
                        if (i > 0) HorizontalDivider(color = c.border)
                        RecordRow(record)
                    }
                }
            }
            SecondaryButton(stringResource(R.string.backup_restore), onClick = onRestore, icon = BaIcons.Download, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.backup_privacy), style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

/** Accent when all is well; amber («موعدش رسیده» tone, never red) when the folder needs a look (SPEC §8). */
@Composable
private fun StatusCard(status: BackupStatus) {
    val c = Ba.colors
    val fine = status.notice == BackupNotice.NONE
    val title = when (status.notice) {
        BackupNotice.NO_FOLDER -> stringResource(R.string.backup_no_folder)
        BackupNotice.FAILED -> stringResource(R.string.backup_failed)
        else -> lastText(status.daysSince)
    }
    val detail = when (status.notice) {
        BackupNotice.NO_FOLDER -> stringResource(R.string.backup_no_folder_help)
        BackupNotice.FAILED -> stringResource(R.string.backup_failed_help)
        else -> status.lastOk?.let { recordDetail(it) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (fine) c.accentSubtle else c.amberSubtle).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Shield, null, tint = if (fine) c.onAccentSubtle else c.amber, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ba.type.subtitle, color = c.text1)
            detail?.let { Text(it, style = Ba.type.caption, color = c.text2) }
        }
    }
}

@Composable
private fun lastText(days: Long?): String = when (days) {
    null -> stringResource(R.string.backup_none_yet)
    0L -> stringResource(R.string.backup_last_today)
    1L -> stringResource(R.string.backup_last_yesterday)
    else -> stringResource(R.string.backup_last_days, PersianDigits.format(days))
}

@Composable
private fun MessageLine(message: BackupMessage) {
    val c = Ba.colors
    val good = message == BackupMessage.SAVED || message == BackupMessage.RESTORED
    val text = when (message) {
        BackupMessage.SAVED -> R.string.backup_saved
        BackupMessage.RESTORED -> R.string.backup_restored
        BackupMessage.FAILED -> R.string.backup_failed_help
        BackupMessage.NOT_OURS -> R.string.backup_not_ours
        BackupMessage.NEWER -> R.string.backup_newer
        BackupMessage.DAMAGED -> R.string.backup_damaged
        BackupMessage.RESTORE_FAILED -> R.string.backup_restore_failed
    }
    Text(stringResource(text), style = Ba.type.body, color = if (good) c.onAccentSubtle else c.amber, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun RecordRow(record: BackupRecordEntity) {
    val c = Ba.colors
    val reason = when (runCatching { BackupReason.valueOf(record.reason) }.getOrNull()) {
        BackupReason.AUTO -> R.string.backup_reason_auto
        BackupReason.BEFORE_RESTORE -> R.string.backup_reason_before_restore
        else -> R.string.backup_reason_manual
    }
    ListRow {
        Column(Modifier.weight(1f)) {
            Text(dayText(record.at), style = Ba.type.subtitle, color = c.text1)
            Text(stringResource(reason), style = Ba.type.caption, color = c.text3)
        }
        Text(sizeText(record.sizeBytes), style = Ba.type.caption, color = c.text3)
    }
}

/** «جمعه ۱۵ آبان · ۴۸ مگ · ۹ عکس». */
@Composable
private fun recordDetail(record: BackupRecordEntity): String =
    stringResource(R.string.backup_detail, dayText(record.at), sizeText(record.sizeBytes), PersianDigits.format(record.photoCount))

@Composable
private fun dayText(at: Instant): String {
    val date = at.atZone(ZoneId.systemDefault()).toLocalDate()
    return stringArrayResource(R.array.weekdays)[Weekdays.index(date.dayOfWeek)] + " " + jalaliDayMonth(date)
}

/** «۴۸ مگ», «۲٫۳ مگ», «۳۲۰ کیلوبایت». */
@Composable
fun sizeText(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return when {
        mb >= 10 -> stringResource(R.string.size_mb, PersianDigits.format(Math.round(mb)))
        mb >= 1 -> stringResource(R.string.size_mb, PersianDigits.format("%.1f".format(java.util.Locale.ROOT, mb)).replace('.', '٫'))
        else -> stringResource(R.string.size_kb, PersianDigits.format(maxOf(1, Math.round(bytes / 1024.0))))
    }
}

/** B2: what's inside the file, the safety behavior in words, then one confirm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreSheet(preview: RestorePreview, running: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = Ba.colors
    val m = preview.manifest
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { if (!running) onDismiss() }, sheetState = sheet, containerColor = c.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Text(stringResource(R.string.restore_title), style = Ba.type.title, color = c.text1)
            Text("\u2066" + preview.displayName + "\u2069", style = Ba.type.caption, color = c.text2)
            val date = m.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val rows = listOf(
                R.string.restore_date to dayText(m.createdAt) + " " + PersianDigits.format(Jalali.fromGregorian(date).year),
                R.string.restore_version to PersianDigits.format(m.appVersion.substringBefore('-')),
                R.string.restore_days to PersianDigits.format(m.counts["day"] ?: 0),
                R.string.restore_sessions to stringResource(R.string.restore_sessions_value, PersianDigits.format(m.counts["session"] ?: 0)),
                R.string.restore_tasks to PersianDigits.format(m.counts["task"] ?: 0),
                R.string.restore_photos to PersianDigits.format(m.photos.size),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(14.dp)) {
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(label), style = Ba.type.body, color = c.text2, modifier = Modifier.weight(1f))
                        Text(value, style = Ba.type.subtitle, color = c.text1)
                    }
                }
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(BaIcons.Shield, null, tint = c.onAccentSubtle, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.restore_note), style = Ba.type.body, color = c.text2, modifier = Modifier.weight(1f))
            }
            PrimaryButton(stringResource(if (running) R.string.restore_running else R.string.restore_confirm), onClick = onConfirm, enabled = !running, modifier = Modifier.fillMaxWidth())
            SecondaryButton(stringResource(R.string.restore_cancel), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
