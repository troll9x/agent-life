package vn.lifeos.app.settings

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetPeriodPreviewTest {
    @Test fun changingStartFromFirstToFifteenthUsesHalfOpenMonthlyRange() {
        val today = LocalDate.parse("2026-10-20")
        assertEquals(BudgetPeriod(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01")), budgetPeriodContaining(today, 1))
        assertEquals(BudgetPeriod(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-11-15")), budgetPeriodContaining(today, 15))
        assertEquals(BudgetPeriod(LocalDate.parse("2026-09-15"), LocalDate.parse("2026-10-15")), budgetPeriodContaining(LocalDate.parse("2026-10-14"), 15))
    }
}
