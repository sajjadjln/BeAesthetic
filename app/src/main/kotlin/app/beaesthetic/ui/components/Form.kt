package app.beaesthetic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import app.beaesthetic.R
import app.beaesthetic.ui.theme.Ba

// Form pieces shared by the task editor (08) and the workout plan editor (10).

@Composable
fun Labeled(label: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = Ba.type.label, color = Ba.colors.text2)
        content()
    }
}

/**
 * A form problem, in neutral words and colors: nothing in the app is ever red. A [scrollTo] above 0 brings
 * it into view; a new value (another failed save) does it again.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErrorLine(text: Int, scrollTo: Int = 0) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(scrollTo) { if (scrollTo > 0) requester.bringIntoView() }
    Text(stringResource(text), style = Ba.type.caption, color = Ba.colors.amber, modifier = Modifier.bringIntoViewRequester(requester))
}

@Composable
fun InputBox(
    value: String,
    hint: String,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    onDone: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    val c = Ba.colors
    val focus = LocalFocusManager.current
    // Number fields show Persian digits as they're typed (D87); the parser reads both.
    val numeric = keyboard.keyboardType == androidx.compose.ui.text.input.KeyboardType.Decimal ||
        keyboard.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number
    BasicTextField(
        value = value,
        onValueChange = { onChange(if (numeric) app.beaesthetic.domain.progress.DecimalInput.display(it) else it) },
        singleLine = true,
        textStyle = Ba.type.body.copy(color = c.text1),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = keyboard.copy(imeAction = ImeAction.Done),
        // Without its own action, «تایید» ends the edit: focus goes and the keyboard closes.
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() ?: focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (value.isEmpty()) Text(hint, style = Ba.type.body, color = c.text3)
                inner()
            }
        },
    )
}

@Composable
fun PickPill(text: String, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(if (small) 34.dp else 40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.accentSubtle else c.surface2)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text, style = if (small) Ba.type.caption else Ba.type.label, color = if (selected) c.onAccentSubtle else c.text1)
    }
}

@Composable
fun Stepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepButton(BaIcons.Minus, stringResource(R.string.cd_less), enabled = value > range.first) { onChange(value - 1) }
        // A minimum width keeps the buttons in place while the label grows («—» → «۱۵ ثانیه»).
        Text(label, style = Ba.type.subtitle, color = c.text1, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 72.dp))
        StepButton(BaIcons.Plus, stringResource(R.string.cd_more), enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { Icon(icon, null, tint = if (enabled) c.text1 else c.text3, modifier = Modifier.size(20.dp)) }
}

@Composable
fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, help: Int, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = c.text2, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = Ba.type.subtitle, color = if (enabled) c.text1 else c.text3)
            Text(stringResource(help), style = Ba.type.caption, color = c.text3)
        }
        if (enabled) BaSwitch(checked, onChange, stringResource(title)) else BaSwitch(checked, {}, stringResource(title))
    }
}
