package vn.lifeos.app

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private fun awaitShell() {
        composeRule.waitUntil(45_000) {
            composeRule.onAllNodesWithText("Bắt đầu với Agent Life").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Hôm nay", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Công việc").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText("Bắt đầu với Agent Life").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Fixture User")
            composeRule.onNodeWithText("Hoàn tất").performClick()
        }
        composeRule.waitUntil(45_000) {
            composeRule.onAllNodesWithText("Hôm nay", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Công việc").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText("Để sau").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Để sau").performClick()
        }
    }

    @Test fun opensAllAreasAndBackReturnsToPreviousArea() {
        awaitShell()
        listOf("Công việc", "Tài chính", "Sức khỏe", "Bản tin", "Trợ lý", "Cài đặt").forEach { area ->
            val key = when (area) {
                "Công việc" -> "work"
                "Tài chính" -> "finance"
                "Sức khỏe" -> "health"
                "Bản tin" -> "news"
                "Trợ lý" -> "assistant"
                else -> "settings"
            }
            if (key in listOf("health", "news", "settings")) {
                composeRule.onNodeWithTag("nav_more").performClick()
                composeRule.onNodeWithTag("more_$key").performClick()
            } else composeRule.onNodeWithTag("nav_$key").performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText(area).onFirst().assertIsDisplayed()
            if (area == "Công việc") {
                composeRule.waitUntil(45_000) { composeRule.onAllNodesWithText("Thêm việc").fetchSemanticsNodes().isNotEmpty() }
                composeRule.onNodeWithText("Thêm việc").assertIsDisplayed()
            }
            else if (area == "Tài chính") {
                composeRule.waitUntil(45_000) {
                    composeRule.onAllNodesWithText("Tài khoản và số dư").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.onNodeWithText("Tài khoản và số dư").assertIsDisplayed()
            } else if (area != "Cài đặt") composeRule.onNodeWithText("Chưa triển khai").assertIsDisplayed()
        }
        Espresso.pressBack()
        composeRule.onAllNodesWithText("Trợ lý").onFirst().assertIsDisplayed()
    }

    @Test fun routeSurvivesRotation() {
        awaitShell()
        composeRule.onNodeWithTag("nav_finance").performClick()
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Tài chính").onFirst().assertIsDisplayed()
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

}
