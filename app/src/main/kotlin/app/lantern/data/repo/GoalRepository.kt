package app.lantern.data.repo

import androidx.room.withTransaction
import app.lantern.data.db.AppDatabase
import app.lantern.data.db.GoalEntity
import app.lantern.data.db.GoalMonthEntity
import app.lantern.data.db.RepeatType
import app.lantern.data.db.TaskEntity
import app.lantern.domain.goals.GoalAction
import app.lantern.domain.goals.GoalDraft
import app.lantern.domain.goals.GoalRules
import app.lantern.domain.goals.GoalState
import app.lantern.domain.goals.GoalWeek
import app.lantern.domain.tasks.TaskIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

/** One goal as the wall and the detail draw it. */
data class GoalView(
    val goal: GoalEntity,
    val months: List<GoalMonthEntity>,
    val actions: List<TaskEntity>,
    val week: GoalWeek?,
    val lastTick: LocalDate?,
) {
    val id: Long get() = goal.id
    val state: GoalState get() = runCatching { GoalState.valueOf(goal.state) }.getOrDefault(GoalState.IDEA)
}

/** Goals (D221): the flow writes a goal, its months and its weekly actions (tasks with a goalId) in one go. */
class GoalRepository(private val db: AppDatabase, private val tasks: TaskRepository) {

    fun observe(today: LocalDate): Flow<List<GoalView>> = combine(
        db.goals().observeAll(),
        db.goals().observeAllMonths(),
        db.goals().observeActions(),
        db.goals().observeTicks(today.minusDays(7)),
        db.goals().observeLastTicks(),
    ) { goals, months, actions, ticks, last ->
        val monthsBy = months.groupBy { it.goalId }
        val actionsBy = actions.groupBy { it.goalId }
        val ticksBy = ticks.groupBy({ it.goalId }, { it.date })
        val lastBy = last.associate { it.goalId to it.date }
        goals.map { g ->
            val acts = actionsBy[g.id].orEmpty()
            val week = if (g.state == GoalState.ACTIVE.name) {
                GoalRules.week(today, acts.map { GoalAction(it.id, it.weekdays) }, ticksBy[g.id].orEmpty(), g.flexDays)
            } else null
            GoalView(g, monthsBy[g.id].orEmpty(), acts, week, lastBy[g.id])
        }
    }

    /** «فعلاً فقط یه ایده» (step ①): a slip under the wall with just the title. */
    suspend fun saveIdea(title: String, ideaId: Long?, today: LocalDate, now: Instant): Long {
        if (ideaId != null) {
            db.goals().get(ideaId)?.let { db.goals().update(it.copy(title = title.trim())); return ideaId }
        }
        return db.goals().insert(GoalEntity(title = title.trim(), state = GoalState.IDEA.name, startDate = today, sortOrder = db.goals().maxSort() + 1, createdAt = now))
    }

    /** «بچسبون به دیوار» (step ⑥). An idea that went through the flow becomes this goal. */
    suspend fun pin(draft: GoalDraft, ideaId: Long?, today: LocalDate, now: Instant): Long {
        require(draft.ready(today))
        val id = db.withTransaction {
            val row = GoalEntity(
                title = draft.title.trim(),
                state = GoalState.ACTIVE.name,
                startDate = today,
                endDate = draft.endDate,
                triedBefore = draft.triedBefore,
                minutesPerWeek = if (draft.triedBefore == true) draft.minutesPerWeek else null,
                triedWhat = draft.triedWhat.trim().takeIf { draft.triedBefore == true },
                stopReason = draft.storedReason.takeIf { draft.triedBefore == true },
                flexDays = draft.flexDays,
                why = draft.why.trim(),
                whyAt = now,
                sortOrder = db.goals().maxSort() + 1,
                createdAt = now,
            )
            val existing = ideaId?.let { db.goals().get(it) }
            val goalId = if (existing != null) {
                db.goals().update(row.copy(id = existing.id, createdAt = existing.createdAt)); existing.id
            } else db.goals().insert(row)
            db.goals().upsertMonths(draft.months.mapIndexed { i, m -> GoalMonthEntity(goalId, i, m.trim().ifBlank { null }) })
            draft.actions.forEachIndexed { i, a ->
                db.tasks().insert(
                    TaskEntity(
                        title = a.title.trim(), icon = TaskIcon.CHECK.name, part = a.part, repeat = RepeatType.WEEKDAYS,
                        weekdays = a.weekdays, startDate = today, sortOrder = 1000 + i, createdAt = now, goalId = goalId,
                    ),
                )
            }
            goalId
        }
        tasks.writeSnapshot(today)
        return id
    }

    suspend fun setMilestone(goalId: Long, monthIndex: Int, text: String) =
        db.goals().upsertMonths(listOf(GoalMonthEntity(goalId, monthIndex, text.trim().ifBlank { null })))

    /** «رسیدم!»: onto the shelf; its actions leave Today, their ticks stay. */
    suspend fun reach(goalId: Long, today: LocalDate, now: Instant) = close(goalId, today, now) { it.copy(state = GoalState.REACHED.name, reachedAt = now) }

    /** «بذارش کنار»: back to the ideas, never "failed". The plan rows stay, so it can come back. */
    suspend fun setAside(goalId: Long, today: LocalDate, now: Instant) = close(goalId, today, now) { it.copy(state = GoalState.IDEA.name, setAsideAt = now) }

    private suspend fun close(goalId: Long, today: LocalDate, now: Instant, change: (GoalEntity) -> GoalEntity) {
        db.withTransaction {
            val g = db.goals().get(goalId) ?: return@withTransaction
            db.goals().update(change(g))
            db.goals().archiveActions(goalId, now)
        }
        tasks.writeSnapshot(today)
    }

    /** The companion offered this goal's why today (D221: once per quiet spell). */
    suspend fun markQuietOffered(goalId: Long, today: LocalDate) {
        db.goals().get(goalId)?.let { db.goals().update(it.copy(quietOfferedOn = today)) }
    }

    /** The first goal gone quiet, if any. */
    suspend fun quietGoal(today: LocalDate): GoalView? =
        observe(today).first().firstOrNull { it.state == GoalState.ACTIVE && GoalRules.isQuiet(it.goal.startDate, it.lastTick, it.goal.quietOfferedOn, today) }

    suspend fun get(goalId: Long): GoalEntity? = db.goals().get(goalId)
}
