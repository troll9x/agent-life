package vn.lifeos.data.finance.reports

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.reports.*
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomFinanceReportsRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(1)
    private val vnd = CurrencyCode.of("VND")
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val clock = Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC)

    @Test fun persistedBudgetAndSameSnapshotAcrossFilters() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("finance-reports.db")
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, "finance-reports.db").build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(2), id(3), id(4), "Personal", "Fixture", zone.id,
                "VND", 15, "ANDROID", "test")) is Outcome.Success)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        val bank = Account(id(5), workspace, "Bank", AccountKind.BANK, vnd, 10000,
            Instant.parse("2026-10-01T00:00:00Z"))
        val savings = bank.copy(id = id(6), name = "Savings", kind = AccountKind.SAVINGS, openingBalanceMinor = 0)
        val category = Category(id(7), workspace, "=SUM(1,2)", CategoryDirection.EXPENSE)
        assertTrue(ledger.createAccount(bank) is Outcome.Success)
        assertTrue(ledger.createAccount(savings) is Outcome.Success)
        assertTrue(ledger.createCategory(category) is Outcome.Success)
        fun at(date: String) = LocalDate.parse(date).atTime(23, 59).atZone(zone).toInstant()
        assertTrue(ledger.post(PostFinanceCommand(id(10), workspace, id(11), TransactionKind.EXPENSE,
            bank.id, Money(100, vnd), at("2026-10-14"), categoryId = category.id)) is Outcome.Success)
        assertTrue(ledger.post(PostFinanceCommand(id(12), workspace, id(13), TransactionKind.EXPENSE,
            bank.id, Money(500, vnd), at("2026-10-16"), categoryId = category.id)) is Outcome.Success)
        assertTrue(ledger.post(PostFinanceCommand(id(14), workspace, id(15), TransactionKind.TRANSFER,
            bank.id, Money(1000, vnd), at("2026-10-17"), destinationAccountId = savings.id)) is Outcome.Success)
        assertTrue(ledger.post(PostFinanceCommand(id(16), workspace, id(17), TransactionKind.REFUND,
            bank.id, Money(200, vnd), at("2026-10-20"), relatedTransactionId = id(12))) is Outcome.Success)
        val reports = RoomFinanceReportsRepository(db)
        val period = reportPeriodContaining(LocalDate.parse("2026-10-20"), 15)
        val filter = ReportFilter(workspace, vnd, period, zone)
        val budget = Budget(id(20), workspace, category.id, period.start, vnd, 1000, 8000)
        assertTrue(reports.saveBudget(budget) is Outcome.Success)
        assertTrue(reports.saveBudget(budget.copy(id = id(21), limitMinor = 600)) is Outcome.Success)
        val snapshot = FinanceReportUseCase.calculate(filter, (reports.load(filter) as Outcome.Success).value)
        assertEquals(300L, snapshot.netExpenseMinor)
        assertEquals(300L, snapshot.budgets.single().spentMinor)
        assertEquals(300L, snapshot.budgets.single().remainingMinor)
        assertEquals(3, snapshot.lines.size)
        assertTrue(snapshot.toSafeCsv().contains("\"'=SUM(1,2)\""))
        val selected = FinanceReportUseCase.calculate(filter.copy(accountId = savings.id),
            (reports.load(filter.copy(accountId = savings.id)) as Outcome.Success).value)
        assertEquals(0L, selected.netExpenseMinor)
        assertEquals(1, selected.lines.size)
        db.close()
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, "finance-reports.db").build()
        val persisted = FinanceReportUseCase.calculate(filter,
            (RoomFinanceReportsRepository(reopened).load(filter) as Outcome.Success).value)
        assertEquals(600L, persisted.budgets.single().budget.limitMinor)
        reopened.close()
    }
}
