package app.beaesthetic.domain.tasks

import app.beaesthetic.domain.time.Part
import app.beaesthetic.domain.time.Weekdays
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskFormTest {

    private val today = LocalDate.of(2026, 10, 11) // Sunday
    private val start = LocalDate.of(2026, 10, 1)

    private fun task(
        id: Long,
        repeat: Repeat = Repeat.Daily,
        part: Part = Part.MORNING,
        sortOrder: Int = 0,
        anchor: String? = null,
        alarm: TaskAlarm? = null,
    ) = Task(id, "t$id", TaskIcon.DROP, part, repeat, start, anchor = anchor, alarm = alarm, sortOrder = sortOrder)

    @Test
    fun `a form round-trips every repeat kind`() {
        val repeats = listOf(
            Repeat.Daily,
            Repeat.OnWeekdays(Weekdays.of(DayOfWeek.SATURDAY, DayOfWeek.FRIDAY)),
            Repeat.EveryNDays(14),
            Repeat.Once(today.plusDays(3)),
        )
        for (r in repeats) {
            val original = task(7, r, sortOrder = 4, anchor = "با ناهار")
            val back = TaskForms.toTask(TaskForms.from(original), original, today, original.sortOrder)
            assertEquals(original, back)
        }
    }

    @Test
    fun `an edit keeps id, start date, action and silenced reminders`() {
        val original = task(3).copy(action = TaskAction.WORKOUT, remindersSilenced = true)
        val form = TaskForms.from(original).copy(title = "  تمرین  ", repeat = RepeatKind.EVERY_N_DAYS, everyNDays = 2)
        val edited = TaskForms.toTask(form, original, today, 0)
        assertEquals(3, edited.id)
        assertEquals(start, edited.startDate)
        assertEquals(TaskAction.WORKOUT, edited.action)
        assertTrue(edited.remindersSilenced)
        assertEquals("تمرین", edited.title)
        assertEquals(Repeat.EveryNDays(2), edited.repeat)
    }

    @Test
    fun `a new task starts today with no action`() {
        val t = TaskForms.toTask(TaskForms.blank(Part.NIGHT, today).copy(title = "نخ دندان"), null, today, 5)
        assertEquals(0, t.id)
        assertEquals(today, t.startDate)
        assertEquals(TaskAction.NONE, t.action)
        assertEquals(Part.NIGHT, t.part)
        assertNull(t.alarm)
        assertNull(t.anchor)
    }

    @Test
    fun `blank title, no weekdays and no alarm days are errors`() {
        val ok = TaskForms.blank(Part.MORNING, today).copy(title = "x")
        assertTrue(TaskForms.errors(ok, today).isEmpty())
        assertEquals(setOf(FormError.NO_TITLE), TaskForms.errors(ok.copy(title = "   "), today))
        assertEquals(setOf(FormError.NO_WEEKDAYS), TaskForms.errors(ok.copy(repeat = RepeatKind.WEEKDAYS, weekdays = 0), today))
        assertEquals(setOf(FormError.NO_ALARM_DAYS), TaskForms.errors(ok.copy(alarmOn = true, alarmWeekdays = 0), today))
        // Alarm days only matter while the alarm is on.
        assertTrue(TaskForms.errors(ok.copy(alarmOn = false, alarmWeekdays = 0), today).isEmpty())
    }

    @Test
    fun `a new one-off can't be in the past, an edited one may keep its past date`() {
        val form = TaskForms.blank(Part.MORNING, today).copy(title = "x", repeat = RepeatKind.ONCE)
        assertTrue(TaskForms.errors(form, today).isEmpty())
        assertEquals(setOf(FormError.BAD_DATE), TaskForms.errors(form.copy(onceDate = today.minusDays(1)), today))
        assertEquals(setOf(FormError.BAD_DATE), TaskForms.errors(form.copy(onceDate = null), today))

        val past = today.minusDays(2)
        val original = task(1, Repeat.Once(past))
        assertTrue(TaskForms.errors(TaskForms.from(original), today, original).isEmpty())
        assertEquals(setOf(FormError.BAD_DATE), TaskForms.errors(TaskForms.from(original).copy(onceDate = past.minusDays(1)), today, original))
    }

    @Test
    fun `every N is kept in range`() {
        val form = TaskForms.blank(Part.MORNING, today).copy(title = "x", repeat = RepeatKind.EVERY_N_DAYS, everyNDays = 500)
        assertEquals(Repeat.EveryNDays(TaskForms.MAX_EVERY_N), TaskForms.toTask(form, null, today, 0).repeat)
        assertEquals(Repeat.EveryNDays(TaskForms.MIN_EVERY_N), TaskForms.toTask(form.copy(everyNDays = 1), null, today, 0).repeat)
    }

    @Test
    fun `a minimum-day-only task is always in the floor`() {
        val form = TaskForms.blank(Part.EVENING, today).copy(title = "۱۰ شنا", repeat = RepeatKind.MIN_DAY_ONLY, inMinimumDay = false)
        assertTrue(TaskForms.toTask(form, null, today, 0).inMinimumDay)
    }

    @Test
    fun `switching the alarm off keeps its time and days`() {
        val original = task(1, alarm = TaskAlarm(14 * 60, Weekdays.of(DayOfWeek.MONDAY), enabled = true))
        val off = TaskForms.toTask(TaskForms.from(original).copy(alarmOn = false), original, today, 0)
        assertEquals(TaskAlarm(14 * 60, Weekdays.of(DayOfWeek.MONDAY), enabled = false), off.alarm)
        // A task that never had an alarm doesn't get one by being saved.
        assertNull(TaskForms.toTask(TaskForms.from(task(2)), task(2), today, 0).alarm)
    }

    @Test
    fun `alarm days default to the task's own days`() {
        val days = Weekdays.of(DayOfWeek.SATURDAY, DayOfWeek.WEDNESDAY)
        assertEquals(days, TaskForms.defaultAlarmDays(TaskForm(repeat = RepeatKind.WEEKDAYS, weekdays = days)))
        assertEquals(Weekdays.ALL, TaskForms.defaultAlarmDays(TaskForm(repeat = RepeatKind.EVERY_N_DAYS)))
    }

    @Test
    fun `new or moved tasks go to the end of their part`() {
        val tasks = listOf(task(1, sortOrder = 0), task(2, sortOrder = 5), task(3, part = Part.NIGHT, sortOrder = 9))
        assertEquals(6, TaskForms.sortOrderFor(TaskForm(part = Part.MORNING), null, tasks))
        assertEquals(0, TaskForms.sortOrderFor(TaskForm(part = Part.MIDDAY), null, tasks))
        // Edited in place: keeps its order.
        assertEquals(5, TaskForms.sortOrderFor(TaskForm(part = Part.MORNING), tasks[1], tasks))
        // Moved from Night to Morning: after the last morning task.
        assertEquals(6, TaskForms.sortOrderFor(TaskForm(part = Part.MORNING), tasks[2], tasks))
    }

    @Test
    fun `anchor suggestions are the most used ones, without the current`() {
        val tasks = listOf(
            task(1, anchor = "با ناهار"), task(2, anchor = "با ناهار"), task(3, anchor = "قبل از خواب"),
            task(4, anchor = " "), task(5),
        )
        assertEquals(listOf("با ناهار", "قبل از خواب"), TaskForms.anchorSuggestions(tasks, ""))
        assertEquals(listOf("قبل از خواب"), TaskForms.anchorSuggestions(tasks, "با ناهار"))
    }

    @Test
    fun `a one-off done before today leaves the list`() {
        val once = task(1, Repeat.Once(today.minusDays(1)))
        assertTrue(TaskForms.isListed(once, emptyList(), today))
        assertTrue(TaskForms.isListed(once, listOf(today), today))
        assertFalse(TaskForms.isListed(once, listOf(today.minusDays(1)), today))
        assertTrue(TaskForms.isListed(task(2), listOf(today.minusDays(1)), today))
    }
}
