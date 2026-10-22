package vn.lifeos.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.*

@RunWith(AndroidJUnit4::class)
class FoundationDatabaseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    @get:Rule val migration = MigrationTestHelper(instrumentation, AgentLifeDatabase::class.java)

    @Test fun migrationKeepsProfileIdCountAndContent() {
        val name = "foundation-migration.db"
        context.deleteDatabase(name)
        val old = migration.createDatabase(name, 1)
        old.execSQL("INSERT INTO workspaces (id,name,ownerLocalProfileId,createdAt,updatedAt,version,originDeviceId) VALUES (?,?,?,?,?,?,?)",
            arrayOf<Any>(W, "Personal", P, 100L, 100L, 1L, D))
        old.execSQL("INSERT INTO profiles (id,workspaceId,displayName,zoneId,baseCurrency,budgetStartDay,createdAt,updatedAt,version,originDeviceId) VALUES (?,?,?,?,?,?,?,?,?,?)",
            arrayOf<Any>(P, W, "Fixture User", "Asia/Ho_Chi_Minh", "VND", 5, 100L, 150L, 2L, D))
        old.close()

        val upgraded = migration.runMigrationsAndValidate(name, 2, true, FoundationMigrations.from1To2)
        upgraded.query("SELECT id,workspaceId,displayName,zoneId,baseCurrency,budgetStartDay,version FROM profiles").use {
            assertTrue(it.moveToFirst())
            assertEquals(P, it.getString(0))
            assertEquals(W, it.getString(1))
            assertEquals("Fixture User", it.getString(2))
            assertEquals("Asia/Ho_Chi_Minh", it.getString(3))
            assertEquals("VND", it.getString(4))
            assertEquals(5, it.getInt(5))
            assertEquals(2L, it.getLong(6))
            assertTrue(!it.moveToNext())
        }
        upgraded.close()
    }

    @Test fun profileAndConnectionsSurviveReopenAndCheckpointsStaySeparate() = runBlocking {
        val name = "foundation-persistence.db"
        context.deleteDatabase(name)
        val fixedClock = Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"), ZoneOffset.UTC)
        fun open() = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name)
            .addMigrations(FoundationMigrations.from1To2).build()
        val firstDb = open()
        val repo = RoomFoundationRepository(firstDb, fixedClock, RandomIdGenerator)
        val created = repo.createPersonalWorkspace(CreatePersonalWorkspace(uuid(W), uuid(P), uuid(D), uuid(O1), "Personal", "Fixture User", "Asia/Ho_Chi_Minh", "VND", 5, "ANDROID", "test"))
        assertTrue(created is Outcome.Success)
        val firstConnection = repo.registerConnection(RegisterConnection(uuid(C1), uuid(W), uuid(O2), uuid(D), "CALENDAR", "account-a", null, setOf("read"), 1, "CONNECTED"))
        val secondConnection = repo.registerConnection(RegisterConnection(uuid(C2), uuid(W), uuid(O3), uuid(D), "CALENDAR", "account-b", null, setOf("read"), 1, "CONNECTED"))
        assertTrue(firstConnection is Outcome.Success)
        assertTrue(secondConnection is Outcome.Success)
        val firstCheckpoint = repo.saveCheckpoint(SaveCheckpoint(uuid(W), uuid(C1), "events", uuid(O4), uuid(D), null, null, "cursor-a", null, null))
        val secondCheckpoint = repo.saveCheckpoint(SaveCheckpoint(uuid(W), uuid(C2), "events", uuid(O5), uuid(D), null, null, "cursor-b", null, null))
        assertTrue(firstCheckpoint is Outcome.Success)
        assertTrue(secondCheckpoint is Outcome.Success)
        assertNotEquals((firstCheckpoint as Outcome.Success).value.id, (secondCheckpoint as Outcome.Success).value.id)
        assertEquals(5, firstDb.foundationDao().auditCount(W))
        firstDb.close()

        val reopened = open()
        val observed = RoomFoundationRepository(reopened, fixedClock, RandomIdGenerator)
        val profile = observed.observeProfile(uuid(W)).first() as Outcome.Success
        assertEquals(uuid(P), profile.value?.id)
        val connections = observed.observeConnections(uuid(W)).first() as Outcome.Success
        assertEquals(2, connections.value.size)
        val a = observed.observeCheckpoints(uuid(C1)).first() as Outcome.Success
        val b = observed.observeCheckpoints(uuid(C2)).first() as Outcome.Success
        assertEquals("cursor-a", a.value.single().completedCursor)
        assertEquals("cursor-b", b.value.single().completedCursor)
        reopened.close()
    }

    @Test fun rejectedCommandsDoNotWriteAuditOrPartialRows() = runBlocking {
        val name = "foundation-atomic.db"
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val repo = RoomFoundationRepository(db, Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"), ZoneOffset.UTC), RandomIdGenerator)
        assertTrue(repo.createPersonalWorkspace(CreatePersonalWorkspace(uuid(W), uuid(P), uuid(D), uuid(O1), "Personal", "Fixture User", "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        assertTrue(repo.registerConnection(RegisterConnection(uuid(C1), uuid(W), uuid(O2), uuid(D), "CALENDAR", "account-a", null, setOf("read"), 1, "CONNECTED")) is Outcome.Success)
        val duplicate = repo.registerConnection(RegisterConnection(uuid(C2), uuid(W), uuid(O3), uuid(D), "CALENDAR", "account-a", null, setOf("read"), 1, "CONNECTED"))
        assertEquals(Outcome.Failure(AppError.Conflict), duplicate)
        val stale = repo.updateProfile(UpdateProfile(uuid(W), uuid(P), uuid(O4), 0, "Changed", "UTC", "VND", 1))
        assertEquals(Outcome.Failure(AppError.Conflict), stale)
        assertEquals(2, db.foundationDao().auditCount(W))
        assertEquals(1, (repo.observeConnections(uuid(W)).first() as Outcome.Success).value.size)
        assertEquals("Fixture User", (repo.observeProfile(uuid(W)).first() as Outcome.Success).value?.displayName)
        db.close()
    }

    private fun uuid(value: String) = Uuid.parse(value)

    companion object {
        private const val W = "00000000-0000-0000-0000-000000000001"
        private const val P = "00000000-0000-0000-0000-000000000002"
        private const val D = "00000000-0000-0000-0000-000000000003"
        private const val C1 = "00000000-0000-0000-0000-000000000004"
        private const val C2 = "00000000-0000-0000-0000-000000000005"
        private const val O1 = "00000000-0000-0000-0000-000000000011"
        private const val O2 = "00000000-0000-0000-0000-000000000012"
        private const val O3 = "00000000-0000-0000-0000-000000000013"
        private const val O4 = "00000000-0000-0000-0000-000000000014"
        private const val O5 = "00000000-0000-0000-0000-000000000015"
    }
}
