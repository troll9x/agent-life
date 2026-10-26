package vn.lifeos.data.planner

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.planner.*

/** Use only with the encrypted AgentLifeDatabase opened by the app storage provider. */
class RoomPlannerRepository(private val database: AgentLifeDatabase, private val clock: Clock) : PlannerRepository {
    private val dao = database.plannerDao()

    override fun observeTasks(workspaceId: Uuid): Flow<Outcome<List<PlannedTask>>> =
        dao.observeTasks(workspaceId.value).map<List<TaskEntity>, Outcome<List<PlannedTask>>> { rows ->
            Outcome.Success(rows.map(TaskEntity::toDomain))
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun createProject(command: CreateProject): Outcome<Project> = write {
        if (command.title.isBlank()) return@write invalid("title")
        if (!actorExists(command.workspaceId, command.originDeviceId)) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val row = ProjectEntity(command.id.value, command.workspaceId.value, command.title.trim(), command.color,
            null, now, now, 1, command.originDeviceId.value)
        dao.insertProject(row)
        Outcome.Success(row.toDomain())
    }

    override suspend fun archiveProject(workspaceId: Uuid, projectId: Uuid, expectedVersion: Long): Outcome<Project> = write {
        val before = dao.project(workspaceId.value, projectId.value) ?: return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        if (dao.archiveProject(workspaceId.value, projectId.value, expectedVersion, now) != 1) return@write Outcome.Failure(AppError.Conflict)
        Outcome.Success(before.copy(archivedAt = now, updatedAt = now, version = before.version + 1).toDomain())
    }

    override suspend fun createTask(command: CreateTask): Outcome<PlannedTask> = write {
        if (command.title.isBlank()) return@write invalid("title")
        if (command.priority !in 1..4) return@write invalid("priority")
        if (command.recurrence != null && command.recurrence !in LOCAL_RECURRENCES) return@write invalid("recurrence")
        if (!actorExists(command.workspaceId, command.originDeviceId)) return@write Outcome.Failure(AppError.NotFound)
        val projectId = command.projectId
        if (projectId != null && dao.project(command.workspaceId.value, projectId.value) == null) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val row = TaskEntity(command.id.value, command.workspaceId.value, command.projectId?.value, command.title.trim(),
            command.note, TaskState.OPEN.name, command.priority, command.dueAt?.toEpochMilli(), null,
            command.recurrence, command.sourceProposalId?.value, now, now, 1, command.originDeviceId.value)
        dao.insertTask(row)
        Outcome.Success(row.toDomain())
    }

    override suspend fun addChecklistItem(command: CreateChecklistItem): Outcome<TaskChecklistItem> = write {
        if (command.title.isBlank()) return@write invalid("title")
        if (command.order < 0) return@write invalid("order")
        if (!actorExists(command.workspaceId, command.originDeviceId) || dao.task(command.workspaceId.value, command.taskId.value) == null) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val row = TaskChecklistItemEntity(command.id.value, command.workspaceId.value, command.taskId.value,
            command.title.trim(), false, command.order, now, now, 1, command.originDeviceId.value)
        dao.insertChecklistItem(row)
        Outcome.Success(row.toDomain())
    }

    override suspend fun setChecklistDone(workspaceId: Uuid, itemId: Uuid, expectedVersion: Long, done: Boolean): Outcome<TaskChecklistItem> = write {
        val before = dao.checklistItem(workspaceId.value, itemId.value) ?: return@write Outcome.Failure(AppError.NotFound)
        if (dao.task(workspaceId.value, before.taskId) == null) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        if (dao.setChecklistDone(workspaceId.value, itemId.value, expectedVersion, done, now) != 1) return@write Outcome.Failure(AppError.Conflict)
        Outcome.Success(before.copy(done = done, updatedAt = now, version = before.version + 1).toDomain())
    }

    override suspend fun createEvent(command: CreateCalendarEvent): Outcome<CalendarEvent> = write {
        if (command.title.isBlank()) return@write invalid("title")
        val startDate = command.startDate
        val endDateExclusive = command.endDateExclusive
        val startAt = command.startAt
        val endAt = command.endAt
        when (command.timeKind) {
            EventTimeKind.ALL_DAY -> if (startDate == null || endDateExclusive == null ||
                !endDateExclusive.isAfter(startDate) || startAt != null || endAt != null) return@write invalid("eventTime")
            EventTimeKind.TIMED -> if (startAt == null || endAt == null ||
                !endAt.isAfter(startAt) || startDate != null || endDateExclusive != null) return@write invalid("eventTime")
        }
        if ((command.sourceConnectionId == null) != (command.externalId == null) || command.externalId?.isBlank() == true) return@write invalid("source")
        if (command.sourceConnectionId == null && command.readonly) return@write invalid("readonly")
        if (command.sourceConnectionId != null && !command.readonly) return@write invalid("readonly")
        if (command.sourceConnectionId == null && command.recurrence != null && command.recurrence !in LOCAL_RECURRENCES) return@write invalid("recurrence")
        if (!actorExists(command.workspaceId, command.originDeviceId)) return@write Outcome.Failure(AppError.NotFound)
        val sourceConnectionId = command.sourceConnectionId
        if (sourceConnectionId != null && !dao.connectionExists(command.workspaceId.value, sourceConnectionId.value)) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val row = CalendarEventEntity(command.id.value, command.workspaceId.value, command.sourceConnectionId?.value,
            command.externalId, command.title.trim(), command.timeKind.name, command.startAt?.toEpochMilli(),
            command.endAt?.toEpochMilli(), command.startDate?.toString(), command.endDateExclusive?.toString(),
            command.eventZoneId.id, command.recurrence, command.sourceWebLink, command.readonly,
            now, now, 1, command.originDeviceId.value)
        dao.insertEvent(row)
        Outcome.Success(row.toDomain())
    }

    override suspend fun addRecurrenceException(command: CreateRecurrenceException): Outcome<RecurrenceException> = write {
        if (!command.originalOccurrenceKey.startsWith("${command.seriesId.value}:")) return@write invalid("originalOccurrenceKey")
        if (!actorExists(command.workspaceId, command.originDeviceId)) return@write Outcome.Failure(AppError.NotFound)
        val series = dao.task(command.workspaceId.value, command.seriesId.value)?.recurrence
            ?: dao.event(command.workspaceId.value, command.seriesId.value)?.recurrence
        if (series == null) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val row = RecurrenceExceptionEntity(command.id.value, command.workspaceId.value, command.seriesId.value,
            command.originalOccurrenceKey, command.overridePayload, command.cancelled,
            now, now, 1, command.originDeviceId.value)
        dao.insertException(row)
        Outcome.Success(row.toDomain())
    }

    override suspend fun scheduleReminder(command: CreateReminder): Outcome<Reminder> = write {
        if (command.occurrenceKey.isBlank() || command.slot.isBlank() || command.platformRequestId.isBlank()) return@write invalid("reminder")
        if (!actorExists(command.workspaceId, command.originDeviceId)) return@write Outcome.Failure(AppError.NotFound)
        val targetExists = when (command.targetType) {
            ReminderTarget.TASK -> dao.task(command.workspaceId.value, command.targetId.value) != null
            ReminderTarget.EVENT -> dao.event(command.workspaceId.value, command.targetId.value) != null
        }
        if (!targetExists) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val row = ReminderEntity(command.id.value, command.workspaceId.value, command.targetType.name,
            command.targetId.value, command.occurrenceKey, command.slot, command.triggerAt.toEpochMilli(),
            ReminderState.PENDING.name, command.platformRequestId, now, now, 1, command.originDeviceId.value)
        dao.insertReminder(row)
        Outcome.Success(row.toDomain())
    }

    override suspend fun updateTaskState(workspaceId: Uuid, taskId: Uuid, expectedVersion: Long, state: TaskState): Outcome<PlannedTask> = write {
        val before = dao.task(workspaceId.value, taskId.value) ?: return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val completedAt = if (state == TaskState.DONE) now else null
        if (dao.updateTaskState(workspaceId.value, taskId.value, expectedVersion, state.name, completedAt, now) != 1) return@write Outcome.Failure(AppError.Conflict)
        Outcome.Success(before.copy(state = state.name, completedAt = completedAt, updatedAt = now,
            version = before.version + 1).toDomain())
    }

    override suspend fun deleteTask(workspaceId: Uuid, taskId: Uuid, expectedVersion: Long): Outcome<Unit> = write {
        if (dao.task(workspaceId.value, taskId.value) == null) return@write Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        if (dao.deleteTask(workspaceId.value, taskId.value, expectedVersion, now) != 1) return@write Outcome.Failure(AppError.Conflict)
        dao.deleteChecklistForTask(workspaceId.value, taskId.value, now)
        dao.cancelRemindersForTask(workspaceId.value, taskId.value, now)
        Outcome.Success(Unit)
    }

    override suspend fun tasksDueBefore(workspaceId: Uuid, exclusiveEnd: Instant): Outcome<List<PlannedTask>> = read {
        if (!dao.workspaceExists(workspaceId.value)) return@read Outcome.Failure(AppError.NotFound)
        Outcome.Success(dao.tasksDueBefore(workspaceId.value, exclusiveEnd.toEpochMilli()).map(TaskEntity::toDomain))
    }

    override suspend fun eventsOnDate(workspaceId: Uuid, date: LocalDate, zoneId: ZoneId): Outcome<List<CalendarEvent>> = read {
        if (!dao.workspaceExists(workspaceId.value)) return@read Outcome.Failure(AppError.NotFound)
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        Outcome.Success(dao.eventsOnDate(workspaceId.value, date.toString(), start, end).map(CalendarEventEntity::toDomain))
    }

    private suspend fun actorExists(workspaceId: Uuid, deviceId: Uuid): Boolean =
        dao.workspaceExists(workspaceId.value) && dao.deviceExists(workspaceId.value, deviceId.value)

    private suspend fun <T> write(block: suspend () -> Outcome<T>): Outcome<T> = try {
        database.withTransaction { block() }
    } catch (e: CancellationException) { throw e }
    catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }

    private suspend fun <T> read(block: suspend () -> Outcome<T>): Outcome<T> = try { block() }
    catch (e: CancellationException) { throw e }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }

    private fun <T> invalid(field: String): Outcome<T> = Outcome.Failure(AppError.InvalidInput(field))

    private companion object { val LOCAL_RECURRENCES = setOf("DAILY", "WEEKLY", "MONTHLY") }
}

private fun ProjectEntity.toDomain() = Project(Uuid.parse(id), Uuid.parse(workspaceId), title, color, archivedAt?.let(Instant::ofEpochMilli), version)
private fun TaskEntity.toDomain() = PlannedTask(Uuid.parse(id), Uuid.parse(workspaceId), projectId?.let(Uuid::parse), title, note,
    TaskState.valueOf(state), priority, dueAt?.let(Instant::ofEpochMilli), completedAt?.let(Instant::ofEpochMilli),
    recurrence, sourceProposalId?.let(Uuid::parse), version)
private fun TaskChecklistItemEntity.toDomain() = TaskChecklistItem(Uuid.parse(id), Uuid.parse(taskId), title, done, sortOrder, version)
private fun CalendarEventEntity.toDomain() = CalendarEvent(Uuid.parse(id), Uuid.parse(workspaceId), sourceConnectionId?.let(Uuid::parse), externalId,
    title, EventTimeKind.valueOf(timeKind), startAt?.let(Instant::ofEpochMilli), endAt?.let(Instant::ofEpochMilli),
    startDate?.let(LocalDate::parse), endDateExclusive?.let(LocalDate::parse), ZoneId.of(eventZoneId), recurrence,
    sourceWebLink, readonly, version)
private fun RecurrenceExceptionEntity.toDomain() = RecurrenceException(Uuid.parse(id), Uuid.parse(seriesId), originalOccurrenceKey, overridePayload, cancelled)
private fun ReminderEntity.toDomain() = Reminder(Uuid.parse(id), ReminderTarget.valueOf(targetType), Uuid.parse(targetId), occurrenceKey,
    slot, Instant.ofEpochMilli(triggerAt), ReminderState.valueOf(state), platformRequestId)
