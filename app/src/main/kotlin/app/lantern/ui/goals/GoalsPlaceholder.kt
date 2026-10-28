package app.lantern.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.sky

/** The Goals tab in v1: only a «در دست ساخت» note (D191). The goal flow comes after v1. */
@Composable
fun GoalsPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().sky().safeDrawingPadding().padding(24.dp),
    ) {
        Text(stringResource(R.string.goals_soon_title), style = Ba.type.title, color = Ba.colors.text1)
        Text(stringResource(R.string.goals_soon_body), style = Ba.type.body, color = Ba.colors.text2, textAlign = TextAlign.Center)
    }
}
