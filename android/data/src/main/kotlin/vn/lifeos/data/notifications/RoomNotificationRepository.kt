package vn.lifeos.data.notifications

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.IntegrationConnectionEntity
import vn.lifeos.data.finance.imports.RoomFinanceInboxRepository
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.FinanceSourceKind
import vn.lifeos.domain.finance.imports.InboundFinanceEvidence
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome
import kotlinx.coroutines.CancellationException

data class FilteredNotification(val packageName: String, val keyHash: String, val postTime: Long,
    val contentHash: String, val title: String?, val body: String?, val contentUnavailable: Boolean)

/** Only filtered, bounded content crosses this boundary. Grant and allowlist are checked again at commit. */
class RoomNotificationRepository(private val database: AgentLifeDatabase, private val clock: Clock,
    private val ids: IdGenerator, private val accessGranted: () -> Boolean) {
    private val dao = database.notificationDao()
    suspend fun selectPackages(workspace: Uuid, packages: Set<String>, enabled: Boolean = true): Outcome<Unit> = safe {
        require(packages.all { it.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) })
        val device = dao.deviceId(workspace.value) ?: throw Abort(Outcome.Failure(AppError.NotFound))
        if (database.foundationDao().activeDevice(workspace.value, device) == null) throw Abort(Outcome.Failure(AppError.NotFound))
        var connection = dao.connection(workspace.value, device)
        if (connection == null) {
            val now = clock.millis()
            connection = IntegrationConnectionEntity(ids.newId().value, workspace.value, "NOTIFICATION",
                accountKey = device, scopes = "notification_access:selected_packages", configVersion = 1,
                state = "NOT_CONNECTED", notificationDeviceId = device, createdAt = now, updatedAt = now, version = 1, originDeviceId = device)
            database.foundationDao().insertConnection(connection)
        }
        dao.disableApps(workspace.value, device)
        packages.forEach { dao.putApp(NotificationAppEntity(workspace.value, device, it, connection.id, enabled)) }
        dao.clearDisabledPayloads(workspace.value, device)
        dao.setState(workspace.value, connection.id, if (!enabled || packages.isEmpty()) "NOT_CONNECTED"
            else if (accessGranted()) "PARTIAL" else "PERMISSION_DENIED", clock.millis())
        if (!enabled || packages.isEmpty()) dao.clearConnectionPayloads(workspace.value, connection.id)
    }

    suspend fun updatePermission(workspace: Uuid) = safe {
        val device = dao.deviceId(workspace.value) ?: return@safe
        val connection = dao.connection(workspace.value, device) ?: return@safe
        if (connection.state == "NOT_CONNECTED") return@safe
        dao.setState(workspace.value, connection.id, if (accessGranted()) "PARTIAL" else "PERMISSION_DENIED", clock.millis())
        if (!accessGranted()) dao.clearConnectionPayloads(workspace.value, connection.id)
    }

    suspend fun capture(workspace: Uuid, input: FilteredNotification): Outcome<Boolean> = safe {
        if (!accessGranted()) return@safe false
        val device = dao.deviceId(workspace.value) ?: return@safe false
        if (database.foundationDao().activeDevice(workspace.value, device) == null) return@safe false
        val allowed = dao.allowed(workspace.value, device, input.packageName) ?: return@safe false
        val connection = database.privacyDao().connection(workspace.value, allowed.connectionId) ?: return@safe false
        if (connection.state !in setOf("PARTIAL", "OK", "CONNECTED") || connection.provider != "NOTIFICATION" ||
            connection.notificationDeviceId != device || !accessGranted()) return@safe false
        require(input.title == null || input.title.length <= 256)
        require(input.body == null || input.body.length <= 4096)
        require(input.keyHash.matches(Regex("[a-f0-9]{64}")) && input.contentHash.matches(Regex("[a-f0-9]{64}")))
        require(input.postTime >= 0)
        dao.purgeExpired(clock.millis())
        if (dao.existing(workspace.value, device, input.keyHash, input.postTime, input.contentHash) != null) return@safe false
        val row = NotificationCaptureEntity(ids.newId().value, workspace.value, connection.id, device, input.packageName,
            input.keyHash, input.postTime, input.contentHash, Math.addExact(dao.revision(workspace.value, device, input.keyHash), 1),
            if (input.contentUnavailable) "MISSING_CONTENT" else "WAITING_PARSER", clock.millis(),
            input.title.takeUnless { input.contentUnavailable }, input.body.takeUnless { input.contentUnavailable }, input.contentUnavailable,
            if (input.contentUnavailable) null else clock.instant().plusSeconds(900).toEpochMilli())
        dao.insertCapture(row)
        // Capture-only adapter v1: parsing belongs to NOTI-02. Never infer amount=0 or transaction time from post time.
        val result = RoomFinanceInboxRepository(database, clock, ids).ingest(InboundFinanceEvidence(ids.newId(), ids.newId(), workspace,
            Uuid.parse(connection.id), FinanceSourceKind.NOTIFICATION, "${input.keyHash}:${input.postTime}",
            "${input.contentHash}:capture-v1", null, null, Instant.ofEpochMilli(row.receivedAt), 1,
            null, null, null, null, null, null,
            "amountMinor,currency,direction,occurredAt,proposedAccountId,proposedCategoryId", 0))
        if (result is Outcome.Failure) throw Abort(result)
        dao.setState(workspace.value, connection.id, "PARTIAL", clock.millis())
        true
    }
    private class Abort(val failure: Outcome.Failure) : RuntimeException()
    private suspend fun <T> safe(block: suspend () -> T): Outcome<T> = try { Outcome.Success(database.withTransaction { block() }) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Abort) { error.failure }
    catch (_: IllegalArgumentException) { Outcome.Failure(AppError.InvalidInput("notification")) }
    catch (_: ArithmeticException) { Outcome.Failure(AppError.InvalidInput("notification")) }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
}
