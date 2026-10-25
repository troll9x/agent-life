package vn.lifeos.data.privacy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.SourceKind
import vn.lifeos.domain.common.SourceRef
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.foundation.RegisterConnection
import vn.lifeos.domain.foundation.SaveCheckpoint
import vn.lifeos.domain.privacy.CachePolicy
import vn.lifeos.domain.privacy.CaptureRecord
import vn.lifeos.domain.privacy.ConnectionLifecycle
import vn.lifeos.domain.privacy.PrivacyPreference
import vn.lifeos.domain.privacy.PrivacyResult

@RunWith(AndroidJUnit4::class)
class PrivacyRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC)

    @Test fun rawIsOptInEncryptedAndExpiredRawKeepsCandidateIdentity() = runBlocking {
        val fixture = fixture("raw")
        val db = fixture.open()
        val lifecycle = RecordingLifecycle()
        val repo = RoomPrivacyRepository(db, context, lifecycle, now, RandomIdGenerator)
        seed(db)
        db.openHelper.writableDatabase.query("PRAGMA secure_delete").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        assertFalse(repo.preference(id(W), SourceKind.MICROSOFT_MAIL).allowRemoteProcessing)
        assertFalse(repo.preference(id(W), SourceKind.HEALTH_CONNECT).allowRemoteProcessing)
        assertEquals(0, repo.preference(id(W), SourceKind.MICROSOFT_MAIL).rawRetentionDays)
        assertEquals(PrivacyResult.Success(Unit), repo.recordCapture(CaptureRecord(id(R1), id(W), id(A), SourceKind.MICROSOFT_MAIL,
            "event-1", "NEEDS_REVIEW", "fixture-private-body")))
        assertNull(db.privacyDao().capture(W, R1)?.rawContent)
        assertEquals(PrivacyResult.Success(Unit), repo.setPreference(PrivacyPreference(id(W), SourceKind.MICROSOFT_MAIL,
            cachePolicy = CachePolicy.DIAGNOSTIC, rawRetentionDays = 1)))
        assertEquals(PrivacyResult.Success(Unit), repo.recordCapture(CaptureRecord(id(R2), id(W), id(A), SourceKind.MICROSOFT_MAIL,
            "event-2", "NEEDS_REVIEW", "fixture-private-body")))
        assertEquals("fixture-private-body", db.privacyDao().capture(W, R2)?.rawContent)
        fixture.provider.close()
        assertFalse(context.getDatabasePath(fixture.name).readBytes().toString(Charsets.ISO_8859_1).contains("fixture-private-body"))

        val reopened = fixture.open()
        val tomorrow = Clock.fixed(Instant.parse("2026-10-27T00:00:00Z"), ZoneOffset.UTC)
        val cleaner = RoomPrivacyRepository(reopened, context, lifecycle, tomorrow, RandomIdGenerator)
        assertEquals(PrivacyResult.Success(1), cleaner.purgeExpiredRaw())
        val retained = reopened.privacyDao().capture(W, R2)
        assertNotNull(retained)
        assertNull(retained?.rawContent)
        assertNull(retained?.rawExpiresAt)
        assertEquals("event-2", retained?.sourceEventId)
        assertEquals("NEEDS_REVIEW", retained?.processingState)
        assertEquals(PrivacyResult.Success(0), cleaner.purgeExpiredRaw())
        assertEquals(PrivacyResult.Success(Unit), cleaner.recordCapture(CaptureRecord(id(R3), id(W), id(A), SourceKind.MICROSOFT_MAIL,
            "event-3", "PENDING", "fixture-private-body")))
        assertEquals("fixture-private-body", reopened.privacyDao().capture(W, R3)?.rawContent)
        assertEquals(PrivacyResult.Success(Unit), cleaner.setPreference(PrivacyPreference(id(W), SourceKind.MICROSOFT_MAIL)))
        assertNull(reopened.privacyDao().capture(W, R3)?.rawContent)
        fixture.provider.close()
    }

    @Test fun disconnectOneAccountLeavesOtherActiveAndCleanupIsIdempotent() = runBlocking {
        val fixture = fixture("disconnect")
        val db = fixture.open()
        val lifecycle = RecordingLifecycle()
        val repo = RoomPrivacyRepository(db, context, lifecycle, now, RandomIdGenerator)
        seed(db)
        val gate = RoomConnectionAccessGate(db)
        assertTrue(gate.isActive(SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-a", id(A))))
        assertTrue(gate.isActive(SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-b", id(B))))
        val aFile = context.cacheDir.resolve("workspaces/$W/sources/$A/cache.bin").apply { parentFile?.mkdirs(); writeText("a-cache") }
        val bFile = context.cacheDir.resolve("workspaces/$W/sources/$B/cache.bin").apply { parentFile?.mkdirs(); writeText("b-cache") }
        assertEquals(PrivacyResult.Success(Unit), repo.disconnect(id(W), id(A), true))
        assertFalse(gate.isActive(SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-a", id(A))))
        assertTrue(gate.isActive(SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-b", id(B))))
        assertNull(db.foundationDao().checkpoint(A, "mail"))
        assertNotNull(db.foundationDao().checkpoint(B, "mail"))
        assertFalse(aFile.exists())
        assertTrue(bFile.exists())
        val audits = db.foundationDao().auditCount(W)
        assertEquals(PrivacyResult.Success(Unit), repo.disconnect(id(W), id(A), true))
        assertEquals(audits, db.foundationDao().auditCount(W))
        assertEquals(listOf(id(A), id(A)), lifecycle.revoked)
        assertFalse(lifecycle.revoked.contains(id(B)))
        bFile.delete()
        bFile.parentFile?.delete()
        fixture.provider.close()
    }

    @Test fun workspaceDeleteOnlyTouchesNamedWorkspaceAndRepeatsSafely() = runBlocking {
        val fixture = fixture("workspace")
        val db = fixture.open()
        val lifecycle = RecordingLifecycle()
        val repo = RoomPrivacyRepository(db, context, lifecycle, now, RandomIdGenerator)
        seed(db)
        val ours = context.filesDir.resolve("workspaces/$W/cache.bin").apply { parentFile?.mkdirs(); writeText("fixture-cache") }
        val other = context.filesDir.resolve("workspaces/$OTHER/cache.bin").apply { parentFile?.mkdirs(); writeText("other-fixture") }
        assertEquals(PrivacyResult.Success(Unit), repo.deleteWorkspace(id(W)))
        assertNull(db.foundationDao().workspace(W))
        assertEquals(0, db.foundationDao().auditCount(W))
        assertFalse(ours.exists())
        assertTrue(other.exists())
        assertEquals(PrivacyResult.Success(Unit), repo.deleteWorkspace(id(W)))
        assertTrue(other.exists())
        other.delete()
        other.parentFile?.delete()
        fixture.provider.close()
    }

    @Test fun revocationFailureStillBlocksRequestsAndCanRetry() = runBlocking {
        val fixture = fixture("revoke")
        val db = fixture.open()
        val lifecycle = RecordingLifecycle().apply { failNextRevoke = true }
        val repo = RoomPrivacyRepository(db, context, lifecycle, now, RandomIdGenerator)
        seed(db)
        assertEquals(PrivacyResult.ExternalCleanupPending, repo.disconnect(id(W), id(A), false))
        val gate = RoomConnectionAccessGate(db)
        assertFalse(gate.isActive(SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-a", id(A))))
        assertTrue(gate.isActive(SourceRef(SourceKind.MICROSOFT_MAIL, "fixture-b", id(B))))
        assertEquals(PrivacyResult.Success(Unit), repo.disconnect(id(W), id(A), false))
        assertEquals(listOf(id(A)), lifecycle.revoked)
        fixture.provider.close()
    }

    private suspend fun seed(db: AgentLifeDatabase) {
        val foundation = RoomFoundationRepository(db, now, RandomIdGenerator)
        assertTrue(foundation.createPersonalWorkspace(CreatePersonalWorkspace(id(W), id(P), id(D), id(O1),
            "Personal", "Fixture User", "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        assertTrue(foundation.registerConnection(RegisterConnection(id(A), id(W), id(O2), id(D), "MICROSOFT",
            "account-a", null, setOf("Mail.Read"), 1, "CONNECTED")) is Outcome.Success)
        assertTrue(foundation.registerConnection(RegisterConnection(id(B), id(W), id(O3), id(D), "MICROSOFT",
            "account-b", null, setOf("Mail.Read"), 1, "CONNECTED")) is Outcome.Success)
        assertTrue(foundation.saveCheckpoint(SaveCheckpoint(id(W), id(A), "mail", id(O4), id(D), null,
            null, "cursor-a", null, null)) is Outcome.Success)
        assertTrue(foundation.saveCheckpoint(SaveCheckpoint(id(W), id(B), "mail", id(O5), id(D), null,
            null, "cursor-b", null, null)) is Outcome.Success)
    }

    private fun id(value: String) = Uuid.parse(value)

    private fun fixture(label: String): Fixture {
        val name = "privacy-$label.db"
        val alias = "agent_life_privacy_test_$label"
        val wrap = "privacy-$label.wrap"
        context.deleteDatabase(name)
        context.noBackupFilesDir.resolve(wrap).delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) }
        return Fixture(name, EncryptedDatabaseProvider(context, name, alias, wrap))
    }

    private class Fixture(val name: String, val provider: EncryptedDatabaseProvider) {
        suspend fun open() = (provider.open() as DatabaseAccess.Ready).database
    }

    private class RecordingLifecycle : ConnectionLifecycle {
        val revoked = mutableListOf<Uuid>()
        var failNextRevoke = false
        override suspend fun cancelWork(connectionId: Uuid) = Unit
        override suspend fun unregister(connectionId: Uuid) = Unit
        override suspend fun revokeCredentials(connectionId: Uuid) {
            if (failNextRevoke) { failNextRevoke = false; throw IllegalStateException("fixture revocation failure") }
            revoked += connectionId
        }
    }

    companion object {
        private const val W = "00000000-0000-0000-0000-000000000101"
        private const val OTHER = "00000000-0000-0000-0000-000000000199"
        private const val P = "00000000-0000-0000-0000-000000000102"
        private const val D = "00000000-0000-0000-0000-000000000103"
        private const val A = "00000000-0000-0000-0000-000000000104"
        private const val B = "00000000-0000-0000-0000-000000000105"
        private const val R1 = "00000000-0000-0000-0000-000000000106"
        private const val R2 = "00000000-0000-0000-0000-000000000107"
        private const val R3 = "00000000-0000-0000-0000-000000000108"
        private const val O1 = "00000000-0000-0000-0000-000000000111"
        private const val O2 = "00000000-0000-0000-0000-000000000112"
        private const val O3 = "00000000-0000-0000-0000-000000000113"
        private const val O4 = "00000000-0000-0000-0000-000000000114"
        private const val O5 = "00000000-0000-0000-0000-000000000115"
    }
}
