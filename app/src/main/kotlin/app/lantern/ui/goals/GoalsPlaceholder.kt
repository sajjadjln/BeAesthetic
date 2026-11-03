package app.lantern.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.components.BaIcons
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LocalCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.sky

/** The Goals tab in v1 (D191, L8): the character (in Simple its icon, D200), a «در دست ساخت» tag and one line. The goal flow comes after v1. */
@Composable
fun GoalsPlaceholder() {
    val c = Ba.colors
    val character = LocalCharacter.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().sky().safeDrawingPadding().padding(24.dp),
    ) {
        val tag = @Composable {
            Text(
                stringResource(R.string.goals_soon_title),
                style = Ba.type.label.copy(fontWeight = FontWeight.Bold),
                color = c.onGlow,
                modifier = Modifier.clip(Px.Small).background(c.glow).padding(horizontal = 11.dp, vertical = 5.dp),
            )
        }
        if (Ba.companion) {
            Box(contentAlignment = Alignment.BottomCenter) {
                CharacterPortrait(character, Mood.BUSY, Modifier.size(180.dp))
                tag()
            }
            PixelDialog(stringResource(R.string.goals_soon_body), Modifier.fillMaxWidth())
        } else {
            // Simple (D200): no character; the tab's own icon and a plain line.
            Icon(BaIcons.Grid, null, tint = c.glow, modifier = Modifier.size(64.dp))
            tag()
            Text(stringResource(R.string.goals_soon_body_simple), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
        }
    }
}
