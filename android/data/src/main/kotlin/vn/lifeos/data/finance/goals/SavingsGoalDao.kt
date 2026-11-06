package vn.lifeos.data.finance.goals

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE workspaceId=:workspaceId ORDER BY archivedAt IS NOT NULL, name, id")
    fun observeGoals(workspaceId: String): Flow<List<SavingsGoalEntity>>
    @Query("SELECT * FROM savings_goal_accounts WHERE workspaceId=:workspaceId")
    fun observeLinks(workspaceId: String): Flow<List<SavingsGoalAccountEntity>>
    @Query("SELECT * FROM savings_goals WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun goal(workspaceId: String, id: String): SavingsGoalEntity?
    @Query("SELECT * FROM savings_goal_accounts WHERE workspaceId=:workspaceId AND goalId=:goalId")
    suspend fun links(workspaceId: String, goalId: String): List<SavingsGoalAccountEntity>
    @Query("UPDATE savings_goals SET targetMinor=:targetMinor, targetDate=:targetDate WHERE workspaceId=:workspaceId AND id=:id AND archivedAt IS NULL")
    suspend fun updateTarget(workspaceId: String, id: String, targetMinor: Long, targetDate: String?): Int
    @Query("UPDATE savings_goals SET archivedAt=:archivedAt WHERE workspaceId=:workspaceId AND id=:id AND archivedAt IS NULL")
    suspend fun archive(workspaceId: String, id: String, archivedAt: Long): Int
    @Insert suspend fun insertGoal(value: SavingsGoalEntity)
    @Insert suspend fun insertLink(value: SavingsGoalAccountEntity)
}
