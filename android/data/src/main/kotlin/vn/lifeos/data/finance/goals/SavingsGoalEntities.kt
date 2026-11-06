package vn.lifeos.data.finance.goals

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.lifeos.data.db.WorkspaceEntity
import vn.lifeos.data.finance.AccountEntity

@Entity(tableName = "savings_goals", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class,
    parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true)])
data class SavingsGoalEntity(@PrimaryKey val id: String, val workspaceId: String, val name: String,
    val currency: String, val targetMinor: Long, val targetDate: String?, val archivedAt: Long?) {
    init { require(targetMinor > 0) }
}

@Entity(tableName = "savings_goal_accounts", primaryKeys = ["goalId", "accountId"], foreignKeys = [
    ForeignKey(entity = SavingsGoalEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "goalId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = AccountEntity::class, parentColumns = ["workspaceId", "id"],
        childColumns = ["workspaceId", "accountId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "goalId"]), Index(value = ["workspaceId", "accountId"])])
data class SavingsGoalAccountEntity(val goalId: String, val accountId: String, val workspaceId: String)
