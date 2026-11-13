package app.lantern.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.AppContainer
import app.lantern.R
import app.lantern.data.db.GoalEntity
import app.lantern.ui.components.BaIcons
import app.lantern.ui.components.EndLayout
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.components.SecondaryButton
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LocalCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.rememberAnimationsOn
import app.lantern.ui.theme.sky
import java.time.ZoneId

/**
 * The bad day (G4, D222): opened by holding a note, or from the companion's one offer for a quiet goal.
 * The companion says one calm line; the why unrolls on a tap; then a small step (Today) or later. It never
 * lists what was missed.
 */
@Composable
fun GoalBadDayScreen(container: AppContainer, goalId: Long, onLater: () -> Unit, onStep: () -> Unit) {
    val goal by remember(goalId) { container.db.goals().observe(goalId) }.collectAsStateWithLifecycle<GoalEntity?>(null)
    val g = goal ?: return
    BadDay(g, onLater, onStep)
}

@Composable
private fun BadDay(g: GoalEntity, onLater: () -> Unit, onStep: () -> Unit) {
    val c = Ba.colors
    val animate = rememberAnimationsOn()
    var open by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onLater)
    Column(Modifier.fillMaxSize().sky().safeDrawingPadding()) {
        EndLayout(
            buttons = {
                PrimaryButton(stringResource(R.string.goal_bad_step), onStep, Modifier.fillMaxWidth(), icon = BaIcons.Flag)
                SecondaryButton(stringResource(R.string.goal_bad_later), onLater, Modifier.fillMaxWidth())
            },
        ) {
            if (Ba.companion) {
                CharacterPortrait(LocalCharacter.current, Mood.CALM, Modifier.size(150.dp))
                PixelDialog(stringResource(R.string.goal_bad_say), Modifier.fillMaxWidth())
            } else {
                Text(stringResource(R.string.goal_bad_say), style = Ba.type.title, color = c.text1, textAlign = TextAlign.Center)
            }
            // The rolled-up why: a tap unrolls it (instant with "Remove animations").
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface)
                    .clickable(enabled = !open, role = Role.Button) { open = true }
                    .padding(14.dp),
            ) {
                Text(stringResource(R.string.goal_bad_why_of, g.title), style = Ba.type.subtitle, color = c.glow, textAlign = TextAlign.Center)
                if (!open) {
                    Icon(BaIcons.ChevronDown, null, tint = c.text3, modifier = Modifier.padding(top = 4.dp).size(20.dp))
                    Text(stringResource(R.string.goal_bad_tap), style = Ba.type.caption, color = c.text3)
                }
            }
            AnimatedVisibility(
                open,
                enter = if (animate) expandVertically(tween(600), expandFrom = Alignment.Top) + fadeIn(tween(600)) else expandVertically(tween(0)),
            ) {
                Why(g.why.orEmpty(), g.whyAt?.atZone(ZoneId.systemDefault())?.toLocalDate(), title = false)
            }
        }
    }
}

/** The companion's one offer on the wall for a quiet goal (D223): one calm line, read it or let it be. */
@Composable
internal fun QuietOffer(title: String, onRead: () -> Unit, onDismiss: () -> Unit) {
    val c = Ba.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clip(Px.Card).background(c.surface).padding(14.dp),
    ) {
        Text(stringResource(R.string.goal_quiet_offer, title), style = Ba.type.body, color = c.text1)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.goal_quiet_read),
                style = Ba.type.label,
                color = c.glow,
                modifier = Modifier.clip(Px.Small).clickable(role = Role.Button, onClick = onRead).padding(6.dp),
            )
            Text(
                stringResource(R.string.goal_bad_later),
                style = Ba.type.label,
                color = c.text3,
                modifier = Modifier.clip(Px.Small).clickable(role = Role.Button, onClick = onDismiss).padding(6.dp),
            )
        }
    }
}
