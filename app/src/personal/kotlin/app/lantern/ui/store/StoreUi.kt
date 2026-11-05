package app.lantern.ui.store

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lantern.AppContainer

// The personal build is installed by hand, not from a store (D207): no update check, no rating ask.

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun SettingsStoreRow(container: AppContainer) = Unit

@Composable
internal fun AboutStoreRows() = Unit

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun TodayStore(container: AppContainer, quiet: Boolean, modifier: Modifier = Modifier) = Unit
