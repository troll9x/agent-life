package vn.lifeos.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.time.Clock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.profile.LocalProfileStore
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.data.planner.RoomPlannerRepository
import vn.lifeos.app.planner.PlannerFeature
import kotlinx.coroutines.CoroutineScope
import vn.lifeos.app.reminders.ReminderEngine
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.domain.finance.FinanceRepository

/** Composition root. UI receives state from a ViewModel, never a database handle. */
class AppContainer(context: Context, databaseProvider: EncryptedDatabaseProvider) {
    private val applicationContext = context.applicationContext
    val clock: Clock = Clock.systemUTC()
    val startupRepository: StartupRepository = EncryptedStartupRepository(databaseProvider)
    val appearanceRepository: AppearanceRepository = AppearanceRepository(context.applicationContext)
    val networkStatus: Flow<Boolean> = NetworkStatusRepository(context.applicationContext).online
    private var reminders: ReminderEngine? = null
    @Synchronized internal fun reminderEngine(database: AgentLifeDatabase): ReminderEngine =
        reminders ?: ReminderEngine(applicationContext, database, clock).also { reminders = it }
    fun profileStore(database: AgentLifeDatabase): LocalProfileStore =
        LocalProfileStore(database, applicationContext, clock, RandomIdGenerator)
    fun financeRepository(database: AgentLifeDatabase): FinanceRepository =
        RoomFinanceRepository(database, clock, RandomIdGenerator)
    internal suspend fun plannerFeature(database: AgentLifeDatabase, workspaceId: Uuid, scope: CoroutineScope): PlannerFeature? {
        val repository = RoomPlannerRepository(database, clock)
        val deviceId = repository.workspaceDeviceId(workspaceId) ?: return null
        return PlannerFeature(repository, workspaceId, deviceId, RandomIdGenerator, clock, scope)
    }
}

fun interface StartupRepository {
    suspend fun open(): DatabaseAccess
}

private class EncryptedStartupRepository(private val provider: EncryptedDatabaseProvider) : StartupRepository {
    override suspend fun open(): DatabaseAccess = provider.open()
}

class AppearanceRepository(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun isDarkTheme(): Boolean = preferences.getBoolean("dark_theme", false)

    fun setDarkTheme(value: Boolean) {
        preferences.edit().putBoolean("dark_theme", value).apply()
    }
}

private class NetworkStatusRepository(context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val online: Flow<Boolean> = callbackFlow {
        fun currentStatus(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        trySend(currentStatus())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(currentStatus()) }
            override fun onLost(network: Network) { trySend(currentStatus()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(currentStatus())
            }
        }
        manager.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}
