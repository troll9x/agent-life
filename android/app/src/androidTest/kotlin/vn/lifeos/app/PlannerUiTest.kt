package vn.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.planner.PlannerDashboard
import vn.lifeos.app.planner.EventDraft
import vn.lifeos.app.planner.PlannerActions
import vn.lifeos.app.planner.PlannerScreen
import vn.lifeos.app.planner.PlannerUiState
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.CalendarEvent
import vn.lifeos.domain.planner.EventTimeKind
import vn.lifeos.domain.planner.PlannedTask
import vn.lifeos.domain.planner.TaskState
import vn.lifeos.domain.planner.TaskChecklistItem
import vn.lifeos.domain.planner.RecurrenceSpec
import vn.lifeos.domain.planner.Frequency
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PlannerUiTest {
    @get:Rule val composeRule = createComposeRule()
    private fun id(number: Int) = Uuid.parse("00000000-0000-4000-8000-${number.toString().padStart(12, '0')}")
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test fun dashboardShowsUndatedOpenTask() {
        val task = PlannedTask(id(2), id(1), null, "Fixture Task", null, TaskState.OPEN, 3, null, null, null, null, 1)
        composeRule.setContent { PlannerDashboard(PlannerUiState(loading = false, tasks = listOf(task)),
            Instant.parse("2026-10-19T02:00:00Z"), zone, onOpenWork = {}) }
        composeRule.onNodeWithText("Fixture Task", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("planner_dashboard_count").assertIsDisplayed()
    }

    @Test fun overlappingMeetingsShowConflictInCalendar() {
        fun event(number: Int, title: String, start: String, end: String) = CalendarEvent(id(number), id(1), null, null,
            title, EventTimeKind.TIMED, Instant.parse(start), Instant.parse(end), null, null, zone, null, null, false, 1)
        val events = listOf(
            event(3, "Fixture 09:00", "2026-10-19T02:00:00Z", "2026-10-19T03:00:00Z"),
            event(4, "Fixture 09:30", "2026-10-19T02:30:00Z", "2026-10-19T03:30:00Z"),
        )
        composeRule.setContent { PlannerScreen(PlannerUiState(loading = false, events = events),
            Instant.parse("2026-10-19T02:00:00Z"), zone, null) }
        composeRule.onNodeWithText("Lịch").performClick()
        assertEquals(2, composeRule.onAllNodesWithTag("calendar_conflict").fetchSemanticsNodes().size)
    }

    @Test fun taskEditorAndCompleteActionUpdateVisibleList() {
        var state by mutableStateOf(PlannerUiState(loading = false))
        val actions = object : StubPlannerActions() {
            override fun createTask(title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) {
                state = state.copy(tasks = listOf(PlannedTask(id(10), id(1), projectId, title, note,
                    TaskState.OPEN, priority, dueAt, null, recurrence, null, 1)), actionVersion = state.actionVersion + 1)
            }
            override fun setTaskState(task: PlannedTask, stateValue: TaskState) {
                state = state.copy(tasks = listOf(task.copy(state = stateValue, version = task.version + 1)),
                    actionVersion = state.actionVersion + 1)
            }
        }
        composeRule.setContent { PlannerScreen(state, Instant.parse("2026-10-19T02:00:00Z"), zone, actions) }
        composeRule.onNodeWithText("Thêm việc").performClick()
        composeRule.onNodeWithTag("task_title").performTextInput("Fixture Task")
        composeRule.onNodeWithText("Lưu").performClick()
        composeRule.onNodeWithText("Fixture Task").assertIsDisplayed()
        composeRule.onAllNodesWithText("Hoàn tất").onLast().performClick()
        composeRule.onNodeWithText("Không có việc trong bộ lọc này.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Hoàn tất").onFirst().performClick()
        composeRule.onNodeWithText("Fixture Task").assertIsDisplayed()
    }

    @Test fun monthlyEditorExplainsMissingDayThirtyOne() {
        composeRule.setContent { PlannerScreen(PlannerUiState(loading = false),
            Instant.parse("2026-10-19T02:00:00Z"), zone, StubPlannerActions()) }
        composeRule.onNodeWithText("Lịch").performClick()
        composeRule.onNodeWithText("Thêm lịch").performClick()
        composeRule.onNodeWithText("Lặp: Không").performClick()
        composeRule.onNodeWithText("Lặp: Hằng ngày").performClick()
        composeRule.onNodeWithText("Lặp: Hằng tuần").performClick()
        composeRule.onNodeWithText("Ngày 31:", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun editingOneOccurrenceDispatchesExceptionAction() {
        val day = LocalDate.parse("2026-10-19")
        val series = CalendarEvent(id(20), id(1), null, null, "Fixture Series", EventTimeKind.ALL_DAY,
            null, null, day, day.plusDays(1), zone, RecurrenceSpec(Frequency.DAILY, count = 2).encode(), null, false, 1)
        var selectedDate: LocalDate? = null
        val actions = object : StubPlannerActions() {
            override fun replaceOccurrence(event: CalendarEvent, originalDate: LocalDate, input: EventDraft) {
                selectedDate = originalDate
            }
        }
        composeRule.setContent { PlannerScreen(PlannerUiState(loading = false, events = listOf(series)),
            Instant.parse("2026-10-19T02:00:00Z"), zone, actions) }
        composeRule.onNodeWithText("Lịch").performClick()
        composeRule.onNodeWithText("Sửa").performClick()
        composeRule.onNodeWithText("Chỉ lần này").performClick()
        composeRule.onNodeWithTag("event_title").performTextReplacement("Changed once")
        composeRule.onNodeWithText("Lưu").performClick()
        assertEquals(day, selectedDate)
    }

    private open class StubPlannerActions : PlannerActions {
        override fun createProject(title: String) = Unit
        override fun createTask(title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) = Unit
        override fun updateTask(task: PlannedTask, title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) = Unit
        override fun setTaskState(task: PlannedTask, state: TaskState) = Unit
        override fun selectChecklist(taskId: Uuid?) = Unit
        override fun addChecklist(taskId: Uuid, title: String, order: Int) = Unit
        override fun setChecklistDone(item: TaskChecklistItem, done: Boolean) = Unit
        override fun createEvent(input: EventDraft) = Unit
        override fun updateEvent(event: CalendarEvent, input: EventDraft) = Unit
        override fun replaceOccurrence(event: CalendarEvent, originalDate: LocalDate, input: EventDraft) = Unit
        override fun clearPlannerError() = Unit
        override fun retryPlanner() = Unit
    }
}
