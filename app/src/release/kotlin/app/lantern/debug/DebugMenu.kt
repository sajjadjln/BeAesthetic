package app.lantern.debug

import androidx.compose.runtime.Composable
import app.lantern.AppContainer

/** Release builds have no debug menu. */
object DebugMenu {
    const val AVAILABLE = false

    @Composable
    @Suppress("UNUSED_PARAMETER")
    fun Screen(container: AppContainer, onClose: () -> Unit) = Unit
}
