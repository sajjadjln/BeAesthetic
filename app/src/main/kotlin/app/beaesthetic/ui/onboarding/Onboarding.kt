package app.beaesthetic.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.AppContainer
import app.beaesthetic.R
import app.beaesthetic.data.repo.Seed
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.JalaliCalendar
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.buddy.Piker
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Step { HELLO, DATES, REMINDERS, PHONE, BACKUP, DONE }

/**
 * Onboarding, the lean flow (flows O1–O5, D142): hello (or restore a backup) → dates → reminders and the
 * notification permission → phone setup → backup folder → done. Day 0, the plan, the identity sentence and
 * milestones have their own places (Progress, Settings, the Week tab), so they aren't repeated here.
 * Reminders, phone and backup open the real screens; coming back continues where it was.
 */
@Composable
fun OnboardingScreen(
    container: AppContainer,
    onOpenReminders: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenBackup: () -> Unit,
    onFinished: () -> Unit,
) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]
    val settings by remember { container.db.settings().observe().filterNotNull() }.collectAsStateWithLifecycle(null)
    val s = settings ?: return

    // A restore from the hello screen sets onboardingDone: leave as soon as it does.
    LaunchedEffect(s.onboardingDone) { if (s.onboardingDone) onFinished() }

    fun finish() {
        scope.launch {
            container.db.settings().setOnboardingDone(true)
            container.scheduler.reconcile("onboarding")
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            if (step != Step.HELLO) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    (1 until Step.entries.size).forEach { i ->
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= stepIndex) c.accent else c.surface2))
                    }
                }
            }
            val pose = when (step) {
                Step.HELLO -> Pose.WAVE
                Step.DATES -> Pose.POINT
                Step.REMINDERS, Step.PHONE -> Pose.THUMBS
                Step.BACKUP -> Pose.THINK
                Step.DONE -> Pose.CHEER
            }
            Piker(pose, c.accent, Outfit.NONE, Modifier.width(if (step == Step.HELLO) 180.dp else 120.dp).padding(top = 12.dp))
            val next = { stepIndex = (stepIndex + 1).coerceAtMost(Step.entries.lastIndex) }
            when (step) {
                Step.HELLO -> {
                    Title(R.string.onb_hello_title, R.string.onb_hello_body)
                    PrimaryButton(stringResource(R.string.onb_start), { next() }, Modifier.fillMaxWidth())
                    SecondaryButton(stringResource(R.string.onb_restore), onOpenBackup, Modifier.fillMaxWidth(), icon = BaIcons.Download)
                    Text(stringResource(R.string.onb_restore_help), style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center)
                }
                Step.DATES -> {
                    Title(R.string.onb_dates_title, R.string.onb_dates_body)
                    Dates(s.programStartDate, s.programEndDate, container.clock.programDate(s.rolloverHour)) { start, end ->
                        scope.launch {
                            // Yalda stays the halfway day only for the default dates; other dates use the midpoint.
                            val halfway = if (start == Seed.PROGRAM_START && end == Seed.PROGRAM_END) Seed.HALFWAY else null
                            container.db.settings().upsert(s.copy(programStartDate = start, programEndDate = end, halfwayDate = halfway))
                        }
                    }
                    PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                }
                Step.REMINDERS -> {
                    Title(R.string.onb_reminders_title, R.string.onb_reminders_body)
                    Notifications()
                    SecondaryButton(stringResource(R.string.onb_reminders_edit), onOpenReminders, Modifier.fillMaxWidth(), icon = BaIcons.Bell)
                    PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                }
                Step.PHONE -> {
                    Title(R.string.onb_phone_title, R.string.onb_phone_body)
                    SecondaryButton(stringResource(R.string.onb_phone_open), onOpenPhone, Modifier.fillMaxWidth(), icon = BaIcons.Phone)
                    PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                }
                Step.BACKUP -> {
                    Title(R.string.onb_backup_title, R.string.onb_backup_body)
                    if (s.backupTreeUri == null) {
                        SecondaryButton(stringResource(R.string.onb_backup_open), onOpenBackup, Modifier.fillMaxWidth(), icon = BaIcons.Folder)
                        PrimaryButton(stringResource(R.string.onb_later), { next() }, Modifier.fillMaxWidth())
                    } else {
                        PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                    }
                }
                Step.DONE -> {
                    Title(R.string.onb_done_title, R.string.onb_done_body)
                    PrimaryButton(stringResource(R.string.onb_done_go), { finish() }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Title(title: Int, body: Int) {
    val c = Ba.colors
    Text(stringResource(title), style = Ba.type.headline, color = c.text1, textAlign = TextAlign.Center)
    Text(stringResource(body), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
}

/** Day 0 and the finish line, each a row that opens the Jalali calendar. */
@Composable
private fun Dates(start: LocalDate, end: LocalDate, today: LocalDate, onChange: (LocalDate, LocalDate) -> Unit) {
    val c = Ba.colors
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    BaCard {
        listOf(R.string.onb_day0 to start, R.string.onb_finish to end).forEachIndexed { i, (label, date) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = if (open == i) null else i }.padding(vertical = 10.dp),
            ) {
                Text(stringResource(label), style = Ba.type.body, color = c.text2, modifier = Modifier.weight(1f))
                Text(jalaliDayMonth(date) + " " + PersianDigits.format(app.beaesthetic.domain.time.Jalali.fromGregorian(date).year), style = Ba.type.subtitle, color = c.text1)
                Icon(BaIcons.Calendar, null, tint = c.text3, modifier = Modifier.padding(start = 10.dp).size(18.dp))
            }
            if (open == i) {
                val range = if (i == 0) today.minusMonths(6)..today.plusMonths(6) else start.plusWeeks(4)..start.plusYears(2)
                JalaliCalendar(date, today, onPick = { d -> open = null; if (i == 0) onChange(d, maxOf(end, d.plusWeeks(4))) else onChange(start, d) }, pickable = range)
            }
        }
        val calendar = ProgramCalendar(start, end)
        Text(
            stringResource(R.string.onb_dates_summary, PersianDigits.format(calendar.totalWeeks), PersianDigits.format(Chapters.of(calendar).size)),
            style = Ba.type.caption,
            color = c.text3,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** The notification permission (API 33+), asked here with the reason on screen (ARCHITECTURE §7). */
@Composable
private fun Notifications() {
    val context = LocalContext.current
    fun granted() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var ok by remember { mutableStateOf(granted()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok = granted() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ok = granted() }
    if (ok) {
        Text(stringResource(R.string.onb_notifications_on), style = Ba.type.label, color = Ba.colors.accent)
    } else {
        PrimaryButton(stringResource(R.string.onb_notifications), { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }, Modifier.fillMaxWidth(), icon = BaIcons.Bell)
    }
}
