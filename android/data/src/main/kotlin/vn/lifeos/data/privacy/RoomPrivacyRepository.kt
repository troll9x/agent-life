package vn.lifeos.data.privacy

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.AuditEventEntity
import vn.lifeos.data.db.PrivacyPreferenceEntity
import vn.lifeos.data.db.SourceCaptureEntity
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.SourceKind
import vn.lifeos.domain.common.SourceRef
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.privacy.CachePolicy
import vn.lifeos.domain.privacy.CaptureRecord
import vn.lifeos.domain.privacy.ConnectionAccessGate
import vn.lifeos.domain.privacy.ConnectionLifecycle
import vn.lifeos.domain.privacy.PrivacyPreference
import vn.lifeos.domain.privacy.PrivacyRepository
import vn.lifeos.domain.privacy.PrivacyResult

/** Uses the shared SQLCipher-opened Room database. No plaintext database fallback. */
class RoomPrivacyRepository(
    private val database: AgentLifeDatabase,
    context: Context,
    private val lifecycle: ConnectionLifecycle,
    private val clock: Clock,
    private val ids: IdGenerator,
) : PrivacyRepository {
    private val dao = database.privacyDao()
    private val files = WorkspaceFiles(context.applicationContext)

    override suspend fun preference(workspaceId: Uuid, sourceKind: SourceKind): PrivacyPreference {
        val saved = dao.preference(workspaceId.value, sourceKind.name)
        return saved?.let {
            PrivacyPreference(workspaceId, sourceKind, it.allowRemoteProcessing, CachePolicy.valueOf(it.cachePolicy), it.rawRetentionDays)
        } ?: PrivacyPreference(workspaceId, sourceKind)
    }

    override suspend fun setPreference(preference: PrivacyPreference): PrivacyResult<Unit> = dbResult {
        database.withTransaction {
            if (dao.activeWorkspace(preference.workspaceId.value) == 0) return@withTransaction PrivacyResult.NotFound
            dao.putPreference(PrivacyPreferenceEntity(preference.workspaceId.value, preference.sourceKind.name,
                preference.allowRemoteProcessing, preference.cachePolicy.name, preference.rawRetentionDays))
            if (preference.cachePolicy == CachePolicy.MINIMAL) {
                dao.clearSourceRaw(preference.workspaceId.value, preference.sourceKind.name)
            }
            audit(preference.workspaceId.value, "SET_PRIVACY", "PrivacyPreference", preference.sourceKind.name)
            PrivacyResult.Success(Unit)
        }
    }

    override suspend fun recordCapture(record: CaptureRecord): PrivacyResult<Unit> = dbResult {
        if (record.sourceEventId.isBlank() || record.processingState.isBlank() || (record.rawContent?.length ?: 0) > 16_384) {
            return@dbResult PrivacyResult.InvalidInput
        }
        database.withTransaction {
            val connection = dao.connection(record.workspaceId.value, record.connectionId.value)
                ?: return@withTransaction PrivacyResult.NotFound
            if (connection.state !in setOf("CONNECTED", "OK") || !providerMatches(record.sourceKind, connection.provider)) {
                return@withTransaction PrivacyResult.InvalidInput
            }
            val policy = preference(record.workspaceId, record.sourceKind)
            val raw = record.rawContent?.takeIf { policy.cachePolicy == CachePolicy.DIAGNOSTIC }
            val now = clock.millis()
            dao.insertCapture(SourceCaptureEntity(record.id.value, record.workspaceId.value, record.connectionId.value,
                record.sourceKind.name, record.sourceEventId, record.processingState, raw,
                raw?.let { now + policy.rawRetentionDays * 86_400_000L }, now))
            PrivacyResult.Success(Unit)
        }
    }

    override suspend fun purgeExpiredRaw(): PrivacyResult<Int> = dbResult {
        PrivacyResult.Success(dao.purgeExpired(clock.millis()))
    }

    override suspend fun disconnect(workspaceId: Uuid, connectionId: Uuid, deleteCache: Boolean): PrivacyResult<Unit> {
        val marked = dbResult {
            database.withTransaction {
                if (dao.connection(workspaceId.value, connectionId.value) == null) return@withTransaction PrivacyResult.NotFound
                val changed = dao.markDisconnected(workspaceId.value, connectionId.value, clock.millis())
                dao.clearConnectionRaw(workspaceId.value, connectionId.value)
                if (deleteCache) {
                    dao.deleteConnectionCaptures(workspaceId.value, connectionId.value)
                    dao.deleteConnectionCheckpoints(workspaceId.value, connectionId.value)
                }
                if (changed > 0) audit(workspaceId.value, "DISCONNECT", "IntegrationConnection", connectionId.value)
                PrivacyResult.Success(Unit)
            }
        }
        if (marked !is PrivacyResult.Success) return marked
        val cleaned = externalCleanup(connectionId)
        if (cleaned !is PrivacyResult.Success) return cleaned
        return if (deleteCache) try {
            withContext(Dispatchers.IO) { files.deleteConnection(workspaceId, connectionId) }
            PrivacyResult.Success(Unit)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { PrivacyResult.StorageFailure }
        else PrivacyResult.Success(Unit)
    }

    override suspend fun deleteWorkspace(workspaceId: Uuid): PrivacyResult<Unit> {
        val connections = try {
            database.withTransaction {
                val owned = dao.connections(workspaceId.value)
                dao.markWorkspaceDisconnected(workspaceId.value, clock.millis())
                owned.map { Uuid.parse(it.id) }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SQLiteException) { return PrivacyResult.StorageFailure }
        for (connectionId in connections) {
            val cleanup = externalCleanup(connectionId)
            if (cleanup !is PrivacyResult.Success) return cleanup
        }
        val deleted = dbResult {
            database.withTransaction {
                dao.deleteCaptures(workspaceId.value)
                dao.deletePreferences(workspaceId.value)
                dao.deleteCheckpoints(workspaceId.value)
                dao.deleteAudits(workspaceId.value)
                dao.deleteConnections(workspaceId.value)
                dao.deleteDevices(workspaceId.value)
                dao.deleteProfiles(workspaceId.value)
                dao.deleteWorkspace(workspaceId.value)
                PrivacyResult.Success(Unit)
            }
        }
        if (deleted !is PrivacyResult.Success) return deleted
        return try {
            withContext(Dispatchers.IO) { files.deleteWorkspace(workspaceId) }
            PrivacyResult.Success(Unit)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { PrivacyResult.StorageFailure }
    }

    private suspend fun externalCleanup(connectionId: Uuid): PrivacyResult<Unit> = try {
        lifecycle.cancelWork(connectionId)
        lifecycle.unregister(connectionId)
        lifecycle.revokeCredentials(connectionId)
        PrivacyResult.Success(Unit)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { PrivacyResult.ExternalCleanupPending }

    private suspend fun audit(workspaceId: String, action: String, type: String, entityId: String) {
        database.foundationDao().insertAudit(AuditEventEntity(ids.newId().value, workspaceId, "LOCAL_USER", action,
            type, entityId, clock.millis()))
    }

    private suspend fun <T> dbResult(block: suspend () -> PrivacyResult<T>): PrivacyResult<T> = try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: SQLiteException) { PrivacyResult.StorageFailure }
}

/** Fail closed: a missing, revoked, or cross-provider connection cannot dispatch HTTP. */
class RoomConnectionAccessGate(private val database: AgentLifeDatabase) : ConnectionAccessGate {
    override suspend fun isActive(source: SourceRef): Boolean {
        val id = source.connectionId ?: return false
        return try {
            val connection = database.privacyDao().connectionById(id.value) ?: return false
            connection.state in setOf("CONNECTED", "OK") && providerMatches(source.kind, connection.provider) &&
                database.privacyDao().activeWorkspace(connection.workspaceId) == 1
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SQLiteException) { false }
    }
}

private fun providerMatches(kind: SourceKind, provider: String): Boolean = when (kind) {
    SourceKind.MICROSOFT_MAIL, SourceKind.MICROSOFT_CALENDAR -> provider == "MICROSOFT"
    SourceKind.GOOGLE_MAIL, SourceKind.GOOGLE_CALENDAR -> provider == "GOOGLE"
    SourceKind.BANK_NOTIFICATION -> provider == "NOTIFICATION"
    else -> provider == kind.name
}

/** Only namespaced app-owned directories are touched; symbolic links are deleted, never followed. */
internal class WorkspaceFiles(private val context: Context) {
    fun deleteWorkspace(workspaceId: Uuid) {
        deleteRelative(workspaceId.value)
    }

    fun deleteConnection(workspaceId: Uuid, connectionId: Uuid) {
        deleteRelative("${workspaceId.value}/sources/${connectionId.value}")
    }

    private fun deleteRelative(relative: String) {
        val roots = listOfNotNull(context.filesDir, context.cacheDir, context.noBackupFilesDir, context.getExternalFilesDir(null))
        for (base in roots) {
            val root = base.toPath().toAbsolutePath().normalize().resolve("workspaces")
            val target = root.resolve(relative).normalize()
            check(target.startsWith(root) && target != root)
            if (Files.isSymbolicLink(root) || Files.isSymbolicLink(root.resolve(relative.substringBefore('/'))) ||
                Files.isSymbolicLink(root.resolve(relative.substringBefore('/') + "/sources"))) {
                throw IOException("Workspace path contains a symbolic link")
            }
            if (!Files.exists(target)) continue
            Files.walkFileTree(target, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }
                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) throw exc
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }
}
