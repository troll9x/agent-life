package vn.lifeos.data.finance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.db.WorkspaceEntity
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.commands.ManualFinanceCommands
import vn.lifeos.domain.finance.refund.ManualRefundAdjustmentCommands
import vn.lifeos.domain.finance.refund.ManualRefundInput
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomFinanceRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val now = Instant.parse("2026-10-29T12:00:00Z")
    private val start = Instant.parse("2026-10-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val vnd = CurrencyCode.of("VND")
    private val workspace = id(801)
    private val bank = Account(id(805), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, start)
    private val cash = Account(id(806), workspace, "Fixture cash", AccountKind.CASH, vnd, 0, start)

    private suspend fun setup(name: String): AgentLifeDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(802), id(803), id(804), "Personal", "Fixture",
                "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        return db
    }

    @Test fun transferIsAtomicIdempotentAndDoesNotCountAsIncomeOrExpense() = runBlocking {
        val db = setup("finance-transfer.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        assertTrue(repo.createAccount(bank) is Outcome.Success)
        assertTrue(repo.createAccount(cash) is Outcome.Success)
        val transfer = PostFinanceCommand(id(810), workspace, id(811), TransactionKind.TRANSFER,
            bank.id, Money(1_000_000, vnd), now, destinationAccountId = cash.id)
        assertTrue(repo.post(transfer) is Outcome.Success)
        assertEquals(9_000_000L, (repo.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(1_000_000L, (repo.balance(workspace, cash.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(Outcome.Failure(AppError.Conflict), repo.post(transfer.copy(id = id(812))))
        assertEquals(0L, (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value.incomeMinor)
        assertEquals(0L, (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value.netExpenseMinor)
        assertEquals(2, db.financeDao().entries(workspace.value, transfer.id.value).size)
        assertEquals("MANUAL", db.financeDao().evidenceForTransaction(workspace.value, transfer.id.value).single().sourceKind)
        db.close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, "finance-transfer.db").build()
        val persisted = RoomFinanceRepository(reopened, clock, RandomIdGenerator)
        assertEquals(9_000_000L, (persisted.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(1_000_000L, (persisted.balance(workspace, cash.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(1, (persisted.observeRecentTransactions(workspace).first() as Outcome.Success).value.size)
        reopened.close()
    }

    @Test fun secondEntryFailureRollsBackHeaderAndFirstEntry() = runBlocking {
        val db = setup("finance-rollback.db")
        val normal = RoomFinanceRepository(db, clock, RandomIdGenerator)
        normal.createAccount(bank); normal.createAccount(cash)
        val duplicateEntryId = IdGenerator { id(820) }
        val repo = RoomFinanceRepository(db, clock, duplicateEntryId)
        val command = PostFinanceCommand(id(821), workspace, id(822), TransactionKind.TRANSFER,
            bank.id, Money(1_000_000, vnd), now, destinationAccountId = cash.id)
        assertEquals(Outcome.Failure(AppError.Conflict), repo.post(command))
        assertEquals(null, db.financeDao().transaction(workspace.value, command.id.value))
        assertTrue(db.financeDao().entries(workspace.value, command.id.value).isEmpty())
        assertEquals(10_000_000L, (normal.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(0L, (normal.balance(workspace, cash.id, now) as Outcome.Success).value.amountMinor)
        db.close()
    }

    @Test fun categoryWorkspaceCurrencyAndRefundLimitsAreChecked() = runBlocking {
        val db = setup("finance-validation.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(bank)
        val expenseCategory = Category(id(830), workspace, "Fixture expense", CategoryDirection.EXPENSE)
        repo.createCategory(expenseCategory)
        val otherWorkspace = id(860)
        db.foundationDao().insertWorkspace(WorkspaceEntity(otherWorkspace.value, "Other", id(861).value,
            now.toEpochMilli(), now.toEpochMilli(), 1, id(862).value))
        assertTrue(repo.createAccount(bank.copy(id = id(864), workspaceId = otherWorkspace)) is Outcome.Success)
        val expense = PostFinanceCommand(id(831), workspace, id(832), TransactionKind.EXPENSE,
            bank.id, Money(300_000, vnd), now, categoryId = expenseCategory.id)
        assertTrue(repo.post(expense) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.InvalidInput("categoryId")), repo.post(expense.copy(id = id(833), operationId = id(834), kind = TransactionKind.INCOME)))
        assertEquals(Outcome.Failure(AppError.InvalidInput("accountId")), repo.post(expense.copy(id = id(835), operationId = id(836), accountId = id(999))))
        assertEquals(Outcome.Failure(AppError.InvalidInput("accountId")), repo.post(expense.copy(id = id(865), operationId = id(866), accountId = id(864))))
        assertEquals(Outcome.Failure(AppError.InvalidInput("amount")), repo.post(expense.copy(id = id(837), operationId = id(838), amount = Money(1, CurrencyCode.of("USD")))))
        val refund = PostFinanceCommand(id(839), workspace, id(840), TransactionKind.REFUND,
            bank.id, Money(100_000, vnd), now, relatedTransactionId = expense.id)
        assertTrue(repo.post(refund) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.InvalidInput("amount")), repo.post(refund.copy(id = id(841), operationId = id(842), amount = Money(250_000, vnd))))
        val report = (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value
        assertEquals(300_000L, report.grossExpenseMinor)
        assertEquals(100_000L, report.refundMinor)
        assertEquals(200_000L, report.netExpenseMinor)
        db.close()
    }

    @Test fun reversalKeepsOriginalAndPostsOppositeEntryOnce() = runBlocking {
        val db = setup("finance-reversal.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(bank)
        val category = Category(id(850), workspace, "Expense", CategoryDirection.EXPENSE)
        repo.createCategory(category)
        val expense = PostFinanceCommand(id(851), workspace, id(852), TransactionKind.EXPENSE,
            bank.id, Money(250_000, vnd), now.minusSeconds(60), categoryId = category.id)
        assertTrue(repo.post(expense) is Outcome.Success)
        val reversal = PostFinanceCommand(id(853), workspace, id(854), TransactionKind.REVERSAL,
            bank.id, Money(250_000, vnd), now, relatedTransactionId = expense.id, note = "Fixture correction")
        assertTrue(repo.post(reversal) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.InvalidInput("relatedTransactionId")),
            repo.post(reversal.copy(id = id(855), operationId = id(856))))
        assertEquals(-250_000L, db.financeDao().entries(workspace.value, expense.id.value).single().deltaMinor)
        assertEquals(250_000L, db.financeDao().entries(workspace.value, reversal.id.value).single().deltaMinor)
        assertEquals(10_000_000L, (repo.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(0L, (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value.netExpenseMinor)
        db.close()
    }

    @Test fun candidateBudgetAndReconciliationDoNotPostLedger() = runBlocking {
        val db = setup("finance-supporting-records.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(bank)
        val category = Category(id(870), workspace, "Fixture expense", CategoryDirection.EXPENSE)
        repo.createCategory(category)
        db.financeDao().insertEvidence(SourceEvidenceEntity(id(871).value, workspace.value, null, "",
            "MANUAL", "fixture-event", null, "", null, null, now.toEpochMilli(), null))
        db.financeDao().insertCandidate(ImportCandidateEntity(id(872).value, workspace.value, id(871).value,
            1, 50_000, "VND", "EXPENSE", now.toEpochMilli(), bank.id.value, category.id.value,
            "", 9_000, "PENDING", null))
        db.financeDao().insertBudget(BudgetEntity(id(873).value, workspace.value, category.id.value,
            "2026-10-01", "VND", 100_000, 8_000))
        db.financeDao().insertReconciliation(ReconciliationEntity(id(874).value, workspace.value, bank.id.value,
            now.toEpochMilli(), 9_900_000, 10_000_000, -100_000, "Fixture discrepancy", "OPEN"))
        assertEquals(10_000_000L, (repo.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertThrows(IllegalArgumentException::class.java) {
            BudgetEntity(id(875).value, workspace.value, category.id.value, "2026-10-01", "VND", -1, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReconciliationEntity(id(876).value, workspace.value, bank.id.value, now.toEpochMilli(),
                9_900_000, 10_000_000, 0, null, "OPEN")
        }
        db.close()
    }

    @Test fun manualCashIncomeAndExpenseStayAfterReopenAndDuplicateSubmit() = runBlocking {
        val name = "finance-manual-cash.db"
        val db = setup(name)
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(cash)
        val incomeCategory = Category(id(880), workspace, "Fixture income", CategoryDirection.INCOME)
        val expenseCategory = Category(id(881), workspace, "Fixture expense", CategoryDirection.EXPENSE)
        repo.createCategory(incomeCategory); repo.createCategory(expenseCategory)
        val commands = ManualFinanceCommands(repo, Locale.forLanguageTag("vi-VN"))
        val income = ManualEntryInput(id(882), id(883), workspace, TransactionKind.INCOME, cash, null,
            incomeCategory, "1.500.000", now.minusSeconds(60), "Fixture", null)
        val expense = ManualEntryInput(id(884), id(885), workspace, TransactionKind.EXPENSE, cash, null,
            expenseCategory, "250.000", now, null, "Fixture expense")
        assertTrue(commands.post(income) is Outcome.Success)
        assertTrue(commands.post(expense) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.Conflict), commands.post(expense))
        assertEquals(1_250_000L, (repo.balance(workspace, cash.id, now) as Outcome.Success).value.amountMinor)
        db.close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val persisted = RoomFinanceRepository(reopened, clock, RandomIdGenerator)
        assertEquals(1_250_000L, (persisted.balance(workspace, cash.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(Outcome.Failure(AppError.Conflict),
            ManualFinanceCommands(persisted, Locale.forLanguageTag("vi-VN")).post(expense))
        val report = (persisted.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value
        assertEquals(1_500_000L, report.incomeMinor)
        assertEquals(250_000L, report.netExpenseMinor)
        assertEquals(2, (persisted.observeRecentTransactions(workspace).first() as Outcome.Success).value.size)
        assertEquals("MANUAL", reopened.financeDao().evidenceForTransaction(workspace.value, expense.transactionId.value).single().sourceKind)
        reopened.close()
    }

    @Test fun refundUsesOriginalCategoryAndActualMonthWithSignedNetExpense() = runBlocking {
        val db = setup("finance-refund-month.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(bank)
        val category = Category(id(890), workspace, "Fixture expense", CategoryDirection.EXPENSE)
        repo.createCategory(category)
        val october = now.minusSeconds(86_400)
        val expense = (repo.post(PostFinanceCommand(id(891), workspace, id(892), TransactionKind.EXPENSE,
            bank.id, Money(500_000, vnd), october, categoryId = category.id)) as Outcome.Success).value
        val row = PostedFinanceRow(expense, listOf(LedgerEntry(id(893), expense.id, bank.id, -500_000, vnd)))
        val commands = ManualRefundAdjustmentCommands(repo, Locale.forLanguageTag("vi-VN"))
        val first = (commands.refund(ManualRefundInput(id(894), id(895), workspace, row, bank,
            "100000", now, "Fixture partial")) as Outcome.Success).value
        assertEquals(expense.id, first.refundOfId)
        assertEquals(category.id, first.categoryId)
        assertEquals(9_600_000L, (repo.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        val same = (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value
        assertEquals(0L, same.incomeMinor)
        assertEquals(500_000L, same.grossExpenseMinor)
        assertEquals(100_000L, same.refundMinor)
        assertEquals(400_000L, same.netExpenseMinor)
        assertEquals(Outcome.Failure(AppError.InvalidInput("amount")), commands.refund(
            ManualRefundInput(id(896), id(897), workspace, row, bank, "600000", now, null)))
        assertEquals(Outcome.Failure(AppError.Conflict), commands.refund(
            ManualRefundInput(id(898), id(895), workspace, row, bank, "100000", now, null)))
        db.close()

        val nextDb = setup("finance-refund-next-month.db")
        val nextRepo = RoomFinanceRepository(nextDb, clock, RandomIdGenerator)
        nextRepo.createAccount(bank); nextRepo.createCategory(category)
        val oldExpense = (nextRepo.post(PostFinanceCommand(id(899), workspace, id(900), TransactionKind.EXPENSE,
            bank.id, Money(500_000, vnd), now.minusSeconds(86_400), categoryId = category.id)) as Outcome.Success).value
        val oldRow = PostedFinanceRow(oldExpense, listOf(LedgerEntry(id(901), oldExpense.id, bank.id, -500_000, vnd)))
        val november = Instant.parse("2026-11-06T12:00:00Z")
        assertTrue(ManualRefundAdjustmentCommands(nextRepo, Locale.forLanguageTag("vi-VN")).refund(
            ManualRefundInput(id(902), id(903), workspace, oldRow, bank, "100000", november, null)) is Outcome.Success)
        val report = (nextRepo.report(FinanceReportQuery(workspace, vnd,
            Instant.parse("2026-11-01T00:00:00Z"), november.plusSeconds(1))) as Outcome.Success).value
        assertEquals(0L, report.incomeMinor)
        assertEquals(0L, report.grossExpenseMinor)
        assertEquals(-100_000L, report.netExpenseMinor)
        nextDb.close()
    }

    @Test fun adjustmentChangesBalanceButNeverIncomeOrExpense() = runBlocking {
        val db = setup("finance-adjustment.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(bank)
        val commands = ManualRefundAdjustmentCommands(repo, Locale.forLanguageTag("vi-VN"))
        assertEquals(Outcome.Failure(AppError.InvalidInput("reason")), commands.adjust(
            ManualAdjustmentInput(id(910), id(911), workspace, bank, "-250000", now, " ")))
        assertTrue(commands.adjust(ManualAdjustmentInput(id(912), id(913), workspace, bank,
            "-250000", now, "Fixture counted cash")) is Outcome.Success)
        assertTrue(commands.adjust(ManualAdjustmentInput(id(914), id(915), workspace, bank,
            "50000", now, "Fixture correction")) is Outcome.Success)
        assertEquals("Fixture counted cash", db.financeDao().transaction(workspace.value, id(912).value)?.note)
        assertEquals(9_800_000L, (repo.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        val report = (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value
        assertEquals(0L, report.incomeMinor)
        assertEquals(0L, report.grossExpenseMinor)
        assertEquals(0L, report.netExpenseMinor)
        db.close()
    }

    @Test fun concurrentPartialRefundsCannotExceedOriginal() = runBlocking {
        val db = setup("finance-refund-race.db")
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.createAccount(bank)
        val category = Category(id(920), workspace, "Fixture expense", CategoryDirection.EXPENSE)
        repo.createCategory(category)
        val original = (repo.post(PostFinanceCommand(id(921), workspace, id(922), TransactionKind.EXPENSE,
            bank.id, Money(500_000, vnd), now.minusSeconds(60), categoryId = category.id)) as Outcome.Success).value
        val row = PostedFinanceRow(original, listOf(LedgerEntry(id(923), original.id, bank.id, -500_000, vnd)))
        val a = async(Dispatchers.Default) { ManualRefundAdjustmentCommands(RoomFinanceRepository(db, clock, RandomIdGenerator),
            Locale.forLanguageTag("vi-VN")).refund(ManualRefundInput(id(924), id(925), workspace, row, bank, "300000", now, null)) }
        val b = async(Dispatchers.Default) { ManualRefundAdjustmentCommands(RoomFinanceRepository(db, clock, RandomIdGenerator),
            Locale.forLanguageTag("vi-VN")).refund(ManualRefundInput(id(926), id(927), workspace, row, bank, "300000", now, null)) }
        val results = listOf(a.await(), b.await())
        assertEquals(1, results.count { it is Outcome.Success })
        assertEquals(1, results.count { it == Outcome.Failure(AppError.InvalidInput("amount")) })
        assertEquals(300_000L, (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value.refundMinor)
        db.close()
    }
}
