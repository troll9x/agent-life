package vn.lifeos.data.finance.goals

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase

@RunWith(AndroidJUnit4::class)
class SavingsGoalMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val migration = MigrationTestHelper(instrumentation, AgentLifeDatabase::class.java)

    @Test fun v5ToV6PreservesWorkspaceAndCreatesGoalTables() {
        val name = "savings-goal-migration.db"
        instrumentation.targetContext.deleteDatabase(name)
        val old = migration.createDatabase(name, 5)
        old.execSQL("INSERT INTO workspaces (id,name,ownerLocalProfileId,createdAt,updatedAt,version,originDeviceId) VALUES (?,?,?,?,?,?,?)",
            arrayOf<Any>("00000000-0000-4000-8000-000000000901", "Fixture", "profile", 1L, 1L, 1L, "device"))
        old.close()
        val upgraded = migration.runMigrationsAndValidate(name, 6, true, SavingsGoalMigration.from5To6)
        upgraded.query("SELECT name FROM workspaces").use {
            assertTrue(it.moveToFirst()); assertEquals("Fixture", it.getString(0))
        }
        upgraded.query("SELECT count(*) FROM savings_goals").use {
            assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
        }
        upgraded.close()
    }
}
