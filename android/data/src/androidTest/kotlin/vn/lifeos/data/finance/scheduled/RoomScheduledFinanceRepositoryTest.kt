package vn.lifeos.data.finance.scheduled

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.data.finance.imports.RoomFinanceInboxRepository
import vn.lifeos.data.reminders.RoomReminderStore
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.imports.*
import vn.lifeos.domain.finance.scheduled.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomScheduledFinanceRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val now = Instant.parse("2026-11-12T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val workspace = id(1301)
    private val vnd = CurrencyCode.of("VND")
    private val bank = Account(id(1305), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, Instant.parse("2026-10-01T00:00:00Z"))
    private val category = Category(id(1306), workspace, "Fixture rent", CategoryDirection.EXPENSE)
    private fun rule(date: String = "2026-11-01") = ScheduledTransaction(id(1310), workspace, "Fixture rent", FinanceRecurrence.MONTHLY,
        LocalDate.parse(date), LocalDate.parse(date), 1_000_000, vnd, TransactionKind.EXPENSE, bank.id, category.id)
    private suspend fun setup(name: String): AgentLifeDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(1302), id(1303), id(1304), "Personal", "Fixture", zone.id, "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        ledger.createAccount(bank); ledger.createCategory(category)
        return db
    }
    private suspend fun snapshot(repo: RoomScheduledFinanceRepository) = (repo.observe(workspace, zone).first() as Outcome.Success).value
    private suspend fun balance(db: AgentLifeDatabase) = (RoomFinanceRepository(db, clock, RandomIdGenerator)
        .balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor
    private fun evidence(n: Int, source: FinanceSourceKind) = InboundFinanceEvidence(id(n), id(n + 100), workspace, null,
        source, "fixture-$n", null, "FIXTURE-RENT-REF", null, now, 1, 1_000_000, vnd, CategoryDirection.EXPENSE,
        now.minusSeconds(60), bank.id, category.id, "", 9_500, true)

    @Test fun dueRentRestartAndConcurrentConfirmationPostOnlyOnce() = runBlocking {
        val name = "scheduled-rent.db"
        val db = setup(name)
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        assertTrue(repo.save(rule(), zone) is Outcome.Success)
        assertEquals(1, (repo.generateDue(workspace, zone) as Outcome.Success).value)
        assertEquals(0, (repo.generateDue(workspace, zone) as Outcome.Success).value)
        assertEquals(10_000_000L, balance(db))
        val plan = snapshot(repo).items.single().plan
        val at = LocalDate.parse("2026-11-01").atTime(9, 0).atZone(zone).toInstant()
        val a = async(Dispatchers.IO) { repo.decide(workspace, plan.id, zone, PlannedDecision.Post(id(1320), at)) }
        val b = async(Dispatchers.IO) { repo.decide(workspace, plan.id, zone, PlannedDecision.Post(id(1321), at)) }
        assertEquals(1, listOf(a.await(), b.await()).count { it is Outcome.Success })
        assertEquals(9_000_000L, balance(db))
        assertEquals(PlannedState.POSTED, snapshot(repo).items.single().plan.state)
        assertEquals(Outcome.Failure(AppError.Conflict), repo.decide(workspace, plan.id, zone, PlannedDecision.Post(id(1322), at)))
        db.close()
        val reopened = Room.databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AgentLifeDatabase::class.java, name).build()
        val again = RoomScheduledFinanceRepository(reopened, clock, RandomIdGenerator)
        assertEquals(0, (again.generateDue(workspace, zone) as Outcome.Success).value)
        assertEquals(1, snapshot(again).items.size)
        assertEquals(9_000_000L, balance(reopened))
        reopened.close()
    }

    @Test fun notificationAndMailLinkToOneOccurrenceAndOneLedgerTransaction() = runBlocking {
        val db = setup("scheduled-evidence.db")
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        repo.save(rule("2026-11-12"), zone); repo.generateDue(workspace, zone)
        val notification = (inbox.ingest(evidence(1330, FinanceSourceKind.NOTIFICATION)) as Outcome.Success).value
        val mail = (inbox.ingest(evidence(1331, FinanceSourceKind.MAIL)) as Outcome.Success).value
        val item = snapshot(repo).items.single()
        assertEquals(2, item.evidence.size)
        assertEquals(10_000_000L, balance(db))
        assertTrue(repo.decide(workspace, item.plan.id, zone, PlannedDecision.Post(id(1332), now)) is Outcome.Failure)
        assertTrue(repo.decide(workspace, item.plan.id, zone, PlannedDecision.Post(id(1333), now, notification.id)) is Outcome.Success)
        val posted = snapshot(repo).items.single().plan.transactionId!!
        assertTrue(repo.decide(workspace, item.plan.id, zone, PlannedDecision.Link(posted, mail.id)) is Outcome.Success)
        assertEquals(9_000_000L, balance(db))
        assertEquals(2, db.financeDao().evidenceForTransaction(workspace.value, posted.value).size)
        assertEquals(1, (RoomFinanceRepository(db, clock, RandomIdGenerator).observeRecentTransactions(workspace).first() as Outcome.Success).value.size)
        assertEquals(0, (repo.generateDue(workspace, zone) as Outcome.Success).value)
        db.close()
    }

    @Test fun existingPaymentLinksAndConflictingLinkRollsBackEvidence() = runBlocking {
        val db = setup("scheduled-existing.db")
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.save(rule("2026-11-12"), zone)
        repo.save(rule("2026-11-12").copy(id = id(1340)), zone)
        repo.generateDue(workspace, zone)
        val posted = (ledger.post(PostFinanceCommand(id(1341), workspace, id(1342), TransactionKind.EXPENSE,
            bank.id, Money(1_000_000, vnd), now.minusSeconds(60), categoryId = category.id)) as Outcome.Success).value
        val items = snapshot(repo).items
        assertEquals(listOf(posted.id), items.first().postedTransactionIds)
        assertTrue(repo.decide(workspace, items.first().plan.id, zone, PlannedDecision.Post(id(1343), now)) is Outcome.Failure)
        assertTrue(repo.decide(workspace, items.first().plan.id, zone, PlannedDecision.Link(posted.id)) is Outcome.Success)
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        val candidate = (inbox.ingest(evidence(1344, FinanceSourceKind.MAIL)) as Outcome.Success).value
        // The unique occurrence/transaction link fails after evidence linking; outer transaction must undo both.
        assertTrue(repo.decide(workspace, items.last().plan.id, zone, PlannedDecision.Link(posted.id, candidate.id)) is Outcome.Failure)
        assertEquals(null, db.financeInboxDao().evidence(workspace.value, candidate.sourceEvidenceId.value)?.transactionId)
        assertEquals("PENDING", db.financeInboxDao().candidate(workspace.value, candidate.id.value)?.state)
        assertTrue(repo.decide(workspace, items.last().plan.id, zone, PlannedDecision.Skip) is Outcome.Success)
        assertEquals(9_000_000L, balance(db))
        db.close()
    }

    @Test fun editingFutureRuleKeepsGeneratedSnapshotAndValidatesVersionsAndCurrency() = runBlocking {
        val db = setup("scheduled-edit.db")
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        repo.save(rule(), zone); repo.generateDue(workspace, zone)
        val before = snapshot(repo).rules.single()
        assertTrue(repo.save(before.copy(amountMinor = 2_000_000), zone, before.version - 1) is Outcome.Failure)
        assertTrue(repo.save(before.copy(currency = CurrencyCode.of("USD")), zone, before.version) is Outcome.Failure)
        assertTrue(repo.save(before.copy(accountId = id(9999)), zone, before.version) is Outcome.Failure)
        assertTrue(repo.save(before.copy(amountMinor = 2_000_000), zone, before.version) is Outcome.Success)
        assertEquals(1_000_000L, snapshot(repo).items.single().plan.amountMinor)
        val ruleAfter = snapshot(repo).rules.single()
        assertTrue(repo.setEnabled(workspace, ruleAfter.id, ruleAfter.version, false) is Outcome.Success)
        val decemberClock = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneOffset.UTC)
        val december = RoomScheduledFinanceRepository(db, decemberClock, RandomIdGenerator)
        assertEquals(0, (december.generateDue(workspace, zone) as Outcome.Success).value)
        val paused = snapshot(repo).rules.single()
        repo.setEnabled(workspace, paused.id, paused.version, true)
        assertEquals(1, (december.generateDue(workspace, zone) as Outcome.Success).value)
        assertEquals(2_000_000L, snapshot(repo).items.first().plan.amountMinor)
        assertEquals(10_000_000L, balance(db))
        db.close()
    }

    @Test fun sharedReminderStoreClaimsFinanceOnceAndNeverPostsMoney() = runBlocking {
        val db = setup("scheduled-reminder.db")
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        repo.save(rule("2026-11-13"), zone)
        val store = RoomReminderStore(db)
        val result = store.reconcile(workspace, scheduledReminderCandidates(db, workspace, zone), now)
        val row = result.schedule.single()
        assertEquals("FINANCE", row.targetType)
        assertEquals("Fixture rent", store.title(row))
        val due = Instant.ofEpochMilli(row.triggerAt)
        val dueRepo = RoomScheduledFinanceRepository(db, Clock.fixed(due, ZoneOffset.UTC), RandomIdGenerator)
        dueRepo.generateDue(workspace, zone)
        assertNotNull(store.claim(row.id, row.triggerAt, due))
        assertEquals(null, store.claim(row.id, row.triggerAt, due))
        assertNotNull(store.snooze(row.id, due))
        dueRepo.decide(workspace, snapshot(repo).items.single().plan.id, zone, PlannedDecision.Skip)
        assertEquals(null, store.snooze(row.id, due))
        assertEquals(null, store.complete(row.id, due))
        assertEquals(10_000_000L, balance(db))
        db.close()
    }

    @Test fun reviewedSeparatePaymentsAndSalaryHaveCorrectSignedBalances() = runBlocking {
        val db = setup("scheduled-separate.db")
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.save(rule("2026-11-12"), zone)
        repo.save(rule("2026-11-12").copy(id = id(1360)), zone)
        repo.generateDue(workspace, zone)
        val items = snapshot(repo).items
        assertTrue(repo.decide(workspace, items.first().plan.id, zone, PlannedDecision.Post(id(1361), now)) is Outcome.Success)
        assertTrue(repo.decide(workspace, items.last().plan.id, zone, PlannedDecision.Post(id(1362), now)) is Outcome.Failure)
        assertTrue(repo.decide(workspace, items.last().plan.id, zone, PlannedDecision.Post(id(1363), now, separatePayment = true)) is Outcome.Success)
        assertEquals(8_000_000L, balance(db))
        val income = category.copy(id = id(1364), direction = CategoryDirection.INCOME)
        ledger.createCategory(income)
        repo.save(rule("2026-11-12").copy(id = id(1365), kind = TransactionKind.INCOME, categoryId = income.id), zone)
        repo.generateDue(workspace, zone)
        val salary = snapshot(repo).items.single { it.plan.kind == TransactionKind.INCOME }.plan
        assertTrue(repo.decide(id(9999), salary.id, zone, PlannedDecision.Post(id(1366), now)) is Outcome.Failure)
        assertEquals(8_000_000L, balance(db))
        assertTrue(repo.decide(workspace, salary.id, zone, PlannedDecision.Post(id(1367), now)) is Outcome.Success)
        assertEquals(9_000_000L, balance(db))
        val report = (ledger.report(FinanceReportQuery(workspace, vnd, now.minusSeconds(86400), now.plusSeconds(1))) as Outcome.Success).value
        assertEquals(1_000_000L, report.incomeMinor); assertEquals(2_000_000L, report.netExpenseMinor)
        db.close()
    }
}
