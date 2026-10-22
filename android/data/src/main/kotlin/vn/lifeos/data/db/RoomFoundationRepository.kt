package vn.lifeos.data.db

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.*

/** Caller supplies a database opened by the encrypted storage provider (DATA-02). */
class RoomFoundationRepository(
    private val database: AgentLifeDatabase,
    private val clock: Clock,
    private val ids: IdGenerator,
) : FoundationRepository {
    private val dao = database.foundationDao()

    override fun observeProfile(workspaceId: Uuid): Flow<Outcome<Profile?>> =
        dao.observeProfile(workspaceId.value).map<ProfileEntity?, Outcome<Profile?>> { Outcome.Success(it?.toDomain()) }
            .catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override fun observeConnections(workspaceId: Uuid): Flow<Outcome<List<Connection>>> =
        dao.observeConnections(workspaceId.value).map<List<IntegrationConnectionEntity>, Outcome<List<Connection>>> {
            Outcome.Success(it.map(IntegrationConnectionEntity::toDomain))
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override fun observeCheckpoints(connectionId: Uuid): Flow<Outcome<List<Checkpoint>>> =
        dao.observeCheckpoints(connectionId.value).map<List<SyncCheckpointEntity>, Outcome<List<Checkpoint>>> {
            Outcome.Success(it.map(SyncCheckpointEntity::toDomain))
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun createPersonalWorkspace(command: CreatePersonalWorkspace): Outcome<Profile> = dbWrite {
        validateProfile(command.displayName, command.zoneId, command.baseCurrency, command.budgetStartDay)?.let { return@dbWrite Outcome.Failure(it) }
        if (command.workspaceName.isBlank()) return@dbWrite Outcome.Failure(AppError.InvalidInput("workspaceName"))
        if (dao.activeWorkspaceCount() != 0) return@dbWrite Outcome.Failure(AppError.Conflict)
        val now = clock.millis()
        val workspace = WorkspaceEntity(command.workspaceId.value, command.workspaceName.trim(), command.profileId.value, now, now, 1, command.deviceId.value)
        val profile = ProfileEntity(command.profileId.value, command.workspaceId.value, command.displayName.trim(), command.zoneId, command.baseCurrency, command.budgetStartDay, now, now, 1, command.deviceId.value)
        val device = DeviceEntity(command.deviceId.value, command.workspaceId.value, command.platform, command.appVersion, now, now, 1, command.deviceId.value)
        dao.insertWorkspace(workspace)
        dao.insertProfile(profile)
        dao.insertDevice(device)
        audit(command.workspaceId.value, "CREATE", "Profile", command.profileId.value, command.operationId.value, now)
        Outcome.Success(profile.toDomain())
    }

    override suspend fun updateProfile(command: UpdateProfile): Outcome<Profile> = dbWrite {
        validateProfile(command.displayName, command.zoneId, command.baseCurrency, command.budgetStartDay)?.let { return@dbWrite Outcome.Failure(it) }
        val before = dao.profile(command.workspaceId.value)
        if (before == null || before.id != command.profileId.value) return@dbWrite Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val changed = dao.updateProfile(before.id, before.workspaceId, command.displayName.trim(), command.zoneId, command.baseCurrency, command.budgetStartDay, now, command.expectedVersion)
        if (changed != 1) return@dbWrite Outcome.Failure(AppError.Conflict)
        audit(before.workspaceId, "UPDATE", "Profile", before.id, command.operationId.value, now)
        Outcome.Success(before.copy(displayName = command.displayName.trim(), zoneId = command.zoneId, baseCurrency = command.baseCurrency, budgetStartDay = command.budgetStartDay, updatedAt = now, version = before.version + 1).toDomain())
    }

    override suspend fun registerConnection(command: RegisterConnection): Outcome<Connection> = dbWrite {
        if (dao.workspace(command.workspaceId.value) == null) return@dbWrite Outcome.Failure(AppError.NotFound)
        if (dao.activeDevice(command.workspaceId.value, command.originDeviceId.value) == null) return@dbWrite Outcome.Failure(AppError.NotFound)
        if (command.provider.isBlank() || command.state.isBlank() || command.configVersion < 1 || command.externalAccountId?.isBlank() == true || command.tenantId?.isBlank() == true || command.scopes.any(String::isBlank)) {
            return@dbWrite Outcome.Failure(AppError.InvalidInput("connection"))
        }
        if (command.provider == "NOTIFICATION" && command.notificationDeviceId == null) return@dbWrite Outcome.Failure(AppError.InvalidInput("notificationDeviceId"))
        val notificationDeviceId = command.notificationDeviceId
        if (notificationDeviceId != null && dao.activeDevice(command.workspaceId.value, notificationDeviceId.value) == null) return@dbWrite Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val entity = IntegrationConnectionEntity(
            id = command.id.value, workspaceId = command.workspaceId.value, provider = command.provider,
            externalAccountId = command.externalAccountId, tenantId = command.tenantId,
            scopes = command.scopes.sorted().joinToString(" "), configVersion = command.configVersion, state = command.state,
            notificationDeviceId = command.notificationDeviceId?.value, createdAt = now, updatedAt = now,
            version = 1, originDeviceId = command.originDeviceId.value,
        )
        dao.insertConnection(entity)
        audit(entity.workspaceId, "CREATE", "IntegrationConnection", entity.id, command.operationId.value, now)
        Outcome.Success(entity.toDomain())
    }

    override suspend fun updateConnectionState(command: UpdateConnectionState): Outcome<Connection> = dbWrite {
        if (command.state.isBlank()) return@dbWrite Outcome.Failure(AppError.InvalidInput("state"))
        val before = dao.connection(command.workspaceId.value, command.connectionId.value) ?: return@dbWrite Outcome.Failure(AppError.NotFound)
        val now = clock.millis()
        val changed = dao.updateConnection(before.id, before.workspaceId, command.state, command.lastAttemptAt, command.lastSuccessfulAt, now, command.expectedVersion)
        if (changed != 1) return@dbWrite Outcome.Failure(AppError.Conflict)
        audit(before.workspaceId, "UPDATE_STATE", "IntegrationConnection", before.id, command.operationId.value, now)
        Outcome.Success(before.copy(state = command.state, lastAttemptAt = command.lastAttemptAt, lastSuccessfulAt = command.lastSuccessfulAt, updatedAt = now, version = before.version + 1).toDomain())
    }

    override suspend fun saveCheckpoint(command: SaveCheckpoint): Outcome<Checkpoint> = dbWrite {
        if (command.resourceKey.isBlank()) return@dbWrite Outcome.Failure(AppError.InvalidInput("resourceKey"))
        if (dao.connection(command.workspaceId.value, command.connectionId.value) == null) return@dbWrite Outcome.Failure(AppError.NotFound)
        if (dao.activeDevice(command.workspaceId.value, command.originDeviceId.value) == null) return@dbWrite Outcome.Failure(AppError.NotFound)
        val before = dao.checkpoint(command.connectionId.value, command.resourceKey)
        val now = clock.millis()
        val saved = if (before == null) {
            if (command.expectedVersion != null) return@dbWrite Outcome.Failure(AppError.Conflict)
            val created = SyncCheckpointEntity(ids.newId().value, command.workspaceId.value, command.connectionId.value, command.resourceKey,
                command.partialCursor, command.completedCursor, command.coveredFrom, command.coveredTo, now, now, 1, command.originDeviceId.value)
            dao.insertCheckpoint(created)
            created
        } else {
            if (before.workspaceId != command.workspaceId.value || command.expectedVersion != before.version) return@dbWrite Outcome.Failure(AppError.Conflict)
            val changed = dao.updateCheckpoint(before.id, before.workspaceId, command.partialCursor, command.completedCursor, command.coveredFrom, command.coveredTo, now, before.version)
            if (changed != 1) return@dbWrite Outcome.Failure(AppError.Conflict)
            before.copy(partialCursor = command.partialCursor, completedCursor = command.completedCursor, coveredFrom = command.coveredFrom, coveredTo = command.coveredTo, updatedAt = now, version = before.version + 1)
        }
        audit(saved.workspaceId, "UPSERT", "SyncCheckpoint", saved.id, command.operationId.value, now)
        Outcome.Success(saved.toDomain())
    }

    private suspend fun audit(workspaceId: String, action: String, entityType: String, entityId: String, operationId: String, now: Long) {
        dao.insertAudit(AuditEventEntity(ids.newId().value, workspaceId, "LOCAL_USER", action, entityType, entityId, now, operationId))
    }

    private suspend fun <T> dbWrite(block: suspend () -> Outcome<T>): Outcome<T> = try {
        database.withTransaction { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: SQLiteConstraintException) {
        Outcome.Failure(AppError.Conflict)
    } catch (_: SQLiteException) {
        Outcome.Failure(AppError.Storage)
    }

    private fun validateProfile(name: String, zone: String, currency: String, startDay: Int): AppError? {
        if (name.isBlank()) return AppError.InvalidInput("displayName")
        if (startDay !in 1..28) return AppError.InvalidInput("budgetStartDay")
        if (!Regex("[A-Z]{3}").matches(currency)) return AppError.InvalidInput("baseCurrency")
        return try { ZoneId.of(zone); null } catch (_: Exception) { AppError.InvalidInput("zoneId") }
    }
}

private fun ProfileEntity.toDomain() = Profile(Uuid.parse(id), Uuid.parse(workspaceId), displayName, zoneId, baseCurrency, budgetStartDay, version)
private fun IntegrationConnectionEntity.toDomain() = Connection(Uuid.parse(id), Uuid.parse(workspaceId), provider, externalAccountId, tenantId, state, version)
private fun SyncCheckpointEntity.toDomain() = Checkpoint(Uuid.parse(id), Uuid.parse(connectionId), resourceKey, partialCursor, completedCursor, version)
