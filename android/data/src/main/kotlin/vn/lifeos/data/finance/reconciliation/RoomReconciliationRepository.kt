package vn.lifeos.data.finance.reconciliation

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.AuditEventEntity
import vn.lifeos.data.finance.*
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.reconciliation.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

class RoomReconciliationRepository(private val database: AgentLifeDatabase, private val clock: Clock,
    private val ids: IdGenerator) : ReconciliationRepository {
    private val dao = database.reconciliationDao()
    private val finance = database.financeDao()
    private val ledger = RoomFinanceRepository(database, clock, ids)

    override fun observe(workspaceId: Uuid): Flow<Outcome<List<ReconciliationView>>> =
        combine(dao.observe(workspaceId.value), database.financeInboxDao().observeOpen(workspaceId.value),
            database.financeInboxDao().observeLedgerCount(workspaceId.value), database.foundationDao().observeConnections(workspaceId.value)) { _, _, _, _ ->
            database.withTransaction { Outcome.Success(dao.snapshots(workspaceId.value).map { view(it) }) as Outcome<List<ReconciliationView>> }
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun capture(command: ObserveBalance): Outcome<Reconciliation> = write {
        if (finance.activeWorkspace(command.workspaceId.value) == null) missing()
        val account = finance.account(command.workspaceId.value, command.accountId.value) ?: missing()
        if (account.currency != command.currency.value) invalid("currency")
        if (command.asOf.isAfter(clock.instant())) invalid("asOf")
        val expected = success(ledger.balance(command.workspaceId, command.accountId, command.asOf)).amountMinor
        val difference = Math.subtractExact(command.observedMinor, expected)
        val row = ReconciliationEntity(command.id.value, command.workspaceId.value, command.accountId.value,
            command.asOf.toEpochMilli(), command.observedMinor, expected, difference, command.note?.trim(), "OPEN")
        finance.insertReconciliation(row)
        audit(command.workspaceId, command.id, "CAPTURE_RECONCILIATION")
        row.domain()
    }
    override suspend fun resolve(workspaceId: Uuid, snapshotId: Uuid): Outcome<Unit> = write {
        if (finance.activeWorkspace(workspaceId.value) == null) missing()
        val row = dao.snapshot(workspaceId.value, snapshotId.value) ?: missing()
        if (row.state != "OPEN") conflict()
        if (view(row).currentDifferenceMinor != 0L) invalid("difference")
        if (dao.resolve(workspaceId.value, snapshotId.value) != 1) conflict()
        audit(workspaceId, snapshotId, "RESOLVE_RECONCILIATION")
    }
    override suspend fun review(workspaceId: Uuid, snapshotId: Uuid): Outcome<ReconciliationReview> = read {
        database.withTransaction {
            if (finance.activeWorkspace(workspaceId.value) == null) missing()
            val row = dao.snapshot(workspaceId.value, snapshotId.value) ?: missing()
            val view = view(row)
            val from = Instant.ofEpochMilli(row.asOfInstant).minusSeconds(7 * 86_400)
            val end = Instant.ofEpochMilli(row.asOfInstant).plusSeconds(7 * 86_400)
            val transactions = finance.transactionsInPeriod(workspaceId.value, from.toEpochMilli(), end.toEpochMilli())
            val entries = finance.entriesForTransactions(workspaceId.value, transactions.map { it.id }).groupBy { it.transactionId }
            val rows = transactions.filter { entries[it.id].orEmpty().any { e -> e.accountId == row.accountId } }
                .map { PostedFinanceRow(it.domain(), entries[it.id].orEmpty().map(LedgerEntryEntity::domain)) }
            val candidates = dao.candidates(workspaceId.value, view.currency.value, row.accountId, from.toEpochMilli(), end.toEpochMilli())
            ReconciliationReview(view, from, end, rows, candidates.map(ImportCandidateEntity::domain))
        }
    }
    private suspend fun view(row: ReconciliationEntity): ReconciliationView {
        val balance = success(ledger.balance(Uuid.parse(row.workspaceId), Uuid.parse(row.accountId), Instant.ofEpochMilli(row.asOfInstant)))
        return ReconciliationView(row.domain(), balance.currency, balance.amountMinor)
    }
    private suspend fun audit(workspace: Uuid, id: Uuid, action: String) = database.foundationDao().insertAudit(
        AuditEventEntity(ids.newId().value, workspace.value, "USER", action, "RECONCILIATION", id.value, clock.millis()))
    private fun invalid(field: String): Nothing = throw Abort(Outcome.Failure(AppError.InvalidInput(field)))
    private fun missing(): Nothing = throw Abort(Outcome.Failure(AppError.NotFound))
    private fun conflict(): Nothing = throw Abort(Outcome.Failure(AppError.Conflict))
    private fun <T> success(result: Outcome<T>): T = when (result) { is Outcome.Success -> result.value; is Outcome.Failure -> throw Abort(result) }
    private class Abort(val failure: Outcome.Failure) : RuntimeException()
    private suspend fun <T> write(block: suspend () -> T): Outcome<T> = read { database.withTransaction { block() } }
    private suspend fun <T> read(block: suspend () -> T): Outcome<T> = try { Outcome.Success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Abort) { error.failure }
    catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
    catch (_: ArithmeticException) { Outcome.Failure(AppError.InvalidInput("amount")) }
    catch (_: IllegalArgumentException) { Outcome.Failure(AppError.InvalidInput("reconciliation")) }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
}

private fun ReconciliationEntity.domain() = Reconciliation(Uuid.parse(id), Uuid.parse(workspaceId), Uuid.parse(accountId),
    Instant.ofEpochMilli(asOfInstant), observedBalanceMinor, expectedBalanceMinor, differenceMinor, note, ReconciliationState.valueOf(state))
private fun LedgerEntryEntity.domain() = LedgerEntry(Uuid.parse(id), Uuid.parse(transactionId), Uuid.parse(accountId), deltaMinor, CurrencyCode.of(currency))
private fun FinancialTransactionEntity.domain() = FinancialTransaction(Uuid.parse(id), Uuid.parse(workspaceId), Uuid.parse(operationId),
    TransactionKind.valueOf(kind), Instant.ofEpochMilli(occurredAt), Instant.ofEpochMilli(postedAt), categoryId?.let(Uuid::parse), payee, note,
    reversesId?.let(Uuid::parse), refundOfId?.let(Uuid::parse), replacementOfId?.let(Uuid::parse), debtId?.let(Uuid::parse))
private fun ImportCandidateEntity.domain() = ImportCandidate(Uuid.parse(id), Uuid.parse(workspaceId), Uuid.parse(sourceEvidenceId), parserVersion,
    amountMinor, currency?.let(CurrencyCode::of), direction?.let(CategoryDirection::valueOf), occurredAt?.let(Instant::ofEpochMilli),
    proposedAccountId?.let(Uuid::parse), proposedCategoryId?.let(Uuid::parse), missingFields, confidenceBasisPoints,
    CandidateState.valueOf(state), matchedTransactionId?.let(Uuid::parse))
