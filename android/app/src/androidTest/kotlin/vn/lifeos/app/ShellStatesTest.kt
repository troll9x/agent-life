package vn.lifeos.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellStatesTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun offlineKeepsLocalNavigationAndDarkModeAvailable() {
        composeRule.setContent {
            var dark by remember { mutableStateOf(false) }
            AgentLifeTheme(darkTheme = dark) {
                AgentLifeScreen(ShellUiState(StartupStatus.Ready, darkTheme = dark, online = false), {}, { dark = it })
            }
        }
        composeRule.onNodeWithText("Đang ngoại tuyến").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mở menu").performClick()
        composeRule.onNodeWithText("Cài đặt").performClick()
        composeRule.onNodeWithText("Chế độ tối").assertIsDisplayed()
        composeRule.onNodeWithTag("dark_theme").assertIsOff()
        composeRule.onNodeWithTag("dark_theme").performClick()
        composeRule.onNodeWithTag("dark_theme").assertIsOn()
    }
}
