package vn.lifeos.data.planner

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
class PlannerMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val migration = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AgentLifeDatabase::class.java)

    @Test fun migrationFrom3KeepsProfileAndCreatesEmptyPlannerTables() {
        val name = "planner-migration.db"
        context.deleteDatabase(name)
        migration.createDatabase(name, 3).apply {
            execSQL("INSERT INTO workspaces (id,name,ownerLocalProfileId,createdAt,updatedAt,version,originDeviceId) VALUES (?,?,?,?,?,?,?)",
                arrayOf<Any>(W, "Personal", P, 100L, 100L, 1L, D))
            execSQL("INSERT INTO profiles (id,workspaceId,displayName,zoneId,baseCurrency,budgetStartDay,createdAt,updatedAt,version,originDeviceId) VALUES (?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any>(P, W, "Fixture User", "Asia/Ho_Chi_Minh", "VND", 15, 100L, 100L, 1L, D))
            close()
        }
        migration.runMigrationsAndValidate(name, 4, true, PlannerMigration.from3To4).use { upgraded ->
            upgraded.query("SELECT displayName,budgetStartDay FROM profiles WHERE id='$P'").use {
                assertTrue(it.moveToFirst())
                assertEquals("Fixture User", it.getString(0))
                assertEquals(15, it.getInt(1))
            }
            for (table in listOf("projects", "tasks", "task_checklist_items", "calendar_events", "recurrence_exceptions", "reminders")) {
                upgraded.query("SELECT COUNT(*) FROM $table").use {
                    assertTrue(it.moveToFirst())
                    assertEquals(0, it.getInt(0))
                }
            }
        }
    }

    companion object {
        private const val W = "00000000-0000-4000-8000-000000000101"
        private const val P = "00000000-0000-4000-8000-000000000102"
        private const val D = "00000000-0000-4000-8000-000000000103"
    }
}
