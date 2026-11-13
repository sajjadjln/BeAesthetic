package app.lantern.ui.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lantern.AppContainer
import app.lantern.domain.time.ProgramCalendar

/** The Goals tab: the wall over the goals in the database (D220). */
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
    val list = goals ?: return
    GoalsWall(list, today, onNew, onOpenIdea, onOpenGoal, onHold)
}
