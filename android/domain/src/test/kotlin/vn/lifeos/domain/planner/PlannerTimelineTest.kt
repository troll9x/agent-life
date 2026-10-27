package vn.lifeos.domain.planner

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.lifeos.domain.common.Uuid

class PlannerTimelineTest {
    private val w = id(1)
    private fun id(number: Int) = Uuid.parse("00000000-0000-4000-8000-${number.toString().padStart(12, '0')}")
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test fun meetingsAtNineAndNineThirtyOverlapButAdjacentDoNot() {
        fun meeting(id: Uuid, title: String, start: String, end: String) = CalendarEvent(id, w, null, null,
            title, EventTimeKind.TIMED, Instant.parse(start), Instant.parse(end), null, null, zone, null, null, false, 1)
        val a = meeting(id(2), "09:00", "2026-10-19T02:00:00Z", "2026-10-19T03:00:00Z")
        val b = meeting(id(3), "09:30", "2026-10-19T02:30:00Z", "2026-10-19T03:30:00Z")
        val c = meeting(id(4), "10:00", "2026-10-19T03:00:00Z", "2026-10-19T04:00:00Z")
        val day = LocalDate.parse("2026-10-19")
        val shown = eventOccurrences(listOf(a, b, c), emptyList(), day, day.plusDays(1), zone)
        assertEquals(listOf("09:30"), overlappingEvents(shown[0], shown.take(2), zone).map { it.title })
        assertFalse(overlappingEvents(shown[0], listOf(shown[2]), zone).isNotEmpty())
    }

    @Test fun monthThirtyOneSkipsFebruaryAndOverlappingQueriesKeepSameKeys() {
        val first = LocalDate.parse("2027-01-31")
        val spec = RecurrenceSpec(Frequency.MONTHLY, interval = 1, count = 3)
        val a = boundedOccurrences(id(5), first, spec, first, first.plusDays(90))
        val b = boundedOccurrences(id(5), first, spec, LocalDate.parse("2027-03-01"), LocalDate.parse("2027-05-01"))
        assertEquals(listOf("2027-01-31", "2027-03-31", "2027-05-31"),
            boundedOccurrences(id(5), first, spec, first, LocalDate.parse("2027-06-01")).map { it.originalDate.toString() })
        assertEquals(a.last().key, b.first().key)
        assertEquals(1, (a + b).distinctBy(LocalOccurrence::key).count { it.originalDate == LocalDate.parse("2027-03-31") })
    }

    @Test fun exceptionRemovesOnlyOneRecurringOccurrence() {
        val first = LocalDate.parse("2026-10-19")
        val event = CalendarEvent(id(6), w, null, null, "Fixture", EventTimeKind.ALL_DAY, null, null,
            first, first.plusDays(1), zone, RecurrenceSpec(Frequency.DAILY, count = 3).encode(), null, false, 1)
        val exception = RecurrenceException(id(7), event.id, occurrenceKey(event.id, first.plusDays(1)), null, true)
        val shown = eventOccurrences(listOf(event), listOf(exception), first, first.plusDays(3), zone)
        assertEquals(listOf(first, first.plusDays(2)), shown.map(EventOccurrence::startDate))
        assertEquals(2, shown.map(EventOccurrence::key).distinct().size)
    }

    @Test fun futureRecurringSeriesDoesNotHideEventsToday() {
        val today = LocalDate.parse("2026-10-19")
        fun event(number: Int, date: LocalDate, recurrence: String?) = CalendarEvent(id(number), w, null, null,
            "Fixture $number", EventTimeKind.ALL_DAY, null, null, date, date.plusDays(1), zone, recurrence, null, false, 1)
        val shown = eventOccurrences(listOf(event(8, today, null), event(9, today.plusMonths(1), "DAILY")),
            emptyList(), today, today.plusDays(1), zone)
        assertEquals(listOf(id(8)), shown.map(EventOccurrence::seriesId))
    }
}
