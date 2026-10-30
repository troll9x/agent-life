package vn.lifeos.data.finance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts WHERE workspaceId=:workspaceId ORDER BY name, id")
    fun observeAccounts(workspaceId: String): Flow<List<AccountEntity>>
    @Query("SELECT * FROM categories WHERE workspaceId=:workspaceId ORDER BY name, id")
    fun observeCategories(workspaceId: String): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM financial_transactions WHERE workspaceId=:workspaceId ORDER BY occurredAt DESC, id DESC LIMIT 50")
    fun observeRecentTransactions(workspaceId: String): Flow<List<FinancialTransactionEntity>>
    @Query("SELECT * FROM ledger_entries WHERE workspaceId=:workspaceId AND transactionId=:transactionId ORDER BY id")
    suspend fun entriesForRow(workspaceId: String, transactionId: String): List<LedgerEntryEntity>
    @Query("SELECT * FROM source_evidence WHERE workspaceId=:workspaceId AND transactionId=:transactionId")
    suspend fun evidenceForTransaction(workspaceId: String, transactionId: String): List<SourceEvidenceEntity>
    @Query("SELECT * FROM accounts WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun account(workspaceId: String, id: String): AccountEntity?
    @Query("SELECT * FROM categories WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun category(workspaceId: String, id: String): CategoryEntity?
    @Query("SELECT * FROM financial_transactions WHERE workspaceId=:workspaceId AND operationId=:operationId")
    suspend fun byOperation(workspaceId: String, operationId: String): FinancialTransactionEntity?
    @Query("SELECT * FROM financial_transactions WHERE workspaceId=:workspaceId AND id=:id")
    suspend fun transaction(workspaceId: String, id: String): FinancialTransactionEntity?
    @Query("SELECT * FROM financial_transactions WHERE workspaceId=:workspaceId AND reversesId=:id")
    suspend fun reversal(workspaceId: String, id: String): FinancialTransactionEntity?
    @Query("SELECT e.* FROM ledger_entries e JOIN financial_transactions t ON t.id=e.transactionId AND t.workspaceId=e.workspaceId WHERE e.workspaceId=:workspaceId AND e.accountId=:accountId AND t.occurredAt>:from AND t.occurredAt<=:through")
    suspend fun entriesForBalance(workspaceId: String, accountId: String, from: Long, through: Long): List<LedgerEntryEntity>
    @Query("SELECT * FROM ledger_entries WHERE workspaceId=:workspaceId AND transactionId=:transactionId ORDER BY id")
    suspend fun entries(workspaceId: String, transactionId: String): List<LedgerEntryEntity>
    @Query("SELECT e.* FROM ledger_entries e JOIN financial_transactions t ON t.id=e.transactionId AND t.workspaceId=e.workspaceId WHERE e.workspaceId=:workspaceId AND t.refundOfId=:expenseId")
    suspend fun refunds(workspaceId: String, expenseId: String): List<LedgerEntryEntity>
    @Query("SELECT t.* FROM financial_transactions t WHERE t.workspaceId=:workspaceId AND t.occurredAt>=:from AND t.occurredAt<:until ORDER BY t.occurredAt, t.id")
    suspend fun transactionsInPeriod(workspaceId: String, from: Long, until: Long): List<FinancialTransactionEntity>
    @Query("SELECT * FROM ledger_entries WHERE workspaceId=:workspaceId AND transactionId IN (:ids)")
    suspend fun entriesForTransactions(workspaceId: String, ids: List<String>): List<LedgerEntryEntity>
    @Query("SELECT id FROM workspaces WHERE id=:workspaceId AND deletedAt IS NULL")
    suspend fun activeWorkspace(workspaceId: String): String?

    @Insert suspend fun insertAccount(value: AccountEntity)
    @Insert suspend fun insertCategory(value: CategoryEntity)
    @Insert suspend fun insertTransaction(value: FinancialTransactionEntity)
    @Insert suspend fun insertEntry(value: LedgerEntryEntity)
    @Insert suspend fun insertEvidence(value: SourceEvidenceEntity)
    @Insert suspend fun insertCandidate(value: ImportCandidateEntity)
    @Insert suspend fun insertBudget(value: BudgetEntity)
    @Insert suspend fun insertReconciliation(value: ReconciliationEntity)
}
