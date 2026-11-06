package vn.lifeos.domain.finance.imports

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.CandidateState
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.FinanceSourceKind
import vn.lifeos.domain.finance.ImportCandidate
import vn.lifeos.domain.finance.PostFinanceCommand
import vn.lifeos.domain.finance.SourceEvidence
import vn.lifeos.domain.foundation.Outcome

/** Parsed metadata only. Raw notification/mail text stays in the separately retained capture store. */
data class InboundFinanceEvidence(val id: Uuid, val candidateId: Uuid, val workspaceId: Uuid,
    val connectionId: Uuid?, val sourceKind: FinanceSourceKind, val sourceEventId: String,
    val revisionHash: String?, val bankReference: String?, val maskedAccount: String?,
    val observedAt: Instant, val parserVersion: Int, val amountMinor: Long?, val currency: CurrencyCode?,
    val direction: CategoryDirection?, val occurredAt: Instant?, val proposedAccountId: Uuid?,
    val proposedCategoryId: Uuid?, val missingFields: String, val confidenceBasisPoints: Int,
    val bankReferenceVerified: Boolean = false)

data class MatchFact(val transactionId: Uuid, val accountId: Uuid, val direction: CategoryDirection,
    val amountMinor: Long, val currency: CurrencyCode, val occurredAt: Instant, val bankReference: String?)
enum class MatchReason { VERIFIED_REFERENCE, AMOUNT_TIME_SUGGESTION }
data class MatchSuggestion(val transactionId: Uuid, val reason: MatchReason,
    val accountId: Uuid, val amountMinor: Long, val occurredAt: Instant)
data class InboxItem(val candidate: ImportCandidate, val evidence: SourceEvidence,
    val suggestions: List<MatchSuggestion>)

sealed interface InboxDecision {
    data class Approve(val command: PostFinanceCommand) : InboxDecision
    data class Duplicate(val transactionId: Uuid) : InboxDecision
    data object Ignore : InboxDecision
}

interface FinanceInboxRepository {
    fun observe(workspaceId: Uuid): Flow<Outcome<List<InboxItem>>>
    suspend fun ingest(input: InboundFinanceEvidence): Outcome<ImportCandidate>
    suspend fun decide(workspaceId: Uuid, candidateId: Uuid, decision: InboxDecision): Outcome<ImportCandidate>
}

object EvidenceMatcher {
    fun suggest(candidate: ImportCandidate, reference: String?, facts: List<MatchFact>): List<MatchSuggestion> {
        val amount = candidate.amountMinor ?: return emptyList()
        val currency = candidate.currency ?: return emptyList()
        val direction = candidate.direction ?: return emptyList()
        val account = candidate.proposedAccountId ?: return emptyList()
        val compatible = facts.filter { it.accountId == account && it.direction == direction &&
            it.currency == currency && it.amountMinor == amount }
        val normalized = reference?.trim()?.takeIf(String::isNotEmpty)
        val exact = if (normalized == null) emptyList() else compatible.filter {
            it.bankReference?.trim()?.equals(normalized, ignoreCase = true) == true
        }
        if (exact.isNotEmpty()) return exact.distinctBy { it.transactionId }
            .map { MatchSuggestion(it.transactionId, MatchReason.VERIFIED_REFERENCE,
                it.accountId, it.amountMinor, it.occurredAt) }
        val occurred = candidate.occurredAt ?: return emptyList()
        // A different confirmed reference is evidence of a different payment, even at the same amount/time.
        return compatible.filter { it.bankReference.isNullOrBlank() &&
            kotlin.math.abs(it.occurredAt.epochSecond - occurred.epochSecond) <= 120L }
            .distinctBy { it.transactionId }
            .map { MatchSuggestion(it.transactionId, MatchReason.AMOUNT_TIME_SUGGESTION,
                it.accountId, it.amountMinor, it.occurredAt) }
    }

    fun initialState(candidate: ImportCandidate, suggestions: List<MatchSuggestion>): CandidateState =
        if (candidate.amountMinor == null || candidate.currency == null || candidate.direction == null ||
            candidate.occurredAt == null || candidate.proposedAccountId == null || suggestions.size > 1)
            CandidateState.NEEDS_REVIEW else CandidateState.PENDING
}
