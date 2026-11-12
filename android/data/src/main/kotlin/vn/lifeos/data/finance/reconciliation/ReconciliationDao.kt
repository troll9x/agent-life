package vn.lifeos.data.finance.reconciliation

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import vn.lifeos.data.finance.AccountEntity
import vn.lifeos.data.finance.ImportCandidateEntity
import vn.lifeos.data.finance.ReconciliationEntity
import vn.lifeos.data.db.IntegrationConnectionEntity

@Dao
interface ReconciliationDao {
    @Query("SELECT * FROM import_candidates WHERE workspaceId=:workspace AND state IN ('PENDING','NEEDS_REVIEW','ERROR') AND (currency IS NULL OR currency=:currency) AND (:accountId IS NULL OR proposedAccountId IS NULL OR proposedAccountId=:accountId) AND (occurredAt IS NULL OR (occurredAt>=:from AND occurredAt<:until))")
    suspend fun periodCandidates(workspace: String, currency: String, accountId: String?, from: Long, until: Long): List<ImportCandidateEntity>
    @Query("SELECT * FROM reconciliations WHERE workspaceId=:workspace ORDER BY asOfInstant DESC,id")
    fun observe(workspace: String): Flow<List<ReconciliationEntity>>
    @Query("SELECT * FROM reconciliations WHERE workspaceId=:workspace ORDER BY asOfInstant DESC,id")
    suspend fun snapshots(workspace: String): List<ReconciliationEntity>
    @Query("SELECT * FROM reconciliations WHERE workspaceId=:workspace AND id=:id")
    suspend fun snapshot(workspace: String, id: String): ReconciliationEntity?
    @Query("UPDATE reconciliations SET state='RESOLVED' WHERE workspaceId=:workspace AND id=:id AND state='OPEN'")
    suspend fun resolve(workspace: String, id: String): Int
    @Query("SELECT * FROM accounts WHERE workspaceId=:workspace AND currency=:currency AND (:accountId IS NULL OR id=:accountId) AND openingAsOf<=:asOf")
    suspend fun accounts(workspace: String, currency: String, accountId: String?, asOf: Long): List<AccountEntity>
    @Query("SELECT * FROM import_candidates WHERE workspaceId=:workspace AND state IN ('PENDING','NEEDS_REVIEW','ERROR') AND (currency IS NULL OR currency=:currency) AND (proposedAccountId IS NULL OR proposedAccountId=:accountId) AND (occurredAt IS NULL OR (occurredAt>=:from AND occurredAt<:until))")
    suspend fun candidates(workspace: String, currency: String, accountId: String, from: Long, until: Long): List<ImportCandidateEntity>
    @Query("SELECT * FROM integration_connections WHERE workspaceId=:workspace AND deletedAt IS NULL AND (provider='NOTIFICATION' OR id IN (SELECT connectionId FROM source_evidence WHERE workspaceId=:workspace AND sourceKind IN ('MAIL','NOTIFICATION','IMPORT')))")
    suspend fun sources(workspace: String): List<IntegrationConnectionEntity>
}
