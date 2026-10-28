package app.lantern.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.lantern.R
import app.lantern.ui.theme.Ba

/** A 24-hour time picker in the app's colors. [minuteOfDay] is the starting time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(minuteOfDay: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Ba.colors
    val state = rememberTimePickerState(initialHour = minuteOfDay / 60, initialMinute = minuteOfDay % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok), color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = c.text2) } },
        text = {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.surface2, selectorColor = c.accent, timeSelectorSelectedContainerColor = c.accentSubtle,
                    timeSelectorSelectedContentColor = c.onAccentSubtle, timeSelectorUnselectedContainerColor = c.surface2,
                ),
            )
        },
        containerColor = c.surface,
    )
}
