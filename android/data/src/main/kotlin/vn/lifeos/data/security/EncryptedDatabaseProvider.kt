package vn.lifeos.data.security

import android.content.Context
import android.os.UserManager
import androidx.room.Room
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.FoundationMigrations
import vn.lifeos.data.planner.PlannerMigration

sealed interface DatabaseAccess {
    data class Ready(val database: AgentLifeDatabase) : DatabaseAccess
    data object UnlockDeviceFirst : DatabaseAccess
    data object RecoveryRequired : DatabaseAccess
    data object StorageUnavailable : DatabaseAccess
}

/** Shared by UI and future workers through the application container. Never opens plaintext Room. */
class EncryptedDatabaseProvider(
    context: Context,
    private val databaseName: String = "agent-life.db",
    keyAlias: String = "agent_life_db_wrap_v1",
    wrappedKeyFileName: String = "agent-life-db-key.wrap",
) {
    private val applicationContext = context.applicationContext
    private val keys = DatabaseKeyManager(applicationContext, keyAlias, wrappedKeyFileName)
    private val mutex = Mutex()
    private var opened: AgentLifeDatabase? = null
    private var openedPassphrase: ByteArray? = null

    suspend fun open(): DatabaseAccess = mutex.withLock {
        opened?.let { return@withLock DatabaseAccess.Ready(it) }
        withContext(Dispatchers.IO) {
            val users = applicationContext.getSystemService(UserManager::class.java)
            if (users?.isUserUnlocked != true) return@withContext DatabaseAccess.UnlockDeviceFirst
            var secret: ByteArray? = null
            var passphrase: ByteArray? = null
            try {
                System.loadLibrary("sqlcipher")
                val file = applicationContext.getDatabasePath(databaseName)
                secret = keys.loadOrCreate(file)
                // ASCII hex keeps the full random 256-bit secret valid as a SQLCipher passphrase.
                val encoded = secret.toHexAscii()
                passphrase = encoded
                val database = Room.databaseBuilder(applicationContext, AgentLifeDatabase::class.java, databaseName)
                    .openHelperFactory(SupportOpenHelperFactory(encoded))
                    .addMigrations(FoundationMigrations.from1To2, FoundationMigrations.from2To3, PlannerMigration.from3To4)
                    .build()
                try {
                    database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM sqlite_master").use { it.moveToFirst() }
                } catch (error: Exception) {
                    database.close()
                    throw error
                }
                opened = database
                openedPassphrase = encoded
                DatabaseAccess.Ready(database)
            } catch (error: CancellationException) {
                throw error
            } catch (_: DatabaseKeyUnavailable) {
                DatabaseAccess.RecoveryRequired
            } catch (_: GeneralSecurityException) {
                DatabaseAccess.RecoveryRequired
            } catch (_: IOException) {
                DatabaseAccess.RecoveryRequired
            } catch (_: RuntimeException) {
                DatabaseAccess.RecoveryRequired
            } catch (_: LinkageError) {
                DatabaseAccess.StorageUnavailable
            } finally {
                secret?.fill(0)
                if (opened == null) passphrase?.fill(0)
            }
        }
    }

    suspend fun close() = mutex.withLock {
        opened?.close()
        opened = null
        openedPassphrase?.fill(0)
        openedPassphrase = null
    }

    private fun ByteArray.toHexAscii(): ByteArray {
        val hex = "0123456789abcdef".toByteArray(Charsets.US_ASCII)
        return ByteArray(size * 2).also { encoded ->
            for (index in indices) {
                val value = this[index].toInt() and 0xff
                encoded[index * 2] = hex[value ushr 4]
                encoded[index * 2 + 1] = hex[value and 15]
            }
        }
    }
}
