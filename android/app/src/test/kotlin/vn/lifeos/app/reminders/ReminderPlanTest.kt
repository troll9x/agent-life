package vn.lifeos.app.reminders

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.PlannedTask
import vn.lifeos.domain.planner.TaskState

class ReminderPlanTest {
    private val id = Uuid.parse("00000000-0000-4000-8000-000000000901")

    @Test fun movedTaskProducesOnlyNewAlarmTime() {
        val now = Instant.parse("2026-10-20T00:00:00Z")
        val task = PlannedTask(id, id, null, "Fixture", null, TaskState.OPEN, 3,
            Instant.parse("2026-10-20T02:00:00Z"), null, null, null, 1)
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        val first = reminderCandidates(listOf(task), emptyList(), emptyList(), now, zone)
        val moved = reminderCandidates(listOf(task.copy(dueAt = Instant.parse("2026-10-20T03:00:00Z"))),
            emptyList(), emptyList(), now, zone)
        assertEquals(listOf(Instant.parse("2026-10-20T02:00:00Z")), first.map { it.triggerAt })
        assertEquals(listOf(Instant.parse("2026-10-20T03:00:00Z")), moved.map { it.triggerAt })
        assertEquals(first.single().occurrenceKey, moved.single().occurrenceKey)
    }
}
