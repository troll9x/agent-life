package vn.lifeos.data.finance.debt

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debt_agreements WHERE workspaceId=:workspaceId ORDER BY createdAt, id")
    suspend fun agreements(workspaceId: String): List<DebtAgreementEntity>
    @Query("SELECT * FROM debt_agreements WHERE workspaceId=:workspaceId ORDER BY createdAt, id")
    fun observeAgreements(workspaceId: String): Flow<List<DebtAgreementEntity>>
    @Query("SELECT * FROM debt_movements WHERE workspaceId=:workspaceId ORDER BY occurredAt, rowid")
    fun observeMovements(workspaceId: String): Flow<List<DebtMovementEntity>>
    @Query("SELECT * FROM debt_agreements WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun agreement(workspaceId: String, id: String): DebtAgreementEntity?
    @Query("SELECT * FROM debt_movements WHERE workspaceId=:workspaceId AND debtId=:debtId ORDER BY occurredAt, rowid")
    suspend fun movements(workspaceId: String, debtId: String): List<DebtMovementEntity>
    @Query("SELECT * FROM debt_movements WHERE workspaceId=:workspaceId AND operationId=:operationId")
    suspend fun byOperation(workspaceId: String, operationId: String): DebtMovementEntity?
    @Insert suspend fun insertAgreement(value: DebtAgreementEntity)
    @Insert suspend fun insertMovement(value: DebtMovementEntity)
}
