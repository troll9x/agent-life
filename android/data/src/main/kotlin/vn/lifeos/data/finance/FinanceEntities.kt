package vn.lifeos.data.finance

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.lifeos.data.db.WorkspaceEntity

@Entity(tableName = "accounts", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class,
    parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true)])
data class AccountEntity(@PrimaryKey val id: String, val workspaceId: String, val name: String, val kind: String,
    val currency: String, val openingBalanceMinor: Long, val openingAsOf: Long, val archivedAt: Long?)

@Entity(tableName = "categories", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class,
    parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true)])
data class CategoryEntity(@PrimaryKey val id: String, val workspaceId: String, val name: String,
    val direction: String, val parentId: String?, val archivedAt: Long?)

@Entity(tableName = "financial_transactions", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class,
    parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true),
        Index(value = ["workspaceId", "operationId"], unique = true), Index(value = ["reversesId"], unique = true),
        Index(value = ["workspaceId", "occurredAt"])])
data class FinancialTransactionEntity(@PrimaryKey val id: String, val workspaceId: String, val operationId: String,
    val kind: String, val occurredAt: Long, val postedAt: Long, val categoryId: String?, val payee: String?,
    val note: String?, val reversesId: String?, val refundOfId: String?, val replacementOfId: String?, val debtId: String?)

@Entity(tableName = "ledger_entries", foreignKeys = [
    ForeignKey(entity = FinancialTransactionEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "transactionId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = AccountEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "accountId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "transactionId"]), Index(value = ["workspaceId", "accountId"])])
data class LedgerEntryEntity(@PrimaryKey val id: String, val workspaceId: String, val transactionId: String,
    val accountId: String, val deltaMinor: Long, val currency: String) {
    init { require(deltaMinor != 0L) { "deltaMinor" } }
}

@Entity(tableName = "source_evidence", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class,
    parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "connectionKey", "sourceEventId", "revisionKey"], unique = true),
        Index(value = ["workspaceId", "transactionId"])])
data class SourceEvidenceEntity(@PrimaryKey val id: String, val workspaceId: String, val connectionId: String?,
    val connectionKey: String, val sourceKind: String, val sourceEventId: String, val revisionHash: String?,
    val revisionKey: String, val bankReference: String?, val maskedAccount: String?, val observedAt: Long,
    val transactionId: String?)

@Entity(tableName = "import_candidates", foreignKeys = [ForeignKey(entity = SourceEvidenceEntity::class,
    parentColumns = ["id"], childColumns = ["sourceEvidenceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["sourceEvidenceId"], unique = true)])
data class ImportCandidateEntity(@PrimaryKey val id: String, val workspaceId: String, val sourceEvidenceId: String,
    val parserVersion: Int, val amountMinor: Long?, val currency: String?, val direction: String?,
    val occurredAt: Long?, val proposedAccountId: String?, val proposedCategoryId: String?, val missingFields: String,
    val confidenceBasisPoints: Int, val state: String, val matchedTransactionId: String?) {
    init { require(confidenceBasisPoints in 0..10_000) { "confidenceBasisPoints" } }
}

@Entity(tableName = "budgets", foreignKeys = [ForeignKey(entity = CategoryEntity::class,
    parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "categoryId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "categoryId"]),
        Index(value = ["workspaceId", "categoryId", "periodStartDate", "currency"], unique = true)])
data class BudgetEntity(@PrimaryKey val id: String, val workspaceId: String, val categoryId: String,
    val periodStartDate: String, val currency: String, val limitMinor: Long, val alertThresholdBasisPoints: Int?) {
    init {
        require(limitMinor >= 0) { "limitMinor" }
        require(alertThresholdBasisPoints == null || alertThresholdBasisPoints in 0..10_000) { "alertThresholdBasisPoints" }
    }
}

@Entity(tableName = "reconciliations", foreignKeys = [ForeignKey(entity = AccountEntity::class,
    parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "accountId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "accountId"]), Index(value = ["workspaceId", "accountId", "asOfInstant"], unique = true)])
data class ReconciliationEntity(@PrimaryKey val id: String, val workspaceId: String, val accountId: String,
    val asOfInstant: Long, val observedBalanceMinor: Long, val expectedBalanceMinor: Long,
    val differenceMinor: Long, val note: String?, val state: String) {
    init { require(differenceMinor == Math.subtractExact(observedBalanceMinor, expectedBalanceMinor)) { "differenceMinor" } }
}
