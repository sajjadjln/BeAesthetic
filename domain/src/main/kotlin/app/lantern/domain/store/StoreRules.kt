package app.lantern.domain.store

import java.time.LocalDate

/** When to ask for a rating and when to show «تازه‌ها» (B4). Pure rules; the app keeps the state. */
object StoreRules {

    /** The 7th day with at least one tick is the first day a rating may be asked. */
    const val RATE_AFTER_TICK_DAYS = 7

    /**
     * Ask for a rating at most once ([answered] = «الان نه» or «دیگه نپرس» was ever tapped, or it was shown),
     * on a good day: today already has a tick, yesterday had one too (never right after a gap), and at
     * least [RATE_AFTER_TICK_DAYS] days so far had a tick, today included.
     */
    fun shouldAskRating(today: LocalDate, tickDays: Set<LocalDate>, answered: Boolean): Boolean {
        if (answered) return false
        if (today !in tickDays || today.minusDays(1) !in tickDays) return false
        return tickDays.count { !it.isAfter(today) } >= RATE_AFTER_TICK_DAYS
    }

    /**
     * Which version's «تازه‌ها» to show, or null. [lastSeen] is the version code the app last ran as (null if
     * never stored); [freshInstall] is true when the app was never updated. Never on a fresh install, once per
     * new version code.
     */
    fun whatsNewFor(current: Int, lastSeen: Int?, freshInstall: Boolean): Int? = when {
        lastSeen == null -> if (freshInstall) null else current
        current > lastSeen -> current
        else -> null
    }

    /** Bazaar 24.7.0. From here on Bazaar answers `getRemoteVersionCode`; older ones only `getVersionCode`. */
    const val BAZAAR_REMOTE_CHECK_SINCE = 2_400_700L

    /** Which update-check call the installed Bazaar ([bazaarVersionCode]) understands. Same rule as BazaarUpdater. */
    fun updateCall(bazaarVersionCode: Long): UpdateCall =
        if (bazaarVersionCode >= BAZAAR_REMOTE_CHECK_SINCE) UpdateCall.REMOTE_VERSION_CODE else UpdateCall.VERSION_CODE

    /**
     * Bazaar's [answer] as a newer version code, or null. Both calls use −1 (or 0 on an unknown call) for
     * "nothing" or an error; `getVersionCode` already answers only when newer, `getRemoteVersionCode` answers
     * the store's code, which may equal [installed].
     */
    fun newerVersion(answer: Long?, installed: Long): Long? = answer?.takeIf { it > 0 && it > installed }

    /**
     * Open Bazaar's in-app update sheet (`bazaar://details/modal`) rather than the app's page: only when a newer
     * version is known and Bazaar is new enough to have the sheet (24.7.0 is the oldest we trust with it).
     */
    fun useUpdateSheet(bazaarVersionCode: Long?, newer: Long?): Boolean =
        newer != null && bazaarVersionCode != null && bazaarVersionCode >= BAZAAR_REMOTE_CHECK_SINCE
}

/** The two methods of Bazaar's update-check service, in their binder order. */
enum class UpdateCall { VERSION_CODE, REMOTE_VERSION_CODE }
