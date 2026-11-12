package vn.lifeos.domain.finance.reconciliation

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.SourceState
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.ImportCandidate
import vn.lifeos.domain.finance.PostedFinanceRow
import vn.lifeos.domain.finance.Reconciliation
import vn.lifeos.domain.finance.reports.ReportSnapshot
import vn.lifeos.domain.foundation.Outcome

data class ObserveBalance(val id: Uuid, val workspaceId: Uuid, val accountId: Uuid, val currency: CurrencyCode,
    val asOf: Instant, val observedMinor: Long, val note: String?)
data class ReconciliationView(val snapshot: Reconciliation, val currency: CurrencyCode, val currentExpectedMinor: Long) {
    init { Math.subtractExact(snapshot.observedBalanceMinor, currentExpectedMinor) }
    val currentDifferenceMinor: Long get() = Math.subtractExact(snapshot.observedBalanceMinor, currentExpectedMinor)
    val ledgerChanged: Boolean get() = currentExpectedMinor != snapshot.expectedBalanceMinor
    val resolved: Boolean get() = snapshot.state == vn.lifeos.domain.finance.ReconciliationState.RESOLVED && currentDifferenceMinor == 0L
}
data class ReconciliationReview(val view: ReconciliationView, val from: Instant, val endExclusive: Instant,
    val ledger: List<PostedFinanceRow>, val candidates: List<ImportCandidate>)
interface ReconciliationRepository {
    fun observe(workspaceId: Uuid): Flow<Outcome<List<ReconciliationView>>>
    suspend fun capture(command: ObserveBalance): Outcome<Reconciliation>
    suspend fun resolve(workspaceId: Uuid, snapshotId: Uuid): Outcome<Unit>
    suspend fun review(workspaceId: Uuid, snapshotId: Uuid): Outcome<ReconciliationReview>
}
data class FinanceSourceQuality(val provider: String, val state: SourceState)
enum class FinanceQualityFlag { UNRECONCILED, MISSING_SOURCE, UNKNOWN_SOURCE_COVERAGE, PENDING_CANDIDATE }
data class FinancialDataQuality(val unreconciledAccountIds: Set<Uuid> = emptySet(), val pendingCandidateCount: Int = 0,
    val sources: List<FinanceSourceQuality> = emptyList(), val sourceCoverageKnown: Boolean = false) {
    val flags: Set<FinanceQualityFlag> get() = buildSet {
        if (unreconciledAccountIds.isNotEmpty()) add(FinanceQualityFlag.UNRECONCILED)
        if (sources.isEmpty() || sources.any { it.state != SourceState.OK }) add(FinanceQualityFlag.MISSING_SOURCE)
        if (!sourceCoverageKnown) add(FinanceQualityFlag.UNKNOWN_SOURCE_COVERAGE)
        if (pendingCandidateCount > 0) add(FinanceQualityFlag.PENDING_CANDIDATE)
    }
    val canClaimComplete: Boolean get() = flags.isEmpty()
}
/** Minimal gate for future advice consumers. No raw evidence, labels or banking identifiers. */
data class FinanceAdviceQuality(val flags: Set<FinanceQualityFlag>, val pendingCandidateCount: Int,
    val unreconciledAccountCount: Int, val canClaimIncomeExpenseComplete: Boolean)
fun ReportSnapshot.qualityForAdvice() = FinanceAdviceQuality(quality.flags, quality.pendingCandidateCount,
    quality.unreconciledAccountIds.size, quality.canClaimComplete)
