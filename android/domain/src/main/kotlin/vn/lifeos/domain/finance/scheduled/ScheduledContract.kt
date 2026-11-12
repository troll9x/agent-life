package vn.lifeos.domain.finance.scheduled

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.foundation.Outcome

enum class FinanceRecurrence { ONCE, DAILY, WEEKLY, MONTHLY, YEARLY }
enum class PlannedState { PENDING, POSTED, SKIPPED }
data class ScheduledTransaction(val id: Uuid, val workspaceId: Uuid, val title: String,
    val recurrence: FinanceRecurrence, val anchorDate: LocalDate, val nextDueDate: LocalDate?,
    val amountMinor: Long, val currency: CurrencyCode, val kind: TransactionKind,
    val accountId: Uuid, val categoryId: Uuid, val enabled: Boolean = true, val version: Long = 1)
data class PlannedTransaction(val id: Uuid, val ruleId: Uuid, val dueDate: LocalDate, val title: String,
    val amountMinor: Long, val currency: CurrencyCode, val kind: TransactionKind, val accountId: Uuid,
    val categoryId: Uuid, val state: PlannedState, val transactionId: Uuid?)
data class ScheduledEvidenceSuggestion(val candidateId: Uuid, val sourceKind: String, val occurredAt: Instant)
data class PlannedItem(val plan: PlannedTransaction, val evidence: List<ScheduledEvidenceSuggestion>,
    val postedTransactionIds: List<Uuid>)
data class ScheduledSnapshot(val rules: List<ScheduledTransaction>, val items: List<PlannedItem>)
sealed interface PlannedDecision {
    data object Skip : PlannedDecision
    data class Post(val operationId: Uuid, val occurredAt: Instant, val candidateId: Uuid? = null,
        val separatePayment: Boolean = false) : PlannedDecision
    data class Link(val transactionId: Uuid, val candidateId: Uuid? = null) : PlannedDecision
}
interface ScheduledFinanceRepository {
    fun observe(workspaceId: Uuid, zone: ZoneId): Flow<Outcome<ScheduledSnapshot>>
    suspend fun save(rule: ScheduledTransaction, zone: ZoneId, expectedVersion: Long? = null): Outcome<ScheduledTransaction>
    suspend fun setEnabled(workspaceId: Uuid, ruleId: Uuid, expectedVersion: Long, enabled: Boolean): Outcome<Unit>
    suspend fun generateDue(workspaceId: Uuid, zone: ZoneId): Outcome<Int>
    suspend fun decide(workspaceId: Uuid, occurrenceId: Uuid, zone: ZoneId, decision: PlannedDecision): Outcome<Unit>
}
object FinanceSchedule {
    fun next(date: LocalDate, recurrence: FinanceRecurrence, anchor: LocalDate): LocalDate? = when (recurrence) {
        FinanceRecurrence.ONCE -> null
        FinanceRecurrence.DAILY -> date.plusDays(1)
        FinanceRecurrence.WEEKLY -> date.plusWeeks(1)
        FinanceRecurrence.MONTHLY -> date.plusMonths(1).withDayOfMonth(1).let { it.withDayOfMonth(minOf(anchor.dayOfMonth, it.lengthOfMonth())) }
        FinanceRecurrence.YEARLY -> LocalDate.of(date.year + 1, anchor.month, 1).let { it.withDayOfMonth(minOf(anchor.dayOfMonth, it.lengthOfMonth())) }
    }
}
