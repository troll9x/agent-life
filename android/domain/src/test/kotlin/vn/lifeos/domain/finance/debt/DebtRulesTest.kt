package vn.lifeos.domain.finance.debt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.lifeos.domain.finance.TransactionKind

class DebtRulesTest {
    @Test fun borrowerAndLenderUseOppositeCashDirectionsWithSamePrincipal() {
        assertEquals(TransactionKind.LOAN_PRINCIPAL_IN, DebtRules.principalKind(DebtRole.BORROWER, DebtAction.ADVANCE))
        assertEquals(TransactionKind.LOAN_PRINCIPAL_OUT, DebtRules.principalKind(DebtRole.BORROWER, DebtAction.REPAY))
        assertEquals(TransactionKind.LOAN_PRINCIPAL_OUT, DebtRules.principalKind(DebtRole.LENDER, DebtAction.ADVANCE))
        assertEquals(TransactionKind.LOAN_PRINCIPAL_IN, DebtRules.principalKind(DebtRole.LENDER, DebtAction.REPAY))
        assertEquals(4_000_000L, DebtRules.validateAmounts(5_000_000, DebtAction.REPAY, 1_000_000, 100_000, 0))
    }
    @Test fun rejectsOverpaymentNegativeEmptyAndOverflow() {
        assertFailsWith<IllegalArgumentException> { DebtRules.validateAmounts(10, DebtAction.REPAY, 11, 0, 0) }
        assertFailsWith<IllegalArgumentException> { DebtRules.validateAmounts(10, DebtAction.REPAY, -1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { DebtRules.validateAmounts(10, DebtAction.ADVANCE, 0, 0, 0) }
        assertFailsWith<ArithmeticException> { DebtRules.validateAmounts(Long.MAX_VALUE, DebtAction.ADVANCE, 1, 0, 0) }
        assertFailsWith<ArithmeticException> { DebtRules.validateAmounts(0, DebtAction.ADVANCE, Long.MAX_VALUE, 1, 0) }
    }
}
