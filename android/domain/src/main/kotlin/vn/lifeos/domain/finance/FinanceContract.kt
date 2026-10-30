package vn.lifeos.domain.finance

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Outcome

enum class AccountKind { CASH, BANK, SAVINGS, CREDIT, OTHER }
enum class CategoryDirection { INCOME, EXPENSE }
enum class TransactionKind { INCOME, EXPENSE, TRANSFER, REFUND, REVERSAL, ADJUSTMENT, LOAN_PRINCIPAL_IN, LOAN_PRINCIPAL_OUT }
enum class CandidateState { PENDING, NEEDS_REVIEW, CONFIRMED, IGNORED, DUPLICATE, ERROR }
enum class ReconciliationState { OPEN, RESOLVED }
enum class FinanceSourceKind { MANUAL, NOTIFICATION, MAIL, IMPORT }

data class Account(val id: Uuid, val workspaceId: Uuid, val name: String, val kind: AccountKind,
    val currency: CurrencyCode, val openingBalanceMinor: Long, val openingAsOf: Instant, val archivedAt: Instant? = null)
data class Category(val id: Uuid, val workspaceId: Uuid, val name: String, val direction: CategoryDirection,
    val parentId: Uuid? = null, val archivedAt: Instant? = null)
data class FinancialTransaction(val id: Uuid, val workspaceId: Uuid, val operationId: Uuid, val kind: TransactionKind,
    val occurredAt: Instant, val postedAt: Instant, val categoryId: Uuid?, val payee: String?, val note: String?,
    val reversesId: Uuid?, val refundOfId: Uuid?, val replacementOfId: Uuid?, val debtId: Uuid?)
data class LedgerEntry(val id: Uuid, val transactionId: Uuid, val accountId: Uuid, val deltaMinor: Long,
    val currency: CurrencyCode)
data class SourceEvidence(val id: Uuid, val workspaceId: Uuid, val connectionId: Uuid?, val sourceKind: String,
    val sourceEventId: String, val revisionHash: String?, val bankReference: String?, val maskedAccount: String?,
    val observedAt: Instant, val transactionId: Uuid?)
data class ImportCandidate(val id: Uuid, val workspaceId: Uuid, val sourceEvidenceId: Uuid, val parserVersion: Int,
    val amountMinor: Long?, val currency: CurrencyCode?, val direction: CategoryDirection?, val occurredAt: Instant?,
    val proposedAccountId: Uuid?, val proposedCategoryId: Uuid?, val missingFields: String, val confidenceBasisPoints: Int,
    val state: CandidateState, val matchedTransactionId: Uuid?)
data class Budget(val id: Uuid, val workspaceId: Uuid, val categoryId: Uuid, val periodStartDate: LocalDate,
    val currency: CurrencyCode, val limitMinor: Long, val alertThresholdBasisPoints: Int?)
data class Reconciliation(val id: Uuid, val workspaceId: Uuid, val accountId: Uuid, val asOfInstant: Instant,
    val observedBalanceMinor: Long, val expectedBalanceMinor: Long, val differenceMinor: Long,
    val note: String?, val state: ReconciliationState)
data class PostedFinanceRow(val transaction: FinancialTransaction, val entries: List<LedgerEntry>)

/** Amount is a positive magnitude; signed ledger deltas are derived from kind. */
data class PostFinanceCommand(val id: Uuid, val workspaceId: Uuid, val operationId: Uuid,
    val kind: TransactionKind, val accountId: Uuid, val amount: Money, val occurredAt: Instant,
    val categoryId: Uuid? = null, val destinationAccountId: Uuid? = null, val relatedTransactionId: Uuid? = null,
    val debtId: Uuid? = null, val payee: String? = null, val note: String? = null,
    val adjustmentDeltaMinor: Long? = null, val sourceKind: FinanceSourceKind = FinanceSourceKind.MANUAL)

data class FinanceReportQuery(val workspaceId: Uuid, val currency: CurrencyCode,
    val startInclusive: Instant, val endExclusive: Instant)
data class FinanceReport(val incomeMinor: Long, val grossExpenseMinor: Long, val refundMinor: Long,
    val netExpenseMinor: Long)

interface FinanceRepository {
    fun observeAccounts(workspaceId: Uuid): Flow<Outcome<List<Account>>>
    fun observeCategories(workspaceId: Uuid): Flow<Outcome<List<Category>>>
    fun observeRecentTransactions(workspaceId: Uuid): Flow<Outcome<List<PostedFinanceRow>>>
    suspend fun createAccount(account: Account): Outcome<Account>
    suspend fun createCategory(category: Category): Outcome<Category>
    suspend fun post(command: PostFinanceCommand): Outcome<FinancialTransaction>
    suspend fun balance(workspaceId: Uuid, accountId: Uuid, asOf: Instant): Outcome<Money>
    suspend fun report(query: FinanceReportQuery): Outcome<FinanceReport>
}
