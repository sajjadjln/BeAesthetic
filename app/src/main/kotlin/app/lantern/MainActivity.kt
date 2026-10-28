package app.lantern

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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.lantern.debug.DebugMenu
import app.lantern.domain.time.Part
import app.lantern.ui.settings.AppearanceScreen
import app.lantern.ui.settings.PhoneScreen
import app.lantern.ui.settings.RemindersScreen
import app.lantern.ui.settings.SettingsDestination
import app.lantern.ui.settings.SettingsScreen
import app.lantern.ui.tasks.TasksRoute as TasksScreenRoute
import app.lantern.ui.tasks.TasksViewModel
import app.lantern.ui.components.BOTTOM_BAR_HEIGHT
import app.lantern.ui.components.BottomBar
import app.lantern.ui.components.Tab
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lantern.ui.backup.BackupRoute as BackupScreenRoute
import app.lantern.ui.backup.BackupViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lantern.ui.theme.Accent
import app.lantern.ui.theme.BaMotion
import app.lantern.ui.theme.LanternTheme
import app.lantern.ui.theme.ThemeMode
import app.lantern.ui.today.TodayRoute
import app.lantern.ui.today.TodayViewModel
import app.lantern.ui.goals.GoalsPlaceholder
import app.lantern.ui.onboarding.OnboardingScreen
import androidx.compose.runtime.remember
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
@Serializable object BackupRoute
@Serializable object GoalsTab
@Serializable object OnboardingRoute

class MainActivity : ComponentActivity() {

    private val container get() = (application as App).container

    private val today: TodayViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as App
                TodayViewModel(app.container)
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
        // The first run seeds the settings row the start route waits for (onboarding has no Today view model).
        lifecycleScope.launch { container.ensureReady() }

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
            // Onboarding asks for notifications with the reason on screen (D142); an onboarded install that
            // lost the permission is asked again on start.
            val onboarded = settings?.onboardingDone ?: return@setContent
            LaunchedEffect(onboarded) {
                if (onboarded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val start: Any = remember { if (onboarded) HomeRoute else OnboardingRoute }
            val nav = rememberNavController()
            DisposableEffect(nav) {
                when (intent?.getStringExtra(EXTRA_OPEN)) {
                    OPEN_PHONE -> nav.navigate(PhoneRoute)
                }
                val listener = Consumer<Intent> { handleIntent(it, nav) }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            val current by nav.currentBackStackEntryAsState()
            val tab = current?.let(::tabOf)
            LanternTheme(accent, mode) {
              Box(Modifier.fillMaxSize()) {
                NavHost(
                    nav,
                    startDestination = start,
                    modifier = Modifier.fillMaxSize(),
                    // Screens slide (D147): forward comes in from the left in RTL and back slides it out again;
                    // tabs slide in their order under the fixed bar. "Remove animations" makes it instant.
                    enterTransition = { slideIntoContainer(direction(initialState, targetState, pop = false), slide) },
                    exitTransition = { slideOutOfContainer(direction(initialState, targetState, pop = false), slide, behind(initialState, targetState)) },
                    popEnterTransition = { slideIntoContainer(direction(initialState, targetState, pop = true), slide, behind(targetState, initialState)) },
                    popExitTransition = { slideOutOfContainer(direction(initialState, targetState, pop = true), slide) },
                ) {
                    composable<HomeRoute> {
                        WithTabs {
                            TodayRoute(
                                today,
                                onOpenSettings = { nav.navigate(SettingsRoute) },
                                onFixReminders = { nav.navigate(PhoneRoute) },
                                onOpenBackup = { nav.navigate(BackupRoute) },
                            )
                        }
                    }
                    composable<GoalsTab> { WithTabs { GoalsPlaceholder() } }
                    composable<OnboardingRoute> {
                        OnboardingScreen(
                            container,
                            onOpenReminders = { nav.navigate(RemindersRoute) },
                            onOpenPhone = { nav.navigate(PhoneRoute) },
                            onOpenBackup = { nav.navigate(BackupRoute) },
                            onFinished = { nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } } },
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(container, DebugMenu.AVAILABLE, onBack = nav::popBackStack) { dest ->
                            when (dest) {
                                SettingsDestination.TASKS -> nav.navigate(TasksRoute)
                                SettingsDestination.REMINDERS -> nav.navigate(RemindersRoute)
                                SettingsDestination.PHONE -> nav.navigate(PhoneRoute)
                                SettingsDestination.APPEARANCE -> nav.navigate(AppearanceRoute)
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
                    composable<BackupRoute> { BackupScreenRoute(viewModel { BackupViewModel(container) }, onBack = nav::popBackStack) }
                    composable<DebugRoute> { DebugMenu.Screen(container, onClose = nav::popBackStack) }
                }
                // One bar for every tab: it stays put while the tabs slide, and slides away under pushed screens.
                AnimatedVisibility(
                    visible = tab != null,
                    enter = slideInVertically(tween(BaMotion.MEDIUM, easing = BaMotion.Standard)) { it },
                    exit = slideOutVertically(tween(BaMotion.MEDIUM, easing = BaMotion.Standard)) { it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    var shown by remember { mutableStateOf(Tab.TODAY) }
                    if (tab != null) shown = tab
                    BottomBar(shown, onSelect = { selectTab(it, shown, nav) })
                }
              }
            }
        }
    }

    private val slide = tween<IntOffset>(BaMotion.MEDIUM, easing = BaMotion.Standard)

    /** Which tab a back stack entry is, or null for a pushed screen. */
    private fun tabOf(entry: NavBackStackEntry): Tab? = entry.destination.let { d ->
        when {
            d.hasRoute<HomeRoute>() -> Tab.TODAY
            d.hasRoute<GoalsTab>() -> Tab.GOALS
            else -> null
        }
    }

    /**
     * Forward slides toward the start (in RTL: in from the left), back toward the end. Between two tabs the
     * direction follows the bar's order, whichever way the back stack moved.
     */
    private fun direction(from: NavBackStackEntry, to: NavBackStackEntry, pop: Boolean): AnimatedContentTransitionScope.SlideDirection {
        val a = tabOf(from)
        val b = tabOf(to)
        val forward = if (a != null && b != null) b.ordinal > a.ordinal else !pop
        return if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
    }

    /** The screen left behind under a pushed one moves a quarter of the way; tabs move all the way. */
    private fun behind(under: NavBackStackEntry, over: NavBackStackEntry): (Int) -> Int =
        if (tabOf(under) != null && tabOf(over) != null) { w -> w } else { w -> w / 4 }

    private fun selectTab(tab: Tab, selected: Tab, nav: NavHostController) {
        if (tab == selected) return
        when (tab) {
            Tab.TODAY -> nav.popBackStack(HomeRoute, inclusive = false)
            Tab.GOALS -> nav.navigate(GoalsTab) { popUpTo<HomeRoute>(); launchSingleTop = true }
        }
    }

    /**
     * A tab screen. The bar is drawn over the bottom of the screen (outside the NavHost, so it doesn't slide
     * with the tab); the screen keeps clear of it and doesn't pad for the navigation bar again.
     */
    @Composable
    private fun WithTabs(content: @Composable () -> Unit) {
        Box(
            Modifier.fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = BOTTOM_BAR_HEIGHT)
                .consumeWindowInsets(WindowInsets.navigationBars),
        ) { content() }
    }

    /** A tapped notification opens Today on its part of the day, or the phone screen for a health warning. */
    private fun handleIntent(intent: Intent?, nav: NavHostController?) {
        intent ?: return
        intent.getStringExtra(EXTRA_PART)?.let { name -> runCatching { Part.valueOf(name) }.getOrNull()?.let(today::selectPart) }
        intent.getStringExtra(EXTRA_REF)?.let { key -> lifecycleScope.launch { container.events.notificationTapped(key) } }
        if (nav != null) {
            when {
                intent.getStringExtra(EXTRA_OPEN) == OPEN_PHONE -> nav.navigate(PhoneRoute)
                intent.hasExtra(EXTRA_PART) -> nav.popBackStack(HomeRoute, inclusive = false)
            }
        }
    }

    companion object {
        const val EXTRA_PART = "part"
        const val EXTRA_REF = "ref"
        const val EXTRA_OPEN = "open"
        const val OPEN_PHONE = "phone"
    }
}
