package app.beaesthetic.ui.theme

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The one vibration helper (D76): light for a tick, medium for a 100% day or a round number, the record
 * pattern only for «رکورد». Nothing vibrates for an untick or anything about a missed day.
 */
class Haptics(private val view: View) {

    fun light() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        view.performHapticFeedback(constant)
    }

    fun medium() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator().vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun record() {
        vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 30), -1))
    }

    private fun vibrator(): Vibrator = view.context.getSystemService(Vibrator::class.java)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
