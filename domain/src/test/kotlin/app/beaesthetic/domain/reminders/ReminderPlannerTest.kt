package app.beaesthetic.domain.reminders

import app.beaesthetic.domain.tasks.DueRules
import app.beaesthetic.domain.tasks.Repeat
import app.beaesthetic.domain.tasks.Task
import app.beaesthetic.domain.tasks.TaskAlarm
import app.beaesthetic.domain.tasks.TaskIcon
import app.beaesthetic.domain.time.Weekdays
import java.time.DayOfWeek
import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Profile
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderPlannerTest {

    private val zone = ZoneId.of("Asia/Tehran")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant()

    private fun slot(profile: Profile, part: Part, h: Int, m: Int = 0, enabled: Boolean = true) =
        ReminderSlot(profile, part, h * 60 + m, enabled)

    private val slots = listOf(
        slot(Profile.NORMAL, Part.MORNING, 7),
        slot(Profile.NORMAL, Part.MIDDAY, 13, 30),
        slot(Profile.NORMAL, Part.EVENING, 19),
        slot(Profile.NORMAL, Part.NIGHT, 23),
        slot(Profile.FRIDAY, Part.MORNING, 7),
        slot(Profile.FRIDAY, Part.MIDDAY, 13, 30),
        slot(Profile.FRIDAY, Part.EVENING, 19, enabled = false),
        slot(Profile.FRIDAY, Part.NIGHT, 23),
    )

    @Test
    fun `plans the next 48 hours and nothing in the past`() {
        val now = at(2026, 10, 14, 12, 0) // Wednesday noon
        val plan = ReminderPlanner.plan(now, zone, slots, 4, lastOpenedAt = now)
        assertTrue(plan.all { it.at > now && it.at <= now + Duration.ofHours(48) })
        assertEquals(
            listOf(
                "block:2026-10-14:MIDDAY", "block:2026-10-14:EVENING", "block:2026-10-14:NIGHT",
                "block:2026-10-15:MORNING", "block:2026-10-15:MIDDAY", "block:2026-10-15:EVENING", "block:2026-10-15:NIGHT",
                "block:2026-10-16:MORNING",
            ),
            plan.map { it.key },
        )
    }

    @Test
    fun `friday has no evening block`() {
        val now = at(2026, 10, 16, 5, 0) // Friday early morning
        val friday = ReminderPlanner.plan(now, zone, slots, 4, now).filter { it.programDate == LocalDate.of(2026, 10, 16) }
        assertEquals(listOf(Part.MORNING, Part.MIDDAY, Part.NIGHT), friday.map { it.part })
    }

    @Test
    fun `a slot after midnight belongs to the evening before`() {
        val late = slots.map { if (it.part == Part.NIGHT) it.copy(minuteOfDay = 30) else it } // 00:30
        val now = at(2026, 10, 14, 20, 0)
        val night = ReminderPlanner.plan(now, zone, late, 4, now).first { it.part == Part.NIGHT }
        assertEquals("block:2026-10-14:NIGHT", night.key)
        assertEquals(at(2026, 10, 15, 0, 30), night.at)
    }

    @Test
    fun `after three days away only morning blocks are planned`() {
        val now = at(2026, 10, 14, 12, 0)
        val plan = ReminderPlanner.plan(now, zone, slots, 4, lastOpenedAt = now - Duration.ofDays(4))
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.all { it.part == Part.MORNING })
    }

    @Test
    fun `keys are stable across re-plans`() {
        val a = ReminderPlanner.plan(at(2026, 10, 14, 12, 0), zone, slots, 4, null)
        val b = ReminderPlanner.plan(at(2026, 10, 14, 12, 5), zone, slots, 4, null)
        assertEquals(a.map { it.key to it.at }.take(5), b.map { it.key to it.at }.take(5))
    }

    @Test
    fun `current part of the day`() {
        fun part(h: Int, m: Int = 0, day: Int = 14) = PartOfDay.current(LocalDateTime.of(2026, 10, day, h, m), slots, 4)
        assertEquals(Part.MORNING, part(5))
        assertEquals(Part.MORNING, part(13, 29))
        assertEquals(Part.MIDDAY, part(13, 30))
        assertEquals(Part.EVENING, part(19))
        assertEquals(Part.NIGHT, part(23))
        assertEquals(Part.NIGHT, part(2, day = 15)) // still the 14th's night
        // Friday: Midday runs until Night.
        assertEquals(Part.MIDDAY, part(20, day = 16))
    }

    @Test
    fun `block content lists pending tasks with anchors and skips silenced ones`() {
        val date = LocalDate.of(2026, 10, 14)
        fun t(id: Long, part: Part, anchor: String? = null, silenced: Boolean = false, repeat: Repeat = Repeat.Daily, floor: Boolean = false) =
            Task(id, "t$id", TaskIcon.DROP, part, repeat, date, inMinimumDay = floor, anchor = anchor, remindersSilenced = silenced)
        val tasks = listOf(
            t(1, Part.MORNING, anchor = "بعد از مسواک"),
            t(2, Part.MORNING, floor = true),
            t(3, Part.MORNING, silenced = true),
            t(4, Part.MORNING, repeat = Repeat.MinDayOnly, floor = true),
            t(5, Part.NIGHT),
        )
        val items = DueRules.itemsFor(date, tasks, mapOf(2L to listOf(date)))
        assertEquals(listOf(BlockLine("t1", "بعد از مسواک", 1)), BlockContent.lines(Part.MORNING, items, minimumMode = false))
        assertEquals(listOf(BlockLine("t4", null, 4)), BlockContent.lines(Part.MORNING, items, minimumMode = true))
        val allDone = DueRules.itemsFor(date, tasks, listOf(1L, 2L).associateWith { listOf(date) })
        assertTrue(BlockContent.lines(Part.MORNING, allDone, minimumMode = false).isEmpty())
    }

    private fun task(id: Long, part: Part, important: Boolean = false, alarm: TaskAlarm? = null, silenced: Boolean = false) =
        Task(id, "t$id", TaskIcon.DROP, part, Repeat.Daily, LocalDate.of(2026, 10, 10), isImportant = important, alarm = alarm, remindersSilenced = silenced)

    @Test
    fun `follow-ups only for blocks with important tasks`() {
        val now = at(2026, 10, 14, 12, 0)
        val tasks = listOf(task(1, Part.NIGHT, important = true), task(2, Part.EVENING), task(3, Part.MIDDAY, important = true, silenced = true))
        val plan = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now, followUpMinutes = 45, tasks = tasks))
        val followUps = plan.filter { it.kind == AlarmKind.FOLLOW_UP }
        assertEquals(listOf("follow:2026-10-14:NIGHT", "follow:2026-10-15:NIGHT"), followUps.map { it.key })
        assertEquals(at(2026, 10, 14, 23, 45), followUps.first().at)
    }

    @Test
    fun `no follow-ups while away`() {
        val now = at(2026, 10, 14, 5, 0)
        val tasks = listOf(task(1, Part.MORNING, important = true))
        val plan = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now - Duration.ofDays(5), tasks = tasks))
        assertTrue(plan.none { it.kind == AlarmKind.FOLLOW_UP })
    }

    @Test
    fun `task alarms ring on their weekdays`() {
        val now = at(2026, 10, 14, 12, 0) // Wednesday
        val alarm = TaskAlarm(14 * 60, Weekdays.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), enabled = true)
        val tasks = listOf(task(7, Part.MIDDAY, alarm = alarm), task(8, Part.MIDDAY, alarm = alarm.copy(enabled = false)))
        val plan = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now, tasks = tasks)).filter { it.kind == AlarmKind.TASK }
        assertEquals(listOf("task:7:2026-10-15"), plan.map { it.key }) // Friday 14:00 is past the 48 h horizon
        assertEquals(7L, plan.first().taskId)
        assertEquals(at(2026, 10, 15, 14, 0), plan.first().at)
    }

    @Test
    fun `a one-off rings on its day only, whatever the weekdays say (D130)`() {
        val now = at(2026, 10, 14, 12, 0) // Wednesday
        val alarm = TaskAlarm(18 * 60, Weekdays.ALL, enabled = true)
        val once = task(7, Part.EVENING, alarm = alarm).copy(repeat = Repeat.Once(LocalDate.of(2026, 10, 15)))
        val plan = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now, tasks = listOf(once))).filter { it.kind == AlarmKind.TASK }
        assertEquals(listOf("task:7:2026-10-15"), plan.map { it.key })
    }

    @Test
    fun `every N days rings on the days it shows, never before (D130)`() {
        val now = at(2026, 10, 14, 12, 0) // Wednesday
        val alarm = TaskAlarm(20 * 60, 0, enabled = true) // no weekdays at all: they're ignored
        val nails = task(7, Part.EVENING, alarm = alarm).copy(repeat = Repeat.EveryNDays(3))
        // Done on Tuesday 13 Oct: due again on Friday 16 Oct, past the horizon.
        val done = mapOf(7L to listOf(LocalDate.of(2026, 10, 13)))
        val notYet = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now, tasks = listOf(nails), completions = done))
        assertTrue(notYet.none { it.kind == AlarmKind.TASK })
        // Done on Sunday 11 Oct: due since Wednesday, so it rings today and tomorrow.
        val due = mapOf(7L to listOf(LocalDate.of(2026, 10, 11)))
        val plan = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now, tasks = listOf(nails), completions = due))
        assertEquals(listOf("task:7:2026-10-14", "task:7:2026-10-15"), plan.filter { it.kind == AlarmKind.TASK }.map { it.key })
    }

    @Test
    fun `an anytime task never rings and makes no follow-up (D127)`() {
        val now = at(2026, 10, 14, 5, 0)
        val alarm = TaskAlarm(9 * 60, Weekdays.ALL, enabled = true)
        val item = task(7, Part.MORNING, important = true, alarm = alarm).copy(repeat = Repeat.Anytime)
        val plan = ReminderPlanner.plan(PlannerInput(now, zone, slots, 4, now, tasks = listOf(item)))
        assertTrue(plan.none { it.kind == AlarmKind.TASK || it.kind == AlarmKind.FOLLOW_UP })
    }

    @Test
    fun `the ritual rings on Friday at its time until that week's review is done`() {
        val now = at(2026, 10, 16, 5, 0) // Friday early morning
        val input = PlannerInput(now, zone, slots, 4, now, ritualMinute = 19 * 60)
        val ritual = ReminderPlanner.plan(input).filter { it.kind == AlarmKind.RITUAL }
        assertEquals(listOf("ritual:2026-10-16"), ritual.map { it.key })
        assertEquals(at(2026, 10, 16, 19, 0), ritual.single().at)
        val done = input.copy(ritualsDone = setOf(LocalDate.of(2026, 10, 10)))
        assertTrue(ReminderPlanner.plan(done).none { it.kind == AlarmKind.RITUAL })
        assertTrue(ReminderPlanner.plan(input.copy(ritualMinute = null)).none { it.kind == AlarmKind.RITUAL })
    }
}
