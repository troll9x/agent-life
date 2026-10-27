package vn.lifeos.app.onboarding

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationStartPromptTest {
    @Test fun promptsOnceOnlyWhenRuntimePermissionIsAvailableAndMissing() {
        assertTrue(notificationPromptNeeded(33, granted = false, handled = false, attempted = false))
        assertFalse(notificationPromptNeeded(32, granted = false, handled = false, attempted = false))
        assertFalse(notificationPromptNeeded(36, granted = true, handled = false, attempted = false))
        assertFalse(notificationPromptNeeded(36, granted = false, handled = true, attempted = false))
        assertFalse(notificationPromptNeeded(36, granted = false, handled = false, attempted = true))
    }
}
