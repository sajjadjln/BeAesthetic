# Pages

Stack: native Android, Kotlin, **Jetpack Compose** (Material 3 base, custom `Ba` theme), Navigation Compose with type-safe routes. No web framework, no Tailwind, no CSS. The HTML mockups in `docs/design/*.html` mirror the Compose UI with CSS tokens and are the design reference.

## HomeRoute (Today)
Entry: `app/src/main/kotlin/app/beaesthetic/ui/today/TodayScreen.kt` (867 lines; render tree `TodayScreen` at 175–256)
Dependencies:
- `app/src/main/kotlin/app/beaesthetic/ui/today/TodayViewModel.kt` (state: TodayUiState, tiles per part, cards, buddy, workout, anytime, banner, backup)
- `app/src/main/kotlin/app/beaesthetic/ui/components/Common.kt` (BaCard, Chip, PrimaryButton, SecondaryButton, RoundIconButton, DayRing, DayDot)
- `app/src/main/kotlin/app/beaesthetic/ui/components/BaIcons.kt`
- `app/src/main/kotlin/app/beaesthetic/ui/components/buddy/Piker.kt`
  - `app/src/main/kotlin/app/beaesthetic/ui/components/buddy/PikerRig.kt` (draws «پیکر»)
- `app/src/main/kotlin/app/beaesthetic/ui/tasks/TaskEditor.kt` (TaskEditorSheet)
  - `app/src/main/kotlin/app/beaesthetic/ui/components/Form.kt`
  - `app/src/main/kotlin/app/beaesthetic/ui/components/JalaliCalendar.kt`
- `app/src/main/kotlin/app/beaesthetic/ui/theme/Theme.kt`, `app/src/main/kotlin/app/beaesthetic/ui/theme/Colors.kt`, `app/src/main/kotlin/app/beaesthetic/ui/theme/Haptics.kt`
- Shell: `app/src/main/kotlin/app/beaesthetic/ui/components/BottomBar.kt` (via MainActivity.WithTabs)

Design references for Today: `docs/design/screens.html` (approved round 3), `docs/design/today-v3.html` (three directions), `docs/design/today-v4.html` (the mix, current exploration). Superdesign baseline: `.superdesign/replica_html_template/today.html` = today-v4 in its default state (08:10, «فهرست» + «شیت»), dark.

## DashboardTab
Entry: `app/src/main/kotlin/app/beaesthetic/ui/dashboard/DashboardScreen.kt`
- `app/src/main/kotlin/app/beaesthetic/ui/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/app/beaesthetic/ui/components/Common.kt`
- `app/src/main/kotlin/app/beaesthetic/ui/components/BottomBar.kt`
