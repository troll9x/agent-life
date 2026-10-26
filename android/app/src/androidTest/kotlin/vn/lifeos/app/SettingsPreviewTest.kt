package vn.lifeos.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.settings.ProfileSettingsScreen
import vn.lifeos.data.profile.LocalProfileSnapshot
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Profile

@RunWith(AndroidJUnit4::class)
class SettingsPreviewTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun changingStartDayUpdatesHalfOpenPeriodPreview() {
        val id = Uuid.parse("00000000-0000-4000-8000-000000000011")
        val profile = Profile(id, id, "Fixture User", "Asia/Ho_Chi_Minh", "VND", 1, 1)
        composeRule.setContent {
            ProfileSettingsScreen(LocalProfileSnapshot(profile, emptyList()), Instant.parse("2026-10-20T00:00:00Z"),
                saving = false, failed = false, darkTheme = false, onThemeChange = {}, onSave = {})
        }
        composeRule.onNodeWithTag("budget_day").performTextReplacement("15")
        composeRule.onNodeWithTag("period_preview").assertTextEquals("Từ 15/10/2026 đến trước 15/11/2026")
    }
}
