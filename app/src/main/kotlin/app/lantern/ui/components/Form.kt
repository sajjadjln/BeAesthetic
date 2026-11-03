package app.lantern.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import app.lantern.R
import app.lantern.ui.theme.Ba

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
        onValueChange = { onChange(if (numeric) app.lantern.domain.text.PersianDigits.format(it).replace('.', '٫').replace(',', '٫').replace('/', '٫') else it) },
        singleLine = true,
        textStyle = Ba.type.body.copy(color = c.text1),
        cursorBrush = SolidColor(c.glow),
        keyboardOptions = keyboard.copy(imeAction = ImeAction.Done),
        // Without its own action, «تایید» ends the edit: focus goes and the keyboard closes.
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() ?: focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().clip(app.lantern.ui.theme.Px.Card).background(c.surface2).padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (value.isEmpty()) Text(hint, style = Ba.type.body, color = c.text3)
                inner()
            }
        },
    )
}

/**
 * A choice pill. In a [ChoiceGroup] pass [drawSelection] false: the group's sliding highlight marks the
 * chosen one (S1), and the pill only turns its text to the accent.
 */
@Composable
fun PickPill(text: String, selected: Boolean, modifier: Modifier = Modifier, small: Boolean = false, drawSelection: Boolean = true, onClick: () -> Unit) {
    val c = Ba.colors
    val ink by animateColorAsState(if (selected) c.glow else c.text1, tween(200), label = "pillText")
    val own = selected && drawSelection
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(if (small) 34.dp else 40.dp)
            .clip(app.lantern.ui.theme.Px.Small)
            .background(if (own) c.glow.copy(alpha = 0.18f) else if (selected) Color.Transparent else Color.White.copy(alpha = 0.06f))
            .then(if (own) Modifier.border(2.dp, c.glow, app.lantern.ui.theme.Px.Small) else Modifier)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text, style = if (small) Ba.type.caption else Ba.type.label, color = ink)
    }
}

/** A − / + stepper. The label rolls up on + and down on − (S4); [label] gives it for a value. */
@Composable
fun Stepper(value: Int, range: IntRange, label: @Composable (Int) -> String, onChange: (Int) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepButton(BaIcons.Minus, stringResource(R.string.cd_less), enabled = value > range.first) { onChange(value - 1) }
        AnimatedContent(
            value,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(240)) { if (up) it else -it } + fadeIn(tween(240)))
                    .togetherWith(slideOutVertically(tween(200)) { if (up) -it else it } + fadeOut(tween(200)))
                    .using(SizeTransform(clip = true))
            },
            label = "stepper",
        ) { n ->
            // A minimum width keeps the buttons in place while the label grows («—» → «۱۵ ثانیه»).
            Text(label(n), style = Ba.type.subtitle, color = c.text1, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 72.dp))
        }
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
