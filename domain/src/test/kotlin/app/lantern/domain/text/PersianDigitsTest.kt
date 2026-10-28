package app.lantern.domain.text

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PersianDigitsTest {
    @Test
    fun `formats and normalizes`() {
        assertEquals("هفته ۵ از ۲۳", PersianDigits.format("هفته 5 از 23"))
        assertEquals("۱۴۰۵", PersianDigits.format(1405))
        assertEquals("12.5", PersianDigits.normalize("۱۲.٥"))
    }

    @Test
    fun `decimals use the Persian separator and drop a trailing zero`() {
        assertEquals("۸٫۵", PersianDigits.format(8.5))
        assertEquals("۱۵", PersianDigits.format(15.0))
        assertEquals("۰", PersianDigits.format(0.0))
    }

    @Test
    fun `thousands use the Persian separator`() {
        assertEquals("۱٬۰۰۰", PersianDigits.grouped(1_000))
        assertEquals("۱۰۰", PersianDigits.grouped(100))
    }
}
