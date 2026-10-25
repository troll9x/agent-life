package vn.lifeos.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider

/** Composition root. UI receives state from a ViewModel, never a database handle. */
class AppContainer(context: Context, databaseProvider: EncryptedDatabaseProvider) {
    val startupRepository: StartupRepository = EncryptedStartupRepository(databaseProvider)
    val appearanceRepository: AppearanceRepository = AppearanceRepository(context.applicationContext)
    val networkStatus: Flow<Boolean> = NetworkStatusRepository(context.applicationContext).online
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
