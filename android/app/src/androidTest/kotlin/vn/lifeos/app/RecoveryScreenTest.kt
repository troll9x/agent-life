package vn.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.security.DatabaseAccess

@RunWith(AndroidJUnit4::class)
class RecoveryScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun keyLossShowsRecoveryGuidance() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent { DatabaseAccessScreen(DatabaseAccess.RecoveryRequired) }
        }
        composeRule.onNodeWithText("Không thể mở dữ liệu đã lưu.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("khôi phục từ bản sao lưu đã xuất", substring = true).assertIsDisplayed()
    }
}
