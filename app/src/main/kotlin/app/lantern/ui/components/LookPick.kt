package app.lantern.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.colorRes
import app.lantern.ui.theme.lanternColors

/**
 * «با همراه» or «ساده» (D200), in onboarding and Settings: two cards with a line each. Picking one changes
 * the whole app at once.
 */
@Composable
fun LookOptions(companion: Boolean, onPick: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth().selectableGroup()) {
        LookOption(R.string.look_companion, R.string.look_companion_help, companion) { onPick(true) }
        LookOption(R.string.look_simple, R.string.look_simple_help, !companion) { onPick(false) }
    }
}

@Composable
private fun LookOption(title: Int, help: Int, on: Boolean, onClick: () -> Unit) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Px.Card)
            .background(if (on) c.glow.copy(alpha = 0.10f) else c.surface)
            .border(2.dp, if (on) c.glow else Color.White.copy(alpha = 0.08f), Px.Card)
            .selectable(on, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = Ba.type.subtitle, color = if (on) c.glow else c.text1)
            Text(stringResource(help), style = Ba.type.caption, color = c.text2)
        }
        // A radio: a ring, filled in the picked color when on.
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .border(if (on) 7.dp else 2.dp, if (on) c.glow else c.text3, CircleShape),
        )
    }
}

/**
 * Simple's color (D200): the four palettes, named by color instead of by character. The pick is stored as the
 * character, so switching back to a companion keeps the same colors.
 */
@Composable
fun ColorSwatches(picked: LanternCharacter, onPick: (LanternCharacter) -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth().selectableGroup()) {
        LanternCharacter.entries.forEach { who ->
            val on = who == picked
            val p = lanternColors(who)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(Px.Card)
                    .background(c.surface)
                    .border(2.dp, if (on) p.glow else Color.White.copy(alpha = 0.08f), Px.Card)
                    .selectable(on, role = Role.RadioButton) { onPick(who) }
                    .padding(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(34.dp).clip(Px.Small).background(Brush.horizontalGradient(0.4f to p.glow, 0.4f to p.sky2)))
                Text(stringResource(who.colorRes), style = Ba.type.caption, color = if (on) c.text1 else c.text2)
            }
        }
    }
}
