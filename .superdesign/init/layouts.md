# Layouts

Stack: native Android, Kotlin, **Jetpack Compose** (Material 3 base, custom `Ba` theme), Navigation Compose with type-safe routes. No web framework, no Tailwind, no CSS. The HTML mockups in `docs/design/*.html` mirror the Compose UI with CSS tokens and are the design reference.

## App shell
`MainActivity` hosts a `NavHost`. Tab screens (Today, Dashboard) are wrapped by `WithTabs`, which puts `BottomBar` under the screen. Today is the root; other tabs sit one step above it. Settings, Tasks, Plan, Backup, Workout etc. are full screens with a `TopBar` (back arrow + title).

## Bottom bar
### `app/src/main/kotlin/app/beaesthetic/ui/components/BottomBar.kt`

```kotlin
package app.beaesthetic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.beaesthetic.R
import app.beaesthetic.ui.theme.Ba

/** The bottom tabs (DESIGN: icons only, RTL from the right). Only built tabs show (D123); Progress and Week join later. */
enum class Tab(val icon: ImageVector, val label: Int) {
    TODAY(BaIcons.CheckCircle, R.string.tab_today),
    DASHBOARD(BaIcons.Grid, R.string.tab_dashboard),
}

@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Ba.colors
    Column(modifier.fillMaxWidth().background(c.surface).navigationBarsPadding()) {
        HorizontalDivider(color = c.border)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().height(60.dp).selectableGroup(),
        ) {
            Tab.entries.forEach { tab ->
                val on = tab == selected
                val label = stringResource(tab.label)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .selectable(on, role = Role.Tab) { onSelect(tab) }
                        .semantics { contentDescription = label },
                ) {
                    Icon(tab.icon, null, tint = if (on) c.text1 else c.text3, modifier = Modifier.size(24.dp))
                    // The active tab's small accent dot.
                    Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(if (on) c.accent else c.surface))
                }
            }
        }
    }
}
```

## Shell wrapper
### `app/src/main/kotlin/app/beaesthetic/MainActivity.kt` (lines 200–220)

```kotlin
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
                }
            })
        }
    }

    /** A tapped notification opens Today on its part of the day, or the phone screen for a health warning. */
```

## Today header (screen-level, not shared)
### `app/src/main/kotlin/app/beaesthetic/ui/today/TodayScreen.kt` (lines 258–290)

```kotlin
@Composable
private fun Header(state: TodayUiState, actions: TodayActions) {
    val colors = Ba.colors
    val weekdays = stringArrayResource(R.array.weekdays)
    val months = stringArrayResource(R.array.jalali_months)
    val dayMonth = jalaliDayMonth(state.date)
    val week = PersianDigits.format(state.week)
    val total = PersianDigits.format(state.totalWeeks)
    val tail = state.headerTail
    val line = when {
        state.week == 0 -> stringResource(R.string.today_line_day0, dayMonth)
        tail is HeaderTail.Counting -> {
            val c = tail.countdown
            val text = when {
                c.isFinishDay && c.toNowruz -> stringResource(R.string.countdown_finish_nowruz)
                c.isFinishDay -> stringResource(R.string.countdown_finish)
                c.toNowruz -> stringResource(R.string.countdown_to_nowruz, PersianDigits.format(c.daysLeft))
                else -> stringResource(R.string.countdown_to_finish, PersianDigits.format(c.daysLeft))
            }
            stringResource(R.string.today_line_countdown, dayMonth, week, total, text)
        }
        tail is HeaderTail.InChapter -> stringResource(R.string.today_line_chapter, dayMonth, week, total, months[tail.monthIndex - 1])
        else -> stringResource(R.string.today_line_week, dayMonth, week, total)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(weekdays[Weekdays.index(state.date.dayOfWeek)], style = Ba.type.headline, color = colors.text1)
            Text(line, style = Ba.type.label, color = colors.text2)
        }
        RoundIconButton(BaIcons.Leaf, stringResource(R.string.cd_minimum_day), actions.toggleMinimum, selected = state.minimumMode)
        RoundIconButton(BaIcons.Gear, stringResource(R.string.cd_settings), actions.openSettings)
    }
}
```
