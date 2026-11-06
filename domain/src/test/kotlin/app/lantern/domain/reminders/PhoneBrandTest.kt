package app.lantern.domain.reminders

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneBrandTest {
    @Test
    fun `xiaomi, redmi and poco are xiaomi`() {
        assertEquals(PhoneBrand.XIAOMI, PhoneBrand.of("Xiaomi", "POCO"))
        assertEquals(PhoneBrand.XIAOMI, PhoneBrand.of("Xiaomi", "Redmi"))
        assertEquals(PhoneBrand.XIAOMI, PhoneBrand.of(null, "poco"))
    }

    @Test
    fun `samsung, huawei and honor`() {
        assertEquals(PhoneBrand.SAMSUNG, PhoneBrand.of("samsung", "samsung"))
        assertEquals(PhoneBrand.HUAWEI, PhoneBrand.of("HUAWEI", "HUAWEI"))
        assertEquals(PhoneBrand.HUAWEI, PhoneBrand.of("HONOR", "HONOR"))
    }

    @Test
    fun `anything else is other`() {
        assertEquals(PhoneBrand.OTHER, PhoneBrand.of("Google", "google"))
        assertEquals(PhoneBrand.OTHER, PhoneBrand.of(null, null))
    }

    @Test
    fun `autostart row for xiaomi and huawei only, recents lock for xiaomi only`() {
        assertTrue(PhoneBrand.XIAOMI.hasAutostart)
        assertTrue(PhoneBrand.HUAWEI.hasAutostart)
        assertFalse(PhoneBrand.SAMSUNG.hasAutostart)
        assertFalse(PhoneBrand.OTHER.hasAutostart)
        assertTrue(PhoneBrand.XIAOMI.hasRecentsLock)
        assertFalse(PhoneBrand.HUAWEI.hasRecentsLock)
    }
}
