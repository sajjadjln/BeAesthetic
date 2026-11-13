package app.lantern.ui.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.AppContainer
import app.lantern.domain.time.ProgramCalendar

/** The Goals tab: the wall over the goals in the database (D222). */
@Composable
fun GoalsRoute(
    container: AppContainer,
    onNew: () -> Unit,
    onOpenIdea: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onHold: (Long) -> Unit,
) {
    val today = remember { container.clock.programDate(ProgramCalendar.DEFAULT_ROLLOVER_HOUR) }
    val goals by remember(today) { container.goals.observe(today) }.collectAsStateWithLifecycle<List<app.lantern.data.repo.GoalView>?>(null)
    // The companion's one offer for a quiet goal (D223): looked up once per visit, marked offered as it shows.
    var offerId by rememberSaveable { mutableLongStateOf(-1L) }
    var looked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(today) {
        if (looked) return@LaunchedEffect
        looked = true
        container.goals.quietGoal(today)?.let { offerId = it.id; container.goals.markQuietOffered(it.id, today) }
    }
    val list = goals ?: return
    val offer = list.firstOrNull { it.id == offerId }
    GoalsWall(
        list, today, onNew, onOpenIdea, onOpenGoal, onHold,
        offer = offer,
        onOfferRead = { offerId = -1L; offer?.let { onHold(it.id) } },
        onOfferDismiss = { offerId = -1L },
    )
}
