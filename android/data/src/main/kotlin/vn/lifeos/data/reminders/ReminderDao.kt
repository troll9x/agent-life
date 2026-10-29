package vn.lifeos.data.reminders

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import vn.lifeos.data.planner.ReminderEntity
import vn.lifeos.data.planner.TaskEntity

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE workspaceId=:workspaceId AND deletedAt IS NULL")
    suspend fun all(workspaceId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id=:id AND deletedAt IS NULL")
    suspend fun byId(id: String): ReminderEntity?

    @Query("SELECT * FROM tasks WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL")
    suspend fun task(workspaceId: String, id: String): TaskEntity?

    @Query("SELECT title FROM calendar_events WHERE workspaceId=:workspaceId AND id=:id AND deletedAt IS NULL")
    suspend fun eventTitle(workspaceId: String, id: String): String?

    @Query("SELECT originDeviceId FROM workspaces WHERE id=:workspaceId AND deletedAt IS NULL")
    suspend fun deviceId(workspaceId: String): String?

    @Insert suspend fun insert(row: ReminderEntity)
    @Update suspend fun update(row: ReminderEntity)

    @Query("UPDATE tasks SET state='DONE', completedAt=:now, updatedAt=:now, version=version+1 WHERE workspaceId=:workspaceId AND id=:id AND version=:version AND deletedAt IS NULL AND state NOT IN ('DONE','CANCELLED')")
    suspend fun completeTask(workspaceId: String, id: String, version: Long, now: Long): Int
}
