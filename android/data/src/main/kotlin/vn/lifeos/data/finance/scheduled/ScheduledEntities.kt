package vn.lifeos.data.finance.scheduled

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.lifeos.data.db.WorkspaceEntity
import vn.lifeos.data.finance.AccountEntity
import vn.lifeos.data.finance.CategoryEntity
import vn.lifeos.data.finance.FinancialTransactionEntity

@Entity(tableName = "scheduled_transactions", foreignKeys = [
    ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = AccountEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "accountId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = CategoryEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "categoryId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "id"], unique = true), Index(value = ["workspaceId", "accountId"]), Index(value = ["workspaceId", "categoryId"])])
data class ScheduledTransactionEntity(@PrimaryKey val id: String, val workspaceId: String, val title: String,
    val recurrence: String, val anchorDate: String, val nextDueDate: String?, val amountMinor: Long,
    val currency: String, val kind: String, val accountId: String, val categoryId: String, val enabled: Boolean, val version: Long)

@Entity(tableName = "planned_transactions", foreignKeys = [
    ForeignKey(entity = ScheduledTransactionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "ruleId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = FinancialTransactionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "transactionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "ruleId", "dueDate"], unique = true), Index(value = ["workspaceId", "transactionId"], unique = true)])
data class PlannedTransactionEntity(@PrimaryKey val id: String, val workspaceId: String, val ruleId: String,
    val dueDate: String, val title: String, val amountMinor: Long, val currency: String, val kind: String,
    val accountId: String, val categoryId: String, val state: String, val transactionId: String?)
