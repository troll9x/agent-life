package vn.lifeos.domain.finance.refund

import java.time.Instant
import java.util.Locale
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.FinanceRepository
import vn.lifeos.domain.finance.PostedFinanceRow
import vn.lifeos.domain.finance.PostFinanceCommand
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

data class ManualRefundInput(val id: Uuid, val operationId: Uuid, val workspaceId: Uuid,
    val original: PostedFinanceRow, val depositAccount: Account, val amountText: String,
    val receivedAt: Instant, val note: String?)

data class ManualAdjustmentInput(val id: Uuid, val operationId: Uuid, val workspaceId: Uuid,
    val account: Account, val signedAmountText: String, val occurredAt: Instant, val reason: String)

/** Input parsing stays in domain; Room rechecks links and remaining refund inside its transaction. */
class ManualRefundAdjustmentCommands(private val repository: FinanceRepository, private val locale: Locale) {
    suspend fun refund(input: ManualRefundInput): Outcome<vn.lifeos.domain.finance.FinancialTransaction> {
        val original = input.original.transaction
        val entry = input.original.entries.singleOrNull()
        if (original.workspaceId != input.workspaceId || original.kind != TransactionKind.EXPENSE ||
            original.categoryId == null || entry == null || entry.deltaMinor >= 0L ||
            input.depositAccount.workspaceId != input.workspaceId || input.depositAccount.archivedAt != null ||
            entry.currency != input.depositAccount.currency) return invalid("originalId")
        val amount = parse(input.amountText, input.depositAccount) ?: return invalid("amount")
        if (amount.amountMinor <= 0L) return invalid("amount")
        return repository.post(PostFinanceCommand(input.id, input.workspaceId, input.operationId,
            TransactionKind.REFUND, input.depositAccount.id, amount, input.receivedAt,
            relatedTransactionId = original.id, note = input.note))
    }

    suspend fun adjust(input: ManualAdjustmentInput): Outcome<vn.lifeos.domain.finance.FinancialTransaction> {
        if (input.account.workspaceId != input.workspaceId || input.account.archivedAt != null ||
            input.reason.isBlank()) return invalid("reason")
        val delta = parse(input.signedAmountText, input.account)?.amountMinor ?: return invalid("amount")
        if (delta == 0L || delta == Long.MIN_VALUE) return invalid("amount")
        return repository.post(PostFinanceCommand(input.id, input.workspaceId, input.operationId,
            TransactionKind.ADJUSTMENT, input.account.id, Money(kotlin.math.abs(delta), input.account.currency),
            input.occurredAt, note = input.reason.trim(), adjustmentDeltaMinor = delta))
    }

    private fun parse(text: String, account: Account): Money? = try { Money.parse(text, account.currency, locale) }
        catch (_: IllegalArgumentException) { null }
        catch (_: ArithmeticException) { null }
    private fun invalid(field: String): Outcome.Failure = Outcome.Failure(AppError.InvalidInput(field))
}
