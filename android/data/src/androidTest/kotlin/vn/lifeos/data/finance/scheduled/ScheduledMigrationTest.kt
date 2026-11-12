package vn.lifeos.data.finance.scheduled

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
class ScheduledMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val migration = MigrationTestHelper(instrumentation, AgentLifeDatabase::class.java)
    @Test fun v7ToV8KeepsExistingDebtAndLedger() {
        val name = "scheduled-migration.db"
        instrumentation.targetContext.deleteDatabase(name)
        val old = migration.createDatabase(name, 7)
        old.execSQL("INSERT INTO workspaces (id,name,ownerLocalProfileId,createdAt,updatedAt,version,originDeviceId) VALUES ('workspace','Fixture','profile',1,1,1,'device')")
        old.execSQL("INSERT INTO accounts VALUES ('account','workspace','Fixture','BANK','VND',1000,1,NULL)")
        old.execSQL("INSERT INTO financial_transactions VALUES ('transaction','workspace','operation','ADJUSTMENT',2,2,NULL,NULL,'Fixture',NULL,NULL,NULL,NULL)")
        old.execSQL("INSERT INTO ledger_entries VALUES ('entry','workspace','transaction','account',100,'VND')")
        old.execSQL("INSERT INTO debt_agreements VALUES ('debt','workspace','BORROWER','partner','Fixture','VND',1000,NULL,1)")
        old.close()
        val upgraded = migration.runMigrationsAndValidate(name, 8, true, ScheduledMigration.from7To8)
        upgraded.query("SELECT deltaMinor FROM ledger_entries WHERE id='entry'").use { assertTrue(it.moveToFirst()); assertEquals(100L, it.getLong(0)) }
        upgraded.query("SELECT openingPrincipalMinor FROM debt_agreements WHERE id='debt'").use { assertTrue(it.moveToFirst()); assertEquals(1000L, it.getLong(0)) }
        upgraded.query("SELECT count(*) FROM planned_transactions").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
        upgraded.close()
    }
}
