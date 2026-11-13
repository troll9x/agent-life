package vn.lifeos.data.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import vn.lifeos.data.db.IntegrationConnectionEntity

data class NotificationCaptureStats(val lastReceivedAt: Long?, val captureCount: Int)

@Dao
interface NotificationDao {
    @Query("SELECT a.workspaceId,a.deviceId,a.packageName,a.connectionId,c.state FROM notification_apps a JOIN integration_connections c ON c.workspaceId=a.workspaceId AND c.id=a.connectionId JOIN workspaces w ON w.id=a.workspaceId JOIN devices d ON d.workspaceId=a.workspaceId AND d.id=a.deviceId WHERE a.enabled=1 AND w.deletedAt IS NULL AND c.deletedAt IS NULL AND d.deletedAt IS NULL AND d.revokedAt IS NULL")
    fun observeGate(): Flow<List<NotificationGateRow>>
    @Query("SELECT c.* FROM integration_connections c WHERE c.workspaceId=:workspace AND c.provider='NOTIFICATION' AND c.notificationDeviceId=:device AND c.deletedAt IS NULL LIMIT 1")
    suspend fun connection(workspace: String, device: String): IntegrationConnectionEntity?
    @Query("SELECT originDeviceId FROM workspaces WHERE id=:workspace AND deletedAt IS NULL")
    suspend fun deviceId(workspace: String): String?
    @Query("SELECT * FROM notification_apps WHERE workspaceId=:workspace AND deviceId=:device AND packageName=:pkg AND enabled=1")
    suspend fun allowed(workspace: String, device: String, pkg: String): NotificationAppEntity?
    @Query("SELECT * FROM notification_apps WHERE workspaceId=:workspace AND deviceId=:device AND enabled=1")
    suspend fun apps(workspace: String, device: String): List<NotificationAppEntity>
    @Query("UPDATE notification_apps SET enabled=0 WHERE workspaceId=:workspace AND deviceId=:device")
    suspend fun disableApps(workspace: String, device: String)
    @Query("UPDATE notification_captures SET title=NULL,body=NULL,payloadExpiresAt=NULL,processingState='DISABLED' WHERE workspaceId=:workspace AND deviceId=:device AND packageName NOT IN (SELECT packageName FROM notification_apps WHERE workspaceId=:workspace AND deviceId=:device AND enabled=1)")
    suspend fun clearDisabledPayloads(workspace: String, device: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putApp(row: NotificationAppEntity)
    @Insert suspend fun insertCapture(row: NotificationCaptureEntity)
    @Query("SELECT * FROM notification_captures WHERE workspaceId=:workspace AND deviceId=:device ORDER BY receivedAt DESC")
    fun observeCaptures(workspace: String, device: String): Flow<List<NotificationCaptureEntity>>
    @Query("SELECT MAX(receivedAt) AS lastReceivedAt, COUNT(*) AS captureCount FROM notification_captures WHERE workspaceId=:workspace AND deviceId=:device")
    fun observeStats(workspace: String, device: String): Flow<NotificationCaptureStats>
    @Query("SELECT * FROM notification_captures WHERE workspaceId=:workspace AND deviceId=:device AND notificationKey=:key AND postTime=:postTime AND contentHash=:hash LIMIT 1")
    suspend fun existing(workspace: String, device: String, key: String, postTime: Long, hash: String): NotificationCaptureEntity?
    @Query("SELECT COALESCE(MAX(revision),0) FROM notification_captures WHERE workspaceId=:workspace AND deviceId=:device AND notificationKey=:key")
    suspend fun revision(workspace: String, device: String, key: String): Int
    @Query("UPDATE integration_connections SET state=:state,lastAttemptAt=:now,updatedAt=:now,version=version+1 WHERE id=:id AND workspaceId=:workspace AND deletedAt IS NULL")
    suspend fun setState(workspace: String, id: String, state: String, now: Long)
    @Query("UPDATE notification_captures SET title=NULL,body=NULL,payloadExpiresAt=NULL,processingState='EXPIRED' WHERE payloadExpiresAt<=:now")
    suspend fun purgeExpired(now: Long): Int
    @Query("SELECT * FROM notification_captures WHERE id=:id AND workspaceId=:workspace AND payloadExpiresAt>:now AND processingState='WAITING_PARSER'")
    suspend fun pendingPayload(workspace: String, id: String, now: Long): NotificationCaptureEntity?
    @Query("UPDATE notification_captures SET title=NULL,body=NULL,payloadExpiresAt=NULL,processingState='PROCESSED' WHERE id=:id AND workspaceId=:workspace")
    suspend fun acknowledge(workspace: String, id: String)
    @Query("UPDATE notification_captures SET title=NULL,body=NULL,payloadExpiresAt=NULL,processingState='DISCONNECTED' WHERE workspaceId=:workspace AND connectionId=:connection")
    suspend fun clearConnectionPayloads(workspace: String, connection: String)
    @Query("DELETE FROM notification_captures WHERE workspaceId=:workspace AND connectionId=:connection")
    suspend fun deleteConnectionCaptures(workspace: String, connection: String)
    @Query("DELETE FROM notification_apps WHERE workspaceId=:workspace AND connectionId=:connection")
    suspend fun deleteConnectionApps(workspace: String, connection: String)
    @Query("DELETE FROM notification_captures WHERE workspaceId=:workspace")
    suspend fun deleteWorkspaceCaptures(workspace: String)
    @Query("DELETE FROM notification_apps WHERE workspaceId=:workspace")
    suspend fun deleteWorkspaceApps(workspace: String)
}
