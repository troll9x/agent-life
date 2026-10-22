package vn.lifeos.domain.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/** ISO currency with a defined number of minor units. */
@JvmInline
value class CurrencyCode private constructor(val value: String) {
    val exponent: Int
        get() = Currency.getInstance(value).defaultFractionDigits

    companion object {
        fun of(value: String): CurrencyCode {
            require(value.length == 3 && value.all { it in 'A'..'Z' }) {
                "Currency must be an uppercase ISO code"
            }
            val currency = Currency.getInstance(value)
            require(currency.defaultFractionDigits >= 0) { "Currency has no minor-unit exponent" }
            return CurrencyCode(value)
        }
    }

    override fun toString(): String = value
}

/** Exact signed amount in currency minor units; no floating point arithmetic. */
data class Money(val amountMinor: Long, val currency: CurrencyCode) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return Money(Math.addExact(amountMinor, other.amountMinor), currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies" }
        return Money(Math.subtractExact(amountMinor, other.amountMinor), currency)
    }

    fun negated(): Money = Money(Math.negateExact(amountMinor), currency)

    companion object {
        /** Parses a user decimal string using the explicitly supplied input locale. */
        fun parse(input: String, currency: CurrencyCode, locale: Locale): Money {
            val symbols = DecimalFormatSymbols.getInstance(locale)
            val decimal = symbols.decimalSeparator
            val grouping = symbols.groupingSeparator
            require(decimal != grouping) { "Invalid locale number symbols" }

            val trimmed = input.trim()
            val negative = trimmed.startsWith('-')
            val unsigned = if (negative) trimmed.drop(1) else trimmed
            val parts = unsigned.split(decimal)
            require(parts.size in 1..2) { "Invalid decimal number" }

            val groups = parts[0].split(grouping)
            require(groups.isNotEmpty() && groups.all { group -> group.isNotEmpty() && group.all(Char::isAsciiDigit) }) {
                "Invalid whole number"
            }
            if (groups.size > 1) {
                require(groups.first().length in 1..3 && groups.drop(1).all { it.length == 3 }) {
                    "Invalid digit grouping"
                }
            }
            val fraction = parts.getOrNull(1)
            require(fraction == null || (fraction.isNotEmpty() && fraction.all(Char::isAsciiDigit))) {
                "Invalid fraction"
            }

            val normalized = buildString {
                if (negative) append('-')
                append(groups.joinToString(""))
                if (fraction != null) append('.').append(fraction)
            }
            val minor = BigDecimal(normalized)
                .setScale(currency.exponent, RoundingMode.UNNECESSARY)
                .movePointRight(currency.exponent)
                .longValueExact()
            return Money(minor, currency)
        }
    }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
