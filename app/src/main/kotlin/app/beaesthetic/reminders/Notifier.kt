package app.beaesthetic.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.beaesthetic.MainActivity
import app.beaesthetic.PersianLocale
import app.beaesthetic.R
import app.beaesthetic.domain.reminders.BlockLine
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Part
import java.time.LocalDate

/**
 * Posts notifications. Text is built from a Persian context, so notifications are Persian with Persian
 * digits even when the phone is set to another language (ARCHITECTURE §7.4). No character, no guilt
 * wording (SPEC §7.2).
 */
class Notifier(appContext: Context) {

    private val context = PersianLocale.wrap(appContext)
    private val manager = NotificationManagerCompat.from(appContext)

    /**
     * Reminder channels vibrate: some phones (Xiaomi among them) leave a channel silent on the wrist unless
     * vibration is set on it. A channel's settings are fixed once created, so changing them needs a new id;
     * the old ids are deleted.
     */
    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        OLD_CHANNELS.forEach(system::deleteNotificationChannel)
        fun reminder(id: String, name: Int, importance: Int) = NotificationChannel(id, context.getString(name), importance).apply {
            enableVibration(true)
            vibrationPattern = VIBRATION
        }
        system.createNotificationChannels(
            listOf(
                reminder(CHANNEL_BLOCKS, R.string.channel_blocks, NotificationManager.IMPORTANCE_DEFAULT),
                reminder(CHANNEL_FOLLOW_UPS, R.string.channel_follow_ups, NotificationManager.IMPORTANCE_HIGH),
                reminder(CHANNEL_TASK_ALARMS, R.string.channel_task_alarms, NotificationManager.IMPORTANCE_HIGH),
                reminder(CHANNEL_RITUAL, R.string.channel_ritual, NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_SYSTEM, context.getString(R.string.channel_system), NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }

    fun canNotify(): Boolean = manager.areNotificationsEnabled() &&
        manager.getNotificationChannel(CHANNEL_BLOCKS)?.importance != NotificationManager.IMPORTANCE_NONE

    /** One notification per block: the count, then each pending task with its «بعد از…» cue. */
    @Suppress("MissingPermission") // canNotify() is checked by the caller.
    fun showBlock(part: Part, lines: List<BlockLine>, key: String) {
        val title = context.getString(R.string.notif_block_title, partName(part), PersianDigits.format(lines.size))
        val texts = lines.map(::lineText)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        texts.forEach(style::addLine)
        val notification = base(CHANNEL_BLOCKS)
            .setContentTitle(title)
            .setContentText(texts.joinToString(context.getString(R.string.list_separator)))
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openToday(part, key))
            .build()
        manager.notify(blockNotificationId(part), notification)
    }

    /** The one follow-up for an important task that is still not done, with «انجام شد» (D3). */
    @Suppress("MissingPermission")
    fun showFollowUp(line: BlockLine, date: LocalDate, key: String) {
        val id = FOLLOW_UP_ID_BASE + line.taskId.toInt()
        val notification = base(CHANNEL_FOLLOW_UPS)
            .setContentTitle(line.title)
            .setContentText(context.getString(R.string.notif_follow_up_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openToday(null, key))
            .addAction(0, context.getString(R.string.notif_action_done), done(line.taskId, date, id))
            .build()
        manager.notify(id, notification)
    }

    /** A task's own alarm (D25): its name and anchor, «انجام شد» and «باز کن». */
    @Suppress("MissingPermission")
    fun showTaskAlarm(line: BlockLine, date: LocalDate, key: String) {
        val id = TASK_ID_BASE + line.taskId.toInt()
        val notification = base(CHANNEL_TASK_ALARMS)
            .setContentTitle(line.title)
            .setContentText(line.anchor)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openToday(null, key))
            .addAction(0, context.getString(R.string.notif_action_done), done(line.taskId, date, id))
            .build()
        manager.notify(id, notification)
    }

    /** The watchdog's quiet, once-a-day warning (ARCHITECTURE §7.5). */
    @Suppress("MissingPermission")
    fun showHealthWarning() {
        val intent = Intent(context, MainActivity::class.java)
            .setData("peykar://phone".toUri())
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_PHONE)
        val notification = base(CHANNEL_SYSTEM)
            .setContentTitle(context.getString(R.string.notif_health_title))
            .setContentText(context.getString(R.string.notif_health_text))
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        manager.notify(HEALTH_NOTIFICATION_ID, notification)
    }

    /** The Friday ritual (M8): one quiet line that opens the Week tab. No "you missed" wording, ever. */
    @Suppress("MissingPermission")
    fun showRitual(key: String) {
        val intent = Intent(context, MainActivity::class.java)
            .setData("peykar://week/$key".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_REF, key)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_WEEK)
        val notification = base(CHANNEL_RITUAL)
            .setContentTitle(context.getString(R.string.notif_ritual_title))
            .setContentText(context.getString(R.string.notif_ritual_body))
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        manager.notify(RITUAL_NOTIFICATION_ID, notification)
    }

    /** A block notification goes away once everything in it is done. */
    fun cancelBlock(part: Part) = manager.cancel(blockNotificationId(part))

    fun cancel(id: Int) = manager.cancel(id)

    /** The test goes through the Blocks channel, so it sounds and vibrates exactly like a real reminder. */
    @Suppress("MissingPermission")
    fun showTest(key: String) {
        val notification = base(CHANNEL_BLOCKS)
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(context.getString(R.string.notif_test_text))
            .setContentIntent(openToday(null, key))
            .build()
        manager.notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun base(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(ContextCompat.getColor(context, R.color.notification_accent))
        .setAutoCancel(true)

    private fun lineText(line: BlockLine): String =
        if (line.anchor.isNullOrBlank()) line.title else context.getString(R.string.notif_line_with_anchor, line.title, line.anchor)

    private fun partName(part: Part): String = context.getString(
        when (part) {
            Part.MORNING -> R.string.part_morning
            Part.MIDDAY -> R.string.part_midday
            Part.EVENING -> R.string.part_evening
            Part.NIGHT -> R.string.part_night
        },
    )

    private fun done(taskId: Long, date: LocalDate, notificationId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(NotificationActionReceiver.ACTION_DONE)
            .setData("peykar://done/$taskId/$date".toUri())
            .putExtra(NotificationActionReceiver.EXTRA_TASK, taskId)
            .putExtra(NotificationActionReceiver.EXTRA_DATE, date.toString())
            .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION, notificationId)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openToday(part: Part?, key: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData("peykar://notification/$key".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_REF, key)
        part?.let { intent.putExtra(MainActivity.EXTRA_PART, it.name) }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val CHANNEL_BLOCKS = "blocks_v2"
        const val CHANNEL_FOLLOW_UPS = "follow_ups_v2"
        const val CHANNEL_TASK_ALARMS = "task_alarms_v2"
        const val CHANNEL_RITUAL = "ritual_v2"
        const val CHANNEL_SYSTEM = "system"
        private val OLD_CHANNELS = listOf("blocks", "follow_ups", "task_alarms", "ritual")
        private val VIBRATION = longArrayOf(0, 250, 150, 250)

        private const val TEST_NOTIFICATION_ID = 900
        private const val HEALTH_NOTIFICATION_ID = 901
        private const val RITUAL_NOTIFICATION_ID = 902
        private const val FOLLOW_UP_ID_BASE = 10_000
        private const val TASK_ID_BASE = 20_000

        fun blockNotificationId(part: Part) = 100 + part.ordinal
    }
}
