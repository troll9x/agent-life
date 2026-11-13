package vn.lifeos.app.banknotifications

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.lifeos.data.notifications.NotificationGateRow
import vn.lifeos.data.notifications.RoomNotificationRepository
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Outcome

internal data class NotificationAppChoice(val packageName: String, val label: String)
internal data class BankNotificationState(val loading: Boolean = true, val permission: Boolean = false,
    val connected: Boolean = false, val selected: Set<String> = emptySet(), val apps: List<NotificationAppChoice> = emptyList(),
    val lastReceivedAt: Long? = null, val captureCount: Int = 0, val error: Boolean = false,
    val sourceState: String = "NOT_CONNECTED")

/** Application-owned IO scope: capture does not depend on an Activity, Compose, or a ViewModel. */
class BankNotificationRuntime(private val context: Context, private val provider: EncryptedDatabaseProvider,
    private val clock: Clock) {
    private val started = AtomicBoolean()
    private val gate = AtomicReference<List<NotificationGateRow>>(emptyList())
    private val mutable = MutableStateFlow(BankNotificationState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ ->
        gate.set(emptyList()); mutable.update { it.copy(loading = false, error = true) }
    })
    internal val state = mutable.asStateFlow()
    private data class Envelope(val workspace: Uuid, val packageName: String, val key: String, val postTime: Long,
        val title: String?, val body: String?)
    private val queue = Channel<Envelope>(32)
    private val component = ComponentName(context, BankNotificationListenerService::class.java)
    private fun granted(): Boolean = context.getSystemService(NotificationManager::class.java)
        .isNotificationListenerAccessGranted(component)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                val access = provider.open()
                if (access !is DatabaseAccess.Ready) { started.set(false); mutable.update { it.copy(loading = false, error = true) }; return@launch }
                val database = access.database
                val repository = RoomNotificationRepository(database, clock, RandomIdGenerator, ::granted)
                launch {
                    for (envelope in queue) {
                        val payload = NotificationFilter.filter(envelope.packageName, setOf(envelope.packageName), false,
                            envelope.key, envelope.postTime) { envelope.title to envelope.body } ?: continue
                        when (repository.capture(envelope.workspace, payload)) {
                            is Outcome.Failure -> mutable.update { it.copy(error = true) }
                            is Outcome.Success -> Unit
                        }
                    }
                }
                launch {
                    combine(database.notificationDao().observeGate(), database.foundationDao().observeActiveWorkspace()) { rows, workspace ->
                        rows.filter { it.workspaceId == workspace?.id }
                    }.collect { current ->
                        gate.set(current)
                        mutable.update { it.copy(selected = current.map { row -> row.packageName }.toSet(), loading = false,
                            sourceState = current.firstOrNull()?.state ?: "NOT_CONNECTED") }
                    }
                }
                launch {
                    database.foundationDao().observeActiveWorkspace().collectLatest { workspace ->
                        mutable.update { it.copy(lastReceivedAt = null, captureCount = 0) }
                        if (workspace != null) {
                            repository.updatePermission(Uuid.parse(workspace.id))
                            database.notificationDao().observeStats(workspace.id, workspace.originDeviceId).collect { stats ->
                                // Aggregate metadata only: raw payloads are never loaded for the UI.
                                mutable.update { it.copy(lastReceivedAt = stats.lastReceivedAt, captureCount = stats.captureCount) }
                            }
                        }
                    }
                }
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                val apps = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                    .map { NotificationAppChoice(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
                    .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
                mutable.update { it.copy(apps = apps, permission = granted(), loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { started.set(false); gate.set(emptyList()); mutable.update { it.copy(loading = false, error = true) } }
        }
    }

    internal fun select(packages: Set<String>) {
        // Close the in-memory gate immediately while configuration is being changed.
        gate.set(emptyList())
        scope.launch {
            val access = provider.open()
            if (access !is DatabaseAccess.Ready) { mutable.update { it.copy(error = true) }; return@launch }
            val workspace = access.database.foundationDao().activeWorkspace() ?: return@launch
            val result = RoomNotificationRepository(access.database, clock, RandomIdGenerator, ::granted)
                .selectPackages(Uuid.parse(workspace.id), packages)
            mutable.update { it.copy(error = result is Outcome.Failure) }
        }
    }

    fun refreshAccess() {
        start()
        val permission = granted()
        if (!permission) gate.set(emptyList())
        mutable.update { it.copy(permission = permission, connected = it.connected && permission) }
        scope.launch {
            val access = provider.open()
            if (access !is DatabaseAccess.Ready) { mutable.update { it.copy(error = true) }; return@launch }
            val workspace = access.database.foundationDao().activeWorkspace() ?: return@launch
            val result = RoomNotificationRepository(access.database, clock, RandomIdGenerator, ::granted)
                .updatePermission(Uuid.parse(workspace.id))
            if (result is Outcome.Failure) mutable.update { it.copy(error = true) }
        }
    }

    fun connectionChanged(connected: Boolean) {
        start()
        mutable.update { it.copy(connected = connected) }
        refreshAccess()
    }

    internal fun reconnect() {
        refreshAccess()
        if (granted() && !mutable.value.connected) NotificationListenerService.requestRebind(component)
    }

    fun receive(sbn: StatusBarNotification) {
        // Check only OS metadata first. Never touch extras for an unselected sender.
        val rows = gate.get().filter { it.packageName == sbn.packageName && it.state in setOf("PARTIAL", "CONNECTED", "OK") }
        if (rows.size != 1 || !granted()) return
        val notification = sbn.notification
        val summary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        if (summary) return
        // A bounded snapshot is all the main-thread callback does. Hashing/validation/SQL run on IO.
        val title: String?
        val body: String?
        try {
            title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.take(256)?.toString()
            body = (notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: notification.extras.getCharSequence(Notification.EXTRA_TEXT))?.take(4096)?.toString()
        } catch (_: RuntimeException) { mutable.update { it.copy(error = true) }; return }
        val row = rows.single()
        if (!queue.trySend(Envelope(Uuid.parse(row.workspaceId), sbn.packageName, sbn.key, sbn.postTime, title, body)).isSuccess)
            mutable.update { it.copy(error = true) }
    }
}
