package app.beaesthetic.ui.settings

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.AppContainer
import app.beaesthetic.BuildConfig
import app.beaesthetic.R
import app.beaesthetic.data.db.ReminderOutcome
import app.beaesthetic.data.db.SettingsEntity
import app.beaesthetic.data.repo.toDomain
import app.beaesthetic.domain.reminders.AlarmKind
import app.beaesthetic.domain.reminders.Attention
import app.beaesthetic.domain.reminders.Health
import app.beaesthetic.domain.reminders.LogRow
import app.beaesthetic.domain.reminders.Outcome
import app.beaesthetic.domain.reminders.ReminderHealth
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Profile
import app.beaesthetic.domain.time.Weekdays
import app.beaesthetic.reminders.AlarmScheduler.Companion.toLogRow
import app.beaesthetic.reminders.PhoneSetup
import app.beaesthetic.ui.clockText
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.BaSwitch
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.DayRing
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.components.TimePickerDialog
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.theme.Accent
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.ui.theme.ThemeMode
import app.beaesthetic.ui.theme.baColors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A settings sub-screen: top bar and a scrolling column. */
@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ba.colors.bg).safeDrawingPadding()) {
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

@Composable
private fun HealthLine(ok: Boolean, action: (@Composable () -> Unit)? = null) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (ok) c.accentSubtle else c.amberSubtle).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(BaIcons.Shield, null, tint = if (ok) c.onAccentSubtle else c.amber, modifier = Modifier.size(22.dp))
        Text(stringResource(if (ok) R.string.health_ok else R.string.health_not_ok), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

// ---------------------------------------------------------------- 06 Settings

enum class SettingsDestination { TASKS, REMINDERS, WORKOUT, BACKUP, APPEARANCE, PHONE, DEBUG }

@Composable
fun SettingsScreen(container: AppContainer, debugAvailable: Boolean, onBack: () -> Unit, onOpen: (SettingsDestination) -> Unit) {
    val c = Ba.colors
    val remindersOk = rememberRemindersOk(container)
    SettingsPage(stringResource(R.string.settings_title), onBack) {
        HealthLine(remindersOk)
        val tiles = listOf(
            Triple(SettingsDestination.TASKS, BaIcons.List, R.string.settings_tasks),
            Triple(SettingsDestination.REMINDERS, BaIcons.Bell, R.string.settings_reminders),
            Triple(SettingsDestination.WORKOUT, BaIcons.Dumbbell, R.string.settings_workout),
            Triple(SettingsDestination.BACKUP, BaIcons.Cloud, R.string.settings_backup),
            Triple(SettingsDestination.APPEARANCE, BaIcons.Palette, R.string.settings_appearance),
            Triple(SettingsDestination.PHONE, BaIcons.Phone, R.string.settings_phone),
        )
        // The workout plan and backup arrive in slices 4 and 6.
        val ready = setOf(SettingsDestination.TASKS, SettingsDestination.REMINDERS, SettingsDestination.APPEARANCE, SettingsDestination.PHONE)
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (dest, icon, label) ->
                    val enabled = dest in ready
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.surface)
                            .clickable(enabled = enabled, role = Role.Button) { onOpen(dest) }
                            .padding(16.dp),
                    ) {
                        Icon(icon, null, tint = if (enabled) c.text2 else c.text3, modifier = Modifier.size(26.dp))
                        Column {
                            Text(stringResource(label), style = Ba.type.tile, color = if (enabled) c.text1 else c.text3)
                            if (!enabled) Text(stringResource(R.string.soon), style = Ba.type.caption, color = c.text3)
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            // The adaptive icon's two vector layers (painterResource can't load an adaptive-icon XML). 108 dp
            // layers show their middle 72 dp, so they are drawn at 1.5× the circle and clipped.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clip(CircleShape)) {
                Image(painterResource(R.drawable.ic_launcher_background), null, modifier = Modifier.requiredSize(72.dp))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, modifier = Modifier.requiredSize(72.dp))
            }
            Column {
                Text(stringResource(R.string.app_name), style = Ba.type.subtitle, color = c.text1)
                Text(stringResource(R.string.settings_version, "\u2066" + PersianDigits.format(BuildConfig.VERSION_NAME) + "\u2069"), style = Ba.type.caption, color = c.text3)
            }
        }
        if (debugAvailable) {
            SecondaryButton("Debug", onClick = { onOpen(SettingsDestination.DEBUG) }, modifier = Modifier.fillMaxWidth())
        }
    }
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
        HealthLine(remindersOk) {
            Box(Modifier.clip(RoundedCornerShape(50)).clickable { scope.launch { testKey = container.scheduler.scheduleTest(Duration.ofMinutes(1)) } }) {
                Chip(stringResource(R.string.reminders_test), ChipKind.ACCENT)
            }
        }
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
        SecondaryButton(
            stringResource(R.string.test_notification),
            onClick = { scope.launch { testKey = container.scheduler.scheduleTest(Duration.ofMinutes(1)) } },
            icon = BaIcons.Bell,
            modifier = Modifier.fillMaxWidth(),
        )
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

@Composable
fun AppearanceScreen(container: AppContainer, onBack: () -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
    val s = settings ?: return
    val names = stringArrayResource(R.array.accent_names)
    val current = runCatching { Accent.valueOf(s.accentColor) }.getOrDefault(Accent.GREEN)
    fun save(update: (SettingsEntity) -> SettingsEntity) = scope.launch { container.db.settings().upsert(update(s)) }

    SettingsPage(stringResource(R.string.appearance_title), onBack) {
        SectionTitle(stringResource(R.string.appearance_color))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Accent.entries.forEach { accent ->
                val swatch = baColors(accent, c.isDark).accent
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .then(if (accent == current) Modifier.border(2.dp, swatch, CircleShape).padding(4.dp) else Modifier)
                            .clip(CircleShape)
                            .background(swatch)
                            .selectable(accent == current, role = Role.RadioButton) { save { it.copy(accentColor = accent.name) } }
                            .semantics { contentDescription = names[accent.ordinal] },
                    ) {
                        if (accent == current) Icon(BaIcons.Check, null, tint = c.onAccent, modifier = Modifier.size(20.dp))
                    }
                    Text(names[accent.ordinal], style = Ba.type.caption, color = c.text2)
                }
            }
        }
        BaCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DayRing(8, 12, PersianDigits.format(8), "", "", ringSize = 88.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..4).forEach { Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(c.grid[it])) }
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(c.grid[0]))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(stringResource(R.string.chip_record), ChipKind.ACCENT)
                        Chip(stringResource(R.string.tag_important), ChipKind.AMBER)
                    }
                    Chip(stringResource(R.string.chip_special), ChipKind.SPECIAL)
                }
            }
        }
        SectionTitle(stringResource(R.string.appearance_mode))
        val modes = ThemeMode.entries
        val mode = runCatching { ThemeMode.valueOf(s.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
        Segmented(
            options = listOf(stringResource(R.string.mode_system), stringResource(R.string.mode_dark), stringResource(R.string.mode_light)),
            selected = mode.ordinal,
            onSelect = { i -> save { it.copy(themeMode = modes[i].name) } },
        )
        BaCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            ListRow {
                Text(stringResource(R.string.appearance_buddy), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                BaSwitch(s.showBuddy, { on -> save { it.copy(showBuddy = on) } }, stringResource(R.string.appearance_buddy))
            }
        }
    }
}
