package app.lantern.domain.reminders

/**
 * Which maker's phone-setup steps to show (B6). Each of these has its own battery and autostart screens;
 * everything else gets Android's own screens only.
 */
enum class PhoneBrand {
    XIAOMI, SAMSUNG, HUAWEI, OTHER;

    /** Has a separate "start when the phone turns on" switch that no public API reads (Xiaomi, Huawei/Honor). */
    val hasAutostart: Boolean get() = this == XIAOMI || this == HUAWEI

    /** A "lock in recent apps" that keeps the app alive after «پاک کردن همه». Only taught for Xiaomi so far. */
    val hasRecentsLock: Boolean get() = this == XIAOMI

    companion object {
        /** From `Build.MANUFACTURER` and `Build.BRAND`. Honor counts as Huawei: same screens, renamed package. */
        fun of(manufacturer: String?, brand: String?): PhoneBrand {
            val names = listOfNotNull(manufacturer, brand).map { it.trim().lowercase() }
            return when {
                names.any { it in setOf("xiaomi", "redmi", "poco") } -> XIAOMI
                names.any { it == "samsung" } -> SAMSUNG
                names.any { it == "huawei" || it == "honor" } -> HUAWEI
                else -> OTHER
            }
        }
    }
}
