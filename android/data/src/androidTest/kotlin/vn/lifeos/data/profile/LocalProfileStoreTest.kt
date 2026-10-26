package vn.lifeos.data.profile

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class LocalProfileStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC)

    @Test fun simultaneousFinishCreatesOneWorkspaceAndSettingsSurviveReopen() = runBlocking {
        val name = "ux02-onboarding.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val firstDb = open()
        val store = LocalProfileStore(firstDb, context, clock, RandomIdGenerator)
        val input = ProfileInput("Fixture User", "Asia/Ho_Chi_Minh", "VND", 1)
        val results = listOf(
            async(Dispatchers.Default) { store.complete(input) },
            async(Dispatchers.Default) { store.complete(input) },
        ).awaitAll()
        assertTrue(results.all { it is Outcome.Success })
        assertEquals(1, firstDb.foundationDao().activeWorkspaceCount())
        val snapshot = (store.observe().first() as Outcome.Success).value!!
        assertEquals("Fixture User", snapshot.profile.displayName)
        val changed = store.update(snapshot.profile, input.copy(budgetStartDay = 15))
        assertTrue(changed is Outcome.Success)
        firstDb.close()

        val reopened = open()
        val readBack = (LocalProfileStore(reopened, context, clock, RandomIdGenerator).observe().first() as Outcome.Success).value!!
        assertEquals(15, readBack.profile.budgetStartDay)
        assertEquals("Asia/Ho_Chi_Minh", readBack.profile.zoneId)
        assertEquals(1, reopened.foundationDao().activeWorkspaceCount())
        reopened.close()
    }
}
