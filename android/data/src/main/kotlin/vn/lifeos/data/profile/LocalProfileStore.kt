package vn.lifeos.data.profile

import android.content.Context
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.IntegrationConnectionEntity
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.foundation.Profile
import vn.lifeos.domain.foundation.UpdateProfile

data class ConnectionSummary(
    val id: Uuid,
    val provider: String,
    val state: String,
    val scopes: List<String>,
    val lastSuccessfulAt: Long?,
)

data class LocalProfileSnapshot(val profile: Profile, val connections: List<ConnectionSummary>)

data class ProfileInput(val displayName: String, val zoneId: String, val baseCurrency: String, val budgetStartDay: Int)

/** Bridges the active personal workspace to the existing FoundationRepository. */
class LocalProfileStore(
    database: AgentLifeDatabase,
    context: Context,
    clock: Clock,
    private val ids: IdGenerator,
) {
    private val dao = database.foundationDao()
    private val foundation = RoomFoundationRepository(database, clock, ids)
    private val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

    fun observe(): Flow<Outcome<LocalProfileSnapshot?>> = dao.observeActiveWorkspace().flatMapLatest { workspace ->
        if (workspace == null) flowOf(Outcome.Success(null))
        else combine(
            foundation.observeProfile(Uuid.parse(workspace.id)),
            dao.observeConnections(workspace.id),
        ) { profileResult, connections ->
            when (profileResult) {
                is Outcome.Failure -> profileResult
                is Outcome.Success -> profileResult.value?.let { profile ->
                    Outcome.Success(LocalProfileSnapshot(profile, connections.map { it.toSummary() }))
                } ?: Outcome.Failure(AppError.Storage)
            }
        }
    }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    suspend fun complete(input: ProfileInput): Outcome<Profile> {
        existingProfile()?.let { return Outcome.Success(it) }
        val command = CreatePersonalWorkspace(
            workspaceId = ids.newId(), profileId = ids.newId(), deviceId = ids.newId(), operationId = ids.newId(),
            workspaceName = "Personal", displayName = input.displayName, zoneId = input.zoneId,
            baseCurrency = input.baseCurrency, budgetStartDay = input.budgetStartDay,
            platform = "ANDROID", appVersion = appVersion,
        )
        val result = foundation.createPersonalWorkspace(command)
        if (result == Outcome.Failure(AppError.Conflict)) {
            existingProfile()?.let { return Outcome.Success(it) }
        }
        return result
    }

    suspend fun update(profile: Profile, input: ProfileInput): Outcome<Profile> = foundation.updateProfile(
        UpdateProfile(profile.workspaceId, profile.id, ids.newId(), profile.version,
            input.displayName, input.zoneId, input.baseCurrency, input.budgetStartDay),
    )

    private suspend fun existingProfile(): Profile? {
        val workspace = dao.activeWorkspace() ?: return null
        return (foundation.observeProfile(Uuid.parse(workspace.id)).first() as? Outcome.Success)?.value
    }

    private fun IntegrationConnectionEntity.toSummary() = ConnectionSummary(
        Uuid.parse(id), provider, state, scopes.split(' ').filter(String::isNotBlank), lastSuccessfulAt,
    )
}
