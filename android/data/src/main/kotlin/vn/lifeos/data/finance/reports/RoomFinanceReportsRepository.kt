package vn.lifeos.data.finance.reports

import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.finance.AccountEntity
import vn.lifeos.data.finance.BudgetEntity
import vn.lifeos.data.finance.CategoryEntity
import vn.lifeos.data.finance.FinancialTransactionEntity
import vn.lifeos.data.finance.LedgerEntryEntity
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.reports.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

class RoomFinanceReportsRepository(private val database: AgentLifeDatabase, private val clock: java.time.Clock = java.time.Clock.systemUTC()) : FinanceReportsRepository {
    private val dao = database.financeDao()

    override suspend fun load(filter: ReportFilter): Outcome<ReportInput> = safe {
        require(filter.start.isBefore(filter.endExclusive))
        database.withTransaction {
            if (dao.activeWorkspace(filter.workspaceId.value) == null) return@withTransaction Outcome.Failure(AppError.NotFound)
            val accountId = filter.accountId
            if (accountId != null) {
                val account = dao.account(filter.workspaceId.value, accountId.value)
                    ?: return@withTransaction Outcome.Failure(AppError.NotFound)
                if (account.currency != filter.currency.value) return@withTransaction Outcome.Failure(AppError.InvalidInput("currency"))
            }
            val rows = dao.transactionsInPeriod(filter.workspaceId.value, filter.start.toEpochMilli(), filter.endExclusive.toEpochMilli())
            val entries = dao.entriesForTransactions(filter.workspaceId.value, rows.map { it.id }).groupBy { it.transactionId }
            val facts = rows.map { row ->
                val original = row.reversesId?.let { dao.transaction(filter.workspaceId.value, it) }
                ReportFact(PostedFinanceRow(row.domain(), entries[row.id].orEmpty().map { it.domain() }),
                    original?.let { TransactionKind.valueOf(it.kind) })
            }
            val categories = dao.categories(filter.workspaceId.value).map { it.domain() }
            val budgets = dao.budgetsInPeriod(filter.workspaceId.value, filter.period.start.toString(), filter.currency.value)
                .map { it.domain() }
            val quality = vn.lifeos.data.finance.reconciliation.RoomFinancialQualityReader(database, clock).load(filter)
            Outcome.Success(ReportInput(facts, categories, budgets, quality))
        }
    }

    override suspend fun saveBudget(budget: Budget): Outcome<Budget> = safe {
        require(budget.limitMinor >= 0 && (budget.alertThresholdBasisPoints == null || budget.alertThresholdBasisPoints in 0..10_000))
        database.withTransaction {
            if (dao.activeWorkspace(budget.workspaceId.value) == null) return@withTransaction Outcome.Failure(AppError.NotFound)
            val category = dao.category(budget.workspaceId.value, budget.categoryId.value)
                ?: return@withTransaction Outcome.Failure(AppError.NotFound)
            if (category.direction != CategoryDirection.EXPENSE.name || category.archivedAt != null)
                return@withTransaction Outcome.Failure(AppError.InvalidInput("categoryId"))
            val existing = dao.budget(budget.workspaceId.value, budget.categoryId.value,
                budget.periodStartDate.toString(), budget.currency.value)
            if (existing == null) dao.insertBudget(BudgetEntity(budget.id.value, budget.workspaceId.value,
                budget.categoryId.value, budget.periodStartDate.toString(), budget.currency.value,
                budget.limitMinor, budget.alertThresholdBasisPoints))
            else dao.updateBudget(existing.id, budget.workspaceId.value, budget.limitMinor, budget.alertThresholdBasisPoints)
            Outcome.Success(budget.copy(id = existing?.id?.let(Uuid::parse) ?: budget.id))
        }
    }

    private suspend fun <T> safe(block: suspend () -> Outcome<T>): Outcome<T> = try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IllegalArgumentException) { Outcome.Failure(AppError.InvalidInput("report")) }
        catch (_: ArithmeticException) { Outcome.Failure(AppError.InvalidInput("amount")) }
        catch (_: Exception) { Outcome.Failure(AppError.Storage) }
}

private fun FinancialTransactionEntity.domain() = FinancialTransaction(Uuid.parse(id), Uuid.parse(workspaceId),
    Uuid.parse(operationId), TransactionKind.valueOf(kind), Instant.ofEpochMilli(occurredAt), Instant.ofEpochMilli(postedAt),
    categoryId?.let(Uuid::parse), payee, note, reversesId?.let(Uuid::parse), refundOfId?.let(Uuid::parse),
    replacementOfId?.let(Uuid::parse), debtId?.let(Uuid::parse))
private fun LedgerEntryEntity.domain() = LedgerEntry(Uuid.parse(id), Uuid.parse(transactionId), Uuid.parse(accountId),
    deltaMinor, CurrencyCode.of(currency))
private fun CategoryEntity.domain() = Category(Uuid.parse(id), Uuid.parse(workspaceId), name,
    CategoryDirection.valueOf(direction), parentId?.let(Uuid::parse), archivedAt?.let(Instant::ofEpochMilli))
private fun BudgetEntity.domain() = Budget(Uuid.parse(id), Uuid.parse(workspaceId), Uuid.parse(categoryId),
    LocalDate.parse(periodStartDate), CurrencyCode.of(currency), limitMinor, alertThresholdBasisPoints)
