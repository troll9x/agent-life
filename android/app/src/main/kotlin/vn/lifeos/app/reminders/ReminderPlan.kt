package vn.lifeos.app.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import vn.lifeos.data.reminders.ReminderCandidate
import vn.lifeos.domain.planner.CalendarEvent
import vn.lifeos.domain.planner.EventTimeKind
import vn.lifeos.domain.planner.PlannedTask
import vn.lifeos.domain.planner.RecurrenceException
import vn.lifeos.domain.planner.RecurrenceSpec
import vn.lifeos.domain.planner.ReminderTarget
import vn.lifeos.domain.planner.TaskState
import vn.lifeos.domain.planner.boundedOccurrences
import vn.lifeos.domain.planner.eventOccurrences

/** A rolling 30-day window keeps alarms bounded; periodic work replenishes it. */
internal fun reminderCandidates(tasks: List<PlannedTask>, events: List<CalendarEvent>,
    exceptions: List<RecurrenceException>, now: Instant, profileZone: ZoneId): List<ReminderCandidate> {
    val today = now.atZone(profileZone).toLocalDate()
    val last = today.plusDays(31)
    val recentCutoff = now.minusSeconds(300)
    val taskCandidates = tasks.filter { it.state != TaskState.DONE && it.state != TaskState.CANCELLED && it.dueAt != null }
        .flatMap { task ->
            val due = task.dueAt!!
            val recurrence = task.recurrence
            val dates = if (recurrence == null) listOf(due.atZone(profileZone).toLocalDate()) else {
                val rule = RecurrenceSpec.parse(recurrence) ?: return@flatMap emptyList()
                val first = due.atZone(profileZone).toLocalDate()
                val end = minOf(last, first.plusYears(5).plusDays(1))
                if (!end.isAfter(maxOf(today, first))) return@flatMap emptyList()
                boundedOccurrences(task.id, first, rule, maxOf(today, first), end).map { it.originalDate }
            }
            dates.mapNotNull { date ->
                val trigger = if (recurrence == null) due else date.atTime(due.atZone(profileZone).toLocalTime()).atZone(profileZone).toInstant()
                val key = if (recurrence == null) task.id.value else "${task.id.value}:$date"
                ReminderCandidate(ReminderTarget.TASK, task.id, key, trigger).takeIf { trigger.isAfter(recentCutoff) && trigger.isBefore(last.atStartOfDay(profileZone).toInstant()) }
            }
        }
    val eventCandidates = (0..4).flatMap { week ->
        val from = today.plusDays(week * 7L)
        val end = minOf(from.plusDays(7), last)
        if (!end.isAfter(from)) emptyList() else runCatching { eventOccurrences(events, exceptions, from, end, profileZone) }.getOrDefault(emptyList())
    }.distinctBy { it.key }.mapNotNull { occurrence ->
        val trigger = if (occurrence.timeKind == EventTimeKind.ALL_DAY)
            occurrence.startDate!!.atTime(9, 0).atZone(occurrence.eventZoneId).toInstant()
            else occurrence.startAt!!
        ReminderCandidate(ReminderTarget.EVENT, occurrence.seriesId, occurrence.key, trigger)
            .takeIf { trigger.isAfter(recentCutoff) && trigger.isBefore(last.atStartOfDay(profileZone).toInstant()) }
    }
    return (taskCandidates + eventCandidates).distinctBy { "${it.target}|${it.targetId}|${it.occurrenceKey}|${it.slot}" }
}
