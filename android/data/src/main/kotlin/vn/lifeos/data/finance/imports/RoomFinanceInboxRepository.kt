package vn.lifeos.data.finance.imports

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.finance.ImportCandidateEntity
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.data.finance.SourceEvidenceEntity
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.imports.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

class RoomFinanceInboxRepository(private val database: AgentLifeDatabase, private val clock: Clock,
    private val ids: IdGenerator) : FinanceInboxRepository {
    private val inbox = database.financeInboxDao()
    private val finance = database.financeDao()
    private val ledger = RoomFinanceRepository(database, clock, ids)

    override fun observe(workspaceId: Uuid): Flow<Outcome<List<InboxItem>>> =
        combine(inbox.observeOpen(workspaceId.value), inbox.observeLedgerCount(workspaceId.value)) { rows, _ -> rows }
            .map<List<ImportCandidateEntity>, Outcome<List<InboxItem>>> { rows ->
            database.withTransaction { Outcome.Success(rows.map { row ->
                val evidence = requireNotNull(inbox.evidence(workspaceId.value, row.sourceEvidenceId))
                InboxItem(row.domain(), evidence.domain(), suggestions(row.domain(), evidence.bankReference))
            }) }
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun ingest(input: InboundFinanceEvidence): Outcome<ImportCandidate> = safe {
        val amountMinor = input.amountMinor
        val currency = input.currency
        val allowedMissing = setOf("amountMinor", "currency", "direction", "occurredAt",
            "proposedAccountId", "proposedCategoryId")
        if (input.sourceKind !in setOf(FinanceSourceKind.NOTIFICATION, FinanceSourceKind.MAIL, FinanceSourceKind.IMPORT) ||
            input.sourceEventId.isBlank() || input.sourceEventId.length > 256 || input.parserVersion < 1 ||
            amountMinor != null && amountMinor <= 0 || input.confidenceBasisPoints !in 0..10_000 ||
            (input.bankReference?.length ?: 0) > 128 || (input.maskedAccount?.length ?: 0) > 64 ||
            input.missingFields.length > 256 || input.missingFields.isNotBlank() &&
            input.missingFields.split(',').any { it.trim() !in allowedMissing } ||
            (!input.bankReference.isNullOrBlank() && !input.bankReferenceVerified)) return@safe invalid("evidence")
        database.withTransaction {
            if (finance.activeWorkspace(input.workspaceId.value) == null) return@withTransaction Outcome.Failure(AppError.NotFound)
            val connectionId = input.connectionId
            if (connectionId != null && inbox.connection(input.workspaceId.value, connectionId.value) == null)
                return@withTransaction invalid("connectionId")
            val accountId = input.proposedAccountId
            if (accountId != null) {
                val account = finance.account(input.workspaceId.value, accountId.value)
                    ?: return@withTransaction invalid("proposedAccountId")
                if (currency != null && account.currency != currency.value) return@withTransaction invalid("currency")
            }
            val connectionKey = input.connectionId?.value ?: "local:${input.sourceKind.name}"
            val revisionKey = input.revisionHash.orEmpty()
            val existing = inbox.evidenceByIdentity(input.workspaceId.value, connectionKey, input.sourceEventId, revisionKey)
            if (existing != null) {
                val candidate = inbox.candidateForEvidence(input.workspaceId.value, existing.id)
                return@withTransaction if (candidate == null) Outcome.Failure(AppError.Conflict) else Outcome.Success(candidate.domain())
            }
            val priorRevisions = inbox.eventRevisions(input.workspaceId.value, connectionKey, input.sourceEventId)
            priorRevisions.forEach { prior ->
                val pending = inbox.candidateForEvidence(input.workspaceId.value, prior.id)
                if (pending != null && pending.state in setOf(CandidateState.PENDING.name, CandidateState.NEEDS_REVIEW.name))
                    inbox.decide(input.workspaceId.value, pending.id, CandidateState.IGNORED.name, null)
            }
            val evidence = SourceEvidenceEntity(input.id.value, input.workspaceId.value, input.connectionId?.value,
                connectionKey, input.sourceKind.name, input.sourceEventId, input.revisionHash, revisionKey,
                input.bankReference?.trim()?.takeIf(String::isNotEmpty), input.maskedAccount,
                input.observedAt.toEpochMilli(), null)
            val proposed = ImportCandidateEntity(input.candidateId.value, input.workspaceId.value, evidence.id,
                input.parserVersion, input.amountMinor, input.currency?.value, input.direction?.name,
                input.occurredAt?.toEpochMilli(), input.proposedAccountId?.value, input.proposedCategoryId?.value,
                input.missingFields, input.confidenceBasisPoints, CandidateState.PENDING.name, null)
            val suggestions = suggestions(proposed.domain(), evidence.bankReference)
            val candidate = proposed.copy(state = if (priorRevisions.isNotEmpty()) CandidateState.NEEDS_REVIEW.name
                else EvidenceMatcher.initialState(proposed.domain(), suggestions).name)
            inbox.insertEvidence(evidence)
            inbox.insertCandidate(candidate)
            Outcome.Success(candidate.domain())
        }
    }

    override suspend fun decide(workspaceId: Uuid, candidateId: Uuid, decision: InboxDecision): Outcome<ImportCandidate> = safe {
        database.withTransaction {
            val candidate = inbox.candidate(workspaceId.value, candidateId.value)
                ?: return@withTransaction Outcome.Failure(AppError.NotFound)
            if (candidate.state !in setOf(CandidateState.PENDING.name, CandidateState.NEEDS_REVIEW.name))
                return@withTransaction Outcome.Failure(AppError.Conflict)
            val evidence = inbox.evidence(workspaceId.value, candidate.sourceEvidenceId)
                ?: return@withTransaction Outcome.Failure(AppError.Storage)
            val targetId: String?
            val state: CandidateState
            when (decision) {
                InboxDecision.Ignore -> { targetId = null; state = CandidateState.IGNORED }
                is InboxDecision.Duplicate -> {
                    val target = targetFact(workspaceId, decision.transactionId, evidence.bankReference)
                        ?: return@withTransaction invalid("transactionId")
                    if (!compatible(candidate.domain(), target, evidence.bankReference) ||
                        finance.reversal(workspaceId.value, decision.transactionId.value) != null)
                        return@withTransaction invalid("transactionId")
                    targetId = decision.transactionId.value; state = CandidateState.DUPLICATE
                }
                is InboxDecision.Approve -> {
                    val command = decision.command
                    if (command.workspaceId != workspaceId || candidate.direction == null ||
                        command.kind !in setOf(TransactionKind.INCOME, TransactionKind.EXPENSE) ||
                        (candidate.proposedAccountId != null && command.accountId.value != candidate.proposedAccountId) ||
                        command.amount.amountMinor != candidate.amountMinor ||
                        command.amount.currency.value != candidate.currency ||
                        command.occurredAt.toEpochMilli() != candidate.occurredAt ||
                        (command.kind == TransactionKind.INCOME) != (candidate.direction == CategoryDirection.INCOME.name))
                        return@withTransaction invalid("command")
                    if (suggestions(candidate.domain(), evidence.bankReference).any { it.reason == MatchReason.VERIFIED_REFERENCE })
                        return@withTransaction Outcome.Failure(AppError.Conflict)
                    val source = FinanceSourceKind.valueOf(evidence.sourceKind)
                    val posted = ledger.post(command.copy(sourceKind = source))
                    if (posted !is Outcome.Success) return@withTransaction posted as Outcome.Failure
                    targetId = posted.value.id.value; state = CandidateState.CONFIRMED
                }
            }
            if (targetId != null && inbox.linkEvidence(workspaceId.value, evidence.id, targetId) != 1)
                throw IllegalStateException("evidence link changed")
            if (inbox.decide(workspaceId.value, candidate.id, state.name, targetId) != 1)
                throw IllegalStateException("candidate decision changed")
            Outcome.Success(candidate.copy(state = state.name, matchedTransactionId = targetId).domain())
        }
    }

    private suspend fun suggestions(candidate: ImportCandidate, reference: String?): List<MatchSuggestion> {
        val account = candidate.proposedAccountId ?: return emptyList()
        val amount = candidate.amountMinor ?: return emptyList()
        val currency = candidate.currency ?: return emptyList()
        val direction = candidate.direction ?: return emptyList()
        val occurred = candidate.occurredAt ?: Instant.EPOCH
        val delta = if (direction == CategoryDirection.INCOME) amount else Math.negateExact(amount)
        val time = occurred.toEpochMilli()
        val facts = inbox.matchRows(candidate.workspaceId.value, account.value, currency.value, delta,
            Math.subtractExact(time, 120_000), Math.addExact(time, 120_000), reference.orEmpty()).facts(reference)
        return EvidenceMatcher.suggest(candidate, reference, facts)
    }

    private suspend fun targetFact(workspaceId: Uuid, transactionId: Uuid, reference: String?): MatchFact? =
        inbox.targetRows(workspaceId.value, transactionId.value).facts(reference).singleOrNull()

    private fun compatible(candidate: ImportCandidate, target: MatchFact, reference: String?): Boolean =
        candidate.amountMinor == target.amountMinor && candidate.currency == target.currency &&
            candidate.direction == target.direction &&
            (candidate.proposedAccountId == null || candidate.proposedAccountId == target.accountId) &&
            (reference.isNullOrBlank() || target.bankReference.isNullOrBlank() ||
                reference.trim().equals(target.bankReference?.trim(), ignoreCase = true))

    private suspend fun <T> safe(block: suspend () -> Outcome<T>): Outcome<T> = try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
        catch (_: IllegalArgumentException) { invalid("evidence") }
        catch (_: ArithmeticException) { invalid("amount") }
        catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
        catch (_: IllegalStateException) { Outcome.Failure(AppError.Conflict) }
    private fun invalid(field: String): Outcome.Failure = Outcome.Failure(AppError.InvalidInput(field))
}

private fun List<MatchRow>.facts(reference: String?): List<MatchFact> = groupBy { it.transactionId }.mapNotNull { (_, rows) ->
    val row = rows.first()
    val direction = when (row.kind) {
        TransactionKind.INCOME.name -> CategoryDirection.INCOME
        TransactionKind.EXPENSE.name -> CategoryDirection.EXPENSE
        else -> return@mapNotNull null
    }
    val known = rows.mapNotNull { it.bankReference?.takeIf(String::isNotBlank) }
    val ref = known.firstOrNull { it.equals(reference, ignoreCase = true) } ?: known.firstOrNull()
    MatchFact(Uuid.parse(row.transactionId), Uuid.parse(row.accountId), direction,
        if (row.deltaMinor < 0) Math.negateExact(row.deltaMinor) else row.deltaMinor,
        CurrencyCode.of(row.currency), Instant.ofEpochMilli(row.occurredAt), ref)
}

private fun ImportCandidateEntity.domain() = ImportCandidate(Uuid.parse(id), Uuid.parse(workspaceId),
    Uuid.parse(sourceEvidenceId), parserVersion, amountMinor, currency?.let(CurrencyCode::of),
    direction?.let(CategoryDirection::valueOf), occurredAt?.let(Instant::ofEpochMilli),
    proposedAccountId?.let(Uuid::parse), proposedCategoryId?.let(Uuid::parse), missingFields,
    confidenceBasisPoints, CandidateState.valueOf(state), matchedTransactionId?.let(Uuid::parse))
private fun SourceEvidenceEntity.domain() = SourceEvidence(Uuid.parse(id), Uuid.parse(workspaceId),
    connectionId?.let(Uuid::parse), sourceKind, sourceEventId, revisionHash, bankReference,
    maskedAccount, Instant.ofEpochMilli(observedAt), transactionId?.let(Uuid::parse))
