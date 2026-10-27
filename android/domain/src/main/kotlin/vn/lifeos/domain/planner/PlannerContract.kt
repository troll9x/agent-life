package vn.lifeos.domain.planner

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Outcome

enum class TaskState { OPEN, DOING, DONE, CANCELLED }
enum class EventTimeKind { TIMED, ALL_DAY }
enum class ReminderState { PENDING, FIRED, CANCELLED }
enum class ReminderTarget { TASK, EVENT }

data class Project(val id: Uuid, val workspaceId: Uuid, val title: String, val color: String?, val archivedAt: Instant?, val version: Long)
data class PlannedTask(
    val id: Uuid, val workspaceId: Uuid, val projectId: Uuid?, val title: String, val note: String?,
    val state: TaskState, val priority: Int, val dueAt: Instant?, val completedAt: Instant?,
    val recurrence: String?, val sourceProposalId: Uuid?, val version: Long,
)
data class TaskChecklistItem(val id: Uuid, val taskId: Uuid, val title: String, val done: Boolean, val order: Int, val version: Long)
data class CalendarEvent(
    val id: Uuid, val workspaceId: Uuid, val sourceConnectionId: Uuid?, val externalId: String?,
    val title: String, val timeKind: EventTimeKind, val startAt: Instant?, val endAt: Instant?,
    val startDate: LocalDate?, val endDateExclusive: LocalDate?, val eventZoneId: ZoneId,
    val recurrence: String?, val sourceWebLink: String?, val readonly: Boolean, val version: Long,
)
data class RecurrenceException(
    val id: Uuid, val seriesId: Uuid, val originalOccurrenceKey: String,
    val overridePayload: String?, val cancelled: Boolean,
)
data class Reminder(
    val id: Uuid, val targetType: ReminderTarget, val targetId: Uuid, val occurrenceKey: String,
    val slot: String, val triggerAt: Instant, val state: ReminderState, val platformRequestId: String,
)

data class CreateProject(val id: Uuid, val workspaceId: Uuid, val originDeviceId: Uuid, val title: String, val color: String? = null)
data class CreateTask(
    val id: Uuid, val workspaceId: Uuid, val originDeviceId: Uuid, val projectId: Uuid?,
    val title: String, val note: String? = null, val priority: Int = 3, val dueAt: Instant? = null,
    val recurrence: String? = null, val sourceProposalId: Uuid? = null,
)
data class CreateChecklistItem(
    val id: Uuid, val workspaceId: Uuid, val originDeviceId: Uuid, val taskId: Uuid,
    val title: String, val order: Int,
)
data class CreateCalendarEvent(
    val id: Uuid, val workspaceId: Uuid, val originDeviceId: Uuid, val title: String,
    val timeKind: EventTimeKind, val eventZoneId: ZoneId,
    val startAt: Instant? = null, val endAt: Instant? = null,
    val startDate: LocalDate? = null, val endDateExclusive: LocalDate? = null,
    val sourceConnectionId: Uuid? = null, val externalId: String? = null,
    val recurrence: String? = null, val sourceWebLink: String? = null, val readonly: Boolean = false,
)
data class CreateRecurrenceException(
    val id: Uuid, val workspaceId: Uuid, val originDeviceId: Uuid, val seriesId: Uuid,
    val originalOccurrenceKey: String, val overridePayload: String? = null, val cancelled: Boolean,
)
data class CreateReminder(
    val id: Uuid, val workspaceId: Uuid, val originDeviceId: Uuid, val targetType: ReminderTarget,
    val targetId: Uuid, val occurrenceKey: String, val slot: String,
    val triggerAt: Instant, val platformRequestId: String,
)

data class UpdateTask(
    val workspaceId: Uuid, val id: Uuid, val expectedVersion: Long, val projectId: Uuid?,
    val title: String, val note: String?, val priority: Int, val dueAt: Instant?, val recurrence: String?,
)

data class UpdateCalendarEvent(
    val workspaceId: Uuid, val id: Uuid, val expectedVersion: Long, val title: String,
    val timeKind: EventTimeKind, val eventZoneId: ZoneId,
    val startAt: Instant? = null, val endAt: Instant? = null,
    val startDate: LocalDate? = null, val endDateExclusive: LocalDate? = null,
    val recurrence: String? = null,
)

data class ReplaceOccurrence(
    val workspaceId: Uuid, val originDeviceId: Uuid, val exceptionId: Uuid,
    val seriesId: Uuid, val originalDate: LocalDate, val replacement: CreateCalendarEvent,
)

interface PlannerRepository {
    fun observeTasks(workspaceId: Uuid): Flow<Outcome<List<PlannedTask>>>
    fun observeProjects(workspaceId: Uuid): Flow<Outcome<List<Project>>>
    fun observeEvents(workspaceId: Uuid): Flow<Outcome<List<CalendarEvent>>>
    fun observeChecklist(workspaceId: Uuid, taskId: Uuid): Flow<Outcome<List<TaskChecklistItem>>>
    fun observeExceptions(workspaceId: Uuid): Flow<Outcome<List<RecurrenceException>>>
    suspend fun createProject(command: CreateProject): Outcome<Project>
    suspend fun archiveProject(workspaceId: Uuid, projectId: Uuid, expectedVersion: Long): Outcome<Project>
    suspend fun createTask(command: CreateTask): Outcome<PlannedTask>
    suspend fun updateTask(command: UpdateTask): Outcome<PlannedTask>
    suspend fun updateTaskState(workspaceId: Uuid, taskId: Uuid, expectedVersion: Long, state: TaskState): Outcome<PlannedTask>
    suspend fun addChecklistItem(command: CreateChecklistItem): Outcome<TaskChecklistItem>
    suspend fun setChecklistDone(workspaceId: Uuid, itemId: Uuid, expectedVersion: Long, done: Boolean): Outcome<TaskChecklistItem>
    suspend fun createEvent(command: CreateCalendarEvent): Outcome<CalendarEvent>
    suspend fun updateEvent(command: UpdateCalendarEvent): Outcome<CalendarEvent>
    suspend fun replaceOccurrence(command: ReplaceOccurrence): Outcome<CalendarEvent>
    suspend fun addRecurrenceException(command: CreateRecurrenceException): Outcome<RecurrenceException>
    suspend fun scheduleReminder(command: CreateReminder): Outcome<Reminder>
    suspend fun deleteTask(workspaceId: Uuid, taskId: Uuid, expectedVersion: Long): Outcome<Unit>
    suspend fun tasksDueBefore(workspaceId: Uuid, exclusiveEnd: Instant): Outcome<List<PlannedTask>>
    suspend fun eventsOnDate(workspaceId: Uuid, date: LocalDate, zoneId: ZoneId): Outcome<List<CalendarEvent>>
}

fun PlannedTask.isOverdue(now: Instant): Boolean =
    dueAt?.isBefore(now) == true && state != TaskState.DONE && state != TaskState.CANCELLED

/** Stable identity for a selected occurrence. No unbounded recurrence expansion occurs here. */
fun occurrenceKey(seriesId: Uuid, originalDate: LocalDate): String = "${seriesId.value}:$originalDate"
fun occurrenceKey(seriesId: Uuid, originalStart: Instant): String = "${seriesId.value}:$originalStart"

data class LocalOccurrence(val originalDate: LocalDate, val key: String)

/** Materializes local recurrence only inside a caller-supplied window of at most 366 days. */
fun localOccurrences(
    seriesId: Uuid, firstDate: LocalDate, recurrence: String,
    startInclusive: LocalDate, endExclusive: LocalDate,
): List<LocalOccurrence> {
    require(recurrence in setOf("DAILY", "WEEKLY", "MONTHLY"))
    val days = ChronoUnit.DAYS.between(startInclusive, endExclusive)
    require(days in 1..366) { "Window must contain 1..366 days" }
    return (0 until days.toInt()).map { startInclusive.plusDays(it.toLong()) }
        .filter { date ->
            !date.isBefore(firstDate) && when (recurrence) {
                "DAILY" -> true
                "WEEKLY" -> ChronoUnit.DAYS.between(firstDate, date) % 7 == 0L
                else -> date.dayOfMonth == firstDate.dayOfMonth
            }
        }.map { LocalOccurrence(it, occurrenceKey(seriesId, it)) }
}
