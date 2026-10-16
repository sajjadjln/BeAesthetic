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
import app.beaesthetic.ui.tasks.TasksRoute as TasksScreenRoute
import app.beaesthetic.ui.tasks.TasksViewModel
import app.beaesthetic.ui.components.BottomBar
import app.beaesthetic.ui.components.Tab
import app.beaesthetic.ui.dashboard.DashboardRoute
import app.beaesthetic.ui.dashboard.DashboardViewModel
import app.beaesthetic.ui.progress.CheckInRoute as CheckInScreenRoute
import app.beaesthetic.ui.progress.CheckInViewModel
import app.beaesthetic.ui.progress.MetricsRoute as MetricsScreenRoute
import app.beaesthetic.ui.progress.ProgressRoute
import app.beaesthetic.ui.progress.RevealRoute as RevealScreenRoute
import app.beaesthetic.ui.progress.ProgressViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.beaesthetic.ui.workout.PlanRoute as PlanScreenRoute
import app.beaesthetic.ui.backup.BackupRoute as BackupScreenRoute
import app.beaesthetic.ui.backup.BackupViewModel
import app.beaesthetic.ui.workout.PlanViewModel
import app.beaesthetic.ui.workout.SessionScreen
import app.beaesthetic.ui.workout.SessionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.beaesthetic.ui.theme.Accent
import app.beaesthetic.ui.theme.BaMotion
import app.beaesthetic.ui.theme.PeykarTheme
import app.beaesthetic.ui.theme.ThemeMode
import app.beaesthetic.ui.today.TodayRoute
import app.beaesthetic.ui.today.TodayViewModel
import app.beaesthetic.ui.week.WeekRoute
import app.beaesthetic.ui.recap.RecapRoute as RecapScreenRoute
import app.beaesthetic.ui.recap.RecapViewModel
import app.beaesthetic.ui.week.WeekViewModel
import app.beaesthetic.ui.week.linesRes
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
@Serializable object BackupRoute
@Serializable object DashboardTab
@Serializable object WorkoutRoute
@Serializable object ProgressTab
@Serializable object WeekTab
@Serializable object RecapRoute
@Serializable object MetricsRoute
@Serializable data class CheckInRoute(val idx: Int)
@Serializable data class RevealRoute(val idx: Int)
@Serializable data class BackfillRoute(val epochDay: Long)

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
                when (intent?.getStringExtra(EXTRA_OPEN)) {
                    OPEN_PHONE -> nav.navigate(PhoneRoute)
                    OPEN_WEEK -> nav.navigate(WeekTab) { popUpTo<HomeRoute>(); launchSingleTop = true }
                }
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
                        WithTabs(Tab.TODAY, nav) {
                            TodayRoute(
                                today,
                                onOpenSettings = { nav.navigate(SettingsRoute) },
                                onFixReminders = { nav.navigate(PhoneRoute) },
                                onWriteLetter = { first -> nav.navigate(LetterRoute(first)) },
                                onOpenWorkout = { nav.navigate(WorkoutRoute) },
                                onOpenPlan = { nav.navigate(PlanRoute) },
                                onOpenBackup = { nav.navigate(BackupRoute) },
                                onOpenCheckIn = { idx -> nav.navigate(CheckInRoute(idx)) },
                                onOpenWeek = { nav.navigate(WeekTab) { popUpTo<HomeRoute>(); launchSingleTop = true } },
                                onOpenRecap = { nav.navigate(RecapRoute) },
                            )
                        }
                    }
                    composable<DashboardTab> {
                        WithTabs(Tab.DASHBOARD, nav) { DashboardRoute(viewModel { DashboardViewModel(container) }) }
                    }
                    composable<ProgressTab> {
                        WithTabs(Tab.PROGRESS, nav) {
                            ProgressRoute(
                                viewModel { ProgressViewModel(container) },
                                container.photos,
                                onCheckIn = { idx -> nav.navigate(CheckInRoute(idx)) },
                                onManage = { nav.navigate(MetricsRoute) },
                                onBackfill = { d -> nav.navigate(BackfillRoute(d.toEpochDay())) },
                                onReveal = { idx -> nav.navigate(RevealRoute(idx)) },
                                onRecap = { nav.navigate(RecapRoute) },
                            )
                        }
                    }
                    composable<RecapRoute> {
                        RecapScreenRoute(
                            viewModel { RecapViewModel(container) },
                            viewModel { ProgressViewModel(container) },
                            viewModel { DashboardViewModel(container) },
                            container.photos,
                            onClose = nav::popBackStack,
                        )
                    }
                    composable<WeekTab> {
                        WithTabs(Tab.WEEK, nav) {
                            WeekRoute(viewModel { WeekViewModel(container) { win -> resources.getStringArray(win.linesRes()).size } })
                        }
                    }
                    composable<MetricsRoute> { MetricsScreenRoute(viewModel { ProgressViewModel(container) }, onBack = nav::popBackStack) }
                    composable<CheckInRoute> { entry ->
                        val idx = entry.toRoute<CheckInRoute>().idx
                        // One check-in per visit: a new view model each time the screen opens.
                        val checkIn: CheckInViewModel = viewModel(factory = viewModelFactory { initializer { CheckInViewModel(container, idx) } })
                        CheckInScreenRoute(checkIn, container.photos, onClose = nav::popBackStack)
                    }
                    composable<RevealRoute> { entry ->
                        RevealScreenRoute(viewModel { ProgressViewModel(container) }, container.photos, entry.toRoute<RevealRoute>().idx, onBack = nav::popBackStack)
                    }
                    composable<BackfillRoute> { entry ->
                        val day = java.time.LocalDate.ofEpochDay(entry.toRoute<BackfillRoute>().epochDay)
                        val session: SessionViewModel = viewModel(factory = viewModelFactory {
                            initializer { SessionViewModel(container, resources.getStringArray(R.array.buddy_workout_done).size, backfill = day) }
                        })
                        SessionScreen(session, onClose = nav::popBackStack, onOpenPlan = { nav.navigate(PlanRoute) })
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(container, DebugMenu.AVAILABLE, onBack = nav::popBackStack) { dest ->
                            when (dest) {
                                SettingsDestination.TASKS -> nav.navigate(TasksRoute)
                                SettingsDestination.REMINDERS -> nav.navigate(RemindersRoute)
                                SettingsDestination.PHONE -> nav.navigate(PhoneRoute)
                                SettingsDestination.APPEARANCE -> nav.navigate(AppearanceRoute)
                                SettingsDestination.WORKOUT -> nav.navigate(PlanRoute)
                                SettingsDestination.BACKUP -> nav.navigate(BackupRoute)
                                SettingsDestination.DEBUG -> nav.navigate(DebugRoute)
                                else -> Unit
                            }
                        }
                    }
                    composable<TasksRoute> { TasksScreenRoute(viewModel { TasksViewModel(container) }, onBack = nav::popBackStack) }
                    composable<RemindersRoute> { RemindersScreen(container, onBack = nav::popBackStack) }
                    composable<PhoneRoute> { PhoneScreen(container, onBack = nav::popBackStack) }
                    composable<AppearanceRoute> { AppearanceScreen(container, onBack = nav::popBackStack) }
                    composable<LetterRoute> { entry ->
                        LetterScreen(container, entry.toRoute<LetterRoute>().first, onDone = nav::popBackStack)
                    }
                    composable<BackupRoute> { BackupScreenRoute(viewModel { BackupViewModel(container) }, onBack = nav::popBackStack) }
                    composable<PlanRoute> { PlanScreenRoute(viewModel { PlanViewModel(container) }, onBack = nav::popBackStack) }
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

    /**
     * A tab screen with the bottom bar under it. The bar takes the navigation-bar inset, so the screen above
     * doesn't pad for it again. Tabs don't stack: Today is the root, the others sit one step above it.
     */
    @Composable
    private fun WithTabs(selected: Tab, nav: NavHostController, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) { content() }
            BottomBar(selected, onSelect = { tab ->
                if (tab == selected) return@BottomBar
                when (tab) {
                    Tab.TODAY -> nav.popBackStack(HomeRoute, inclusive = false)
                    Tab.DASHBOARD -> nav.navigate(DashboardTab) { popUpTo<HomeRoute>(); launchSingleTop = true }
                    Tab.PROGRESS -> nav.navigate(ProgressTab) { popUpTo<HomeRoute>(); launchSingleTop = true }
                    Tab.WEEK -> nav.navigate(WeekTab) { popUpTo<HomeRoute>(); launchSingleTop = true }
                }
            })
        }
    }

    /** A tapped notification opens Today on its part of the day, or the phone screen for a health warning. */
    private fun handleIntent(intent: Intent?, nav: NavHostController?) {
        intent ?: return
        intent.getStringExtra(EXTRA_PART)?.let { name -> runCatching { Part.valueOf(name) }.getOrNull()?.let(today::selectPart) }
        intent.getStringExtra(EXTRA_REF)?.let { key -> lifecycleScope.launch { container.events.notificationTapped(key) } }
        if (nav != null) {
            when {
                intent.getStringExtra(EXTRA_OPEN) == OPEN_PHONE -> nav.navigate(PhoneRoute)
                intent.getStringExtra(EXTRA_OPEN) == OPEN_WEEK -> nav.navigate(WeekTab) { popUpTo<HomeRoute>(); launchSingleTop = true }
                intent.hasExtra(EXTRA_PART) -> nav.popBackStack(HomeRoute, inclusive = false)
            }
        }
    }

    companion object {
        const val EXTRA_PART = "part"
        const val EXTRA_REF = "ref"
        const val EXTRA_OPEN = "open"
        const val OPEN_PHONE = "phone"
        const val OPEN_WEEK = "week"
    }
}
