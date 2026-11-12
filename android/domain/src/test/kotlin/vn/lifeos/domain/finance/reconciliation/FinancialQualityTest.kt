package vn.lifeos.domain.finance.reconciliation

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.reports.*

class FinancialQualityTest {
    @Test fun threePendingCandidatesKeepReportAndAdviceIncomplete() {
        val workspace = Uuid.parse("00000000-0000-4000-8000-000000001401")
        val filter = ReportFilter(workspace, CurrencyCode.of("VND"), reportPeriodContaining(LocalDate.parse("2026-11-12"), 1), ZoneId.of("UTC"))
        val quality = FinancialDataQuality(setOf(workspace), 3)
        val report = FinanceReportUseCase.calculate(filter, ReportInput(emptyList(), emptyList(), emptyList(), quality))
        assertEquals(3, report.quality.pendingCandidateCount)
        assertTrue(FinanceQualityFlag.UNRECONCILED in report.quality.flags)
        assertTrue(FinanceQualityFlag.MISSING_SOURCE in report.quality.flags)
        assertFalse(report.qualityForAdvice().canClaimIncomeExpenseComplete)
        assertEquals(3, report.qualityForAdvice().pendingCandidateCount)
    }
    @Test fun missingCoverageIsNotZeroOrCompleteByDefault() {
        assertTrue(FinanceQualityFlag.UNKNOWN_SOURCE_COVERAGE in FinancialDataQuality().flags)
        assertFalse(FinancialDataQuality().canClaimComplete)
    }
}
