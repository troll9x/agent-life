package vn.lifeos.data.planner

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.lifeos.data.db.IntegrationConnectionEntity
import vn.lifeos.data.db.WorkspaceEntity

@Entity(tableName = "projects", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true)])
data class ProjectEntity(
    @PrimaryKey val id: String, val workspaceId: String, val title: String, val color: String?,
    val archivedAt: Long?, val createdAt: Long, val updatedAt: Long, val version: Long,
    val originDeviceId: String, val deletedAt: Long? = null,
)

@Entity(tableName = "tasks", foreignKeys = [
    ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ProjectEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "projectId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true), Index(value = ["workspaceId", "projectId"]), Index(value = ["workspaceId", "state", "dueAt"]), Index(value = ["workspaceId", "sourceProposalId"], unique = true)])
data class TaskEntity(
    @PrimaryKey val id: String, val workspaceId: String, val projectId: String?, val title: String,
    val note: String?, val state: String, val priority: Int, val dueAt: Long?, val completedAt: Long?,
    val recurrence: String?, val sourceProposalId: String?, val createdAt: Long, val updatedAt: Long,
    val version: Long, val originDeviceId: String, val deletedAt: Long? = null,
)

@Entity(tableName = "task_checklist_items", foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "taskId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["workspaceId", "taskId", "sortOrder"]), Index(value = ["workspaceId", "taskId", "id"], unique = true)])
data class TaskChecklistItemEntity(
    @PrimaryKey val id: String, val workspaceId: String, val taskId: String, val title: String,
    val done: Boolean, val sortOrder: Int, val createdAt: Long, val updatedAt: Long, val version: Long,
    val originDeviceId: String, val deletedAt: Long? = null,
)

@Entity(tableName = "calendar_events", foreignKeys = [
    ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = IntegrationConnectionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "sourceConnectionId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true), Index(value = ["workspaceId", "sourceConnectionId", "externalId"], unique = true), Index(value = ["workspaceId", "startDate", "endDateExclusive"]), Index(value = ["workspaceId", "startAt", "endAt"])])
data class CalendarEventEntity(
    @PrimaryKey val id: String, val workspaceId: String, val sourceConnectionId: String?, val externalId: String?,
    val title: String, val timeKind: String, val startAt: Long?, val endAt: Long?,
    val startDate: String?, val endDateExclusive: String?, val eventZoneId: String,
    val recurrence: String?, val sourceWebLink: String?, val readonly: Boolean,
    val createdAt: Long, val updatedAt: Long, val version: Long, val originDeviceId: String, val deletedAt: Long? = null,
)

@Entity(tableName = "recurrence_exceptions", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("workspaceId"), Index(value = ["workspaceId", "seriesId", "originalOccurrenceKey"], unique = true)])
data class RecurrenceExceptionEntity(
    @PrimaryKey val id: String, val workspaceId: String, val seriesId: String, val originalOccurrenceKey: String,
    val overridePayload: String?, val cancelled: Boolean, val createdAt: Long, val updatedAt: Long,
    val version: Long, val originDeviceId: String, val deletedAt: Long? = null,
)

@Entity(tableName = "reminders", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("workspaceId"), Index(value = ["workspaceId", "targetType", "targetId", "occurrenceKey", "slot"], unique = true), Index(value = ["workspaceId", "state", "triggerAt"])])
data class ReminderEntity(
    @PrimaryKey val id: String, val workspaceId: String, val targetType: String, val targetId: String,
    val occurrenceKey: String, val slot: String, val triggerAt: Long, val state: String,
    val platformRequestId: String, val createdAt: Long, val updatedAt: Long, val version: Long,
    val originDeviceId: String, val deletedAt: Long? = null,
)
