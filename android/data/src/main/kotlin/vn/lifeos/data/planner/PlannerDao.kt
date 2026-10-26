package vn.lifeos.data.planner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    @Query("SELECT EXISTS(SELECT 1 FROM workspaces WHERE id=:workspaceId AND deletedAt IS NULL)")
    suspend fun workspaceExists(workspaceId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM devices WHERE id=:deviceId AND workspaceId=:workspaceId AND deletedAt IS NULL AND revokedAt IS NULL)")
    suspend fun deviceExists(workspaceId: String, deviceId: String): Boolean

    @Query("SELECT * FROM projects WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL")
    suspend fun project(workspaceId: String, id: String): ProjectEntity?

    @Query("UPDATE projects SET archivedAt=:now, updatedAt=:now, version=version+1 WHERE workspaceId=:workspaceId AND id=:projectId AND version=:expectedVersion AND deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun archiveProject(workspaceId: String, projectId: String, expectedVersion: Long, now: Long): Int

    @Query("SELECT * FROM tasks WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL")
    suspend fun task(workspaceId: String, id: String): TaskEntity?

    @Query("SELECT * FROM task_checklist_items WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL")
    suspend fun checklistItem(workspaceId: String, id: String): TaskChecklistItemEntity?

    @Query("SELECT * FROM calendar_events WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL")
    suspend fun event(workspaceId: String, id: String): CalendarEventEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM integration_connections WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL)")
    suspend fun connectionExists(workspaceId: String, id: String): Boolean

    @Insert suspend fun insertProject(entity: ProjectEntity)
    @Insert suspend fun insertTask(entity: TaskEntity)
    @Insert suspend fun insertChecklistItem(entity: TaskChecklistItemEntity)
    @Insert suspend fun insertEvent(entity: CalendarEventEntity)
    @Insert suspend fun insertException(entity: RecurrenceExceptionEntity)
    @Insert suspend fun insertReminder(entity: ReminderEntity)

    @Query("SELECT * FROM tasks WHERE workspaceId=:workspaceId AND deletedAt IS NULL ORDER BY createdAt, id")
    fun observeTasks(workspaceId: String): Flow<List<TaskEntity>>

    @Query("SELECT task_checklist_items.* FROM task_checklist_items INNER JOIN tasks ON tasks.workspaceId=task_checklist_items.workspaceId AND tasks.id=task_checklist_items.taskId WHERE task_checklist_items.workspaceId=:workspaceId AND task_checklist_items.taskId=:taskId AND task_checklist_items.deletedAt IS NULL AND tasks.deletedAt IS NULL ORDER BY sortOrder, task_checklist_items.id")
    suspend fun checklist(workspaceId: String, taskId: String): List<TaskChecklistItemEntity>

    @Query("SELECT * FROM tasks WHERE workspaceId=:workspaceId AND deletedAt IS NULL AND dueAt IS NOT NULL AND dueAt<:endExclusive AND state NOT IN ('DONE','CANCELLED') ORDER BY dueAt, id")
    suspend fun tasksDueBefore(workspaceId: String, endExclusive: Long): List<TaskEntity>

    @Query("SELECT * FROM calendar_events WHERE workspaceId=:workspaceId AND deletedAt IS NULL AND ((timeKind='ALL_DAY' AND startDate<=:date AND endDateExclusive>:date) OR (timeKind='TIMED' AND startAt<:endAt AND endAt>:startAt)) ORDER BY timeKind, startDate, startAt, id")
    suspend fun eventsOnDate(workspaceId: String, date: String, startAt: Long, endAt: Long): List<CalendarEventEntity>

    @Query("UPDATE tasks SET state=:state, completedAt=:completedAt, updatedAt=:now, version=version+1 WHERE workspaceId=:workspaceId AND id=:taskId AND version=:expectedVersion AND deletedAt IS NULL AND state!=:state")
    suspend fun updateTaskState(workspaceId: String, taskId: String, expectedVersion: Long, state: String, completedAt: Long?, now: Long): Int

    @Query("UPDATE task_checklist_items SET done=:done, updatedAt=:now, version=version+1 WHERE workspaceId=:workspaceId AND id=:itemId AND version=:expectedVersion AND deletedAt IS NULL AND done!=:done")
    suspend fun setChecklistDone(workspaceId: String, itemId: String, expectedVersion: Long, done: Boolean, now: Long): Int

    @Query("UPDATE tasks SET deletedAt=:deletedAt, updatedAt=:deletedAt, version=version+1 WHERE workspaceId=:workspaceId AND id=:taskId AND version=:expectedVersion AND deletedAt IS NULL")
    suspend fun deleteTask(workspaceId: String, taskId: String, expectedVersion: Long, deletedAt: Long): Int

    @Query("UPDATE task_checklist_items SET deletedAt=:deletedAt, updatedAt=:deletedAt, version=version+1 WHERE workspaceId=:workspaceId AND taskId=:taskId AND deletedAt IS NULL")
    suspend fun deleteChecklistForTask(workspaceId: String, taskId: String, deletedAt: Long): Int

    @Query("UPDATE reminders SET state='CANCELLED', updatedAt=:now, version=version+1 WHERE workspaceId=:workspaceId AND targetType='TASK' AND targetId=:taskId AND state='PENDING' AND deletedAt IS NULL")
    suspend fun cancelRemindersForTask(workspaceId: String, taskId: String, now: Long): Int

    @Query("SELECT * FROM reminders WHERE workspaceId=:workspaceId AND targetType='TASK' AND targetId=:taskId AND deletedAt IS NULL")
    suspend fun remindersForTask(workspaceId: String, taskId: String): List<ReminderEntity>
}
