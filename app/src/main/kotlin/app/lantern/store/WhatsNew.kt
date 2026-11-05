package app.lantern.store

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.components.PrimaryButton
import app.lantern.ui.theme.Ba

/** «تازه‌ها» (B4): once after an update to a version that has a text (`whats_new_<versionCode>`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(text: Int, onDone: () -> Unit) {
    val c = Ba.colors
    ModalBottomSheet(onDismissRequest = onDone, containerColor = c.surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.whats_new_title), style = Ba.type.title, color = c.text1)
            Text(stringResource(text), style = Ba.type.body, color = c.text2, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
            PrimaryButton(stringResource(R.string.whats_new_ok), onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
