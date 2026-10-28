package app.lantern.ui.settings

import app.lantern.AppLanguage
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.TodayLayout
import app.lantern.ui.theme.art
import app.lantern.ui.theme.sky
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.AppContainer
import app.lantern.BuildConfig
import app.lantern.R
import app.lantern.data.db.ReminderOutcome
import app.lantern.data.db.SettingsEntity
import app.lantern.data.repo.toDomain
import app.lantern.domain.reminders.AlarmKind
import app.lantern.domain.reminders.Attention
import app.lantern.domain.reminders.Health
import app.lantern.domain.reminders.LogRow
import app.lantern.domain.reminders.Outcome
import app.lantern.domain.reminders.ReminderHealth
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.domain.time.Profile
import app.lantern.domain.time.Weekdays
import app.lantern.reminders.AlarmScheduler.Companion.toLogRow
import app.lantern.reminders.PhoneSetup
import app.lantern.ui.clockText
import app.lantern.ui.components.BaCard
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.BaSwitch
import app.lantern.ui.components.Chip
import app.lantern.ui.components.ChipKind
import app.lantern.ui.components.ListRow
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.components.SectionTitle
import app.lantern.ui.components.TimePickerDialog
import app.lantern.ui.components.TopBar
import app.lantern.ui.theme.Ba
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A settings sub-screen: top bar and a scrolling column. */
@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        TopBar(title, onBack, stringResource(R.string.cd_back))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) { content() }
    }
}

/**
 * True when reminders are fine: no problem the user hasn't seen yet, and notifications and exact alarms on.
 * The same rule as Today's banner (ReminderHealth.attention), so the two never disagree.
 */
@Composable
private fun rememberRemindersOk(container: AppContainer): Boolean {
    val health = remember { container.db.reminderLog().observeSince(Instant.now() - ReminderHealth.WINDOW).map { rows -> ReminderHealth.check(rows.map { it.toLogRow() }, container.clock.now()) } }
        .collectAsStateWithLifecycle(null).value
    val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
    var systemOk by remember { mutableStateOf(container.notifier.canNotify() && container.scheduler.canScheduleExact()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { systemOk = container.notifier.canNotify() && container.scheduler.canScheduleExact() }
    if (health == null) return true
    return ReminderHealth.attention(health, settings?.healthSeenAt, systemOk) == Attention.NONE
}

/**
 * The test reminder is for the first day, before any real one has had a chance to arrive. Once one has, the
 * health card says more than a test would, so the button goes (friends' feedback, D147).
 */
@Composable
private fun rememberNoRealReminderYet(container: AppContainer): Boolean =
    remember { container.db.reminderLog().observeSince(Instant.now() - ReminderHealth.WINDOW).map { rows -> ReminderHealth.check(rows.map { it.toLogRow() }, container.clock.now()).checked == 0 } }
        .collectAsStateWithLifecycle(false).value

@Composable
private fun HealthLine(ok: Boolean, action: (@Composable () -> Unit)? = null) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(Px.Card).background(if (ok) c.accentSubtle else c.amberSubtle).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Shield, null, tint = if (ok) c.onAccentSubtle else c.amber, modifier = Modifier.size(22.dp))
        Text(stringResource(if (ok) R.string.health_ok else R.string.health_not_ok), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

// ---------------------------------------------------------------- 06 Settings

enum class SettingsDestination { TASKS, REMINDERS, BACKUP, PHONE, DEBUG }

@Composable
fun SettingsScreen(container: AppContainer, debugAvailable: Boolean, onBack: () -> Unit, onOpen: (SettingsDestination) -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val remindersOk = rememberRemindersOk(container)
    val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
    val character = LanternCharacter.of(settings?.character)
    val layout = TodayLayout.of(settings?.todayLayout)
    SettingsPage(stringResource(R.string.settings_title), onBack) {
        HealthLine(remindersOk)

        // «همراه» (D189): the current character with a line, and the four; a tap re-themes the app at once.
        SectionLabel(R.string.settings_companion)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).border(2.dp, c.glow.copy(alpha = 0.35f), Px.Card).padding(12.dp),
        ) {
            CharacterPortrait(character, Mood.CALM, Modifier.size(70.dp), breathe = false)
            Column(Modifier.weight(1f)) {
                Text(stringResource(character.nameRes), style = Ba.type.title, color = c.glow)
                Text(stringResource(character.settingsLine()), style = Ba.type.caption, color = c.text2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanternCharacter.entries.forEach { who ->
                val on = who == character
                val name = stringResource(who.nameRes)
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(Px.Card)
                        .background(c.surface)
                        .border(if (on) 3.dp else 2.dp, if (on) c.glow else Color.White.copy(alpha = 0.12f), Px.Card)
                        .selectable(on, role = Role.RadioButton) { scope.launch { container.db.settings().setCharacter(who.name); app.lantern.widget.TodayWidget.refresh(context) } }
                        .semantics { contentDescription = name }
                        .padding(4.dp),
                ) {
                    Image(
                        painterResource(who.art(Mood.CALM)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (on) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.45f) }),
                        alpha = if (on) 1f else 0.7f,
                        modifier = Modifier.fillMaxSize().clip(Px.Small),
                    )
                }
            }
        }

        // «صفحه‌ی امروز» (D188): the two layouts; Today swaps at once.
        SectionLabel(R.string.settings_today_layout)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodayLayout.entries.forEach { option ->
                val on = option == layout
                Text(
                    stringResource(if (option == TodayLayout.LIST) R.string.layout_list else R.string.layout_parts),
                    style = Ba.type.label,
                    color = if (on) c.glow else c.text2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(Px.Card)
                        .background(if (on) c.glow.copy(alpha = 0.15f) else c.surface)
                        .border(if (on) 3.dp else 2.dp, if (on) c.glow else Color.White.copy(alpha = 0.12f), Px.Card)
                        .selectable(on, role = Role.RadioButton) { scope.launch { container.db.settings().setTodayLayout(option.name) } }
                        .padding(vertical = 14.dp),
                )
            }
        }

        // «زبان» (D176): like the phone, Persian or English. The screen is redrawn in the new language.
        SectionLabel(R.string.settings_language)
        val language = remember { AppLanguage.choice(context) }
        Segmented(
            listOf(stringResource(R.string.lang_system), stringResource(R.string.lang_fa), stringResource(R.string.lang_en)),
            AppLanguage.Choice.entries.indexOf(language),
        ) { i ->
            val pick = AppLanguage.Choice.entries[i]
            if (pick != language) {
                AppLanguage.set(context, pick)
                (context as? android.app.Activity)?.recreate()
            }
        }

        SectionLabel(R.string.settings_more)
        val rows = listOf(
            Triple(SettingsDestination.TASKS, BaIcons.List, R.string.settings_tasks),
            Triple(SettingsDestination.REMINDERS, BaIcons.Bell, R.string.settings_reminders),
            Triple(SettingsDestination.BACKUP, BaIcons.Cloud, R.string.settings_backup),
            Triple(SettingsDestination.PHONE, BaIcons.Phone, R.string.settings_phone),
        )
        val backup by remember { container.backup.observeStatus() }.collectAsStateWithLifecycle(null)
        rows.forEach { (dest, icon, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Px.Card)
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable(role = Role.Button) { onOpen(dest) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Box(Modifier.size(30.dp).clip(Px.Small).background(c.glow.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = c.glow, modifier = Modifier.size(16.dp))
                }
                Text(stringResource(label), style = Ba.type.tile, color = c.text1, modifier = Modifier.weight(1f))
                if (dest == SettingsDestination.BACKUP) backup?.let { b -> BackupTileLine(b) }
            }
        }
        Text(
            stringResource(R.string.settings_version, "\u2066" + PersianDigits.format(BuildConfig.VERSION_NAME) + "\u2069"),
            style = Ba.type.caption,
            color = c.text3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        if (debugAvailable) {
            SecondaryButton("Debug", onClick = { onOpen(SettingsDestination.DEBUG) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SectionLabel(text: Int) {
    Text(stringResource(text), style = Ba.type.label, color = Ba.colors.text3, modifier = Modifier.padding(top = 10.dp))
}

/** A short line in each character's voice under its name (settings-task.html). */
private fun LanternCharacter.settingsLine(): Int = when (this) {
    LanternCharacter.KNIGHT -> R.string.companion_knight
    LanternCharacter.VAMPIRE -> R.string.companion_vampire
    LanternCharacter.WIZARD -> R.string.companion_wizard
    LanternCharacter.GOTH -> R.string.companion_goth
}

/** «۳ روز پیش» under «پشتیبان», amber when the folder needs a look (SPEC §8). */
@Composable
private fun BackupTileLine(status: app.lantern.backup.BackupStatus) {
    val c = Ba.colors
    val text = when {
        !status.hasFolder -> stringResource(R.string.settings_backup_none)
        status.notice == app.lantern.domain.backup.BackupNotice.FAILED -> stringResource(R.string.backup_failed)
        status.daysSince == null -> stringResource(R.string.backup_none_yet)
        status.daysSince == 0L -> stringResource(R.string.settings_backup_today)
        else -> stringResource(R.string.settings_backup_days, PersianDigits.format(status.daysSince))
    }
    val fine = status.notice == app.lantern.domain.backup.BackupNotice.NONE
    Text(text, style = Ba.type.caption, color = if (fine) c.text3 else c.amber)
}

// ---------------------------------------------------------------- 09 Reminders

@Composable
fun RemindersScreen(container: AppContainer, onBack: () -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    val remindersOk = rememberRemindersOk(container)
    val slots by container.db.slots().observeAll().collectAsStateWithLifecycle(emptyList())
    val tasks by container.db.tasks().observeActive().collectAsStateWithLifecycle(emptyList())
    val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
    var profile by remember { mutableStateOf(Profile.NORMAL) }
    var editing by remember { mutableStateOf<Pair<Int, (Int) -> Unit>?>(null) }
    var pickDelay by remember { mutableStateOf(false) }
    var testKey by remember { mutableStateOf<String?>(null) }
    val partNames = listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night).map { stringResource(it) }

    fun changed(block: suspend () -> Unit) = scope.launch {
        block()
        container.scheduler.reconcile("reminders-edit")
    }

    SettingsPage(stringResource(R.string.reminders_title), onBack) {
        val offerTest = rememberNoRealReminderYet(container)
        HealthLine(remindersOk, if (!offerTest) null else { ->
            Box(Modifier.clip(RoundedCornerShape(50)).clickable { scope.launch { testKey = container.scheduler.scheduleTest(Duration.ofMinutes(1)) } }) {
                Chip(stringResource(R.string.reminders_test), ChipKind.ACCENT)
            }
        })
        testKey?.let { TestStatus(container, it) }
        Segmented(
            options = listOf(stringResource(R.string.profile_normal), stringResource(R.string.profile_friday)),
            selected = profile.ordinal,
            onSelect = { profile = Profile.entries[it] },
        )
        BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Part.entries.forEachIndexed { i, part ->
                val slot = slots.firstOrNull { it.profile == profile && it.part == part } ?: return@forEachIndexed
                if (i > 0) HorizontalDivider(color = c.border)
                ListRow(onClick = { editing = slot.time to { t -> changed { container.db.slots().upsert(slot.copy(time = t)) } } }) {
                    Icon(BaIcons.of(part), null, tint = c.text2, modifier = Modifier.size(22.dp))
                    Text(partNames[part.ordinal], style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                    Text(clockText(slot.time), style = Ba.type.title, color = if (slot.enabled) c.text1 else c.text3)
                    BaSwitch(slot.enabled, { on -> changed { container.db.slots().upsert(slot.copy(enabled = on)) } }, partNames[part.ordinal])
                }
            }
        }
        SectionTitle(stringResource(R.string.task_alarms))
        val withAlarm = tasks.filter { it.alarmTime != null }
        if (withAlarm.isEmpty()) {
            Text(stringResource(R.string.task_alarms_empty), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            val weekdays = stringArrayResource(R.array.weekdays)
            BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                withAlarm.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = c.border)
                    ListRow {
                        Icon(BaIcons.of(t.toDomain().icon), null, tint = c.text2, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = Ba.type.subtitle, color = c.text1)
                            val days = if (t.alarmWeekdays == Weekdays.ALL) stringResource(R.string.every_day)
                            else (0..6).filter { t.alarmWeekdays and (1 shl it) != 0 }.joinToString("، ") { weekdays[it] }
                            Text(days, style = Ba.type.caption, color = c.text3)
                        }
                        Text(clockText(t.alarmTime!!), style = Ba.type.title, color = c.text1)
                        BaSwitch(t.alarmEnabled, { on -> changed { container.db.tasks().setAlarmEnabled(t.id, on) } }, t.title)
                    }
                }
            }
        }
        settings?.let { s ->
            BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                ListRow(onClick = { pickDelay = true }) {
                    Text(stringResource(R.string.follow_up_delay), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.minutes, PersianDigits.format(s.followUpMinutes)), style = Ba.type.body, color = c.text2)
                }
                HorizontalDivider(color = c.border)
                ListRow(onClick = { editing = s.ritualTime to { t -> changed { container.db.settings().upsert(s.copy(ritualTime = t)) } } }) {
                    Text(stringResource(R.string.friday_review), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                    Text(clockText(s.ritualTime), style = Ba.type.title, color = c.text1)
                }
            }
            if (pickDelay) {
                ChoiceDialog(
                    options = listOf(30, 45, 60, 90),
                    label = { stringResource(R.string.minutes, PersianDigits.format(it)) },
                    selected = s.followUpMinutes,
                    onPick = { m -> pickDelay = false; changed { container.db.settings().upsert(s.copy(followUpMinutes = m)) } },
                    onDismiss = { pickDelay = false },
                )
            }
        }
    }

    editing?.let { (minute, onSet) ->
        TimePickerDialog(minute, onPick = { onSet(it); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
private fun <T> ChoiceDialog(options: List<T>, label: @Composable (T) -> String, selected: T, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    val c = Ba.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = c.text2) } },
        containerColor = c.surface,
        text = {
            Column {
                options.forEach { o ->
                    ListRow(onClick = { onPick(o) }) {
                        Text(label(o), style = Ba.type.subtitle, color = if (o == selected) c.accent else c.text1, modifier = Modifier.weight(1f))
                        if (o == selected) Icon(BaIcons.Check, null, tint = c.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
    )
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = Ba.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(4.dp),
    ) {
        options.forEachIndexed { i, text ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (i == selected) c.surface else c.surface2)
                    .selectable(i == selected, role = Role.Tab) { onSelect(i) },
            ) {
                Text(text, style = Ba.type.label, color = if (i == selected) c.text1 else c.text2)
            }
        }
    }
}

/** Watches the reminder log for the test alarm: «رسید!» once it fires and shows. */
@Composable
private fun TestStatus(container: AppContainer, key: String) {
    val row by remember(key) { container.db.reminderLog().observeLatest(key) }.collectAsStateWithLifecycle(null)
    // A test that arrives proves reminders work now: older problems count as seen, like opening «گوشی» (D96).
    LaunchedEffect(row?.outcome) {
        if (row?.outcome == ReminderOutcome.SHOWN) {
            container.db.settings().get()?.let { container.db.settings().upsert(it.copy(healthSeenAt = container.clock.now())) }
        }
    }
    val text = when (row?.outcome) {
        ReminderOutcome.SHOWN -> stringResource(R.string.test_arrived)
        ReminderOutcome.NOTIFICATIONS_OFF -> stringResource(R.string.test_blocked)
        else -> stringResource(R.string.test_scheduled)
    }
    Text(text, style = Ba.type.body, color = if (row?.outcome == ReminderOutcome.SHOWN) Ba.colors.accent else Ba.colors.text2, modifier = Modifier.padding(horizontal = 4.dp))
}

// ---------------------------------------------------------------- N2 Phone

@Composable
fun PhoneScreen(container: AppContainer, onBack: () -> Unit) {
    val c = Ba.colors
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
    val rows by remember { container.db.reminderLog().observeSince(Instant.now() - ReminderHealth.WINDOW) }.collectAsStateWithLifecycle(emptyList())
    // Read before the first frame, so a row never shows «باز کن» for a moment and then drops it.
    var notifOk by remember { mutableStateOf(container.notifier.canNotify()) }
    var exactOk by remember { mutableStateOf(container.scheduler.canScheduleExact()) }
    var batteryOk by remember { mutableStateOf(PhoneSetup.isIgnoringBattery(context)) }
    // Xiaomi's autostart, read from the phone when it can be; null = the phone won't say.
    var autostart by remember { mutableStateOf(PhoneSetup.autostartState(context)) }
    var openedAutostart by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf<Ask?>(null) }
    var testKey by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifOk = container.notifier.canNotify()
        exactOk = container.scheduler.canScheduleExact()
        batteryOk = PhoneSetup.isIgnoringBattery(context)
        autostart = PhoneSetup.autostartState(context)
        // Back from Xiaomi's autostart screen and the phone can't tell us: ask, never assume.
        if (openedAutostart && autostart == null) ask = Ask.AUTOSTART
        openedAutostart = false
        // Re-plan with whatever is allowed now (exact alarms may have just been switched on).
        scope.launch { container.scheduler.reconcile("phone-screen") }
    }
    fun confirm(which: Ask, on: Boolean) = scope.launch {
        container.db.settings().get()?.let {
            container.db.settings().upsert(if (which == Ask.AUTOSTART) it.copy(autostartConfirmed = on) else it.copy(recentsLockConfirmed = on))
        }
    }
    // Opening this screen counts as seeing the problems: Today's banner goes quiet until something new happens.
    LaunchedEffect(Unit) {
        container.db.settings().get()?.let { container.db.settings().upsert(it.copy(healthSeenAt = container.clock.now())) }
    }
    val xiaomi = remember { PhoneSetup.isXiaomi() }
    val now = container.clock.now()
    val logRows = rows.map { it.toLogRow() }
    val health = ReminderHealth.check(logRows, now)

    SettingsPage(stringResource(R.string.phone_title), onBack) {
        BaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.phone_this_week), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                if (health.checked > 0) {
                    Chip(stringResource(R.string.phone_on_time, PersianDigits.format(health.onTime), PersianDigits.format(health.checked)), if (health.ok) ChipKind.ACCENT else ChipKind.NEUTRAL)
                }
            }
            Text(stringResource(R.string.phone_week_help), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(top = 2.dp))
            HealthBars(logRows, health, now, Modifier.padding(vertical = 10.dp))
            Text(healthDetail(health), style = Ba.type.caption, color = c.text2)
        }
        // Each row opens the real setting. The tick shows the phone's real state; only what no API can read
        // (autostart on some versions, the recents lock) is confirmed by the user after doing it. Those come
        // from settings, so the rows wait for settings to load.
        if (settings != null) BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            CheckRow(stringResource(R.string.row_notifications), notifOk, stringResource(R.string.row_notifications_help)) { PhoneSetup.openNotifications(activity) }
            HorizontalDivider(color = c.border)
            CheckRow(stringResource(R.string.row_exact), exactOk, stringResource(R.string.row_exact_help)) { PhoneSetup.openExactAlarms(activity) }
            if (xiaomi) {
                HorizontalDivider(color = c.border)
                val known = autostart
                CheckRow(
                    stringResource(R.string.row_autostart),
                    known ?: (settings?.autostartConfirmed == true),
                    stringResource(if (known == null) R.string.row_autostart_help_unknown else R.string.row_autostart_help),
                ) {
                    openedAutostart = true
                    PhoneSetup.openAutostart(activity)
                }
            }
            HorizontalDivider(color = c.border)
            CheckRow(stringResource(R.string.row_battery), batteryOk, stringResource(if (xiaomi) R.string.row_battery_help_xiaomi else R.string.row_battery_help)) {
                PhoneSetup.openBattery(activity)
            }
            if (xiaomi) {
                HorizontalDivider(color = c.border)
                CheckRow(stringResource(R.string.row_recents), settings?.recentsLockConfirmed == true, stringResource(R.string.row_recents_help)) {
                    ask = Ask.RECENTS
                }
            }
        }
        ask?.let { which ->
            val c2 = Ba.colors
            AlertDialog(
                onDismissRequest = { ask = null },
                containerColor = c2.surface,
                title = { Text(stringResource(if (which == Ask.AUTOSTART) R.string.ask_autostart_title else R.string.ask_recents_title), style = Ba.type.title, color = c2.text1) },
                text = { Text(stringResource(if (which == Ask.AUTOSTART) R.string.ask_autostart_text else R.string.ask_recents_text), style = Ba.type.body, color = c2.text2) },
                confirmButton = { TextButton(onClick = { confirm(which, true); ask = null }) { Text(stringResource(if (which == Ask.AUTOSTART) R.string.ask_yes_autostart else R.string.ask_yes_recents), color = c2.accent) } },
                dismissButton = { TextButton(onClick = { confirm(which, false); ask = null }) { Text(stringResource(R.string.ask_not_yet), color = c2.text2) } },
            )
        }
        if (health.checked == 0 || testKey != null) {
            SecondaryButton(
                stringResource(R.string.test_notification),
                onClick = { scope.launch { testKey = container.scheduler.scheduleTest(Duration.ofMinutes(1)) } },
                icon = BaIcons.Bell,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        testKey?.let { TestStatus(container, it) }
    }
}

@Composable
private fun healthDetail(health: Health): String {
    val weekdays = stringArrayResource(R.array.weekdays)
    val parts = listOf(R.string.part_morning, R.string.part_midday, R.string.part_evening, R.string.part_night).map { stringResource(it) }
    fun where(row: LogRow): Pair<String, String> {
        val day = row.scheduledAt.atZone(ZoneId.systemDefault())
        return weekdays[Weekdays.index(day.dayOfWeek)] to (row.part?.let { parts[it.ordinal] } ?: clockText(day.hour * 60 + day.minute))
    }
    return when {
        health.checked == 0 -> stringResource(R.string.phone_nothing_yet)
        health.missed.isNotEmpty() -> where(health.missed.last()).let { (d, p) -> stringResource(R.string.phone_missed_detail, d, p) }
        health.blocked.isNotEmpty() -> where(health.blocked.last()).let { (d, p) -> stringResource(R.string.phone_blocked_detail, d, p) }
        health.late.isNotEmpty() -> health.late.last().let { r ->
            val (d, p) = where(r)
            stringResource(R.string.phone_late_detail, PersianDigits.format(Duration.between(r.scheduledAt, r.firedAt).toMinutes()), d, p)
        }
        else -> stringResource(R.string.phone_all_good)
    }
}

/** One bar per reminder this week: full height in the accent when on time, shorter and neutral when not. */
@Composable
private fun HealthBars(rows: List<LogRow>, health: Health, now: Instant, modifier: Modifier = Modifier) {
    val c = Ba.colors
    val checked = rows.filter {
        it.kind != AlarmKind.TEST && it.outcome != Outcome.CANCELLED && it.outcome != Outcome.DEVICE_OFF &&
            it.scheduledAt <= now - ReminderHealth.GRACE
    }
    val bad = (health.late + health.missed + health.blocked).map { it.id }.toSet()
    Canvas(modifier.fillMaxWidth().height(36.dp)) {
        if (checked.isEmpty()) return@Canvas
        val gap = 2.dp.toPx()
        val w = ((size.width - gap * (checked.size - 1)) / checked.size).coerceIn(1f, 8.dp.toPx())
        checked.forEachIndexed { i, row ->
            val isBad = row.id in bad
            val h = if (isBad) size.height * .45f else size.height
            // Time flows right → left in RTL: the oldest bar sits on the right.
            val x = size.width - (i + 1) * w - i * gap
            drawRoundRect(if (isBad) c.text3 else c.accent, Offset(x, size.height - h), Size(w, h), CornerRadius(w / 2))
        }
    }
}

private enum class Ask { AUTOSTART, RECENTS }

/** One setup row: the whole row opens the setting; the circle only shows the state, it can't be ticked. */
@Composable
private fun CheckRow(label: String, ok: Boolean, help: String?, onOpen: () -> Unit) {
    val c = Ba.colors
    ListRow(onClick = onOpen) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (ok) c.accent else c.surface)
                .then(if (ok) Modifier else Modifier.border(2.dp, c.border, CircleShape)),
        ) {
            if (ok) Icon(BaIcons.Check, null, tint = c.onAccent, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(label, style = Ba.type.subtitle, color = c.text1)
            help?.let { Text(it, style = Ba.type.caption, color = c.text3) }
        }
        if (!ok) {
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(c.surface2).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.open), style = Ba.type.label, color = c.text1)
            }
        }
    }
}

// ---------------------------------------------------------------- 06b Appearance
