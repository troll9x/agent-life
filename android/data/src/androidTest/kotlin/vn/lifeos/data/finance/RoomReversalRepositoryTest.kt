package vn.lifeos.data.finance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.finance.reversal.RoomReversalRepository
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomReversalRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(901)
    private val vnd = CurrencyCode.of("VND")
    private val oct = Instant.parse("2026-10-29T12:00:00Z")
    private val nov = Instant.parse("2026-11-06T12:00:00Z")
    private val opening = Instant.parse("2026-10-01T00:00:00Z")
    private val clock = Clock.fixed(nov, ZoneOffset.UTC)
    private val bank = Account(id(905), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, opening)
    private val category = Category(id(906), workspace, "Fixture expense", CategoryDirection.EXPENSE)

    private suspend fun fixture(name: String): Pair<AgentLifeDatabase, RoomFinanceRepository> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(902), id(903), id(904), "Personal", "Fixture",
                "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val repo = RoomFinanceRepository(db, clock, RandomIdGenerator)
        assertTrue(repo.createAccount(bank) is Outcome.Success)
        assertTrue(repo.createCategory(category) is Outcome.Success)
        return db to repo
    }

    private suspend fun original(repo: RoomFinanceRepository): FinancialTransaction {
        val command = PostFinanceCommand(id(910), workspace, id(911), TransactionKind.EXPENSE,
            bank.id, Money(500_000, vnd), oct, categoryId = category.id)
        return (repo.post(command) as Outcome.Success).value
    }

    private fun correction(source: FinancialTransaction, date: Instant = oct.plusSeconds(60),
        transaction: Uuid = id(914), operation: Uuid = id(915), sourceAccount: Account = bank) =
        ReplacePostedTransaction(source.id, source.postedAt, id(912), id(913),
            ManualEntryInput(transaction, operation, workspace, TransactionKind.EXPENSE,
                sourceAccount, null, category, "600000", date, null, "Fixture corrected"))

    @Test fun replacementInSamePeriodChangesBalanceByOnlyDifference() = runBlocking {
        val (db, repo) = fixture("finance-replace-same.db")
        val source = original(repo)
        val service = RoomReversalRepository(db, clock, RandomIdGenerator)
        val result = service.replace(correction(source)) as Outcome.Success
        assertEquals(source.id, result.value.reversal.reversesId)
        assertEquals(source.id, result.value.replacement.replacementOfId)
        assertEquals(9_400_000L, (repo.balance(workspace, bank.id, nov) as Outcome.Success).value.amountMinor)
        assertEquals(600_000L, (repo.report(FinanceReportQuery(workspace, vnd, opening, nov)) as Outcome.Success).value.netExpenseMinor)
        assertEquals(3, db.financeDao().transactionsInPeriod(workspace.value, opening.toEpochMilli(), nov.toEpochMilli()).size)
        assertEquals(Outcome.Failure(AppError.Conflict), service.replace(correction(source)))
        db.close()
    }

    @Test fun failedReplacementRollsBackReversalAndAllowsLaterCorrection() = runBlocking {
        val (db, repo) = fixture("finance-replace-rollback.db")
        val source = original(repo)
        val service = RoomReversalRepository(db, clock, RandomIdGenerator)
        val invalid = correction(source, sourceAccount = bank.copy(id = id(999)))
        assertEquals(Outcome.Failure(AppError.InvalidInput("accountId")), service.replace(invalid))
        assertNull(db.financeDao().reversal(workspace.value, source.id.value))
        assertNull(db.financeDao().transaction(workspace.value, id(914).value))
        assertEquals(9_500_000L, (repo.balance(workspace, bank.id, nov) as Outcome.Success).value.amountMinor)
        assertTrue(service.replace(correction(source)) is Outcome.Success)
        db.close()
    }

    @Test fun competingClientsAndStaleVersionCannotReverseTwice() = runBlocking {
        val (db, repo) = fixture("finance-replace-race.db")
        val source = original(repo)
        val first = RoomReversalRepository(db, clock, RandomIdGenerator)
        val second = RoomReversalRepository(db, clock, RandomIdGenerator)
        val a = async(Dispatchers.Default) { first.replace(correction(source)) }
        val b = async(Dispatchers.Default) { second.replace(correction(source, transaction = id(916), operation = id(917))) }
        val results = listOf(a.await(), b.await())
        assertEquals(1, results.count { it is Outcome.Success })
        assertEquals(1, results.count { it == Outcome.Failure(AppError.Conflict) })
        assertEquals(3, db.financeDao().transactionsInPeriod(workspace.value, opening.toEpochMilli(), nov.toEpochMilli()).size)
        assertEquals(Outcome.Failure(AppError.Conflict), first.replace(correction(source.copy(postedAt = oct))))
        db.close()
    }

    @Test fun laterCorrectionKeepsOriginalPeriodAndRefundNeedsReview() = runBlocking {
        val (db, repo) = fixture("finance-replace-period.db")
        val source = original(repo)
        val service = RoomReversalRepository(db, clock, RandomIdGenerator)
        assertTrue(service.replace(correction(source, date = nov)) is Outcome.Success)
        val october = repo.report(FinanceReportQuery(workspace, vnd, opening, Instant.parse("2026-11-01T00:00:00Z"))) as Outcome.Success
        val november = repo.report(FinanceReportQuery(workspace, vnd, Instant.parse("2026-11-01T00:00:00Z"), nov.plusSeconds(1))) as Outcome.Success
        assertEquals(500_000L, october.value.netExpenseMinor)
        assertEquals(100_000L, november.value.netExpenseMinor)
        db.close()

        val (refundDb, refundRepo) = fixture("finance-replace-refund.db")
        val refundable = original(refundRepo)
        assertTrue(refundRepo.post(PostFinanceCommand(id(918), workspace, id(919), TransactionKind.REFUND,
            bank.id, Money(100_000, vnd), nov, relatedTransactionId = refundable.id)) is Outcome.Success)
        assertEquals(Outcome.Failure(AppError.Conflict),
            RoomReversalRepository(refundDb, clock, RandomIdGenerator).replace(correction(refundable, date = nov)))
        assertNull(refundDb.financeDao().reversal(workspace.value, refundable.id.value))
        refundDb.close()
    }

    @Test fun transferCorrectionReversesBothEntriesBeforeMovingNewAmount() = runBlocking {
        val (db, repo) = fixture("finance-replace-transfer.db")
        val cash = Account(id(920), workspace, "Fixture cash", AccountKind.CASH, vnd, 0, opening)
        assertTrue(repo.createAccount(cash) is Outcome.Success)
        val transfer = (repo.post(PostFinanceCommand(id(921), workspace, id(922), TransactionKind.TRANSFER,
            bank.id, Money(500_000, vnd), oct, destinationAccountId = cash.id)) as Outcome.Success).value
        val command = ReplacePostedTransaction(transfer.id, transfer.postedAt, id(923), id(924),
            ManualEntryInput(id(925), id(926), workspace, TransactionKind.TRANSFER,
                bank, cash, null, "600000", oct.plusSeconds(60), null, null))
        val changed = (RoomReversalRepository(db, clock, RandomIdGenerator).replace(command) as Outcome.Success).value
        val sourceEntries = db.financeDao().entries(workspace.value, transfer.id.value).associateBy { it.accountId }
        val undoEntries = db.financeDao().entries(workspace.value, changed.reversal.id.value).associateBy { it.accountId }
        assertEquals(2, undoEntries.size)
        sourceEntries.forEach { (accountId, entry) -> assertEquals(-entry.deltaMinor, undoEntries[accountId]?.deltaMinor) }
        assertEquals(9_400_000L, (repo.balance(workspace, bank.id, nov) as Outcome.Success).value.amountMinor)
        assertEquals(600_000L, (repo.balance(workspace, cash.id, nov) as Outcome.Success).value.amountMinor)
        assertEquals(0L, (repo.report(FinanceReportQuery(workspace, vnd, opening, nov)) as Outcome.Success).value.netExpenseMinor)
        db.close()
    }
}
