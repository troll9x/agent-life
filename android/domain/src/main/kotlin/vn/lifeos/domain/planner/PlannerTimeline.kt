package vn.lifeos.domain.planner

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import vn.lifeos.domain.common.Uuid

data class EventOccurrence(
    val seriesId: Uuid, val key: String, val title: String, val timeKind: EventTimeKind,
    val startAt: Instant?, val endAt: Instant?, val startDate: LocalDate?, val endDateExclusive: LocalDate?,
    val eventZoneId: ZoneId, val readonly: Boolean, val recurring: Boolean,
)

fun eventOccurrences(
    events: List<CalendarEvent>, exceptions: List<RecurrenceException>,
    startInclusive: LocalDate, endExclusive: LocalDate, displayZone: ZoneId,
): List<EventOccurrence> {
    require(endExclusive.isAfter(startInclusive) && ChronoUnit.DAYS.between(startInclusive, endExclusive) <= 7)
    val cancelled = exceptions.filter { it.cancelled }.map { it.originalOccurrenceKey }.toSet()
    val windowStart = startInclusive.atStartOfDay(displayZone).toInstant()
    val windowEnd = endExclusive.atStartOfDay(displayZone).toInstant()
    return events.flatMap { event ->
        val firstDate = event.startDate ?: event.startAt!!.atZone(event.eventZoneId).toLocalDate()
        val dates = if (event.recurrence == null) listOf(firstDate) else {
            val spec = RecurrenceSpec.parse(event.recurrence) ?: return@flatMap emptyList()
            val lookback = if (event.timeKind == EventTimeKind.ALL_DAY) ChronoUnit.DAYS.between(event.startDate, event.endDateExclusive)
                else ChronoUnit.DAYS.between(event.startAt!!.atZone(event.eventZoneId).toLocalDate(), event.endAt!!.atZone(event.eventZoneId).toLocalDate()) + 1
            val from = maxOf(firstDate, startInclusive.minusDays(lookback.coerceAtLeast(1)))
            if (!from.isBefore(endExclusive)) return@flatMap emptyList()
            if (endExclusive.isAfter(firstDate.plusYears(5).plusDays(1))) return@flatMap emptyList()
            boundedOccurrences(event.id, firstDate, spec, from, endExclusive).map(LocalOccurrence::originalDate)
        }
        dates.mapNotNull { date ->
            val key = if (event.recurrence == null) event.id.value else occurrenceKey(event.id, date)
            if (key in cancelled) return@mapNotNull null
            val occurrence = if (event.timeKind == EventTimeKind.ALL_DAY) {
                val duration = ChronoUnit.DAYS.between(event.startDate, event.endDateExclusive)
                EventOccurrence(event.id, key, event.title, event.timeKind, null, null, date, date.plusDays(duration),
                    event.eventZoneId, event.readonly, event.recurrence != null)
            } else {
                val start = date.atTime(event.startAt!!.atZone(event.eventZoneId).toLocalTime()).atZone(event.eventZoneId).toInstant()
                val end = start.plus(Duration.between(event.startAt, event.endAt))
                EventOccurrence(event.id, key, event.title, event.timeKind, start, end, null, null,
                    event.eventZoneId, event.readonly, event.recurrence != null)
            }
            val interval = occurrence.instantRange(displayZone)
            occurrence.takeIf { interval.first < windowEnd && interval.second > windowStart }
        }
    }.distinctBy(EventOccurrence::key).sortedWith(compareBy({ it.startDate?.toString() ?: it.startAt.toString() }, EventOccurrence::key))
}

fun overlappingEvents(candidate: EventOccurrence, others: List<EventOccurrence>, zone: ZoneId): List<EventOccurrence> {
    val (start, end) = candidate.instantRange(zone)
    return others.filter { it.key != candidate.key && it.seriesId != candidate.seriesId }.filter {
        val (otherStart, otherEnd) = it.instantRange(zone)
        start < otherEnd && otherStart < end
    }
}

private fun EventOccurrence.instantRange(zone: ZoneId): Pair<Instant, Instant> = when (timeKind) {
    EventTimeKind.ALL_DAY -> startDate!!.atStartOfDay(zone).toInstant() to endDateExclusive!!.atStartOfDay(zone).toInstant()
    EventTimeKind.TIMED -> startAt!! to endAt!!
}
