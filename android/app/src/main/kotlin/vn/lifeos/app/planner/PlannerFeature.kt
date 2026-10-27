package vn.lifeos.app.planner

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.planner.*

internal data class PlannerUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val projects: List<Project> = emptyList(),
    val tasks: List<PlannedTask> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val exceptions: List<RecurrenceException> = emptyList(),
    val checklist: List<TaskChecklistItem> = emptyList(),
    val checklistTaskId: Uuid? = null,
    val busy: Boolean = false,
    val actionVersion: Long = 0,
)

internal interface PlannerActions {
    fun createProject(title: String)
    fun createTask(title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?)
    fun updateTask(task: PlannedTask, title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?)
    fun setTaskState(task: PlannedTask, state: TaskState)
    fun selectChecklist(taskId: Uuid?)
    fun addChecklist(taskId: Uuid, title: String, order: Int)
    fun setChecklistDone(item: TaskChecklistItem, done: Boolean)
    fun createEvent(input: EventDraft)
    fun updateEvent(event: CalendarEvent, input: EventDraft)
    fun replaceOccurrence(event: CalendarEvent, originalDate: LocalDate, input: EventDraft)
    fun clearPlannerError()
    fun retryPlanner()
}

/** ViewModel-owned planner coordinator; Compose receives only state and callbacks. */
internal class PlannerFeature(
    private val repository: PlannerRepository,
    private val workspaceId: Uuid,
    private val deviceId: Uuid,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(PlannerUiState())
    val state: StateFlow<PlannerUiState> = mutable.asStateFlow()
    private var action: Job? = null
    private var checklistJob: Job? = null
    private var observeJob: Job? = null

    init { retry() }

    fun retry() {
        observeJob?.cancel()
        mutable.value = mutable.value.copy(loading = true, loadFailed = false, error = null)
        observeJob = scope.launch {
            combine(repository.observeProjects(workspaceId), repository.observeTasks(workspaceId),
                repository.observeEvents(workspaceId), repository.observeExceptions(workspaceId)) { projects, tasks, events, exceptions ->
                PlannerResult(projects, tasks, events, exceptions)
            }.collect { results ->
                val projects = results.projects as? Outcome.Success
                val tasks = results.tasks as? Outcome.Success
                val events = results.events as? Outcome.Success
                val exceptions = results.exceptions as? Outcome.Success
                if (projects == null || tasks == null || events == null || exceptions == null) {
                    mutable.value = mutable.value.copy(loading = false, loadFailed = true, error = "Không đọc được dữ liệu công việc/lịch.")
                } else {
                    mutable.value = mutable.value.copy(loading = false, loadFailed = false, error = null,
                        projects = projects.value, tasks = tasks.value,
                        events = events.value, exceptions = exceptions.value)
                }
            }
        }
    }

    fun selectChecklist(taskId: Uuid?) {
        checklistJob?.cancel()
        mutable.value = mutable.value.copy(checklistTaskId = taskId, checklist = emptyList())
        if (taskId != null) checklistJob = scope.launch {
            repository.observeChecklist(workspaceId, taskId).collect { result ->
                mutable.value = when (result) {
                    is Outcome.Success -> mutable.value.copy(checklist = result.value)
                    is Outcome.Failure -> mutable.value.copy(error = "Không đọc được checklist.")
                }
            }
        }
    }

    fun createProject(title: String) = submit { repository.createProject(CreateProject(ids.newId(), workspaceId, deviceId, title)) }
    fun createTask(title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) = submit {
        repository.createTask(CreateTask(ids.newId(), workspaceId, deviceId, projectId, title, note, priority, dueAt, recurrence))
    }
    fun updateTask(task: PlannedTask, title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) = submit {
        repository.updateTask(UpdateTask(workspaceId, task.id, task.version, projectId, title, note, priority, dueAt, recurrence))
    }
    fun setTaskState(task: PlannedTask, state: TaskState) = submit { repository.updateTaskState(workspaceId, task.id, task.version, state) }
    fun addChecklist(taskId: Uuid, title: String, order: Int) = submit {
        repository.addChecklistItem(CreateChecklistItem(ids.newId(), workspaceId, deviceId, taskId, title, order))
    }
    fun setChecklistDone(item: TaskChecklistItem, done: Boolean) = submit { repository.setChecklistDone(workspaceId, item.id, item.version, done) }

    fun createEvent(input: EventDraft) = submit { repository.createEvent(input.toCommand(ids.newId(), workspaceId, deviceId)) }
    fun updateEvent(event: CalendarEvent, input: EventDraft) = submit {
        repository.updateEvent(input.toUpdate(event, workspaceId))
    }
    fun replaceOccurrence(event: CalendarEvent, originalDate: LocalDate, input: EventDraft) = submit {
        repository.replaceOccurrence(ReplaceOccurrence(workspaceId, deviceId, ids.newId(), event.id, originalDate,
            input.copy(recurrence = null).toCommand(ids.newId(), workspaceId, deviceId)))
    }

    fun clearError() { mutable.value = mutable.value.copy(error = null) }

    private fun submit(operation: suspend () -> Outcome<*>) {
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null)
            val result = try { operation() } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            mutable.value = if (result is Outcome.Success) mutable.value.copy(busy = false, actionVersion = mutable.value.actionVersion + 1)
                else mutable.value.copy(busy = false, error = "Không lưu được. Kiểm tra dữ liệu hoặc phiên bản mới nhất rồi thử lại.")
        }
    }
}

private data class PlannerResult(
    val projects: Outcome<List<Project>>, val tasks: Outcome<List<PlannedTask>>,
    val events: Outcome<List<CalendarEvent>>, val exceptions: Outcome<List<RecurrenceException>>,
)

internal data class EventDraft(
    val title: String, val timeKind: EventTimeKind, val zoneId: ZoneId,
    val startAt: Instant? = null, val endAt: Instant? = null,
    val startDate: LocalDate? = null, val endDateExclusive: LocalDate? = null,
    val recurrence: String? = null,
) {
    fun toCommand(id: Uuid, workspace: Uuid, device: Uuid) = CreateCalendarEvent(id, workspace, device, title,
        timeKind, zoneId, startAt, endAt, startDate, endDateExclusive, recurrence = recurrence)
    fun toUpdate(event: CalendarEvent, workspace: Uuid) = UpdateCalendarEvent(workspace, event.id, event.version,
        title, timeKind, zoneId, startAt, endAt, startDate, endDateExclusive, recurrence)
}
