package vn.lifeos.domain.finance.scheduled

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FinanceScheduleTest {
    @Test fun monthEndKeepsAnchorAcrossShortMonths() {
        val jan = LocalDate.parse("2027-01-31")
        val feb = FinanceSchedule.next(jan, FinanceRecurrence.MONTHLY, jan)!!
        assertEquals(LocalDate.parse("2027-02-28"), feb)
        assertEquals(LocalDate.parse("2027-03-31"), FinanceSchedule.next(feb, FinanceRecurrence.MONTHLY, jan))
    }
    @Test fun leapYearAndSimpleRecurrencesAreCalendarDates() {
        val leap = LocalDate.parse("2024-02-29")
        assertEquals(LocalDate.parse("2025-02-28"), FinanceSchedule.next(leap, FinanceRecurrence.YEARLY, leap))
        assertEquals(leap.plusDays(1), FinanceSchedule.next(leap, FinanceRecurrence.DAILY, leap))
        assertEquals(leap.plusDays(7), FinanceSchedule.next(leap, FinanceRecurrence.WEEKLY, leap))
        assertNull(FinanceSchedule.next(leap, FinanceRecurrence.ONCE, leap))
    }
}
