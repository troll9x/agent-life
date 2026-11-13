package vn.lifeos.data.notifications

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.domain.common.*
import vn.lifeos.domain.foundation.*

@RunWith(AndroidJUnit4::class)
class RoomNotificationRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(1601)
    private val device = id(1603)
    private val now = Instant.parse("2026-11-13T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private suspend fun setup(name: String): AgentLifeDatabase {
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(CreatePersonalWorkspace(
            workspace, id(1602), device, id(1604), "Personal", "Fixture", "Asia/Ho_Chi_Minh", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        return db
    }
    private fun payload(pkg: String = "fixture.bank", missing: Boolean = false, hash: Char = 'b') = FilteredNotification(
        pkg, "a".repeat(64), now.toEpochMilli(), hash.toString().repeat(64), if (missing) null else "Fixture",
        if (missing) null else "Fixture credited 50000 VND", missing)
    private suspend fun captures(db: AgentLifeDatabase) = db.notificationDao().observeCaptures(workspace.value, device.value).first()

    @Test fun unselectedAndRevokedPackagesLeaveNoCaptureOrCandidateAndNeverPostLedger() = runBlocking {
        val db = setup("notification-gate.db")
        var granted = true
        val repo = RoomNotificationRepository(db, clock, RandomIdGenerator) { granted }
        assertTrue(repo.selectPackages(workspace, setOf("fixture.bank")) is Outcome.Success)
        assertEquals(Outcome.Success(false), repo.capture(workspace, payload("spoof.bank")))
        granted = false
        assertEquals(Outcome.Success(false), repo.capture(workspace, payload()))
        repo.updatePermission(workspace)
        assertEquals("PERMISSION_DENIED", db.notificationDao().connection(workspace.value, device.value)!!.state)
        assertTrue(captures(db).isEmpty())
        assertTrue(db.financeInboxDao().observeOpen(workspace.value).first().isEmpty())
        assertEquals(0L, db.financeInboxDao().observeLedgerCount(workspace.value).first())
        assertTrue(repo.selectPackages(id(9999), setOf("fixture.bank")) is Outcome.Failure)
        db.close()
    }

    @Test fun durableDeduplicatedCaptureAndHiddenContentCandidateKeepAmountNull() = runBlocking {
        val name = "notification-durable.db"
        val db = setup(name)
        val repo = RoomNotificationRepository(db, clock, RandomIdGenerator) { true }
        repo.selectPackages(workspace, setOf("fixture.bank"))
        assertEquals(Outcome.Success(true), repo.capture(workspace, payload(missing = true)))
        assertEquals(Outcome.Success(false), repo.capture(workspace, payload(missing = true)))
        val candidate = db.financeInboxDao().observeOpen(workspace.value).first().single()
        assertNull(candidate.amountMinor); assertTrue(candidate.missingFields.contains("amountMinor"))
        assertEquals("NEEDS_REVIEW", candidate.state)
        val hidden = captures(db).single()
        assertEquals("MISSING_CONTENT", hidden.processingState); assertNull(hidden.body); assertNull(hidden.payloadExpiresAt)
        assertEquals(Outcome.Success(true), repo.capture(workspace, payload(hash = 'c')))
        assertEquals(2, captures(db).maxOf { it.revision })
        assertEquals(0L, db.financeInboxDao().observeLedgerCount(workspace.value).first())
        db.close()
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertEquals(2, captures(reopened).size)
        assertTrue(captures(reopened).any { it.body == "Fixture credited 50000 VND" })
        reopened.close()
    }

    @Test fun expiryAcknowledgementUnselectionAndRevocationClearTemporaryPayload() = runBlocking {
        val db = setup("notification-retention.db")
        var granted = true
        val repo = RoomNotificationRepository(db, clock, RandomIdGenerator) { granted }
        repo.selectPackages(workspace, setOf("fixture.bank")); repo.capture(workspace, payload())
        var row = captures(db).single()
        assertNotNull(db.notificationDao().pendingPayload(workspace.value, row.id, clock.millis()))
        assertNull(db.notificationDao().pendingPayload(workspace.value, row.id, clock.millis() + 900_000))
        assertEquals(1, db.notificationDao().purgeExpired(clock.millis() + 900_000))
        assertNull(captures(db).single().body)
        repo.capture(workspace, payload(hash = 'c'))
        row = captures(db).first { it.contentHash.startsWith('c') }
        db.notificationDao().acknowledge(workspace.value, row.id)
        assertNull(captures(db).first { it.id == row.id }.body)
        repo.capture(workspace, payload(hash = 'd')); repo.selectPackages(workspace, emptySet())
        assertTrue(captures(db).all { it.body == null && it.title == null })
        assertEquals(Outcome.Success(false), repo.capture(workspace, payload(hash = 'e')))
        repo.selectPackages(workspace, setOf("fixture.bank")); repo.capture(workspace, payload(hash = 'f'))
        granted = false; repo.updatePermission(workspace)
        assertTrue(captures(db).all { it.body == null && it.title == null })
        db.close()
    }

    @Test fun privacyDisconnectClearsQueueAndAllowlistButKeepsReviewEvidence() = runBlocking {
        val db = setup("notification-disconnect.db")
        val repo = RoomNotificationRepository(db, clock, RandomIdGenerator) { true }
        repo.selectPackages(workspace, setOf("fixture.bank")); repo.capture(workspace, payload())
        val connection = db.notificationDao().connection(workspace.value, device.value)!!
        val lifecycle = object : vn.lifeos.domain.privacy.ConnectionLifecycle {
            override suspend fun cancelWork(connectionId: Uuid) = Unit
            override suspend fun unregister(connectionId: Uuid) = Unit
            override suspend fun revokeCredentials(connectionId: Uuid) = Unit
        }
        val privacy = vn.lifeos.data.privacy.RoomPrivacyRepository(db, context, lifecycle, clock, RandomIdGenerator)
        assertEquals(vn.lifeos.domain.privacy.PrivacyResult.Success(Unit), privacy.disconnect(workspace, Uuid.parse(connection.id), true))
        assertTrue(captures(db).isEmpty())
        assertTrue(db.notificationDao().apps(workspace.value, device.value).isEmpty())
        assertEquals(1, db.financeInboxDao().observeOpen(workspace.value).first().size)
        assertEquals(Outcome.Success(false), repo.capture(workspace, payload(hash = 'd')))
        db.close()
    }
}
