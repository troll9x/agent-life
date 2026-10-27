package vn.lifeos.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.profile.LocalProfileSnapshot
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Profile

@RunWith(AndroidJUnit4::class)
class ShellStatesTest {
    @get:Rule val composeRule = createComposeRule()
    private val id = Uuid.parse("00000000-0000-4000-8000-000000000011")
    private val snapshot = LocalProfileSnapshot(Profile(id, id, "Fixture User", "Asia/Ho_Chi_Minh", "VND", 1, 1), emptyList())

    @Test fun offlineKeepsLocalNavigationAndDarkModeAvailable() {
        composeRule.setContent {
            var dark by remember { mutableStateOf(false) }
            AgentLifeTheme(darkTheme = dark) {
                AgentLifeScreen(ShellUiState(StartupStatus.Ready, darkTheme = dark, online = false, profile = ProfileStatus.Ready(snapshot)), {}, { dark = it })
            }
        }
        composeRule.onNodeWithText("Đang ngoại tuyến").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_more").performClick()
        composeRule.onNodeWithTag("more_settings").performClick()
        composeRule.onNodeWithText("Chế độ tối").assertIsDisplayed()
        composeRule.onNodeWithTag("dark_theme").assertIsOff()
        composeRule.onNodeWithTag("dark_theme").performClick()
        composeRule.onNodeWithTag("dark_theme").assertIsOn()
    }
}
