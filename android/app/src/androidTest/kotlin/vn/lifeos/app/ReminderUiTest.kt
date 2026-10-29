package vn.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneId
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.planner.PlannerScreen
import vn.lifeos.app.planner.PlannerUiState
import vn.lifeos.app.reminders.ReminderStatusPanel
import vn.lifeos.app.reminders.ReminderAlarms
import vn.lifeos.app.reminders.notificationAllowed
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.PlannedTask
import vn.lifeos.domain.planner.TaskState

@RunWith(AndroidJUnit4::class)
class ReminderUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun deniedNotificationsShowLimitation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(!notificationAllowed(context))
        composeRule.setContent { ReminderStatusPanel(false) {} }
        composeRule.onNodeWithText("Thông báo đang tắt", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Mở cài đặt thông báo").assertIsDisplayed()
    }

    @Test fun deniedExactAlarmAccessShowsInexactLimitation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(!ReminderAlarms(context).exactAllowed())
        composeRule.setContent { ReminderStatusPanel(false) {} }
        composeRule.onNodeWithText("không cam kết đúng phút", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Mở quyền báo thức").assertIsDisplayed()
    }

    @Test fun reminderLinkIdentifiesTaskInPlanner() {
        val id = Uuid.parse("00000000-0000-4000-8000-000000000993")
        val task = PlannedTask(id, id, null, "Fixture linked task", null, TaskState.OPEN, 3,
            Instant.parse("2026-10-28T02:00:00Z"), null, null, null, 1)
        composeRule.setContent { PlannerScreen(PlannerUiState(loading = false, tasks = listOf(task)),
            Instant.parse("2026-10-27T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"), null, "TASK", id.value) }
        composeRule.onNodeWithTag("reminder_target").assertIsDisplayed()
        composeRule.onNodeWithText("Fixture linked task").assertIsDisplayed()
    }
}
