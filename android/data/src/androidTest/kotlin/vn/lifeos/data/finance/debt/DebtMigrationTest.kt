package vn.lifeos.data.finance.debt

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
class DebtMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val migration = MigrationTestHelper(instrumentation, AgentLifeDatabase::class.java)

    @Test fun v6ToV7PreservesLedgerAndSavings() {
        val name = "debt-migration.db"
        instrumentation.targetContext.deleteDatabase(name)
        val old = migration.createDatabase(name, 6)
        old.execSQL("INSERT INTO workspaces (id,name,ownerLocalProfileId,createdAt,updatedAt,version,originDeviceId) VALUES ('workspace','Fixture','profile',1,1,1,'device')")
        old.execSQL("INSERT INTO accounts VALUES ('account','workspace','Fixture','SAVINGS','VND',1000,1,NULL)")
        old.execSQL("INSERT INTO financial_transactions VALUES ('transaction','workspace','operation','ADJUSTMENT',2,2,NULL,NULL,'Fixture',NULL,NULL,NULL,NULL)")
        old.execSQL("INSERT INTO ledger_entries VALUES ('entry','workspace','transaction','account',100,'VND')")
        old.execSQL("INSERT INTO savings_goals VALUES ('goal','workspace','Fixture','VND',10000,NULL,NULL)")
        old.execSQL("INSERT INTO savings_goal_accounts VALUES ('goal','account','workspace')")
        old.close()
        val upgraded = migration.runMigrationsAndValidate(name, 7, true, DebtMigration.from6To7)
        upgraded.query("SELECT deltaMinor FROM ledger_entries WHERE id='entry'").use {
            assertTrue(it.moveToFirst()); assertEquals(100L, it.getLong(0))
        }
        upgraded.query("SELECT accountId FROM savings_goal_accounts WHERE goalId='goal'").use {
            assertTrue(it.moveToFirst()); assertEquals("account", it.getString(0))
        }
        upgraded.query("SELECT count(*) FROM debt_movements").use {
            assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
        }
        upgraded.close()
    }
}
