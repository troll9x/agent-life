package vn.lifeos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoundationDao {
    @Query("SELECT * FROM workspaces WHERE id = :id AND deletedAt IS NULL")
    suspend fun workspace(id: String): WorkspaceEntity?

    @Query("SELECT COUNT(*) FROM workspaces WHERE deletedAt IS NULL")
    suspend fun activeWorkspaceCount(): Int

    @Query("SELECT * FROM workspaces WHERE deletedAt IS NULL ORDER BY createdAt, id LIMIT 1")
    suspend fun activeWorkspace(): WorkspaceEntity?

    @Query("SELECT * FROM workspaces WHERE deletedAt IS NULL ORDER BY createdAt, id LIMIT 1")
    fun observeActiveWorkspace(): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM devices WHERE id = :id AND workspaceId = :workspaceId AND deletedAt IS NULL AND revokedAt IS NULL")
    suspend fun activeDevice(workspaceId: String, id: String): DeviceEntity?

    @Query("SELECT * FROM profiles WHERE workspaceId = :workspaceId AND deletedAt IS NULL LIMIT 1")
    fun observeProfile(workspaceId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE workspaceId = :workspaceId AND deletedAt IS NULL LIMIT 1")
    suspend fun profile(workspaceId: String): ProfileEntity?

    @Query("SELECT * FROM integration_connections WHERE workspaceId = :workspaceId AND deletedAt IS NULL ORDER BY provider, id")
    fun observeConnections(workspaceId: String): Flow<List<IntegrationConnectionEntity>>

    @Query("SELECT * FROM integration_connections WHERE id = :id AND workspaceId = :workspaceId AND deletedAt IS NULL")
    suspend fun connection(workspaceId: String, id: String): IntegrationConnectionEntity?

    @Query("SELECT * FROM sync_checkpoints WHERE connectionId = :connectionId AND resourceKey = :resourceKey AND deletedAt IS NULL")
    suspend fun checkpoint(connectionId: String, resourceKey: String): SyncCheckpointEntity?

    @Query("SELECT * FROM sync_checkpoints WHERE connectionId = :connectionId AND deletedAt IS NULL ORDER BY resourceKey")
    fun observeCheckpoints(connectionId: String): Flow<List<SyncCheckpointEntity>>

    @Insert suspend fun insertWorkspace(entity: WorkspaceEntity)
    @Insert suspend fun insertProfile(entity: ProfileEntity)
    @Insert suspend fun insertDevice(entity: DeviceEntity)
    @Insert suspend fun insertConnection(entity: IntegrationConnectionEntity)
    @Insert suspend fun insertCheckpoint(entity: SyncCheckpointEntity)
    @Insert suspend fun insertAudit(entity: AuditEventEntity)

    @Query("UPDATE profiles SET displayName = :displayName, zoneId = :zoneId, baseCurrency = :baseCurrency, budgetStartDay = :budgetStartDay, updatedAt = :updatedAt, version = version + 1 WHERE id = :id AND workspaceId = :workspaceId AND version = :expectedVersion AND deletedAt IS NULL")
    suspend fun updateProfile(id: String, workspaceId: String, displayName: String, zoneId: String, baseCurrency: String, budgetStartDay: Int, updatedAt: Long, expectedVersion: Long): Int

    @Query("UPDATE integration_connections SET state = :state, lastAttemptAt = :lastAttemptAt, lastSuccessfulAt = :lastSuccessfulAt, updatedAt = :updatedAt, version = version + 1 WHERE id = :id AND workspaceId = :workspaceId AND version = :expectedVersion AND deletedAt IS NULL")
    suspend fun updateConnection(id: String, workspaceId: String, state: String, lastAttemptAt: Long?, lastSuccessfulAt: Long?, updatedAt: Long, expectedVersion: Long): Int

    @Query("UPDATE sync_checkpoints SET partialCursor = :partialCursor, completedCursor = :completedCursor, coveredFrom = :coveredFrom, coveredTo = :coveredTo, updatedAt = :updatedAt, version = version + 1 WHERE id = :id AND workspaceId = :workspaceId AND version = :expectedVersion AND deletedAt IS NULL")
    suspend fun updateCheckpoint(id: String, workspaceId: String, partialCursor: String?, completedCursor: String?, coveredFrom: Long?, coveredTo: Long?, updatedAt: Long, expectedVersion: Long): Int

    @Query("SELECT COUNT(*) FROM audit_events WHERE workspaceId = :workspaceId")
    suspend fun auditCount(workspaceId: String): Int
}
