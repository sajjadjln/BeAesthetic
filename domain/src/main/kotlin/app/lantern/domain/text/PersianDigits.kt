package app.lantern.domain.text

/**
 * Display digits: Persian in Persian, Latin in English (D174); input accepts Persian, Arabic-Indic and
 * Latin digits.
 */
object PersianDigits {
    /** Set from the app's language at start (English shows Latin digits). */
    @Volatile var latin: Boolean = false

    private const val PERSIAN = "۰۱۲۳۴۵۶۷۸۹"
    private const val ARABIC = "٠١٢٣٤٥٦٧٨٩"

    fun format(text: String): String = if (latin) text else buildString(text.length) {
        for (c in text) append(if (c in '0'..'9') PERSIAN[c - '0'] else c)
    }

    fun format(number: Int): String = format(number.toString())

    fun format(number: Long): String = format(number.toString())

    /** «۱٬۰۰۰»: thousands grouped with the Persian separator. */
    fun grouped(number: Int): String = "%,d".format(java.util.Locale.ROOT, number).let { if (latin) it else format(it.replace(',', '٬')) }

    /** «۸٫۵», «۱۵»: no trailing «٫۰», and the Persian decimal separator. */
    fun format(number: Double): String {
        val text = java.math.BigDecimal.valueOf(number).stripTrailingZeros().toPlainString()
        return if (latin) text else format(text.replace('.', '٫'))
    }

    /** Turns any supported digits into Latin digits, for parsing. */
    fun normalize(text: String): String = buildString(text.length) {
        for (c in text) {
            val p = PERSIAN.indexOf(c)
            val a = ARABIC.indexOf(c)
            append(
                when {
                    p >= 0 -> '0' + p
                    a >= 0 -> '0' + a
                    else -> c
                },
            )
        }
    }
}
