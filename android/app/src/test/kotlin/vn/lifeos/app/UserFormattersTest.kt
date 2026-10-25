package vn.lifeos.app

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserFormattersTest {
    @Test fun appliesProfileZoneAndCurrencyWithoutFloatingPoint() {
        val formatter = UserFormatters("Asia/Ho_Chi_Minh", "VND")
        assertEquals("01/01/2026 07:30", formatter.dateTime(Instant.parse("2026-01-01T00:30:00Z")))
        val amount = formatter.money(1_234_567L)
        assertTrue(amount.contains("1.234.567"), amount)
        assertTrue(amount.contains("₫") || amount.contains("VND"), amount)
    }
}
