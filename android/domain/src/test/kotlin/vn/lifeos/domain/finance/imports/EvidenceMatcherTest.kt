package vn.lifeos.domain.finance.imports

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.CandidateState
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.ImportCandidate

class EvidenceMatcherTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val time = Instant.parse("2026-10-20T10:00:00Z")
    private val currency = CurrencyCode.of("VND")
    private val candidate = ImportCandidate(id(1), id(2), id(3), 1, 200_000, currency,
        CategoryDirection.INCOME, time, id(4), null, "", 8000, CandidateState.PENDING, null)
    private fun fact(n: Int, reference: String?, whenAt: Instant = time) = MatchFact(id(n), id(4),
        CategoryDirection.INCOME, 200_000, currency, whenAt, reference)

    @Test fun verifiedReferenceWinsOverFingerprintAndDifferentReferenceNeverMerges() {
        val suggestions = EvidenceMatcher.suggest(candidate, "A-123", listOf(
            fact(10, null), fact(11, "B-456"), fact(12, "A-123", time.plusSeconds(3600))))
        assertEquals(listOf(MatchSuggestion(id(12), MatchReason.VERIFIED_REFERENCE,
            id(4), 200_000, time.plusSeconds(3600))), suggestions)
        assertTrue(EvidenceMatcher.suggest(candidate, "A-123", listOf(fact(11, "B-456"))).isEmpty())
    }

    @Test fun twoSameAmountTimeWithoutReferenceMustWaitForReview() {
        val suggestions = EvidenceMatcher.suggest(candidate, null, listOf(fact(10, null), fact(11, null)))
        assertEquals(2, suggestions.size)
        assertEquals(CandidateState.NEEDS_REVIEW, EvidenceMatcher.initialState(candidate, suggestions))
    }

    @Test fun singleFingerprintIsSuggestionOnly() {
        val suggestions = EvidenceMatcher.suggest(candidate, "A-123", listOf(fact(10, null)))
        assertEquals(MatchReason.AMOUNT_TIME_SUGGESTION, suggestions.single().reason)
        assertEquals(CandidateState.PENDING, EvidenceMatcher.initialState(candidate, suggestions))
    }

    @Test fun verifiedReferenceStillSuggestsWhenParsedTimeMissing() {
        val incomplete = candidate.copy(occurredAt = null)
        val suggestions = EvidenceMatcher.suggest(incomplete, "A-123", listOf(fact(10, "A-123")))
        assertEquals(id(10), suggestions.single().transactionId)
        assertEquals(CandidateState.NEEDS_REVIEW, EvidenceMatcher.initialState(incomplete, suggestions))
    }
}
