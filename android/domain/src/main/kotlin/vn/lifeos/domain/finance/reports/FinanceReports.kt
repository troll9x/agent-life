package vn.lifeos.domain.finance.reports

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.math.BigInteger
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Budget
import vn.lifeos.domain.finance.Category
import vn.lifeos.domain.finance.PostedFinanceRow
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.foundation.Outcome

data class ReportPeriod(val start: LocalDate, val endExclusive: LocalDate) {
    fun previous() = ReportPeriod(start.minusMonths(1), endExclusive.minusMonths(1))
    fun previousComparable(asOf: LocalDate): ReportPeriod {
        val prior = previous()
        if (asOf.isBefore(start) || !asOf.isBefore(endExclusive)) return prior
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(start, asOf) + 1
        val comparableEnd = prior.start.plusDays(elapsedDays)
        return prior.copy(endExclusive = if (comparableEnd.isBefore(prior.endExclusive)) comparableEnd else prior.endExclusive)
    }
}

fun reportPeriodContaining(date: LocalDate, startDay: Int): ReportPeriod {
    require(startDay in 1..28)
    val month = YearMonth.from(date).let { if (date.dayOfMonth < startDay) it.minusMonths(1) else it }
    return ReportPeriod(month.atDay(startDay), month.plusMonths(1).atDay(startDay))
}

data class ReportFilter(val workspaceId: Uuid, val currency: CurrencyCode, val period: ReportPeriod,
    val zone: ZoneId, val accountId: Uuid? = null) {
    val start: Instant get() = period.start.atStartOfDay(zone).toInstant()
    val endExclusive: Instant get() = period.endExclusive.atStartOfDay(zone).toInstant()
}

data class ReportFact(val row: PostedFinanceRow, val originalKind: TransactionKind?)
data class ReportInput(val facts: List<ReportFact>, val categories: List<Category>, val budgets: List<Budget>)

interface FinanceReportsRepository {
    suspend fun load(filter: ReportFilter): Outcome<ReportInput>
    suspend fun saveBudget(budget: Budget): Outcome<Budget>
}

data class ReportLine(val id: Uuid, val occurredAt: Instant, val kind: TransactionKind,
    val categoryId: Uuid?, val categoryName: String, val payee: String?,
    val incomeMinor: Long, val grossExpenseMinor: Long, val refundMinor: Long) {
    val netExpenseMinor: Long get() = Math.subtractExact(grossExpenseMinor, refundMinor)
}

data class BudgetStatus(val budget: Budget, val categoryName: String, val spentMinor: Long) {
    val remainingMinor: Long get() = Math.subtractExact(budget.limitMinor, spentMinor)
    val alert: Boolean get() = budget.alertThresholdBasisPoints?.let {
        BigInteger.valueOf(spentMinor).multiply(BigInteger.valueOf(10_000L)) >=
            BigInteger.valueOf(budget.limitMinor).multiply(BigInteger.valueOf(it.toLong()))
    } ?: false
}

data class ReportSnapshot(val filter: ReportFilter, val lines: List<ReportLine>, val budgets: List<BudgetStatus>) {
    val incomeMinor: Long get() = lines.fold(0L) { sum, line -> Math.addExact(sum, line.incomeMinor) }
    val grossExpenseMinor: Long get() = lines.fold(0L) { sum, line -> Math.addExact(sum, line.grossExpenseMinor) }
    val refundMinor: Long get() = lines.fold(0L) { sum, line -> Math.addExact(sum, line.refundMinor) }
    val netExpenseMinor: Long get() = Math.subtractExact(grossExpenseMinor, refundMinor)
    val surplusMinor: Long get() = Math.subtractExact(incomeMinor, netExpenseMinor)
    fun categoryLines(categoryId: Uuid) = lines.filter { it.categoryId == categoryId }
}

object FinanceReportUseCase {
    fun calculate(filter: ReportFilter, input: ReportInput): ReportSnapshot {
        val names = input.categories.associate { it.id to it.name }
        val lines = input.facts.mapNotNull { fact ->
            val transaction = fact.row.transaction
            val matching = fact.row.entries.filter { it.currency == filter.currency }
            if (matching.isEmpty() || filter.accountId != null && matching.none { it.accountId == filter.accountId }) return@mapNotNull null
            val amount = matching.fold(0L) { sum, entry -> Math.addExact(sum, entry.deltaMinor) }
            val kind = if (transaction.kind == TransactionKind.REVERSAL) fact.originalKind else transaction.kind
            val income = if (kind == TransactionKind.INCOME) amount else 0L
            val expense = if (kind == TransactionKind.EXPENSE) Math.negateExact(amount) else 0L
            val refund = if (kind == TransactionKind.REFUND) amount else 0L
            ReportLine(transaction.id, transaction.occurredAt, transaction.kind, transaction.categoryId,
                transaction.categoryId?.let { names[it] } ?: "Không có nhóm", transaction.payee, income, expense, refund)
        }
        val budgets = input.budgets.filter { it.currency == filter.currency && it.periodStartDate == filter.period.start }
            .map { budget -> BudgetStatus(budget, names[budget.categoryId] ?: "Nhóm đã xóa",
                lines.filter { it.categoryId == budget.categoryId }.fold(0L) { sum, line -> Math.addExact(sum, line.netExpenseMinor) }) }
        return ReportSnapshot(filter, lines, budgets)
    }
}

/** RFC 4180 CSV; spreadsheet control prefixes are forced to text before quoting. */
fun ReportSnapshot.toSafeCsv(): String = buildString {
    append("id,occurred_at,kind,category,payee,income_minor,gross_expense_minor,refund_minor,net_expense_minor,currency\r\n")
    lines.forEach { line ->
        append(listOf(csvCell(line.id.value), csvCell(line.occurredAt.toString()), csvCell(line.kind.name),
            csvCell(line.categoryName), csvCell(line.payee.orEmpty()), line.incomeMinor.toString(),
            line.grossExpenseMinor.toString(), line.refundMinor.toString(), line.netExpenseMinor.toString(),
            csvCell(filter.currency.value)).joinToString(",")).append("\r\n")
    }
}

private fun csvCell(value: String): String {
    val unsafe = value.trimStart().firstOrNull() in setOf('=', '+', '-', '@') ||
        value.firstOrNull()?.let { it == '\t' || it == '\r' || it == '\n' } == true
    val safe = if (unsafe) "'" + value else value
    return "\"${safe.replace("\"", "\"\"")}\""
}
