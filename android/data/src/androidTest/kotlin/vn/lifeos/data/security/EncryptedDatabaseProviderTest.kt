package vn.lifeos.data.security

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.UserManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseProviderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun ordinarySqliteCannotReadCiphertextAndBackgroundReopenKeepsProfile() = runBlocking {
        val name = "cipher-reopen.db"
        val alias = "agent_life_test_reopen"
        val wrapName = "cipher-reopen.wrap"
        resetFixture(name, alias, wrapName)
        val provider = EncryptedDatabaseProvider(context, name, alias, wrapName)
        val first = provider.open() as DatabaseAccess.Ready
        val repo = RoomFoundationRepository(first.database, Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC), RandomIdGenerator)
        assertTrue(repo.createPersonalWorkspace(CreatePersonalWorkspace(id(W), id(P), id(D), id(O), "Personal", "Fixture User", "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        provider.close()

        val file = context.getDatabasePath(name)
        assertTrue(file.exists())
        assertFalse(file.inputStream().use { stream -> stream.readNBytes(16).contentEquals("SQLite format 3\u0000".toByteArray()) })
        var plaintextReadFailed = false
        try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery("SELECT displayName FROM profiles", null).use { it.moveToFirst() }
            }
        } catch (_: SQLiteException) {
            plaintextReadFailed = true
        }
        assertTrue("The platform SQLite engine must reject the encrypted file", plaintextReadFailed)

        assertTrue(context.getSystemService(UserManager::class.java).isUserUnlocked)
        val reopened = EncryptedDatabaseProvider(context, name, alias, wrapName)
        val profile = withContext(Dispatchers.IO) {
            val ready = reopened.open() as DatabaseAccess.Ready
            RoomFoundationRepository(ready.database, Clock.systemUTC(), RandomIdGenerator).observeProfile(id(W)).first()
        } as Outcome.Success
        assertEquals("Fixture User", profile.value?.displayName)
        reopened.close()
    }

    @Test fun missingKeystoreAliasRequiresRecoveryAndPreservesDatabase() = runBlocking {
        val name = "cipher-key-loss.db"
        val alias = "agent_life_test_key_loss"
        val wrapName = "cipher-key-loss.wrap"
        resetFixture(name, alias, wrapName)
        val provider = EncryptedDatabaseProvider(context, name, alias, wrapName)
        val first = provider.open() as DatabaseAccess.Ready
        val repo = RoomFoundationRepository(first.database, Clock.systemUTC(), RandomIdGenerator)
        assertTrue(repo.createPersonalWorkspace(CreatePersonalWorkspace(id(W), id(P), id(D), id(O), "Personal", "Fixture User", "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        provider.close()

        val databaseFile = context.getDatabasePath(name)
        val before = databaseFile.sha256()
        val wrappedFile = context.noBackupFilesDir.resolve(wrapName)
        assertTrue(wrappedFile.exists())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }

        val restarted = EncryptedDatabaseProvider(context, name, alias, wrapName)
        assertEquals(DatabaseAccess.RecoveryRequired, restarted.open())
        assertArrayEquals(before, databaseFile.sha256())
        assertTrue(wrappedFile.exists())
    }

    @Test fun existingPlaintextDatabaseIsNeverSilentlyReplaced() = runBlocking {
        val name = "cipher-legacy-plaintext.db"
        val alias = "agent_life_test_plaintext"
        val wrapName = "cipher-legacy-plaintext.wrap"
        resetFixture(name, alias, wrapName)
        context.openOrCreateDatabase(name, 0, null).use { it.execSQL("CREATE TABLE fixture(id INTEGER PRIMARY KEY)") }
        val file = context.getDatabasePath(name)
        val before = file.sha256()
        assertEquals(DatabaseAccess.RecoveryRequired, EncryptedDatabaseProvider(context, name, alias, wrapName).open())
        assertArrayEquals(before, file.sha256())
        assertFalse(context.noBackupFilesDir.resolve(wrapName).exists())
    }

    @Test fun missingDatabaseWithWrappedKeyRequiresRecovery() = runBlocking {
        val name = "cipher-missing-db.db"
        val alias = "agent_life_test_missing_db"
        val wrapName = "cipher-missing-db.wrap"
        resetFixture(name, alias, wrapName)
        val provider = EncryptedDatabaseProvider(context, name, alias, wrapName)
        assertTrue(provider.open() is DatabaseAccess.Ready)
        provider.close()
        val file = context.getDatabasePath(name)
        assertTrue(file.exists())
        assertTrue(file.delete())
        val wrappedFile = context.noBackupFilesDir.resolve(wrapName)
        val before = wrappedFile.sha256()
        assertEquals(DatabaseAccess.RecoveryRequired, EncryptedDatabaseProvider(context, name, alias, wrapName).open())
        assertFalse(file.exists())
        assertArrayEquals(before, wrappedFile.sha256())
    }

    private fun resetFixture(name: String, alias: String, wrapName: String) {
        context.deleteDatabase(name)
        context.noBackupFilesDir.resolve(wrapName).delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) }
    }

    private fun java.io.File.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(readBytes())
    private fun id(value: String) = Uuid.parse(value)

    private companion object {
        const val W = "00000000-0000-0000-0000-000000000001"
        const val P = "00000000-0000-0000-0000-000000000002"
        const val D = "00000000-0000-0000-0000-000000000003"
        const val O = "00000000-0000-0000-0000-000000000004"
    }
}
