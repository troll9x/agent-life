package vn.lifeos.data.finance.scheduled

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.AuditEventEntity
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.data.finance.ImportCandidateEntity
import vn.lifeos.data.finance.imports.RoomFinanceInboxRepository
import vn.lifeos.domain.common.*
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.imports.InboxDecision
import vn.lifeos.domain.finance.scheduled.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

class RoomScheduledFinanceRepository(private val database: AgentLifeDatabase, private val clock: Clock,
    private val ids: IdGenerator) : ScheduledFinanceRepository {
    private val dao = database.scheduledDao()
    private val finance = database.financeDao()
    private val inboxDao = database.financeInboxDao()
    private val ledger = RoomFinanceRepository(database, clock, ids)
    private val inbox = RoomFinanceInboxRepository(database, clock, ids)

    override fun observe(workspaceId: Uuid, zone: ZoneId): Flow<Outcome<ScheduledSnapshot>> =
        combine(dao.observeRules(workspaceId.value), dao.observePlans(workspaceId.value),
            inboxDao.observeOpen(workspaceId.value), inboxDao.observeLedgerCount(workspaceId.value)) { _, _, _, _ ->
            database.withTransaction {
                val candidates = dao.openCandidates(workspaceId.value)
                val items = dao.plans(workspaceId.value).map { row ->
                    val evidence = candidates.filter { compatible(row, it, zone) }.mapNotNull {
                        val source = inboxDao.evidence(workspaceId.value, it.sourceEvidenceId) ?: return@mapNotNull null
                        ScheduledEvidenceSuggestion(Uuid.parse(it.id), source.sourceKind, Instant.ofEpochMilli(it.occurredAt!!))
                    }
                    PlannedItem(row.domain(), evidence, matchingTransactions(row, zone))
                }
                Outcome.Success(ScheduledSnapshot(dao.rules(workspaceId.value).map(ScheduledTransactionEntity::domain), items)) as Outcome<ScheduledSnapshot>
            }
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun save(rule: ScheduledTransaction, zone: ZoneId, expectedVersion: Long?): Outcome<ScheduledTransaction> = write {
        if (finance.activeWorkspace(rule.workspaceId.value) == null) missing()
        if (rule.title.isBlank() || rule.amountMinor <= 0 || rule.kind !in setOf(TransactionKind.INCOME, TransactionKind.EXPENSE)) invalid("template")
        val account = finance.account(rule.workspaceId.value, rule.accountId.value) ?: invalid("accountId")
        val category = finance.category(rule.workspaceId.value, rule.categoryId.value) ?: invalid("categoryId")
        if (account.currency != rule.currency.value || account.archivedAt != null) invalid("accountId")
        if (category.archivedAt != null || category.direction != rule.kind.name) invalid("categoryId")
        val before = dao.rules(rule.workspaceId.value).firstOrNull { it.id == rule.id.value }
        if (expectedVersion == null && before != null || expectedVersion != null && before?.version != expectedVersion) conflict()
        if (before != null) {
            success(generateDue(rule.workspaceId, zone))
            val nextDueDate = rule.nextDueDate
            if (nextDueDate == null || !nextDueDate.isAfter(clock.instant().atZone(zone).toLocalDate())) invalid("nextDueDate")
        } else if (rule.nextDueDate == null) invalid("nextDueDate")
        val saved = rule.copy(title = rule.title.trim(), version = if (before == null) 1 else before.version + 1)
        if (before == null) dao.insertRule(saved.entity()) else dao.updateRule(saved.entity())
        audit(rule.workspaceId, rule.id, "SAVE_SCHEDULE")
        saved
    }

    override suspend fun setEnabled(workspaceId: Uuid, ruleId: Uuid, expectedVersion: Long, enabled: Boolean): Outcome<Unit> = write {
        val row = dao.rules(workspaceId.value).firstOrNull { it.id == ruleId.value } ?: missing()
        if (row.version != expectedVersion) conflict()
        dao.updateRule(row.copy(enabled = enabled, version = row.version + 1))
        audit(workspaceId, ruleId, "TOGGLE_SCHEDULE")
    }

    override suspend fun generateDue(workspaceId: Uuid, zone: ZoneId): Outcome<Int> = write {
        if (finance.activeWorkspace(workspaceId.value) == null) missing()
        val today = clock.instant().atZone(zone).toLocalDate()
        var generated = 0
        val existing = dao.plans(workspaceId.value).map { it.ruleId to it.dueDate }.toMutableSet()
        for (rule in dao.rules(workspaceId.value).filter { it.enabled }) {
            var date = rule.nextDueDate?.let(LocalDate::parse)
            var advanced = false
            while (date != null && date <= today && generated < 256) {
                if (existing.add(rule.id to date.toString())) {
                    dao.insertPlan(PlannedTransactionEntity(ids.newId().value, workspaceId.value, rule.id, date.toString(),
                        rule.title, rule.amountMinor, rule.currency, rule.kind, rule.accountId, rule.categoryId, "PENDING", null))
                    generated++
                }
                date = FinanceSchedule.next(date, FinanceRecurrence.valueOf(rule.recurrence), LocalDate.parse(rule.anchorDate))
                advanced = true
            }
            if (advanced) dao.updateRule(rule.copy(nextDueDate = date?.toString(), version = rule.version + 1))
        }
        generated
    }

    override suspend fun decide(workspaceId: Uuid, occurrenceId: Uuid, zone: ZoneId, decision: PlannedDecision): Outcome<Unit> = write {
        val row = dao.plan(workspaceId.value, occurrenceId.value) ?: missing()
        val alreadyPosted = row.state == "POSTED"
        if (row.state != "PENDING" && !(alreadyPosted && decision is PlannedDecision.Link && decision.candidateId != null)) conflict()
        val transactionId: Uuid? = when (decision) {
            PlannedDecision.Skip -> null
            is PlannedDecision.Link -> {
                if (alreadyPosted && row.transactionId != decision.transactionId.value) conflict()
                if (decision.transactionId !in matchingTransactions(row, zone)) invalid("transactionId")
                decision.candidateId?.let { candidateId ->
                    val candidate = inboxDao.candidate(workspaceId.value, candidateId.value) ?: missing()
                    if (!compatible(row, candidate, zone)) invalid("candidateId")
                    success(inbox.decide(workspaceId, candidateId, InboxDecision.Duplicate(decision.transactionId)))
                }
                decision.transactionId
            }
            is PlannedDecision.Post -> {
                if (!decision.separatePayment && matchingTransactions(row, zone).isNotEmpty()) conflict()
                val candidateId = decision.candidateId
                val candidate = candidateId?.let { inboxDao.candidate(workspaceId.value, it.value) ?: missing() }
                if (candidate != null && !compatible(row, candidate, zone)) invalid("candidateId")
                if (!decision.separatePayment && candidate == null && dao.openCandidates(workspaceId.value).any { compatible(row, it, zone) }) conflict()
                val at = candidate?.occurredAt?.let(Instant::ofEpochMilli) ?: decision.occurredAt
                if (at.isAfter(clock.instant())) invalid("occurredAt")
                val command = PostFinanceCommand(ids.newId(), workspaceId, decision.operationId, TransactionKind.valueOf(row.kind),
                    Uuid.parse(row.accountId), Money(row.amountMinor, CurrencyCode.of(row.currency)), at,
                    categoryId = Uuid.parse(row.categoryId), note = row.title)
                if (candidateId == null) success(ledger.post(command)).id
                else success(inbox.decide(workspaceId, candidateId, InboxDecision.Approve(command))).matchedTransactionId
                    ?: invalid("candidateId")
            }
        }
        if (!alreadyPosted && dao.decide(workspaceId.value, row.id,
                if (decision == PlannedDecision.Skip) "SKIPPED" else "POSTED", transactionId?.value) != 1) conflict()
        audit(workspaceId, occurrenceId, if (decision == PlannedDecision.Skip) "SKIP_PLANNED" else "CONFIRM_PLANNED")
    }

    private fun compatible(row: PlannedTransactionEntity, candidate: ImportCandidateEntity, zone: ZoneId): Boolean =
        candidate.amountMinor == row.amountMinor && candidate.currency == row.currency && candidate.direction == row.kind &&
            candidate.proposedAccountId == row.accountId && candidate.occurredAt?.let {
                kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(row.dueDate), Instant.ofEpochMilli(it).atZone(zone).toLocalDate())) <= 3
            } == true

    private suspend fun matchingTransactions(row: PlannedTransactionEntity, zone: ZoneId): List<Uuid> {
        val date = LocalDate.parse(row.dueDate)
        return finance.transactionsInPeriod(row.workspaceId, date.minusDays(3).atStartOfDay(zone).toInstant().toEpochMilli(),
            date.plusDays(4).atStartOfDay(zone).toInstant().toEpochMilli()).filter { transaction ->
            transaction.kind == row.kind && transaction.debtId == null && finance.reversal(row.workspaceId, transaction.id) == null &&
                finance.entries(row.workspaceId, transaction.id).singleOrNull()?.let {
                    it.accountId == row.accountId && it.currency == row.currency && Math.absExact(it.deltaMinor) == row.amountMinor
                } == true
        }.map { Uuid.parse(it.id) }
    }
    private suspend fun audit(workspace: Uuid, id: Uuid, action: String) {
        database.foundationDao().insertAudit(AuditEventEntity(ids.newId().value, workspace.value, "USER", action, "SCHEDULED_FINANCE", id.value, clock.millis()))
    }
    private fun invalid(field: String): Nothing = throw Abort(Outcome.Failure(AppError.InvalidInput(field)))
    private fun missing(): Nothing = throw Abort(Outcome.Failure(AppError.NotFound))
    private fun conflict(): Nothing = throw Abort(Outcome.Failure(AppError.Conflict))
    private fun <T> success(result: Outcome<T>): T = when (result) { is Outcome.Success -> result.value; is Outcome.Failure -> throw Abort(result) }
    private class Abort(val failure: Outcome.Failure) : RuntimeException()
    private suspend fun <T> write(block: suspend () -> T): Outcome<T> = try { Outcome.Success(database.withTransaction { block() }) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Abort) { error.failure }
    catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
    catch (_: ArithmeticException) { Outcome.Failure(AppError.InvalidInput("amount")) }
    catch (_: IllegalArgumentException) { Outcome.Failure(AppError.InvalidInput("schedule")) }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
}

private fun ScheduledTransaction.entity() = ScheduledTransactionEntity(id.value, workspaceId.value, title, recurrence.name,
    anchorDate.toString(), nextDueDate?.toString(), amountMinor, currency.value, kind.name, accountId.value, categoryId.value, enabled, version)
private fun ScheduledTransactionEntity.domain() = ScheduledTransaction(Uuid.parse(id), Uuid.parse(workspaceId), title,
    FinanceRecurrence.valueOf(recurrence), LocalDate.parse(anchorDate), nextDueDate?.let(LocalDate::parse), amountMinor,
    CurrencyCode.of(currency), TransactionKind.valueOf(kind), Uuid.parse(accountId), Uuid.parse(categoryId), enabled, version)
private fun PlannedTransactionEntity.domain() = PlannedTransaction(Uuid.parse(id), Uuid.parse(ruleId), LocalDate.parse(dueDate),
    title, amountMinor, CurrencyCode.of(currency), TransactionKind.valueOf(kind), Uuid.parse(accountId), Uuid.parse(categoryId),
    PlannedState.valueOf(state), transactionId?.let(Uuid::parse))
