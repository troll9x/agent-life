package vn.lifeos.domain.foundation

import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.Uuid

sealed interface AppError {
    data class InvalidInput(val field: String) : AppError
    data object Conflict : AppError
    data object NotFound : AppError
    data object Storage : AppError
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

data class Profile(
    val id: Uuid,
    val workspaceId: Uuid,
    val displayName: String,
    val zoneId: String,
    val baseCurrency: String,
    val budgetStartDay: Int,
    val version: Long,
)

data class Connection(
    val id: Uuid,
    val workspaceId: Uuid,
    val provider: String,
    val externalAccountId: String?,
    val tenantId: String?,
    val state: String,
    val version: Long,
)

data class Checkpoint(
    val id: Uuid,
    val connectionId: Uuid,
    val resourceKey: String,
    val partialCursor: String?,
    val completedCursor: String?,
    val version: Long,
)

data class CreatePersonalWorkspace(
    val workspaceId: Uuid,
    val profileId: Uuid,
    val deviceId: Uuid,
    val operationId: Uuid,
    val workspaceName: String,
    val displayName: String,
    val zoneId: String,
    val baseCurrency: String,
    val budgetStartDay: Int,
    val platform: String,
    val appVersion: String,
)

data class UpdateProfile(
    val workspaceId: Uuid,
    val profileId: Uuid,
    val operationId: Uuid,
    val expectedVersion: Long,
    val displayName: String,
    val zoneId: String,
    val baseCurrency: String,
    val budgetStartDay: Int,
)

data class RegisterConnection(
    val id: Uuid,
    val workspaceId: Uuid,
    val operationId: Uuid,
    val originDeviceId: Uuid,
    val provider: String,
    val externalAccountId: String?,
    val tenantId: String?,
    val scopes: Set<String>,
    val configVersion: Int,
    val state: String,
    val notificationDeviceId: Uuid? = null,
)

data class UpdateConnectionState(
    val workspaceId: Uuid,
    val connectionId: Uuid,
    val operationId: Uuid,
    val expectedVersion: Long,
    val state: String,
    val lastAttemptAt: Long?,
    val lastSuccessfulAt: Long?,
)

data class SaveCheckpoint(
    val workspaceId: Uuid,
    val connectionId: Uuid,
    val resourceKey: String,
    val operationId: Uuid,
    val originDeviceId: Uuid,
    val expectedVersion: Long?,
    val partialCursor: String?,
    val completedCursor: String?,
    val coveredFrom: Long?,
    val coveredTo: Long?,
)

interface FoundationRepository {
    fun observeProfile(workspaceId: Uuid): Flow<Outcome<Profile?>>
    fun observeConnections(workspaceId: Uuid): Flow<Outcome<List<Connection>>>
    fun observeCheckpoints(connectionId: Uuid): Flow<Outcome<List<Checkpoint>>>
    suspend fun createPersonalWorkspace(command: CreatePersonalWorkspace): Outcome<Profile>
    suspend fun updateProfile(command: UpdateProfile): Outcome<Profile>
    suspend fun registerConnection(command: RegisterConnection): Outcome<Connection>
    suspend fun updateConnectionState(command: UpdateConnectionState): Outcome<Connection>
    suspend fun saveCheckpoint(command: SaveCheckpoint): Outcome<Checkpoint>
}
