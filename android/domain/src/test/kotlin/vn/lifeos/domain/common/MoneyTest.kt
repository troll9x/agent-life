package vn.lifeos.domain.common

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    private val vnd = CurrencyCode.of("VND")
    private val usd = CurrencyCode.of("USD")
    private val vietnamese = Locale.forLanguageTag("vi-VN")

    @Test
    fun vietnameseGroupingParsesIntoExactVndMinorUnits() {
        assertEquals(Money(1_000_000L, vnd), Money.parse("1.000.000", vnd, vietnamese))
        assertEquals(Money(12_500L, vnd), Money.parse("12.500", vnd, vietnamese))
        assertEquals(Money(123L, usd), Money.parse("1.23", usd, Locale.US))
    }

    @Test
    fun rejectsFractionalVndAndMalformedGrouping() {
        assertFailsWith<ArithmeticException> { Money.parse("12.500,5", vnd, vietnamese) }
        assertFailsWith<IllegalArgumentException> { Money.parse("12.50", vnd, vietnamese) }
        assertFailsWith<IllegalArgumentException> { Money.parse("12,50", vnd, Locale.US) }
    }

    @Test
    fun additionChecksCurrencyAndOverflow() {
        assertEquals(Money(3L, usd), Money(1L, usd) + Money(2L, usd))
        assertFailsWith<IllegalArgumentException> { Money(1L, vnd) + Money(1L, usd) }
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE, vnd) + Money(1L, vnd) }
        assertFailsWith<ArithmeticException> { Money.parse("9223372036854775808", vnd, Locale.US) }
        assertFailsWith<ArithmeticException> { Money(Long.MIN_VALUE, vnd).negated() }
    }
}
