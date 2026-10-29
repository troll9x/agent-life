package vn.lifeos.domain.finance

import vn.lifeos.domain.common.Money

/** Pure rules shared by all manual and imported posting paths. */
object LedgerRules {
    fun entries(kind: TransactionKind, amount: Money, source: Account, destination: Account? = null): List<Pair<Account, Long>> {
        require(amount.amountMinor > 0) { "amount" }
        require(source.currency == amount.currency) { "currency" }
        return when (kind) {
            TransactionKind.INCOME, TransactionKind.REFUND, TransactionKind.LOAN_PRINCIPAL_IN -> listOf(source to amount.amountMinor)
            TransactionKind.EXPENSE, TransactionKind.LOAN_PRINCIPAL_OUT -> listOf(source to Math.negateExact(amount.amountMinor))
            TransactionKind.TRANSFER -> {
                require(destination != null && destination.id != source.id) { "destinationAccountId" }
                require(destination.currency == source.currency) { "currency" }
                listOf(source to Math.negateExact(amount.amountMinor), destination to amount.amountMinor)
                    .also { require(Math.addExact(it[0].second, it[1].second) == 0L) { "unbalanced transfer" } }
            }
            TransactionKind.ADJUSTMENT -> error("Adjustment requires a signed, reviewed command")
            TransactionKind.REVERSAL -> error("Reversal must copy opposite deltas of the original transaction")
        }
    }

    fun balance(opening: Long, deltas: Iterable<Long>): Long = deltas.fold(opening, Math::addExact)
}
