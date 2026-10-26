package vn.lifeos.app.settings

import java.time.LocalDate
import java.time.YearMonth

/** A half-open local-date interval. Existing transaction timestamps are not modified. */
internal data class BudgetPeriod(val start: LocalDate, val endExclusive: LocalDate)

internal fun budgetPeriodContaining(today: LocalDate, startDay: Int): BudgetPeriod {
    require(startDay in 1..28)
    val month = YearMonth.from(today)
    val startMonth = if (today.dayOfMonth >= startDay) month else month.minusMonths(1)
    return BudgetPeriod(startMonth.atDay(startDay), startMonth.plusMonths(1).atDay(startDay))
}
