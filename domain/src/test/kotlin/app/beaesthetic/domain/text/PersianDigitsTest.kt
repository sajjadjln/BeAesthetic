package app.beaesthetic.domain.text

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PersianDigitsTest {
    @Test
    fun `formats and normalizes`() {
        assertEquals("هفته ۵ از ۲۳", PersianDigits.format("هفته 5 از 23"))
        assertEquals("۱۴۰۵", PersianDigits.format(1405))
        assertEquals("12.5", PersianDigits.normalize("۱۲.٥"))
    }
}
