# Routes

Stack: native Android, Kotlin, **Jetpack Compose** (Material 3 base, custom `Ba` theme), Navigation Compose with type-safe routes. No web framework, no Tailwind, no CSS. The HTML mockups in `docs/design/*.html` mirror the Compose UI with CSS tokens and are the design reference.

Type-safe routes in `app/src/main/kotlin/app/beaesthetic/MainActivity.kt`; start destination `HomeRoute`.

| Route | Screen | Layout | What it renders |
|---|---|---|---|
| `HomeRoute` | `ui/today/TodayScreen.kt` (`TodayRoute`) | Tabs (Today active) | **Today / home**: header, day ring + week dots, «پیکر» row, today's workout card, part-of-day switcher, 2-column task tiles, «هر وقت» card, health and backup rows |
| `DashboardTab` | `ui/dashboard/DashboardScreen.kt` | Tabs | Whole-program grid of days, summary counts, week rows |
| `SettingsRoute` | `ui/settings/SettingsScreens.kt` | TopBar | Settings tiles (tasks, reminders, phone, appearance, plan, backup) |
| `TasksRoute` | `ui/tasks/TasksScreen.kt` | TopBar | Tasks list with part filter + editor sheet |
| `PlanRoute` / `WorkoutRoute` | workout plan editor / session | TopBar / full screen | Plan editing; logging sets |
| `BackupRoute`, `RemindersRoute`, `PhoneRoute`, `AppearanceRoute`, `LetterRoute` | settings sub-screens | TopBar | |

### `app/src/main/kotlin/app/beaesthetic/MainActivity.kt` (lines 73–86)

```kotlin
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

```

### `app/src/main/kotlin/app/beaesthetic/MainActivity.kt` (lines 139–198)

```kotlin
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
                            )
                        }
                    }
                    composable<DashboardTab> {
                        WithTabs(Tab.DASHBOARD, nav) { DashboardRoute(viewModel { DashboardViewModel(container) }) }
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
```
