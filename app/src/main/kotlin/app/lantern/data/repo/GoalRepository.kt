package app.lantern.data.repo

import androidx.room.withTransaction
import app.lantern.data.db.AppDatabase
import app.lantern.data.db.GoalEntity
import app.lantern.data.db.GoalMilestoneEntity
import app.lantern.data.db.RepeatType
import app.lantern.data.db.TaskEntity
import app.lantern.domain.goals.GoalDraft
import app.lantern.domain.goals.GoalPlans
import app.lantern.domain.goals.GoalStatus
import java.time.Instant
import java.time.LocalDate

class GoalRepository(private val db: AppDatabase, private val tasks: TaskRepository) {
    /** The note, its plan and all Today actions commit together; a failed pin leaves no partial goal. */
    suspend fun pin(d: GoalDraft, today: LocalDate, now: Instant): Long = db.withTransaction {
        require(GoalPlans.valid(d))
        db.goals().findCreation(d.creationKey)?.let { return@withTransaction it.id }
        val range = GoalPlans.range(d.actions, d.flexDays)
        val id = db.goals().insert(GoalEntity(
            creationKey = d.creationKey, title = d.title.trim(), why = d.why.trim(), startDate = d.start, finishDate = d.finish,
            triedBefore = d.triedBefore == true, baselineMinutes = if (d.triedBefore == true) d.baselineMinutes else 0,
            triedWhat = if (d.triedBefore == true) d.triedWhat.trim() else "",
            obstacle = if (d.triedBefore == true) d.obstacle.trim() else "",
            flexDays = d.flexDays, weeklyLow = range.first, weeklyHigh = range.last, createdAt = now,
        ))
        db.goals().insertMilestones(GoalPlans.months(d.start, d.finish, d.calendar).map { month ->
            GoalMilestoneEntity(goalId = id, startDate = month.start, endDate = month.end, statement = d.milestones[month.start].orEmpty().trim())
        })
        d.actions.forEachIndexed { index, action ->
            db.tasks().insert(TaskEntity(
                title = action.title.trim(), icon = "CHECK", part = action.part, repeat = RepeatType.WEEKDAYS,
                weekdays = action.weekdays, startDate = d.start, endDate = d.finish,
                anchor = action.cue.trim().ifBlank { null }, goalId = id, sortOrder = index, createdAt = now,
            ))
        }
        tasks.writeSnapshot(today)
        id
    }

    /** Setting aside and achieving hide linked tasks and alarms while keeping all history. */
    suspend fun setStatus(id: Long, status: GoalStatus, today: LocalDate, now: Instant) = db.withTransaction {
        checkNotNull(db.goals().get(id))
        db.goals().setStatus(id, status.name, if (status == GoalStatus.ACHIEVED) now else null)
        tasks.writeSnapshot(today)
    }
}
