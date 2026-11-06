package vn.lifeos.data.finance.imports

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
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
import vn.lifeos.domain.finance.imports.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomFinanceInboxRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(1)
    private val vnd = CurrencyCode.of("VND")
    private val now = Instant.parse("2026-10-25T12:00:00Z")
    private val occurred = Instant.parse("2026-10-20T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val bank = Account(id(5), workspace, "Fixture bank", AccountKind.BANK, vnd, 1_000_000,
        Instant.parse("2026-10-01T00:00:00Z"))
    private val income = Category(id(6), workspace, "Fixture salary", CategoryDirection.INCOME)
    private val expense = Category(id(7), workspace, "Fixture expense", CategoryDirection.EXPENSE)

    private suspend fun setup(name: String): AgentLifeDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(2), id(3), id(4), "Personal", "Fixture", "UTC",
                "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        assertTrue(ledger.createAccount(bank) is Outcome.Success)
        assertTrue(ledger.createCategory(income) is Outcome.Success)
        assertTrue(ledger.createCategory(expense) is Outcome.Success)
        return db
    }

    private fun inbound(n: Int, kind: FinanceSourceKind, event: String, reference: String?,
        direction: CategoryDirection = CategoryDirection.INCOME,
        amount: Long = 200_000, at: Instant = occurred) = InboundFinanceEvidence(
            id(n), id(n + 1), workspace, null, kind, event, null, reference, "***0000", now,
            1, amount, vnd, direction, at, bank.id,
            if (direction == CategoryDirection.INCOME) income.id else expense.id, "", 9000, reference != null)

    private fun post(n: Int, kind: TransactionKind, amount: Long = 200_000): PostFinanceCommand =
        PostFinanceCommand(id(n), workspace, id(n + 1), kind, bank.id, Money(amount, vnd), occurred,
            categoryId = if (kind == TransactionKind.INCOME) income.id else expense.id)

    @Test fun manualSalaryNotificationAndMailBecomeOneLedgerRow() = runBlocking {
        val db = setup("inbox-salary.db")
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        val manual = post(20, TransactionKind.INCOME)
        assertTrue(ledger.post(manual) is Outcome.Success)
        val notification = inbound(30, FinanceSourceKind.NOTIFICATION, "notify-1", "SALARY-123")
        assertTrue(inbox.ingest(notification) is Outcome.Success)
        val first = (inbox.observe(workspace).first() as Outcome.Success).value.single()
        assertEquals(MatchReason.AMOUNT_TIME_SUGGESTION, first.suggestions.single().reason)
        assertEquals(manual.id, first.suggestions.single().transactionId)
        assertEquals(CandidateState.DUPLICATE,
            (inbox.decide(workspace, notification.candidateId, InboxDecision.Duplicate(manual.id)) as Outcome.Success).value.state)
        val mail = inbound(40, FinanceSourceKind.MAIL, "mail-1", "SALARY-123")
        assertTrue(inbox.ingest(mail) is Outcome.Success)
        val second = (inbox.observe(workspace).first() as Outcome.Success).value.single()
        assertEquals(MatchReason.VERIFIED_REFERENCE, second.suggestions.single().reason)
        assertEquals(manual.id, second.suggestions.single().transactionId)
        assertEquals(Outcome.Failure(AppError.Conflict), inbox.decide(workspace, mail.candidateId,
            InboxDecision.Approve(post(50, TransactionKind.INCOME))))
        assertTrue(inbox.decide(workspace, mail.candidateId, InboxDecision.Duplicate(manual.id)) is Outcome.Success)
        assertEquals(CandidateState.DUPLICATE, (inbox.ingest(mail) as Outcome.Success).value.state)
        assertEquals(3, db.financeDao().evidenceForTransaction(workspace.value, manual.id.value).size)
        assertEquals(1_200_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(1, (ledger.observeRecentTransactions(workspace).first() as Outcome.Success).value.size)
        db.close()
    }

    @Test fun sameAmountDifferentReferenceAreTwoRowsAndAmbiguityWaits() = runBlocking {
        val db = setup("inbox-different-reference.db")
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        val first = inbound(60, FinanceSourceKind.NOTIFICATION, "notify-a", "REF-A", CategoryDirection.EXPENSE)
        val second = inbound(70, FinanceSourceKind.MAIL, "mail-b", "REF-B", CategoryDirection.EXPENSE)
        assertTrue(inbox.ingest(first) is Outcome.Success)
        assertTrue(inbox.decide(workspace, first.candidateId, InboxDecision.Approve(post(80, TransactionKind.EXPENSE))) is Outcome.Success)
        assertTrue(inbox.ingest(second) is Outcome.Success)
        assertTrue((inbox.observe(workspace).first() as Outcome.Success).value.single().suggestions.isEmpty())
        assertTrue(inbox.decide(workspace, second.candidateId, InboxDecision.Approve(post(90, TransactionKind.EXPENSE))) is Outcome.Success)
        assertEquals(600_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        assertEquals(2, (ledger.observeRecentTransactions(workspace).first() as Outcome.Success).value.size)
        val manualA = post(100, TransactionKind.INCOME)
        val manualB = post(110, TransactionKind.INCOME)
        assertTrue(ledger.post(manualA) is Outcome.Success)
        assertTrue(ledger.post(manualB) is Outcome.Success)
        val ambiguous = inbound(120, FinanceSourceKind.MAIL, "mail-ambiguous", null)
        assertEquals(CandidateState.NEEDS_REVIEW, (inbox.ingest(ambiguous) as Outcome.Success).value.state)
        assertEquals(2, (inbox.observe(workspace).first() as Outcome.Success).value.single().suggestions.size)
        assertTrue(inbox.decide(workspace, ambiguous.candidateId, InboxDecision.Ignore) is Outcome.Success)
        assertEquals(1_000_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        db.close()
    }

    @Test fun updatedSourceRevisionSupersedesPendingWithoutPosting() = runBlocking {
        val db = setup("inbox-revision.db")
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        val first = inbound(130, FinanceSourceKind.MAIL, "mail-update", null).copy(revisionHash = "r1")
        val second = inbound(140, FinanceSourceKind.MAIL, "mail-update", null).copy(revisionHash = "r2")
        assertEquals(CandidateState.PENDING, (inbox.ingest(first) as Outcome.Success).value.state)
        assertEquals(CandidateState.NEEDS_REVIEW, (inbox.ingest(second) as Outcome.Success).value.state)
        assertEquals(listOf(second.candidateId), (inbox.observe(workspace).first() as Outcome.Success).value.map { it.candidate.id })
        assertEquals(CandidateState.IGNORED,
            (inbox.ingest(first) as Outcome.Success).value.state)
        assertEquals(Outcome.Failure(AppError.InvalidInput("evidence")),
            inbox.ingest(inbound(150, FinanceSourceKind.MAIL, "unverified", "REF-1").copy(bankReferenceVerified = false)))
        assertEquals(Outcome.Failure(AppError.InvalidInput("evidence")),
            inbox.ingest(inbound(160, FinanceSourceKind.MAIL, "raw-text", null).copy(missingFields = "raw message")))
        val ledger = RoomFinanceRepository(db, clock, RandomIdGenerator)
        assertEquals(1_000_000L, (ledger.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor)
        db.close()
    }
}
