package app.lantern.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.theme.Ba

/** The bottom tabs, icons only, RTL from the right: Today · Goals (D160). */
enum class Tab(val icon: ImageVector, val label: Int) {
    TODAY(BaIcons.CheckCircle, R.string.tab_today),
    GOALS(BaIcons.Grid, R.string.tab_goals),
}

/** The bar's own height, above the navigation-bar inset; tab screens keep this much clear at the bottom. */
val BOTTOM_BAR_HEIGHT = 61.dp

@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Column(modifier.fillMaxWidth().background(c.surface).navigationBarsPadding()) {
        HorizontalDivider(color = c.border)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().height(60.dp).selectableGroup(),
        ) {
            Tab.entries.forEach { tab ->
                val on = tab == selected
                val label = stringResource(tab.label)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .selectable(on, role = Role.Tab) { onSelect(tab) }
                        .semantics { contentDescription = label },
                ) {
                    Icon(tab.icon, null, tint = if (on) c.text1 else c.text3, modifier = Modifier.size(24.dp))
                    // The active tab's small accent dot.
                    Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(if (on) c.accent else c.surface))
                }
            }
        }
    }
}
