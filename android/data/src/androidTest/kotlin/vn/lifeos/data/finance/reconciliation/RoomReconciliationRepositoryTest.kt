package vn.lifeos.data.finance.reconciliation

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.db.IntegrationConnectionEntity
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.data.finance.imports.RoomFinanceInboxRepository
import vn.lifeos.data.finance.reports.RoomFinanceReportsRepository
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.imports.InboundFinanceEvidence
import vn.lifeos.domain.finance.reconciliation.*
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput
import vn.lifeos.domain.finance.refund.ManualRefundAdjustmentCommands
import vn.lifeos.domain.finance.reports.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomReconciliationRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val now = Instant.parse("2026-11-12T12:00:00Z")
    private val at = now.minusSeconds(3600)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val workspace = id(1401)
    private val vnd = CurrencyCode.of("VND")
    private val bank = Account(id(1405), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, now.minusSeconds(86400))
    private val category = Category(id(1406), workspace, "Fixture expense", CategoryDirection.EXPENSE)
    private suspend fun setup(name: String): AgentLifeDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(1402), id(1403), id(1404), "Personal", "Fixture", "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        ledger.createAccount(bank); ledger.createCategory(category)
        assertTrue(ledger.post(PostFinanceCommand(id(1410), workspace, id(1411), TransactionKind.EXPENSE,
            bank.id, Money(600_000, vnd), at, categoryId = category.id)) is Outcome.Success)
        return db
    }
    private fun capture(n: Int = 1420, observed: Long = 9_300_000) = ObserveBalance(id(n), workspace, bank.id, vnd, at, observed, "Fixture observation")
    private fun filter() = ReportFilter(workspace, vnd, reportPeriodContaining(LocalDate.parse("2026-11-12"), 1), ZoneId.of("UTC"), bank.id)
    private suspend fun quality(db: AgentLifeDatabase) = FinanceReportUseCase.calculate(filter(),
        (RoomFinanceReportsRepository(db, clock).load(filter()) as Outcome.Success).value).quality

    @Test fun sameInstantSnapshotPreservesLedgerAndSurvivesReopen() = runBlocking {
        val name = "reconciliation-snapshot.db"
        val db = setup(name)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        ledger.post(PostFinanceCommand(id(1412), workspace, id(1413), TransactionKind.EXPENSE,
            bank.id, Money(200_000, vnd), at.plusSeconds(1), categoryId = category.id))
        val repo = RoomReconciliationRepository(db, clock, RandomIdGenerator)
        val row = (repo.capture(capture()) as Outcome.Success).value
        assertEquals(9_400_000L, row.expectedBalanceMinor)
        assertEquals(9_300_000L, row.observedBalanceMinor)
        assertEquals(-100_000L, row.differenceMinor)
        assertEquals(9_200_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(2, (ledger.observeRecentTransactions(workspace).first() as Outcome.Success).value.size)
        assertEquals(Outcome.Failure(AppError.InvalidInput("difference")), repo.resolve(workspace, row.id))
        val review = (repo.review(workspace, row.id) as Outcome.Success).value
        assertEquals(2, review.ledger.size)
        assertTrue(review.from <= at && review.endExclusive > at)
        db.close()
        val reopened = Room.databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AgentLifeDatabase::class.java, name).build()
        val persisted = (RoomReconciliationRepository(reopened, clock, RandomIdGenerator).observe(workspace).first() as Outcome.Success).value.single()
        assertEquals(-100_000L, persisted.snapshot.differenceMinor)
        assertFalse(persisted.resolved)
        reopened.close()
    }

    @Test fun explicitFin09AdjustmentAndResolutionKeepOriginalSnapshot() = runBlocking {
        val db = setup("reconciliation-adjust.db")
        val repo = RoomReconciliationRepository(db, clock, RandomIdGenerator)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.capture(capture())
        val commands = ManualRefundAdjustmentCommands(ledger, Locale.forLanguageTag("vi-VN"))
        val input = ManualAdjustmentInput(id(1430), id(1431), workspace, bank, "-100000", at, "")
        assertTrue(commands.adjust(input) is Outcome.Failure)
        assertTrue(commands.adjust(input.copy(reason = "Fixture correction")) is Outcome.Success)
        assertTrue(repo.resolve(workspace, id(1420)) is Outcome.Success)
        val view = (repo.observe(workspace).first() as Outcome.Success).value.single()
        assertTrue(view.resolved); assertTrue(view.ledgerChanged)
        assertEquals(9_400_000L, view.snapshot.expectedBalanceMinor)
        assertEquals(-100_000L, view.snapshot.differenceMinor)
        assertEquals(9_300_000L, view.currentExpectedMinor)
        assertFalse(quality(db).unreconciledAccountIds.contains(bank.id))
        val report = (ledger.report(FinanceReportQuery(workspace, vnd, bank.openingAsOf, now)) as Outcome.Success).value
        assertEquals(600_000L, report.netExpenseMinor)
        assertEquals(0L, report.incomeMinor)
        assertEquals(Outcome.Failure(AppError.Conflict), commands.adjust(input.copy(reason = "Fixture correction")))
        // A later backdated entry changes the previously verified balance and invalidates its quality status.
        ledger.post(PostFinanceCommand(id(1432), workspace, id(1433), TransactionKind.EXPENSE,
            bank.id, Money(1_000, vnd), at.minusSeconds(1), categoryId = category.id))
        assertTrue(quality(db).unreconciledAccountIds.contains(bank.id))
        assertFalse((repo.observe(workspace).first() as Outcome.Success).value.single().resolved)
        db.close()
    }

    @Test fun threePendingCandidatesAndMissingSourceFlowIntoReportAndAdviceGate() = runBlocking {
        val db = setup("reconciliation-quality.db")
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        repeat(3) { n ->
            inbox.ingest(InboundFinanceEvidence(id(1440 + n), id(1450 + n), workspace, null, FinanceSourceKind.NOTIFICATION,
                "fixture-event-$n", null, null, null, at, 1, 50_000, vnd, CategoryDirection.EXPENSE,
                at, bank.id, category.id, "", 9_000))
        }
        val report = FinanceReportUseCase.calculate(filter(), (RoomFinanceReportsRepository(db, clock).load(filter()) as Outcome.Success).value)
        assertEquals(3, report.quality.pendingCandidateCount)
        assertTrue(FinanceQualityFlag.PENDING_CANDIDATE in report.quality.flags)
        assertTrue(FinanceQualityFlag.MISSING_SOURCE in report.quality.flags)
        assertFalse(report.qualityForAdvice().canClaimIncomeExpenseComplete)
        assertEquals(3, report.qualityForAdvice().pendingCandidateCount)
        assertEquals(600_000L, report.netExpenseMinor)
        val repo = RoomReconciliationRepository(db, clock, RandomIdGenerator)
        repo.capture(capture())
        assertEquals(3, (repo.review(workspace, id(1420)) as Outcome.Success).value.candidates.size)
        db.foundationDao().insertConnection(IntegrationConnectionEntity(id(1460).value, workspace.value, "NOTIFICATION",
            scopes = "fixture", configVersion = 1, state = "PERMISSION_DENIED", createdAt = 1, updatedAt = 1, version = 1, originDeviceId = id(1404).value))
        assertTrue(FinanceQualityFlag.MISSING_SOURCE in quality(db).flags)
        assertTrue(FinanceQualityFlag.UNKNOWN_SOURCE_COVERAGE in quality(db).flags)
        db.close()
    }

    @Test fun validatesCurrencyTimeOverflowWorkspaceAndDuplicateSnapshot() = runBlocking {
        val db = setup("reconciliation-validation.db")
        val repo = RoomReconciliationRepository(db, clock, RandomIdGenerator)
        assertTrue(repo.capture(capture().copy(currency = CurrencyCode.of("USD"))) is Outcome.Failure)
        assertTrue(repo.capture(capture().copy(workspaceId = id(9999))) is Outcome.Failure)
        assertTrue(repo.capture(capture().copy(asOf = bank.openingAsOf.minusSeconds(1))) is Outcome.Failure)
        assertTrue(repo.capture(capture().copy(asOf = now.plusSeconds(1))) is Outcome.Failure)
        assertTrue(repo.capture(capture().copy(observedMinor = Long.MIN_VALUE)) is Outcome.Failure)
        assertTrue(repo.capture(capture()) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.Conflict), repo.capture(capture(1470)))
        assertTrue(repo.review(id(9999), id(1420)) is Outcome.Failure)
        assertTrue(repo.resolve(id(9999), id(1420)) is Outcome.Failure)
        db.close()
    }
}
