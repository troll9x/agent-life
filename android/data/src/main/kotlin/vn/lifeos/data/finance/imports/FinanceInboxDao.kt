package vn.lifeos.data.finance.imports

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import vn.lifeos.data.finance.ImportCandidateEntity
import vn.lifeos.data.finance.SourceEvidenceEntity

data class MatchRow(val transactionId: String, val kind: String, val occurredAt: Long,
    val accountId: String, val deltaMinor: Long, val currency: String, val bankReference: String?)

@Dao
interface FinanceInboxDao {
    @Query("SELECT id FROM integration_connections WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun connection(workspaceId: String, id: String): String?
    @Query("SELECT COUNT(*) FROM financial_transactions WHERE workspaceId=:workspaceId")
    fun observeLedgerCount(workspaceId: String): Flow<Long>
    @Query("SELECT * FROM import_candidates WHERE workspaceId=:workspaceId AND state IN ('PENDING','NEEDS_REVIEW') ORDER BY id")
    fun observeOpen(workspaceId: String): Flow<List<ImportCandidateEntity>>
    @Query("SELECT * FROM import_candidates WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun candidate(workspaceId: String, id: String): ImportCandidateEntity?
    @Query("SELECT * FROM import_candidates WHERE workspaceId=:workspaceId AND sourceEvidenceId=:evidenceId")
    suspend fun candidateForEvidence(workspaceId: String, evidenceId: String): ImportCandidateEntity?
    @Query("SELECT * FROM source_evidence WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun evidence(workspaceId: String, id: String): SourceEvidenceEntity?
    @Query("SELECT * FROM source_evidence WHERE workspaceId=:workspaceId AND connectionKey=:connectionKey AND sourceEventId=:eventId AND revisionKey=:revisionKey LIMIT 1")
    suspend fun evidenceByIdentity(workspaceId: String, connectionKey: String, eventId: String, revisionKey: String): SourceEvidenceEntity?
    @Query("SELECT * FROM source_evidence WHERE workspaceId=:workspaceId AND connectionKey=:connectionKey AND sourceEventId=:eventId ORDER BY observedAt DESC")
    suspend fun eventRevisions(workspaceId: String, connectionKey: String, eventId: String): List<SourceEvidenceEntity>
    @Query("UPDATE import_candidates SET state=:state, matchedTransactionId=:transactionId WHERE workspaceId=:workspaceId AND id=:id AND state IN ('PENDING','NEEDS_REVIEW')")
    suspend fun decide(workspaceId: String, id: String, state: String, transactionId: String?): Int
    @Query("UPDATE source_evidence SET transactionId=:transactionId WHERE workspaceId=:workspaceId AND id=:id AND transactionId IS NULL")
    suspend fun linkEvidence(workspaceId: String, id: String, transactionId: String): Int
    @Query("SELECT t.id AS transactionId, t.kind AS kind, t.occurredAt AS occurredAt, l.accountId AS accountId, l.deltaMinor AS deltaMinor, l.currency AS currency, s.bankReference AS bankReference FROM financial_transactions t JOIN ledger_entries l ON l.transactionId=t.id AND l.workspaceId=t.workspaceId LEFT JOIN source_evidence s ON s.transactionId=t.id AND s.workspaceId=t.workspaceId WHERE t.workspaceId=:workspaceId AND l.accountId=:accountId AND l.currency=:currency AND l.deltaMinor=:delta AND (t.occurredAt BETWEEN :from AND :until OR s.bankReference=:reference COLLATE NOCASE)")
    suspend fun matchRows(workspaceId: String, accountId: String, currency: String, delta: Long,
        from: Long, until: Long, reference: String): List<MatchRow>
    @Query("SELECT t.id AS transactionId, t.kind AS kind, t.occurredAt AS occurredAt, l.accountId AS accountId, l.deltaMinor AS deltaMinor, l.currency AS currency, s.bankReference AS bankReference FROM financial_transactions t JOIN ledger_entries l ON l.transactionId=t.id AND l.workspaceId=t.workspaceId LEFT JOIN source_evidence s ON s.transactionId=t.id AND s.workspaceId=t.workspaceId WHERE t.workspaceId=:workspaceId AND t.id=:transactionId")
    suspend fun targetRows(workspaceId: String, transactionId: String): List<MatchRow>
    @Insert suspend fun insertEvidence(value: SourceEvidenceEntity)
    @Insert suspend fun insertCandidate(value: ImportCandidateEntity)
}
