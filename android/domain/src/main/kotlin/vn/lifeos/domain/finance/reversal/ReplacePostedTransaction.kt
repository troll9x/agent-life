package vn.lifeos.domain.finance.reversal

import java.time.Instant
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.FinancialTransaction
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.foundation.Outcome

/** IDs stay fixed across retries of the same user decision. */
data class ReplacePostedTransaction(
    val originalId: Uuid,
    val expectedPostedAt: Instant,
    val reversalId: Uuid,
    val reversalOperationId: Uuid,
    val replacement: ManualEntryInput,
)

data class ReplacementResult(val reversal: FinancialTransaction, val replacement: FinancialTransaction)

interface ReversalRepository {
    suspend fun replace(command: ReplacePostedTransaction): Outcome<ReplacementResult>
}
