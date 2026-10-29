package vn.lifeos.domain.finance

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid

class LedgerRulesTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(1)
    private val opening = Instant.parse("2026-10-01T00:00:00Z")
    private val vnd = CurrencyCode.of("VND")
    private val bank = Account(id(2), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, opening)
    private val cash = Account(id(3), workspace, "Fixture cash", AccountKind.CASH, vnd, 0, opening)

    @Test fun transferVectorBalancesWithoutIncomeOrExpense() {
        val entries = LedgerRules.entries(TransactionKind.TRANSFER, Money(1_000_000, vnd), bank, cash)
        assertEquals(0L, entries.sumOf { it.second })
        assertEquals(9_000_000, LedgerRules.balance(bank.openingBalanceMinor, listOf(entries[0].second)))
        assertEquals(1_000_000, LedgerRules.balance(cash.openingBalanceMinor, listOf(entries[1].second)))
    }

    @Test fun transferRejectsCurrencyMismatchAndSameAccount() {
        assertFailsWith<IllegalArgumentException> { LedgerRules.entries(TransactionKind.TRANSFER, Money(1, vnd), bank, bank) }
        assertFailsWith<IllegalArgumentException> { LedgerRules.entries(TransactionKind.TRANSFER, Money(1, vnd), bank,
            cash.copy(currency = CurrencyCode.of("USD"))) }
    }

    @Test fun balanceRejectsOverflow() {
        assertFailsWith<ArithmeticException> { LedgerRules.balance(Long.MAX_VALUE, listOf(1)) }
    }
}
