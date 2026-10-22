package vn.lifeos.domain.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeWindowTest {
    @Test
    fun hanoiClockUsesProfileZone() {
        val clock = FixtureFactory.clock()
        val local = localNow(clock, ZoneId.of("Asia/Ho_Chi_Minh"))
        assertEquals(8, local.hour)
        assertEquals(59, local.minute)
        assertEquals(LocalDate.of(2026, 10, 19), local.toLocalDate())
    }

    @Test
    fun springDstDayIsTwentyThreeHoursWithExclusiveEnd() {
        val date = LocalDate.of(2026, 3, 8)
        val window = LocalDateRange(date, date.plusDays(1), ZoneId.of("America/New_York"))
            .toTimeWindow()
        assertEquals(Instant.parse("2026-03-08T05:00:00Z"), window.startInclusive)
        assertEquals(Instant.parse("2026-03-09T04:00:00Z"), window.endExclusive)
        assertTrue(window.startInclusive in window)
        assertFalse(window.endExclusive in window)
    }

    @Test
    fun fallDstDayIsTwentyFiveHours() {
        val date = LocalDate.of(2026, 11, 1)
        val window = LocalDateRange(date, date.plusDays(1), ZoneId.of("America/New_York"))
            .toTimeWindow()
        assertEquals(Instant.parse("2026-11-01T04:00:00Z"), window.startInclusive)
        assertEquals(Instant.parse("2026-11-02T05:00:00Z"), window.endExclusive)
    }
}
