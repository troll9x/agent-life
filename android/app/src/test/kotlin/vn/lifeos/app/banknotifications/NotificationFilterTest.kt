package vn.lifeos.app.banknotifications

import kotlin.test.*
import org.junit.Test

class NotificationFilterTest {
    @Test fun spoofedPackageAndGroupSummaryNeverReadContent() {
        val content = { error("Unselected content must not be accessed") }
        assertNull(NotificationFilter.filter("fake.bank", setOf("fixture.bank"), false, "key", 1, content))
        assertNull(NotificationFilter.filter("fixture.bank", setOf("fixture.bank"), true, "key", 1, content))
    }
    @Test fun otpIsDroppedBeforeQueueEvenWithBankLikeText() {
        listOf("OTP 123456", "Mã xác thực 123456", "MÃ XÁC THỰC 123456", "Ma xac thuc 123456",
            "Your one-time code is 123456", "Verification code: 123456; credited 50000 VND").forEach { text ->
            assertNull(NotificationFilter.filter("fixture.bank", setOf("fixture.bank"), false, "key", 1) { "Fixture" to text })
        }
    }
    @Test fun hiddenBodyRemainsMissingAndContainsNoPlaceholderOrAmountGuess() {
        listOf(null, "", "Content hidden", "Nội dung đã ẩn").forEach { text ->
            val row = NotificationFilter.filter("fixture.bank", setOf("fixture.bank"), false, "sensitive-key", 1) { "Fixture bank" to text }!!
            assertTrue(row.contentUnavailable)
            assertNull(row.body); assertNull(row.title)
            assertEquals(64, row.keyHash.length)
            assertFalse(row.keyHash.contains("sensitive-key"))
        }
    }
    @Test fun boundsAndIdentityAreStableWithoutParsingMoney() {
        fun input(key: String) = NotificationFilter.filter("fixture.bank", setOf("fixture.bank"), false, key, 1) {
            "T".repeat(500) to "Fixture credited 50000 VND ".repeat(500)
        }!!
        val a = input("key")
        assertEquals(256, a.title!!.length); assertTrue(a.body!!.length <= 4096)
        assertFalse(a.contentUnavailable)
        assertEquals(a, input("key")); assertNotEquals(a.keyHash, input("other").keyHash)
    }
}
