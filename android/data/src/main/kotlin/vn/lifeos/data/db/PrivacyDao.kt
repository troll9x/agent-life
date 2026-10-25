package vn.lifeos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrivacyDao {
    @Query("SELECT * FROM privacy_preferences WHERE workspaceId = :workspaceId AND sourceKind = :sourceKind")
    suspend fun preference(workspaceId: String, sourceKind: String): PrivacyPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPreference(value: PrivacyPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCapture(value: SourceCaptureEntity): Long

    @Query("SELECT * FROM source_captures WHERE id = :id AND workspaceId = :workspaceId")
    suspend fun capture(workspaceId: String, id: String): SourceCaptureEntity?

    @Query("UPDATE source_captures SET rawContent = NULL, rawExpiresAt = NULL WHERE rawContent IS NOT NULL AND rawExpiresAt <= :now")
    suspend fun purgeExpired(now: Long): Int

    @Query("UPDATE source_captures SET rawContent = NULL, rawExpiresAt = NULL WHERE workspaceId = :workspaceId AND connectionId = :connectionId AND rawContent IS NOT NULL")
    suspend fun clearConnectionRaw(workspaceId: String, connectionId: String): Int

    @Query("UPDATE source_captures SET rawContent = NULL, rawExpiresAt = NULL WHERE workspaceId = :workspaceId AND sourceKind = :sourceKind AND rawContent IS NOT NULL")
    suspend fun clearSourceRaw(workspaceId: String, sourceKind: String): Int

    @Query("DELETE FROM source_captures WHERE workspaceId = :workspaceId AND connectionId = :connectionId")
    suspend fun deleteConnectionCaptures(workspaceId: String, connectionId: String)

    @Query("DELETE FROM sync_checkpoints WHERE workspaceId = :workspaceId AND connectionId = :connectionId")
    suspend fun deleteConnectionCheckpoints(workspaceId: String, connectionId: String)

    @Query("SELECT * FROM integration_connections WHERE workspaceId = :workspaceId AND id = :connectionId AND deletedAt IS NULL")
    suspend fun connection(workspaceId: String, connectionId: String): IntegrationConnectionEntity?

    @Query("SELECT * FROM integration_connections WHERE id = :connectionId AND deletedAt IS NULL")
    suspend fun connectionById(connectionId: String): IntegrationConnectionEntity?

    @Query("SELECT * FROM integration_connections WHERE workspaceId = :workspaceId")
    suspend fun connections(workspaceId: String): List<IntegrationConnectionEntity>

    @Query("UPDATE integration_connections SET state = 'NOT_CONNECTED', updatedAt = :now, version = version + 1 WHERE workspaceId = :workspaceId AND id = :connectionId AND state != 'NOT_CONNECTED'")
    suspend fun markDisconnected(workspaceId: String, connectionId: String, now: Long): Int

    @Query("UPDATE integration_connections SET state = 'NOT_CONNECTED', updatedAt = :now, version = version + 1 WHERE workspaceId = :workspaceId AND state != 'NOT_CONNECTED'")
    suspend fun markWorkspaceDisconnected(workspaceId: String, now: Long)

    @Query("SELECT COUNT(*) FROM integration_connections WHERE id = :connectionId AND workspaceId = :workspaceId AND state IN ('CONNECTED', 'OK') AND deletedAt IS NULL")
    suspend fun activeConnection(workspaceId: String, connectionId: String): Int

    @Query("SELECT COUNT(*) FROM workspaces WHERE id = :workspaceId AND deletedAt IS NULL")
    suspend fun activeWorkspace(workspaceId: String): Int

    @Query("DELETE FROM source_captures WHERE workspaceId = :workspaceId")
    suspend fun deleteCaptures(workspaceId: String)
    @Query("DELETE FROM privacy_preferences WHERE workspaceId = :workspaceId")
    suspend fun deletePreferences(workspaceId: String)
    @Query("DELETE FROM sync_checkpoints WHERE workspaceId = :workspaceId")
    suspend fun deleteCheckpoints(workspaceId: String)
    @Query("DELETE FROM audit_events WHERE workspaceId = :workspaceId")
    suspend fun deleteAudits(workspaceId: String)
    @Query("DELETE FROM integration_connections WHERE workspaceId = :workspaceId")
    suspend fun deleteConnections(workspaceId: String)
    @Query("DELETE FROM devices WHERE workspaceId = :workspaceId")
    suspend fun deleteDevices(workspaceId: String)
    @Query("DELETE FROM profiles WHERE workspaceId = :workspaceId")
    suspend fun deleteProfiles(workspaceId: String)
    @Query("DELETE FROM workspaces WHERE id = :workspaceId")
    suspend fun deleteWorkspace(workspaceId: String)
}
