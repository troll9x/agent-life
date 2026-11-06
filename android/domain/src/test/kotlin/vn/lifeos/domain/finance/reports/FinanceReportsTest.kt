package vn.lifeos.domain.finance.reports

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*

class FinanceReportsTest {
    private val workspace = id(1)
    private val account = id(2)
    private val savings = id(3)
    private val category = Category(id(4), workspace, "=SUM(1,2)", CategoryDirection.EXPENSE)
    private val currency = CurrencyCode.of("VND")
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val period = reportPeriodContaining(LocalDate.parse("2026-10-20"), 15)
    private val filter = ReportFilter(workspace, currency, period, zone)

    @Test fun periodExcludesPreviousDayAndPreviousHasSameBoundaries() {
        assertEquals(LocalDate.parse("2026-10-15"), period.start)
        assertEquals(LocalDate.parse("2026-11-15"), period.endExclusive)
        assertEquals(LocalDate.parse("2026-09-15"), period.previous().start)
        assertEquals(LocalDate.parse("2026-09-21"), period.previousComparable(LocalDate.parse("2026-10-20")).endExclusive)
        val before = LocalDate.parse("2026-10-14").atTime(23, 59).atZone(zone).toInstant()
        assertTrue(before.isBefore(filter.start))
    }

    @Test fun transferRefundReversalAndCsvReconcile() {
        val expense = fact(10, TransactionKind.EXPENSE, "2026-10-16T00:00:00Z", -1000, category.id)
        val transfer = fact(11, TransactionKind.TRANSFER, "2026-10-17T00:00:00Z", -500, null,
            listOf(entry(11, account, -500), entry(11, savings, 500)))
        val reversal = fact(12, TransactionKind.REVERSAL, "2026-10-18T00:00:00Z", 1000, category.id,
            originalKind = TransactionKind.EXPENSE)
        val replacement = fact(13, TransactionKind.EXPENSE, "2026-10-19T00:00:00Z", -800, category.id)
        val refund = fact(14, TransactionKind.REFUND, "2026-10-20T00:00:00Z", 200, category.id)
        val budget = Budget(id(20), workspace, category.id, period.start, currency, 1000, 8000)
        val report = FinanceReportUseCase.calculate(filter, ReportInput(
            listOf(expense, transfer, reversal, replacement, refund), listOf(category), listOf(budget)))
        assertEquals(0L, report.incomeMinor)
        assertEquals(800L, report.grossExpenseMinor)
        assertEquals(200L, report.refundMinor)
        assertEquals(600L, report.netExpenseMinor)
        assertEquals(600L, report.budgets.single().spentMinor)
        assertEquals(400L, report.budgets.single().remainingMinor)
        assertFalse(report.budgets.single().alert)
        assertEquals(600L, report.lines.sumOf { it.netExpenseMinor })
        assertTrue(report.toSafeCsv().contains("\"'=SUM(1,2)\""))
        assertTrue(report.toSafeCsv().contains(",-1000,"))
        assertEquals(4, report.categoryLines(category.id).size)
        assertEquals(0L, FinanceReportUseCase.calculate(filter.copy(accountId = savings),
            ReportInput(listOf(transfer), listOf(category), listOf(budget))).netExpenseMinor)
    }

    @Test fun refundOnlyCountsInReceivedPeriod() {
        val refund = fact(14, TransactionKind.REFUND, "2026-10-20T00:00:00Z", 200, category.id)
        val current = FinanceReportUseCase.calculate(filter, ReportInput(listOf(refund), listOf(category), emptyList()))
        val previous = FinanceReportUseCase.calculate(filter.copy(period = period.previous()),
            ReportInput(emptyList(), listOf(category), emptyList()))
        assertEquals(-200L, current.netExpenseMinor)
        assertEquals(0L, previous.netExpenseMinor)
    }

    private fun fact(n: Int, kind: TransactionKind, occurred: String, delta: Long, categoryId: Uuid?,
        entries: List<LedgerEntry> = listOf(entry(n, account, delta)), originalKind: TransactionKind? = null): ReportFact {
        val transaction = FinancialTransaction(id(n), workspace, id(n + 100), kind, Instant.parse(occurred),
            Instant.parse(occurred), categoryId, null, null, null, null, null, null)
        return ReportFact(PostedFinanceRow(transaction, entries), originalKind)
    }
    private fun entry(n: Int, accountId: Uuid, delta: Long) = LedgerEntry(id(n + accountId.value.takeLast(1).toInt() + 200),
        id(n), accountId, delta, currency)
    private fun id(n: Int) = Uuid.parse("00000000-0000-0000-0000-${n.toString().padStart(12, '0')}")
}
