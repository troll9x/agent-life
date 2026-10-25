package vn.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun recoveryGuidanceHasRetryAction() {
        var retries = 0
        composeRule.setContent {
            AgentLifeTheme(false) {
                AgentLifeScreen(ShellUiState(StartupStatus.Recovery), { retries++ }, {})
            }
        }
        composeRule.onNodeWithText("Không thể mở dữ liệu đã lưu").assertIsDisplayed()
        composeRule.onNodeWithText("khôi phục từ bản sao lưu đã xuất", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Thử lại").performClick()
        assertEquals(1, retries)
    }
}
