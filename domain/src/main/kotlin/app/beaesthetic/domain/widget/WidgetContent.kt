package app.beaesthetic.domain.widget

import app.beaesthetic.domain.tasks.DayItem
import app.beaesthetic.domain.time.Part

/** A pending task on the widget: one tap ticks it. */
data class WidgetTask(val taskId: Long, val title: String, val important: Boolean)

/**
 * The home-screen widget (D67, D143): today's count, and what's left in the current part of the day, so it
 * answers "what do I do now?" without opening the app. Never lists what was missed in earlier parts.
 */
data class WidgetContent(
    val done: Int,
    val total: Int,
    /** The part [pending] belongs to: the current one, or the next later one with something open. */
    val part: Part,
    val pending: List<WidgetTask>,
    val more: Int,
    /** True when [part] is later than now: the current part is done, so the widget shows what comes next. */
    val upcoming: Boolean = false,
) {
    val dayComplete: Boolean get() = total > 0 && done == total
    val partComplete: Boolean get() = pending.isEmpty()

    companion object {
        const val MAX = 3

        /** Like Today's ring: the visible tasks, or only the floor on a minimum day. */
        fun of(items: List<DayItem>, minimumMode: Boolean, part: Part): WidgetContent {
            val visible = if (minimumMode) items.filter { it.inFloor } else items.filterNot { it.minDayOnly }
            // The current part first; when it's done, the next later part with something open. Never earlier ones.
            val shown = Part.entries.filter { it >= part }.firstOrNull { p -> visible.any { it.task.part == p && !it.done } } ?: part
            val pending = visible.filter { it.task.part == shown && !it.done }
                .sortedByDescending { it.task.isImportant }
            return WidgetContent(
                done = visible.count { it.done },
                total = visible.size,
                part = shown,
                pending = pending.take(MAX).map { WidgetTask(it.task.id, it.task.title, it.task.isImportant) },
                more = (pending.size - MAX).coerceAtLeast(0),
                upcoming = shown != part,
            )
        }
    }
}
