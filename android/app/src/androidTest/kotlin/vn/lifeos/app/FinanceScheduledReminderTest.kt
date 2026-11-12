package vn.lifeos.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.reminders.ACTION_FIRE
import vn.lifeos.app.reminders.ReminderActionReceiver
import vn.lifeos.app.reminders.ReminderEngine
import vn.lifeos.data.finance.scheduled.RoomScheduledFinanceRepository
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.scheduled.*
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class FinanceScheduledReminderTest {
    @Test fun commonEngineSchedulesOneAlarmAndCancelsPausedFutureRule() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val fixture = RandomIdGenerator.newId().value
        val provider = EncryptedDatabaseProvider(app, "scheduled-reminder-$fixture.db", "scheduled_reminder_$fixture", "scheduled-reminder-$fixture.wrap")
        val db = (provider.open() as DatabaseAccess.Ready).database
        val zone = ZoneId.systemDefault()
        val clock = app.appContainer.clock
        val store = app.appContainer.profileStore(db)
        store.complete(ProfileInput("Fixture scheduled alarm", zone.id, "VND", 1))
        val workspace = (store.observe().first() as Outcome.Success).value!!.profile.workspaceId
        val ledger = app.appContainer.financeRepository(db)
        val bank = Account(RandomIdGenerator.newId(), workspace, "Fixture bank alarm", AccountKind.BANK,
            CurrencyCode.of("VND"), 10_000_000, clock.instant().minusSeconds(86400))
        val category = Category(RandomIdGenerator.newId(), workspace, "Fixture rent alarm", CategoryDirection.EXPENSE)
        ledger.createAccount(bank); ledger.createCategory(category)
        val date = clock.instant().atZone(zone).toLocalDate().plusDays(1)
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        val rule = (repo.save(ScheduledTransaction(RandomIdGenerator.newId(), workspace, "Fixture rent alarm",
            FinanceRecurrence.MONTHLY, date, date, 1_000_000, bank.currency, TransactionKind.EXPENSE, bank.id, category.id), zone) as Outcome.Success).value
        val engine = ReminderEngine(app, db, clock)
        assertTrue(engine.reconcile(workspace, zone))
        assertTrue(engine.reconcile(workspace, zone))
        val row = db.reminderDao().all(workspace.value).single { it.targetType == "FINANCE" }
        assertEquals(date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), row.triggerAt)
        val intent = Intent(app, ReminderActionReceiver::class.java).apply {
            action = ACTION_FIRE; data = Uri.parse("agentlife://alarm/${row.id}")
        }
        assertNotNull(PendingIntent.getBroadcast(app, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        assertEquals(0, (repo.observe(workspace, zone).first() as Outcome.Success).value.items.size)
        repo.setEnabled(workspace, rule.id, rule.version, false)
        assertTrue(engine.reconcile(workspace, zone))
        assertEquals("CANCELLED", db.reminderDao().byId(row.id)?.state)
        assertNull(PendingIntent.getBroadcast(app, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        assertEquals(10_000_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        provider.close()
    }
}
