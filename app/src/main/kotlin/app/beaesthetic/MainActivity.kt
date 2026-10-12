package app.beaesthetic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.beaesthetic.debug.DebugMenu
import app.beaesthetic.domain.time.Part
import app.beaesthetic.ui.letters.LetterScreen
import app.beaesthetic.ui.linesRes
import app.beaesthetic.ui.settings.AppearanceScreen
import app.beaesthetic.ui.settings.PhoneScreen
import app.beaesthetic.ui.settings.RemindersScreen
import app.beaesthetic.ui.settings.SettingsDestination
import app.beaesthetic.ui.settings.SettingsScreen
import app.beaesthetic.ui.tasks.TasksScreen
import app.beaesthetic.ui.workout.PlanScreen
import app.beaesthetic.ui.workout.SessionScreen
import app.beaesthetic.ui.workout.SessionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.beaesthetic.ui.theme.Accent
import app.beaesthetic.ui.theme.BaMotion
import app.beaesthetic.ui.theme.PeykarTheme
import app.beaesthetic.ui.theme.ThemeMode
import app.beaesthetic.ui.today.TodayRoute
import app.beaesthetic.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Type-safe routes (ARCHITECTURE §2). */
@Serializable object HomeRoute
@Serializable object SettingsRoute
@Serializable object TasksRoute
@Serializable object RemindersRoute
@Serializable object PhoneRoute
@Serializable object AppearanceRoute
@Serializable object DebugRoute
@Serializable data class LetterRoute(val first: Boolean)
@Serializable object PlanRoute
@Serializable object WorkoutRoute

class MainActivity : ComponentActivity() {

    private val container get() = (application as App).container

    private val today: TodayViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as App
                TodayViewModel(app.container) { moment -> app.resources.getStringArray(moment.linesRes()).size }
            }
        }
    }

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(PersianLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent, null)
        // Onboarding (slice 9) will ask with an explanation; until then, ask on first start.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by container.db.settings().observe().collectAsStateWithLifecycle(null)
            val accent = settings?.accentColor?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.GREEN
            val mode = settings?.themeMode?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
            // Status and navigation bar icons follow the app's theme, not only the phone's.
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            val nav = rememberNavController()
            DisposableEffect(nav) {
                if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_PHONE) nav.navigate(PhoneRoute)
                val listener = Consumer<Intent> { handleIntent(it, nav) }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            PeykarTheme(accent, mode) {
                NavHost(
                    nav,
                    startDestination = HomeRoute,
                    // Fade-through between screens (D76); "Remove animations" makes it instant.
                    enterTransition = { fadeIn(tween(BaMotion.MEDIUM)) },
                    exitTransition = { fadeOut(tween(BaMotion.SHORT)) },
                ) {
                    composable<HomeRoute> {
                        TodayRoute(
                            today,
                            onOpenSettings = { nav.navigate(SettingsRoute) },
                            onFixReminders = { nav.navigate(PhoneRoute) },
                            onWriteLetter = { first -> nav.navigate(LetterRoute(first)) },
                            onOpenWorkout = { nav.navigate(WorkoutRoute) },
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(container, DebugMenu.AVAILABLE, onBack = nav::popBackStack) { dest ->
                            when (dest) {
                                SettingsDestination.TASKS -> nav.navigate(TasksRoute)
                                SettingsDestination.REMINDERS -> nav.navigate(RemindersRoute)
                                SettingsDestination.PHONE -> nav.navigate(PhoneRoute)
                                SettingsDestination.APPEARANCE -> nav.navigate(AppearanceRoute)
                                SettingsDestination.WORKOUT -> nav.navigate(PlanRoute)
                                SettingsDestination.DEBUG -> nav.navigate(DebugRoute)
                                else -> Unit
                            }
                        }
                    }
                    composable<TasksRoute> { TasksScreen(container, onBack = nav::popBackStack) }
                    composable<RemindersRoute> { RemindersScreen(container, onBack = nav::popBackStack) }
                    composable<PhoneRoute> { PhoneScreen(container, onBack = nav::popBackStack) }
                    composable<AppearanceRoute> { AppearanceScreen(container, onBack = nav::popBackStack) }
                    composable<LetterRoute> { entry ->
                        LetterScreen(container, entry.toRoute<LetterRoute>().first, onDone = nav::popBackStack)
                    }
                    composable<PlanRoute> { PlanScreen(container, onBack = nav::popBackStack) }
                    composable<WorkoutRoute> {
                        // One session per visit: a new view model each time the screen opens.
                        val session: SessionViewModel = viewModel(factory = viewModelFactory {
                            initializer { SessionViewModel(container, resources.getStringArray(R.array.buddy_workout_done).size) }
                        })
                        SessionScreen(
                            session,
                            onClose = nav::popBackStack,
                            onOpenPlan = { nav.navigate(PlanRoute) { popUpTo<HomeRoute>() } },
                        )
                    }
                    composable<DebugRoute> { DebugMenu.Screen(container, onClose = nav::popBackStack) }
                }
            }
        }
    }

    /** A tapped notification opens Today on its part of the day, or the phone screen for a health warning. */
    private fun handleIntent(intent: Intent?, nav: NavHostController?) {
        intent ?: return
        intent.getStringExtra(EXTRA_PART)?.let { name -> runCatching { Part.valueOf(name) }.getOrNull()?.let(today::selectPart) }
        intent.getStringExtra(EXTRA_REF)?.let { key -> lifecycleScope.launch { container.events.notificationTapped(key) } }
        if (nav != null) {
            if (intent.getStringExtra(EXTRA_OPEN) == OPEN_PHONE) nav.navigate(PhoneRoute) else if (intent.hasExtra(EXTRA_PART)) nav.popBackStack(HomeRoute, inclusive = false)
        }
    }

    companion object {
        const val EXTRA_PART = "part"
        const val EXTRA_REF = "ref"
        const val EXTRA_OPEN = "open"
        const val OPEN_PHONE = "phone"
    }
}
