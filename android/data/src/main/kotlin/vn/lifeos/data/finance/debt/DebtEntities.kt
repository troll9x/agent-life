package vn.lifeos.data.finance.debt

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.lifeos.data.db.WorkspaceEntity
import vn.lifeos.data.finance.FinancialTransactionEntity

@Entity(tableName = "debt_agreements", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class,
    parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true)])
data class DebtAgreementEntity(@PrimaryKey val id: String, val workspaceId: String, val role: String,
    val counterpartyId: String, val counterpartyLabel: String, val currency: String,
    val openingPrincipalMinor: Long, val dueDate: String?, val createdAt: Long)

@Entity(tableName = "debt_movements", foreignKeys = [
    ForeignKey(entity = DebtAgreementEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "debtId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = FinancialTransactionEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "transactionId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = FinancialTransactionEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "interestTransactionId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = FinancialTransactionEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "feeTransactionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "debtId"]), Index(value = ["workspaceId", "operationId"], unique = true),
        Index(value = ["workspaceId", "transactionId"], unique = true),
        Index(value = ["workspaceId", "interestTransactionId"], unique = true),
        Index(value = ["workspaceId", "feeTransactionId"], unique = true), Index(value = ["reversesMovementId"], unique = true)])
data class DebtMovementEntity(@PrimaryKey val id: String, val workspaceId: String, val debtId: String,
    val operationId: String, val transactionId: String?, val principalDeltaMinor: Long,
    val interestTransactionId: String?, val feeTransactionId: String?, val interestMinor: Long, val feeMinor: Long,
    val occurredAt: Long, val reversesMovementId: String?, val note: String?)
