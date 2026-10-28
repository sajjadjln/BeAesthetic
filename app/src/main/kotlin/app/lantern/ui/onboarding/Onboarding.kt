package app.lantern.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import app.lantern.ui.theme.BaMotion
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
import app.lantern.AppContainer
import app.lantern.R
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.EndLayout
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.TodayLayout
import app.lantern.ui.theme.sky
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private enum class Step { CHARACTER, LAYOUT, REMINDERS, PHONE, BACKUP, DONE }

/**
 * Onboarding (D186, D188, roadmap L3): pick the character (the app re-themes as you tap), pick Today's
 * layout, then reminders and the notification permission → phone setup → backup folder → done.
 * A backup can be restored from the first step. Reminders, phone and backup open the real screens;
 * coming back continues where it was.
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
    val character = LanternCharacter.of(s.character)

    // A restore from the hello screen sets onboardingDone: leave as soon as it does.
    LaunchedEffect(s.onboardingDone) { if (s.onboardingDone) onFinished() }

    fun finish() {
        scope.launch {
            container.db.settings().setOnboardingDone(true)
            container.scheduler.reconcile("onboarding")
        }
    }

    val back = { stepIndex = (stepIndex - 1).coerceAtLeast(0) }
    BackHandler(enabled = stepIndex > 0) { back() }

    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        // Back and the progress bar stay put; only the step under them slides.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        ) {
            if (stepIndex > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = back),
                ) {
                    Icon(BaIcons.Back, stringResource(R.string.cd_back), tint = c.text1, modifier = Modifier.size(24.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    (1 until Step.entries.size).forEach { i ->
                        val fill by animateColorAsState(if (i <= stepIndex) c.glow else c.surface2, tween(BaMotion.MEDIUM), label = "step")
                        Box(Modifier.weight(1f).height(6.dp).background(fill))
                    }
                }
            }
        }
        val next = { stepIndex = (stepIndex + 1).coerceAtMost(Step.entries.lastIndex) }
        AnimatedContent(
            step,
            transitionSpec = {
                // Forward slides toward the start (in from the left in RTL), back the other way.
                val dir = if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                val spec = tween<IntOffset>(BaMotion.MEDIUM, easing = BaMotion.Standard)
                (slideIntoContainer(dir, spec) + fadeIn(tween(BaMotion.MEDIUM))) togetherWith
                    (slideOutOfContainer(dir, spec) { it / 3 } + fadeOut(tween(BaMotion.SHORT)))
            },
            label = "onboarding",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { st ->
            // The content in the middle, the step's buttons at the bottom in thumb reach, main one last (D147).
            EndLayout(
                buttons = {
                    when (st) {
                        Step.CHARACTER -> {
                            PrimaryButton(stringResource(R.string.onb_char_go, stringResource(character.nameRes)), { next() }, Modifier.fillMaxWidth())
                            Text(
                                stringResource(R.string.onb_restore),
                                style = Ba.type.label,
                                color = c.text3,
                                modifier = Modifier.clip(Px.Small).clickable(role = Role.Button, onClick = onOpenBackup).padding(8.dp),
                            )
                        }
                        Step.LAYOUT -> PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                        Step.REMINDERS -> {
                            SecondaryButton(stringResource(R.string.onb_reminders_edit), onOpenReminders, Modifier.fillMaxWidth(), icon = BaIcons.Bell)
                            PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                        }
                        Step.PHONE -> {
                            SecondaryButton(stringResource(R.string.onb_phone_open), onOpenPhone, Modifier.fillMaxWidth(), icon = BaIcons.Phone)
                            PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                        }
                        Step.BACKUP -> if (s.backupTreeUri == null) {
                            SecondaryButton(stringResource(R.string.onb_backup_open), onOpenBackup, Modifier.fillMaxWidth(), icon = BaIcons.Folder)
                            PrimaryButton(stringResource(R.string.onb_later), { next() }, Modifier.fillMaxWidth())
                        } else {
                            PrimaryButton(stringResource(R.string.onb_next), { next() }, Modifier.fillMaxWidth())
                        }
                        Step.DONE -> PrimaryButton(stringResource(R.string.onb_done_go), { finish() }, Modifier.fillMaxWidth())
                    }
                },
            ) {
                when (st) {
                    Step.CHARACTER -> CharacterStep(character) { pick -> scope.launch { container.db.settings().setCharacter(pick.name) } }
                    Step.LAYOUT -> LayoutStep(character, TodayLayout.of(s.todayLayout)) { pick -> scope.launch { container.db.settings().setTodayLayout(pick.name) } }
                    Step.REMINDERS -> {
                        Title(R.string.onb_reminders_title, R.string.onb_reminders_body)
                        Notifications()
                    }
                    Step.PHONE -> Title(R.string.onb_phone_title, R.string.onb_phone_body)
                    Step.BACKUP -> Title(R.string.onb_backup_title, R.string.onb_backup_body)
                    Step.DONE -> Title(R.string.onb_done_title, R.string.onb_done_body)
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
