package vn.lifeos.data.privacy

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.FoundationMigrations

@RunWith(AndroidJUnit4::class)
class PrivacyMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val migration = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AgentLifeDatabase::class.java)

    @Test fun migrationFrom2KeepsWorkspaceAndAddsPrivacyTables() {
        val name = "privacy-migration.db"
        context.deleteDatabase(name)
        migration.createDatabase(name, 2).apply {
            execSQL("INSERT INTO workspaces (id,name,ownerLocalProfileId,createdAt,updatedAt,version,originDeviceId) VALUES (?,?,?,?,?,?,?)",
                arrayOf<Any>(W, "Personal", P, 100L, 100L, 1L, D))
            close()
        }
        migration.runMigrationsAndValidate(name, 3, true, FoundationMigrations.from2To3).use { upgraded ->
            upgraded.query("SELECT name FROM workspaces WHERE id = '$W'").use {
                assertTrue(it.moveToFirst())
                assertEquals("Personal", it.getString(0))
            }
            upgraded.query("SELECT COUNT(*) FROM privacy_preferences").use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
            upgraded.query("SELECT COUNT(*) FROM source_captures").use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        }
    }

    companion object {
        private const val W = "00000000-0000-0000-0000-000000000201"
        private const val P = "00000000-0000-0000-0000-000000000202"
        private const val D = "00000000-0000-0000-0000-000000000203"
    }
}
