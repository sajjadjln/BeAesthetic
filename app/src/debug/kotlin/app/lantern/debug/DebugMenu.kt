package app.lantern.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.sky
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Spacer
import app.lantern.AppContainer
import app.lantern.domain.reminders.BlockContent
import app.lantern.domain.reminders.PartOfDay
import app.lantern.domain.reminders.ReminderSlot
import app.lantern.ui.theme.Ba
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Debug builds only (ARCHITECTURE §4): check reminders on the real phone without waiting for 23:00.
 * It is a developer tool, so it is in English and left-to-right; it never ships in a release build.
 */
object DebugMenu {
    const val AVAILABLE = true

    private val time = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm:ss", java.util.Locale.ROOT)
    private fun fmt(i: Instant?) = i?.atZone(ZoneId.systemDefault())?.format(time) ?: "—"

    @Composable
    fun Screen(container: AppContainer, onClose: () -> Unit) {
        val scope = rememberCoroutineScope()
        var message by remember { mutableStateOf("") }
        val upcoming by remember { container.db.reminderLog().observeUpcoming(Instant.now()) }.collectAsStateWithLifecycle(emptyList())
        val recent by remember { container.db.reminderLog().observeRecent(25) }.collectAsStateWithLifecycle(emptyList())
        val events by remember { container.db.events().observeRecent(25) }.collectAsStateWithLifecycle(emptyList())
        LaunchedEffect(Unit) { message = "exact alarms allowed: ${container.scheduler.canScheduleExact()} · notifications: ${container.notifier.canNotify()}" }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LazyColumn(
                Modifier.fillMaxSize().background(Ba.colors.bg).safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClose) { Text("Close") }
                        Text("Debug", style = Ba.type.headline, color = Ba.colors.text1)
                    }
                }
                item { Text(message, style = Ba.type.caption, color = Ba.colors.text2) }
                item { ThemeGallery(container) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            scope.launch {
                                container.scheduler.scheduleTest(Duration.ofMinutes(1))
                                message = "Test alarm set for 1 minute from now. Lock the phone and wait."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Test alarm in 1 minute") }
                        Button(onClick = {
                            scope.launch {
                                val settings = container.db.settings().get() ?: return@launch
                                val slots = container.db.slots().all().map { ReminderSlot(it.profile, it.part, it.time, it.enabled) }
                                val local = container.clock.localNow()
                                val part = PartOfDay.current(local, slots, settings.rolloverHour)
                                val day = container.tasks.day(container.clock.programDate(settings.rolloverHour))
                                val lines = BlockContent.lines(part, day.items, day.minimumMode)
                                if (lines.isEmpty()) {
                                    message = "$part: nothing pending, a real block would show nothing."
                                } else {
                                    container.notifier.showBlock(part, lines, "debug:$part")
                                    message = "Showed the $part block (${lines.size} items)."
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Show the current block now") }
                        OutlinedButton(onClick = {
                            scope.launch { container.scheduler.reconcile("debug"); message = "Reconciled." }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Reconcile now") }
                        // Fake "now" (ARCHITECTURE §4) to see chapter starts and the countdown. Alarms are not
                        // re-planned while the clock is shifted; "Real clock" puts everything back.
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("1 Azar" to "2026-11-22T08:00", "1 Esfand" to "2027-02-20T08:00", "29 Esfand" to "2027-03-20T08:00").forEach { (label, at) ->
                                OutlinedButton(onClick = {
                                    val target = java.time.LocalDateTime.parse(at).atZone(ZoneId.systemDefault()).toInstant()
                                    container.clock.offset = Duration.between(Instant.now(), target)
                                    message = "Clock shifted to $label. Go back to Today."
                                }, modifier = Modifier.weight(1f)) { Text(label) }
                            }
                        }
                        // A week later.
                        OutlinedButton(onClick = {
                            container.clock.offset = container.clock.offset.plusDays(7)
                            message = "Clock moved a week ahead. Go back to Today."
                        }, modifier = Modifier.fillMaxWidth()) { Text("+1 week") }
                        OutlinedButton(onClick = {
                            container.clock.offset = Duration.ZERO
                            scope.launch { container.scheduler.reconcile("debug-clock-reset"); message = "Real clock." }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Real clock (now: ${'$'}{fmt(container.clock.now())})") }
                    }
                }
                item { Text("Upcoming alarms (${upcoming.size})", style = Ba.type.title, color = Ba.colors.text1) }
                items(upcoming, key = { "u${it.id}" }) { Line("${fmt(it.scheduledAt)}  ${it.key}") }
                item { Text("Reminder log", style = Ba.type.title, color = Ba.colors.text1) }
                items(recent, key = { "r${it.id}" }) {
                    val late = it.firedAt?.let { f -> Duration.between(it.scheduledAt, f).seconds }
                    Line("${fmt(it.scheduledAt)}  ${it.key}  ${it.outcome}${late?.let { s -> "  (+${s}s)" } ?: ""}")
                }
                item { Text("Usage log", style = Ba.type.title, color = Ba.colors.text1) }
                items(events, key = { "e${it.id}" }) { Line("${fmt(it.at)}  ${it.type}  ${it.ref ?: ""}  ${it.valueMinutes ?: ""}") }
            }
        }
    }

    @Composable
    private fun Line(text: String) = Text(text, style = Ba.type.caption, color = Ba.colors.text2)

    /**
     * Lantern L1: flip the character and see the theme's pieces at once. The pick is saved, so the whole
     * app re-themes with it.
     */
    @Composable
    private fun ThemeGallery(container: AppContainer) {
        val scope = rememberCoroutineScope()
        val settings by remember { container.db.settings().observe() }.collectAsStateWithLifecycle(null)
        val character = LanternCharacter.of(settings?.character)
        val c = Ba.colors
        Column(Modifier.fillMaxWidth().clip(Px.Card).sky().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Theme (Lantern L1)", style = Ba.type.title, color = c.glow)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LanternCharacter.entries.forEach { ch ->
                    val on = ch == character
                    Box(
                        Modifier.weight(1f).clip(Px.Small).background(if (on) c.glow else c.surface)
                            .clickable { scope.launch { container.db.settings().setCharacter(ch.name) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(ch.name.lowercase(), style = Ba.type.label, color = if (on) c.onGlow else c.text2) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Mood.entries.forEach { m -> CharacterPortrait(character, m, Modifier.weight(1f).size(100.dp)) }
            }
            PixelDialog("سلام! این جعبه‌ی گفت‌وگوئه، حرف‌به‌حرف تایپ می‌شه.", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("glow" to c.glow, "done" to c.accent, "due" to c.amber, "special" to c.special).forEach { (name, color) ->
                    Box(Modifier.weight(1f).clip(Px.Card).background(color).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(name, style = Ba.type.caption, color = c.onGlow)
                    }
                }
            }
        }
    }
}
