package vn.lifeos.app

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.onboarding.NotificationPermissionContent
import vn.lifeos.app.onboarding.NotificationStartPrompt
import vn.lifeos.app.onboarding.notificationPermissionGranted
import vn.lifeos.app.onboarding.OnboardingScreen
import vn.lifeos.data.profile.ProfileInput

@RunWith(AndroidJUnit4::class)
class OnboardingPermissionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun denialRemovesRequestActionInsteadOfPromptingAgain() {
        var requests = 0
        composeRule.setContent {
            var denied by remember { mutableStateOf(false) }
            NotificationPermissionContent(granted = false, denied = denied) {
                requests++
                denied = true
            }
        }
        composeRule.onNodeWithText("Bật thông báo").performClick()
        assertEquals(1, requests)
        composeRule.onNodeWithText("Đã từ chối.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Bật thông báo").assertDoesNotExist()
        composeRule.waitForIdle()
        assertEquals(1, requests)
    }

    @Test fun localProfileCanFinishWithoutAnyPermissionRequest() {
        var saved: ProfileInput? = null
        composeRule.setContent { OnboardingScreen(saving = false, failed = false) { saved = it } }
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Fixture User")
        composeRule.onNodeWithText("Hoàn tất").performClick()
        assertEquals(ProfileInput("Fixture User", "Asia/Ho_Chi_Minh", "VND", 1), saved)
    }

    @Test fun firstUseOffersNotificationPermissionOnceAndCanBeDeferred() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted(context))
        val preferences = context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            composeRule.setContent { NotificationStartPrompt() }
            composeRule.onNodeWithText("Cho phép Agent Life gửi thông báo", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText("Để sau").performClick()
            composeRule.onNodeWithText("Bật thông báo").assertDoesNotExist()
            assertTrue(preferences.getBoolean("start_prompt_handled", false))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test fun enablingAtFirstUseOpensAndroidPermissionDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted(context))
        val preferences = context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            composeRule.setContent { NotificationStartPrompt() }
            composeRule.onNodeWithText("Bật thông báo").performClick()
            composeRule.waitUntil(10_000) {
                instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("permissioncontroller") == true
            }
            assertTrue(preferences.getBoolean("request_attempted", false))
        } finally {
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            preferences.edit().clear().commit()
        }
    }
}
