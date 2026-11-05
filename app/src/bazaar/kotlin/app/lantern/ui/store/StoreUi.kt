package app.lantern.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.lantern.AppContainer
import app.lantern.R
import app.lantern.domain.store.StoreRules
import app.lantern.store.BazaarStore
import app.lantern.ui.components.BaIcons
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.Px
import kotlinx.coroutines.flow.first

/**
 * Settings → «بررسی به‌روزرسانی» (B4): opens the Bazaar page. When Bazaar knows a newer version the row
 * says so, quietly: «نسخه‌ی تازه اومده».
 */
@Composable
internal fun SettingsStoreRow(container: AppContainer) {
    val c = Ba.colors
    val context = LocalContext.current
    var newer by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { newer = BazaarStore.newerVersion(context) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Px.Card)
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(role = Role.Button) {
                newer?.let { container.store.updateLineClosedFor = it }
                BazaarStore.openPage(context)
            }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(Modifier.size(30.dp).clip(Px.Small).background(c.glow.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(BaIcons.Cloud, null, tint = c.glow, modifier = Modifier.size(16.dp))
        }
        Text(
            stringResource(if (newer != null) R.string.update_new else R.string.update_check),
            style = Ba.type.tile, color = c.text1, modifier = Modifier.weight(1f),
        )
        if (newer != null) Text(stringResource(R.string.update_open), style = Ba.type.label, color = c.glow)
    }
}

/** «درباره» → «امتیاز بده تو بازار», always there. */
@Composable
internal fun AboutStoreRows() {
    val c = Ba.colors
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Px.Card)
            .background(c.surface)
            .clickable(role = Role.Button) { BazaarStore.openRating(context) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(stringResource(R.string.about_rate), style = Ba.type.tile, color = c.text1, modifier = Modifier.weight(1f))
        Icon(BaIcons.ChevronForward, null, tint = c.text3)
    }
}

/**
 * On Today (B4): the update line once per new store version, and the rating ask once ever
 * ([StoreRules.shouldAskRating]). [quiet] while another sheet is open.
 */
@Composable
internal fun TodayStore(container: AppContainer, quiet: Boolean, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val context = LocalContext.current
    var newer by remember { mutableStateOf<Long?>(null) }
    var askRate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        newer = BazaarStore.newerVersion(context)?.takeIf { it > container.store.updateLineClosedFor }
    }
    LaunchedEffect(Unit) {
        if (container.store.rateAnswered) return@LaunchedEffect
        val rollover = container.db.settings().observe().first { it != null }!!.rolloverHour
        container.db.completions().observeAll().collect { rows ->
            val today = container.clock.programDate(rollover)
            if (!askRate && StoreRules.shouldAskRating(today, rows.map { it.date }.toSet(), container.store.rateAnswered)) askRate = true
        }
    }

    newer?.let { version ->
        fun close() { container.store.updateLineClosedFor = version; newer = null }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(Px.Card)
                .background(c.surface)
                .clickable(role = Role.Button) { close(); BazaarStore.openPage(context) }
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        ) {
            Text(stringResource(R.string.update_new), style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.update_open), style = Ba.type.label, color = c.glow)
            TextButton(onClick = ::close) { Text(stringResource(R.string.update_later), style = Ba.type.label, color = c.text3) }
        }
    }

    if (askRate && !quiet) {
        fun answer() { container.store.rateAnswered = true; askRate = false }
        AlertDialog(
            onDismissRequest = ::answer,
            containerColor = c.surface,
            title = { Text(stringResource(R.string.rate_title), style = Ba.type.title, color = c.text1) },
            text = { Text(stringResource(R.string.rate_text), style = Ba.type.body, color = c.text2) },
            confirmButton = {
                TextButton(onClick = { answer(); BazaarStore.openRating(context) }) { Text(stringResource(R.string.rate_yes), color = c.accent) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = ::answer) { Text(stringResource(R.string.rate_never), color = c.text3) }
                    TextButton(onClick = ::answer) { Text(stringResource(R.string.rate_later), color = c.text2) }
                }
            },
        )
    }
}
