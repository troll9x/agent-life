package vn.lifeos.domain.planner

import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import vn.lifeos.domain.common.Uuid

class PlannerRulesTest {
    private val id = Uuid.parse("00000000-0000-4000-8000-000000000101")

    @Test fun nullableDeadlineIsNeverOverdueAndCompletionStopsOverdue() {
        val task = PlannedTask(id, id, null, "Fixture", null, TaskState.OPEN, 3, null, null, null, null, 1)
        val now = Instant.parse("2026-10-20T00:00:00Z")
        assertFalse(task.isOverdue(now))
        assertTrue(task.copy(dueAt = now.minusSeconds(1)).isOverdue(now))
        assertFalse(task.copy(dueAt = now.minusSeconds(1), state = TaskState.DONE).isOverdue(now))
    }

    @Test fun occurrenceKeyUsesOriginalDateNotViewZone() {
        val date = LocalDate.parse("2026-10-19")
        assertEquals("${id.value}:2026-10-19", occurrenceKey(id, date))
        assertEquals(occurrenceKey(id, date), occurrenceKey(id, LocalDate.parse("2026-10-19")))
    }

    @Test fun recurrenceIsWindowBoundedAndStableAcrossOverlappingQueries() {
        val first = LocalDate.parse("2026-10-19")
        val a = localOccurrences(id, first, "WEEKLY", first, first.plusDays(15))
        val b = localOccurrences(id, first, "WEEKLY", first.plusDays(7), first.plusDays(22))
        assertEquals(listOf(first, first.plusDays(7), first.plusDays(14)), a.map { it.originalDate })
        assertEquals(a[1].key, b[0].key)
        assertFailsWith<IllegalArgumentException> { localOccurrences(id, first, "DAILY", first, first.plusDays(367)) }
    }
}
