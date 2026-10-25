package vn.lifeos.domain.privacy

import vn.lifeos.domain.common.SourceKind
import vn.lifeos.domain.common.SourceRef
import vn.lifeos.domain.common.Uuid

enum class CachePolicy { MINIMAL, DIAGNOSTIC }

data class PrivacyPreference(
    val workspaceId: Uuid,
    val sourceKind: SourceKind,
    val allowRemoteProcessing: Boolean = false,
    val cachePolicy: CachePolicy = CachePolicy.MINIMAL,
    val rawRetentionDays: Int = 0,
) {
    init {
        require(rawRetentionDays in 0..7)
        require((cachePolicy == CachePolicy.DIAGNOSTIC) == (rawRetentionDays > 0))
    }
}

data class CaptureRecord(
    val id: Uuid,
    val workspaceId: Uuid,
    val connectionId: Uuid,
    val sourceKind: SourceKind,
    val sourceEventId: String,
    val processingState: String,
    val rawContent: String? = null,
)

sealed interface PrivacyResult<out T> {
    data class Success<T>(val value: T) : PrivacyResult<T>
    data object NotFound : PrivacyResult<Nothing>
    data object InvalidInput : PrivacyResult<Nothing>
    data object StorageFailure : PrivacyResult<Nothing>
    /** DB access is already disabled; retry revocation/cancellation before deleting metadata. */
    data object ExternalCleanupPending : PrivacyResult<Nothing>
}

/** All HTTP calls tied to a connection must consult this gate immediately before dispatch. */
fun interface ConnectionAccessGate {
    suspend fun isActive(source: SourceRef): Boolean
}

interface ConnectionLifecycle {
    suspend fun cancelWork(connectionId: Uuid)
    suspend fun unregister(connectionId: Uuid)
    suspend fun revokeCredentials(connectionId: Uuid)
}

interface PrivacyRepository {
    suspend fun preference(workspaceId: Uuid, sourceKind: SourceKind): PrivacyPreference
    suspend fun setPreference(preference: PrivacyPreference): PrivacyResult<Unit>
    suspend fun recordCapture(record: CaptureRecord): PrivacyResult<Unit>
    suspend fun purgeExpiredRaw(): PrivacyResult<Int>
    suspend fun disconnect(workspaceId: Uuid, connectionId: Uuid, deleteCache: Boolean): PrivacyResult<Unit>
    suspend fun deleteWorkspace(workspaceId: Uuid): PrivacyResult<Unit>
}
