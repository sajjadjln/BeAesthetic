package app.lantern.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import app.lantern.ui.theme.BaColors
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.lanternColors
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.lantern.App
import app.lantern.MainActivity
import app.lantern.AppLanguage
import app.lantern.R
import app.lantern.domain.reminders.PartOfDay
import app.lantern.domain.reminders.ReminderSlot
import app.lantern.domain.text.PersianDigits
import app.lantern.domain.time.Part
import app.lantern.domain.time.ProgramCalendar
import app.lantern.domain.widget.WidgetContent

/**
 * The Today widget (D67, D143): «امروز · ۵ از ۱۲», then up to three tasks still open in the current part of
 * the day, each with a circle that ticks it in one tap. Done means the accent, never red; earlier parts are
 * never listed. A tap anywhere else opens Today. Colors are the picked character's night theme (D187); done
 * stays green.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as App).container
        container.ensureReady()
        // Live: a tick anywhere, a new part of the day or the 04:00 rollover redraws the open session.
        val content = combine(
            container.db.settings().observe().filterNotNull(),
            container.db.slots().observeAll(),
            container.clock.minuteTicks(),
        ) { s, slots, local ->
            Triple(ProgramCalendar.programDate(local, s.rolloverHour), PartOfDay.current(local, slots.map { ReminderSlot(it.profile, it.part, it.time, it.enabled) }, s.rolloverHour), Unit)
        }.distinctUntilChanged().flatMapLatest { (today, part) ->
            container.tasks.observeDay(today).map { day -> WidgetContent.of(day.items, day.minimumMode, part) }
        }
        val res = AppLanguage.wrap(context).resources
        // The launcher lays the widget out in the phone's own direction. On an LTR phone the rows are built
        // mirrored, so the widget still reads right to left like the app (D87).
        // The app's own context is always Persian (RTL), so the system's configuration is the launcher's.
        val ltrPhone = android.content.res.Resources.getSystem().configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_LTR &&
            AppLanguage.locale(context).language == "fa"
        val colors = lanternColors(LanternCharacter.of(container.db.settings().get()?.character))
        provideContent {
            val w by content.collectAsState(initial = null)
            GlanceTheme { Body(w, res, ltrPhone, colors) }
        }
    }

    @Composable
    private fun Body(w: WidgetContent?, res: android.content.res.Resources, ltrPhone: Boolean, c: BaColors) {
        val start = if (ltrPhone) Alignment.End else Alignment.Start
        val bg = ColorProvider(day = c.surface, night = c.surface)
        val text1 = ColorProvider(day = c.text1, night = c.text1)
        val text2 = ColorProvider(day = c.text2, night = c.text2)
        val accent = ColorProvider(day = c.accent, night = c.accent)
        val ring = ColorProvider(day = c.text3, night = c.text3)
        val glow = ColorProvider(day = c.glow, night = c.glow)
        Column(
            horizontalAlignment = start,
            modifier = GlanceModifier.fillMaxSize().background(bg).cornerRadius(6.dp).padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (w == null) return@Column
            RtlRow(ltrPhone, GlanceModifier.fillMaxWidth()) {
                listOf(
                    { Text(RLM + res.getString(R.string.tab_today), style = TextStyle(color = glow, fontSize = 16.sp, fontWeight = FontWeight.Bold)) },
                    { Spacer(GlanceModifier.width(8.dp)) },
                    {
                        Text(
                            RLM + res.getString(R.string.widget_count, PersianDigits.format(w.done), PersianDigits.format(w.total)),
                            style = TextStyle(color = if (w.dayComplete) accent else text2, fontSize = 13.sp),
                        )
                    },
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            when {
                w.total == 0 -> Text(RLM + res.getString(R.string.widget_empty), style = TextStyle(color = text2, fontSize = 13.sp))
                w.dayComplete -> Text(RLM + res.getString(R.string.widget_day_done), style = TextStyle(color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                w.partComplete -> Text(RLM + res.getString(R.string.widget_part_done, partName(res, w.part)), style = TextStyle(color = accent, fontSize = 14.sp))
                else -> {
                    if (w.upcoming) Text(RLM + res.getString(R.string.widget_next_part, partName(res, w.part)), style = TextStyle(color = text2, fontSize = 12.sp))
                    w.pending.forEach { t ->
                        RtlRow(
                            ltrPhone,
                            GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable(actionRunCallback<TickAction>(actionParametersOf(TickAction.TASK to t.taskId))),
                        ) {
                            listOfNotNull(
                                {
                                    Box(GlanceModifier.size(20.dp).cornerRadius(10.dp).background(ring), contentAlignment = Alignment.Center) {
                                        Box(GlanceModifier.size(16.dp).cornerRadius(8.dp).background(bg)) {}
                                    }
                                },
                                { Spacer(GlanceModifier.width(10.dp)) },
                                { Text(RLM + t.title, style = TextStyle(color = text1, fontSize = 14.sp), maxLines = 1) },
                                if (t.important) ({ Spacer(GlanceModifier.width(6.dp)) }) else null,
                                if (t.important) ({ Text(RLM + res.getString(R.string.tag_important), style = TextStyle(color = ColorProvider(Color(0xFFE0A030), Color(0xFFE8B95A)), fontSize = 11.sp)) }) else null,
                            )
                        }
                    }
                    if (w.more > 0) Text(RLM + res.getString(R.string.widget_more, PersianDigits.format(w.more)), style = TextStyle(color = text2, fontSize = 12.sp))
                }
            }
        }
    }

    /** A row whose items are given right to left; mirrored on an LTR phone and pushed to the right edge. */
    @Composable
    private fun RtlRow(ltrPhone: Boolean, modifier: GlanceModifier, items: () -> List<@Composable () -> Unit>) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = if (ltrPhone) Alignment.End else Alignment.Start,
        ) {
            val list = items()
            (if (ltrPhone) list.reversed() else list).forEach { it() }
        }
    }

    private fun partName(res: android.content.res.Resources, part: Part) = res.getString(
        when (part) {
            Part.MORNING -> R.string.part_morning
            Part.MIDDAY -> R.string.part_midday
            Part.EVENING -> R.string.part_evening
            Part.NIGHT -> R.string.part_night
        },
    )

    companion object {
        /** A right-to-left mark: the launcher's TextView may be LTR, and «۰ از ۳» must still read right to left. */
        private const val RLM = "\u200F"

        /** Redraws every placed widget: after a tick anywhere, a block reminder, and each app open. */
        suspend fun refresh(context: Context) = runCatching { TodayWidget().updateAll(context) }
    }
}

/** A circle on the widget: ticks the task for today, like a tap on Today (one tap to log). */
class TickAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK] ?: return
        val container = (context.applicationContext as App).container
        container.ensureReady()
        val s = container.db.settings().get() ?: return
        val today = container.clock.programDate(s.rolloverHour)
        // Only ticks: an open task here is never done, so a stale widget can't untick anything.
        if (container.tasks.day(today).items.any { it.task.id == taskId && !it.done }) {
            container.tasks.complete(taskId, today, container.clock.now())
            container.events.firstTickIfNeeded(today, s.rolloverHour)
        }
        container.cancelEmptyBlocks(today)
    }

    companion object {
        val TASK = ActionParameters.Key<Long>("task")
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
