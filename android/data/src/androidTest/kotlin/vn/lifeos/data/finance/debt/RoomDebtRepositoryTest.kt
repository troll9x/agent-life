package vn.lifeos.data.finance.debt

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
import vn.lifeos.domain.common.*
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.debt.*
import vn.lifeos.domain.foundation.*
import vn.lifeos.domain.foundation.AppError

@RunWith(AndroidJUnit4::class)
class RoomDebtRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val now = Instant.parse("2026-11-12T10:00:00Z")
    private val start = Instant.parse("2026-11-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val vnd = CurrencyCode.of("VND")
    private val workspace = id(1001)
    private val bank = Account(id(1005), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, start)
    private val interest = Category(id(1006), workspace, "Fixture interest", CategoryDirection.EXPENSE)
    private val fee = Category(id(1007), workspace, "Fixture fee", CategoryDirection.EXPENSE)
    private val agreement = DebtAgreement(id(1010), workspace, DebtRole.BORROWER, id(1011), "Fixture partner", vnd,
        0, LocalDate.parse("2026-12-12"))
    private suspend fun setup(name: String): AgentLifeDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(1002), id(1003), id(1004), "Personal", "Fixture",
                "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        ledger.createAccount(bank); ledger.createCategory(interest); ledger.createCategory(fee)
        return db
    }
    private fun command(n: Int, action: DebtAction, principal: Long, interestMinor: Long = 0, feeMinor: Long = 0) =
        PostDebtMovement(id(n), workspace, agreement.id, agreement.counterpartyId, id(n + 100), bank.id,
            vnd, action, principal, interestMinor, feeMinor, interest.id.takeIf { interestMinor > 0 },
            fee.id.takeIf { feeMinor > 0 }, now)
    private suspend fun position(repo: RoomDebtRepository) = (repo.observe(workspace).first() as Outcome.Success).value.single()
    private suspend fun report(repo: RoomFinanceRepository) =
        (repo.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value

    @Test fun borrowerPrincipalCostsCorrectionAndReopenReconcile() = runBlocking {
        val name = "debt-borrower.db"
        val db = setup(name)
        val repo = RoomDebtRepository(db, clock, RandomIdGenerator)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        assertTrue(repo.create(agreement) is Outcome.Success)
        assertTrue(repo.post(command(1020, DebtAction.ADVANCE, 5_000_000)) is Outcome.Success)
        assertTrue(repo.post(command(1021, DebtAction.REPAY, 1_000_000)) is Outcome.Success)
        assertEquals(4_000_000L, position(repo).outstandingMinor)
        assertEquals(0L, report(ledger).incomeMinor); assertEquals(0L, report(ledger).netExpenseMinor)
        assertEquals(14_000_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertTrue(repo.reverse(ReverseDebtMovement(id(1039), workspace, agreement.id, id(1020), id(1139), now,
            "Cannot undo advance after repayment")) is Outcome.Failure)
        assertTrue(repo.post(command(1022, DebtAction.REPAY, 0, 100_000)) is Outcome.Success)
        assertEquals(100_000L, report(ledger).netExpenseMinor)
        val split = (repo.post(command(1023, DebtAction.REPAY, 500_000, 50_000, 20_000)) as Outcome.Success).value
        assertEquals(3_500_000L, position(repo).outstandingMinor)
        assertNotNull(split.transactionId); assertNotNull(split.interestTransactionId); assertNotNull(split.feeTransactionId)
        assertEquals(170_000L, report(ledger).netExpenseMinor)
        assertEquals(150_000L, position(repo).interestMinor); assertEquals(20_000L, position(repo).feeMinor)
        assertEquals(interest.id.value, db.financeDao().transaction(workspace.value, split.interestTransactionId!!.value)?.categoryId)
        assertEquals(fee.id.value, db.financeDao().transaction(workspace.value, split.feeTransactionId!!.value)?.categoryId)
        assertEquals(Outcome.Failure(AppError.InvalidInput("debtId")), ledger.post(PostFinanceCommand(id(1040), workspace,
            id(1041), TransactionKind.REVERSAL, bank.id, Money(500_000, vnd), now, relatedTransactionId = split.transactionId)))
        val correction = ReverseDebtMovement(id(1030), workspace, agreement.id, split.id, id(1130), now, "Fixture correction")
        assertTrue(repo.reverse(correction) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.Conflict), repo.reverse(correction.copy(id = id(1031), operationId = id(1131))))
        assertEquals(4_000_000L, position(repo).outstandingMinor)
        assertEquals(100_000L, report(ledger).netExpenseMinor)
        assertEquals(13_900_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        position(repo).movements.filter { it.transactionId != null }.forEach { movement ->
            assertEquals(movement.principalDeltaMinor,
                db.financeDao().entries(workspace.value, movement.transactionId!!.value).single().deltaMinor)
        }
        db.openHelper.readableDatabase.query("SELECT count(*) FROM audit_events WHERE action='REVERSE_DEBT_MOVEMENT'").use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
        db.close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val persisted = position(RoomDebtRepository(reopened, clock, RandomIdGenerator))
        assertEquals(4_000_000L, persisted.outstandingMinor)
        assertEquals(5, persisted.movements.size)
        assertEquals(agreement.dueDate, persisted.agreement.dueDate)
        assertEquals(100_000L, persisted.interestMinor)
        reopened.close()
    }

    @Test fun lenderCollectsIncomeSeparatelyAndRejectsInvalidCommandsAtomically() = runBlocking {
        val db = setup("debt-lender.db")
        val repo = RoomDebtRepository(db, clock, RandomIdGenerator)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.create(agreement.copy(role = DebtRole.LENDER, openingPrincipalMinor = 1_000_000))
        ledger.createCategory(interest.copy(direction = CategoryDirection.INCOME, id = id(1050)))
        ledger.createCategory(fee.copy(direction = CategoryDirection.INCOME, id = id(1051)))
        assertTrue(repo.post(command(1052, DebtAction.ADVANCE, 5_000_000)) is Outcome.Success)
        assertTrue(repo.post(command(1053, DebtAction.REPAY, 1_000_000, 100_000, 20_000)
            .copy(interestCategoryId = id(1050), feeCategoryId = id(1051))) is Outcome.Success)
        assertEquals(5_000_000L, position(repo).outstandingMinor)
        assertEquals(120_000L, report(ledger).incomeMinor); assertEquals(0L, report(ledger).netExpenseMinor)
        assertEquals(6_120_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        val count = position(repo).movements.size
        assertTrue(repo.post(command(1054, DebtAction.REPAY, 5_000_001)) is Outcome.Failure)
        assertTrue(repo.post(command(1055, DebtAction.REPAY, 1).copy(currency = CurrencyCode.of("USD"))) is Outcome.Failure)
        assertTrue(repo.post(command(1056, DebtAction.REPAY, 1).copy(counterpartyId = id(9999))) is Outcome.Failure)
        assertTrue(repo.post(command(1057, DebtAction.REPAY, 1).copy(workspaceId = id(9999))) is Outcome.Failure)
        assertTrue(repo.post(command(1061, DebtAction.REPAY, 1).copy(accountId = id(9999))) is Outcome.Failure)
        assertTrue(repo.post(command(1058, DebtAction.REPAY, 1).copy(occurredAt = now.minusSeconds(1))) is Outcome.Failure)
        assertTrue(repo.post(command(1059, DebtAction.REPAY, 1).copy(occurredAt = now.plusSeconds(1))) is Outcome.Failure)
        // Principal and interest are already inserted when fee validation fails: all must roll back.
        assertTrue(repo.post(command(1060, DebtAction.REPAY, 1_000_000, 100_000, 20_000)
            .copy(interestCategoryId = id(1050), feeCategoryId = fee.id)) is Outcome.Failure)
        assertEquals(null, db.financeDao().byOperation(workspace.value, id(1160).value))
        assertEquals(count, position(repo).movements.size)
        assertEquals(6_120_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        db.close()
    }

    @Test fun duplicateMovementFailureAndConcurrentOverpaymentLeaveNoPartialLedger() = runBlocking {
        val db = setup("debt-atomic.db")
        val repo = RoomDebtRepository(db, clock, RandomIdGenerator)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        repo.create(agreement.copy(openingPrincipalMinor = 1_000_000))
        val first = command(1070, DebtAction.REPAY, 100_000)
        assertTrue(repo.post(first) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.Conflict), repo.post(first))
        // A new ledger header is inserted first; movement ID collision must roll it back.
        assertEquals(Outcome.Failure(AppError.Conflict), repo.post(first.copy(operationId = id(1171))))
        assertEquals(null, db.financeDao().byOperation(workspace.value, id(1171).value))
        val a = async(Dispatchers.IO) { repo.post(command(1072, DebtAction.REPAY, 800_000)) }
        val b = async(Dispatchers.IO) { repo.post(command(1073, DebtAction.REPAY, 800_000)) }
        val results = listOf(a.await(), b.await())
        assertEquals(1, results.count { it is Outcome.Success })
        assertEquals(100_000L, position(repo).outstandingMinor)
        assertEquals(2, position(repo).movements.size)
        assertEquals(9_100_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(0L, report(ledger).netExpenseMinor)
        db.close()
    }
}
