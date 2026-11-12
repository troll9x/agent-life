package vn.lifeos.data.finance.scheduled

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledDao {
    @Query("SELECT * FROM import_candidates WHERE workspaceId=:workspace AND state IN ('PENDING','NEEDS_REVIEW')")
    suspend fun openCandidates(workspace: String): List<vn.lifeos.data.finance.ImportCandidateEntity>
    @Query("SELECT * FROM scheduled_transactions WHERE workspaceId=:workspace ORDER BY title,id")
    suspend fun rules(workspace: String): List<ScheduledTransactionEntity>
    @Query("SELECT * FROM scheduled_transactions WHERE workspaceId=:workspace")
    fun observeRules(workspace: String): Flow<List<ScheduledTransactionEntity>>
    @Query("SELECT * FROM planned_transactions WHERE workspaceId=:workspace ORDER BY dueDate DESC,id")
    suspend fun plans(workspace: String): List<PlannedTransactionEntity>
    @Query("SELECT * FROM planned_transactions WHERE workspaceId=:workspace")
    fun observePlans(workspace: String): Flow<List<PlannedTransactionEntity>>
    @Query("SELECT * FROM planned_transactions WHERE workspaceId=:workspace AND id=:id")
    suspend fun plan(workspace: String, id: String): PlannedTransactionEntity?
    @Insert suspend fun insertRule(row: ScheduledTransactionEntity)
    @Update suspend fun updateRule(row: ScheduledTransactionEntity)
    @Insert suspend fun insertPlan(row: PlannedTransactionEntity)
    @Query("UPDATE planned_transactions SET state=:state,transactionId=:transactionId WHERE workspaceId=:workspace AND id=:id AND state='PENDING'")
    suspend fun decide(workspace: String, id: String, state: String, transactionId: String?): Int
    @Query("SELECT title FROM scheduled_transactions WHERE workspaceId=:workspace AND id=:ruleId AND enabled=1 AND nextDueDate=:date UNION ALL SELECT title FROM planned_transactions WHERE workspaceId=:workspace AND ruleId=:ruleId AND dueDate=:date AND state='PENDING' LIMIT 1")
    suspend fun reminderTitle(workspace: String, ruleId: String, date: String): String?
}
