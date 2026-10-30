package vn.lifeos.data.finance

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

/** Ledger writes use one encrypted Room transaction; posted headers/entries are never updated. */
class RoomFinanceRepository(private val database: AgentLifeDatabase, private val clock: Clock,
    private val ids: IdGenerator) : FinanceRepository {
    private val dao = database.financeDao()

    override fun observeAccounts(workspaceId: Uuid): Flow<Outcome<List<Account>>> =
        dao.observeAccounts(workspaceId.value).map<List<AccountEntity>, Outcome<List<Account>>> { rows ->
            Outcome.Success(rows.map(AccountEntity::domain))
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override fun observeCategories(workspaceId: Uuid): Flow<Outcome<List<Category>>> =
        dao.observeCategories(workspaceId.value).map<List<CategoryEntity>, Outcome<List<Category>>> { rows ->
            Outcome.Success(rows.map(CategoryEntity::domain))
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override fun observeRecentTransactions(workspaceId: Uuid): Flow<Outcome<List<PostedFinanceRow>>> =
        dao.observeRecentTransactions(workspaceId.value).map<List<FinancialTransactionEntity>, Outcome<List<PostedFinanceRow>>> { rows ->
            Outcome.Success(rows.map { row ->
                PostedFinanceRow(row.domain(), dao.entriesForRow(workspaceId.value, row.id).map(LedgerEntryEntity::domain))
            })
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun createAccount(account: Account): Outcome<Account> = write {
        if (account.name.isBlank()) return@write invalid("name")
        if (dao.activeWorkspace(account.workspaceId.value) == null) return@write Outcome.Failure(AppError.NotFound)
        dao.insertAccount(AccountEntity(account.id.value, account.workspaceId.value, account.name.trim(), account.kind.name,
            account.currency.value, account.openingBalanceMinor, account.openingAsOf.toEpochMilli(), account.archivedAt?.toEpochMilli()))
        Outcome.Success(account.copy(name = account.name.trim()))
    }

    override suspend fun createCategory(category: Category): Outcome<Category> = write {
        if (category.name.isBlank()) return@write invalid("name")
        if (dao.activeWorkspace(category.workspaceId.value) == null) return@write Outcome.Failure(AppError.NotFound)
        val parentId = category.parentId
        if (parentId != null && dao.category(category.workspaceId.value, parentId.value) == null)
            return@write invalid("parentId")
        dao.insertCategory(CategoryEntity(category.id.value, category.workspaceId.value, category.name.trim(),
            category.direction.name, category.parentId?.value, category.archivedAt?.toEpochMilli()))
        Outcome.Success(category.copy(name = category.name.trim()))
    }

    override suspend fun post(command: PostFinanceCommand): Outcome<FinancialTransaction> = write {
        if (dao.activeWorkspace(command.workspaceId.value) == null) return@write Outcome.Failure(AppError.NotFound)
        if (dao.byOperation(command.workspaceId.value, command.operationId.value) != null) return@write Outcome.Failure(AppError.Conflict)
        val sourceRow = dao.account(command.workspaceId.value, command.accountId.value) ?: return@write invalid("accountId")
        val source = sourceRow.domain()
        if (source.archivedAt != null) return@write invalid("accountId")
        if (!command.occurredAt.isAfter(source.openingAsOf)) return@write invalid("occurredAt")
        if (command.amount.amountMinor <= 0 || command.amount.currency != source.currency) return@write invalid("amount")
        val destination = command.destinationAccountId?.let { dao.account(command.workspaceId.value, it.value)?.domain() }
        if (command.kind == TransactionKind.TRANSFER) {
            if (destination == null || destination.archivedAt != null || !command.occurredAt.isAfter(destination.openingAsOf))
                return@write invalid("destinationAccountId")
        } else if (command.destinationAccountId != null) return@write invalid("destinationAccountId")
        val expectedDirection = when (command.kind) {
            TransactionKind.INCOME -> CategoryDirection.INCOME
            TransactionKind.EXPENSE -> CategoryDirection.EXPENSE
            else -> null
        }
        val category = command.categoryId?.let { dao.category(command.workspaceId.value, it.value) }
        if (expectedDirection != null && (category == null || category.direction != expectedDirection.name || category.archivedAt != null))
            return@write invalid("categoryId")
        if (expectedDirection == null && command.categoryId != null) return@write invalid("categoryId")
        val adjustment = command.adjustmentDeltaMinor
        if (command.kind == TransactionKind.ADJUSTMENT && (command.note.isNullOrBlank() || adjustment == null ||
                adjustment == 0L || adjustment == Long.MIN_VALUE ||
                kotlin.math.abs(adjustment) != command.amount.amountMinor)) return@write invalid("adjustmentDeltaMinor")
        // Debt posting waits for a persisted DebtAgreement; never write a dangling principal link.
        if (command.kind in setOf(TransactionKind.LOAN_PRINCIPAL_IN, TransactionKind.LOAN_PRINCIPAL_OUT))
            return@write invalid("debtId")
        if (command.debtId != null)
            return@write invalid("debtId")

        val related = command.relatedTransactionId?.let { dao.transaction(command.workspaceId.value, it.value) }
        val entries: List<Pair<Account, Long>> = when (command.kind) {
            TransactionKind.REFUND -> {
                if (related?.kind != TransactionKind.EXPENSE.name || dao.reversal(command.workspaceId.value, related.id) != null)
                    return@write invalid("relatedTransactionId")
                val original = dao.entries(command.workspaceId.value, related.id).singleOrNull() ?: return@write invalid("relatedTransactionId")
                if (original.currency != command.amount.currency.value) return@write invalid("currency")
                val refunded = dao.refunds(command.workspaceId.value, related.id).fold(0L) { sum, it -> Math.addExact(sum, it.deltaMinor) }
                if (command.amount.amountMinor > Math.subtractExact(Math.negateExact(original.deltaMinor), refunded))
                    return@write invalid("amount")
                LedgerRules.entries(command.kind, command.amount, source)
            }
            TransactionKind.REVERSAL -> {
                if (related == null || related.kind == TransactionKind.REVERSAL.name ||
                    dao.reversal(command.workspaceId.value, related.id) != null ||
                    dao.refunds(command.workspaceId.value, related.id).isNotEmpty()) return@write invalid("relatedTransactionId")
                val original = dao.entries(command.workspaceId.value, related.id)
                if (original.none { it.accountId == source.id.value } ||
                    original.any { it.currency != command.amount.currency.value } ||
                    original.maxOfOrNull { kotlin.math.abs(it.deltaMinor) } != command.amount.amountMinor)
                    return@write invalid("amount")
                original.map { entry ->
                    val account = dao.account(command.workspaceId.value, entry.accountId)?.domain()
                        ?: return@write invalid("relatedTransactionId")
                    if (account.currency.value != entry.currency) return@write invalid("currency")
                    account to Math.negateExact(entry.deltaMinor)
                }.also { if (it.isEmpty()) return@write invalid("relatedTransactionId") }
            }
            TransactionKind.ADJUSTMENT -> listOf(source to command.adjustmentDeltaMinor!!)
            else -> {
                if (command.relatedTransactionId != null) return@write invalid("relatedTransactionId")
                LedgerRules.entries(command.kind, command.amount, source, destination)
            }
        }
        if (entries.any { (account, _) -> !command.occurredAt.isAfter(account.openingAsOf) }) return@write invalid("occurredAt")
        val now = clock.instant()
        val header = FinancialTransactionEntity(command.id.value, command.workspaceId.value, command.operationId.value,
            command.kind.name, command.occurredAt.toEpochMilli(), now.toEpochMilli(),
            if (command.kind == TransactionKind.REVERSAL) related?.categoryId else category?.id,
            command.payee?.trim(), command.note?.trim(),
            related?.id.takeIf { command.kind == TransactionKind.REVERSAL },
            related?.id.takeIf { command.kind == TransactionKind.REFUND }, null, command.debtId?.value)
        dao.insertTransaction(header)
        entries.forEach { (account, delta) ->
            require(delta != 0L) { "deltaMinor" }
            dao.insertEntry(LedgerEntryEntity(ids.newId().value, command.workspaceId.value, header.id,
                account.id.value, delta, account.currency.value))
        }
        if (command.sourceKind == FinanceSourceKind.MANUAL) {
            dao.insertEvidence(SourceEvidenceEntity(ids.newId().value, command.workspaceId.value, null, "",
                FinanceSourceKind.MANUAL.name, command.operationId.value, null, "", null, null,
                now.toEpochMilli(), header.id))
        }
        Outcome.Success(header.domain())
    }

    override suspend fun balance(workspaceId: Uuid, accountId: Uuid, asOf: Instant): Outcome<Money> = read {
        val account = dao.account(workspaceId.value, accountId.value) ?: return@read Outcome.Failure(AppError.NotFound)
        if (asOf.toEpochMilli() < account.openingAsOf) return@read invalid("asOf")
        val deltas = dao.entriesForBalance(workspaceId.value, accountId.value, account.openingAsOf, asOf.toEpochMilli())
        Outcome.Success(Money(LedgerRules.balance(account.openingBalanceMinor, deltas.map { it.deltaMinor }),
            CurrencyCode.of(account.currency)))
    }

    override suspend fun report(query: FinanceReportQuery): Outcome<FinanceReport> = read {
        if (!query.startInclusive.isBefore(query.endExclusive)) return@read invalid("period")
        val transactions = dao.transactionsInPeriod(query.workspaceId.value, query.startInclusive.toEpochMilli(), query.endExclusive.toEpochMilli())
        val byId = dao.entriesForTransactions(query.workspaceId.value, transactions.map { it.id }).groupBy { it.transactionId }
        var income = 0L; var expense = 0L; var refunds = 0L
        for (transaction in transactions) {
            val amount = byId[transaction.id].orEmpty().filter { it.currency == query.currency.value }
                .fold(0L) { sum, entry -> Math.addExact(sum, entry.deltaMinor) }
            when (transaction.kind) {
                TransactionKind.INCOME.name -> income = Math.addExact(income, amount)
                TransactionKind.EXPENSE.name -> expense = Math.subtractExact(expense, amount)
                TransactionKind.REFUND.name -> refunds = Math.addExact(refunds, amount)
                TransactionKind.REVERSAL.name -> {
                    val original = transaction.reversesId?.let { dao.transaction(query.workspaceId.value, it) }
                    when (original?.kind) {
                        TransactionKind.INCOME.name -> income = Math.addExact(income, amount)
                        TransactionKind.EXPENSE.name -> expense = Math.subtractExact(expense, amount)
                        TransactionKind.REFUND.name -> refunds = Math.addExact(refunds, amount)
                        else -> Unit
                    }
                }
            }
        }
        Outcome.Success(FinanceReport(income, expense, refunds, Math.subtractExact(expense, refunds)))
    }

    private suspend fun <T> write(block: suspend () -> Outcome<T>): Outcome<T> = try {
        database.withTransaction { block() }
    } catch (error: CancellationException) { throw error }
    catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
    catch (_: IllegalArgumentException) { invalid("command") }
    catch (_: IllegalStateException) { invalid("command") }
    catch (_: ArithmeticException) { invalid("amount") }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }

    private suspend fun <T> read(block: suspend () -> Outcome<T>): Outcome<T> = try { block() }
    catch (error: CancellationException) { throw error }
    catch (_: ArithmeticException) { invalid("amount") }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }

    private fun invalid(field: String): Outcome.Failure = Outcome.Failure(AppError.InvalidInput(field))
}

private fun AccountEntity.domain() = Account(Uuid.parse(id), Uuid.parse(workspaceId), name, AccountKind.valueOf(kind),
    CurrencyCode.of(currency), openingBalanceMinor, Instant.ofEpochMilli(openingAsOf), archivedAt?.let(Instant::ofEpochMilli))
private fun CategoryEntity.domain() = Category(Uuid.parse(id), Uuid.parse(workspaceId), name,
    CategoryDirection.valueOf(direction), parentId?.let(Uuid::parse), archivedAt?.let(Instant::ofEpochMilli))
private fun LedgerEntryEntity.domain() = LedgerEntry(Uuid.parse(id), Uuid.parse(transactionId), Uuid.parse(accountId),
    deltaMinor, CurrencyCode.of(currency))
private fun FinancialTransactionEntity.domain() = FinancialTransaction(Uuid.parse(id), Uuid.parse(workspaceId),
    Uuid.parse(operationId), TransactionKind.valueOf(kind), Instant.ofEpochMilli(occurredAt), Instant.ofEpochMilli(postedAt),
    categoryId?.let(Uuid::parse), payee, note, reversesId?.let(Uuid::parse), refundOfId?.let(Uuid::parse),
    replacementOfId?.let(Uuid::parse), debtId?.let(Uuid::parse))
