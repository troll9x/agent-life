package vn.lifeos.domain.finance.commands

import java.time.Instant
import java.util.Locale
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

data class ManualEntryInput(val transactionId: Uuid, val operationId: Uuid, val workspaceId: Uuid,
    val kind: TransactionKind, val source: Account, val destination: Account?, val category: Category?,
    val amountText: String, val occurredAt: Instant, val payee: String?, val note: String?)

/** Parses the user's positive magnitude with an explicit locale before entering the ledger. */
class ManualFinanceCommands(private val repository: FinanceRepository, private val locale: Locale) {
    suspend fun post(input: ManualEntryInput): Outcome<FinancialTransaction> {
        if (input.kind !in setOf(TransactionKind.INCOME, TransactionKind.EXPENSE, TransactionKind.TRANSFER))
            return invalid("kind")
        if (input.source.workspaceId != input.workspaceId || input.source.archivedAt != null) return invalid("accountId")
        if (input.kind == TransactionKind.TRANSFER) {
            val destination = input.destination ?: return invalid("destinationAccountId")
            if (destination.workspaceId != input.workspaceId || destination.id == input.source.id ||
                destination.currency != input.source.currency || destination.archivedAt != null) return invalid("destinationAccountId")
            if (input.category != null) return invalid("categoryId")
        } else {
            if (input.destination != null) return invalid("destinationAccountId")
            val direction = if (input.kind == TransactionKind.INCOME) CategoryDirection.INCOME else CategoryDirection.EXPENSE
            val category = input.category ?: return invalid("categoryId")
            if (category.workspaceId != input.workspaceId || category.direction != direction ||
                category.archivedAt != null) return invalid("categoryId")
        }
        val amount = try { Money.parse(input.amountText, input.source.currency, locale) }
            catch (_: IllegalArgumentException) { return invalid("amount") }
            catch (_: ArithmeticException) { return invalid("amount") }
        if (amount.amountMinor <= 0) return invalid("amount")
        return repository.post(PostFinanceCommand(input.transactionId, input.workspaceId, input.operationId,
            input.kind, input.source.id, amount, input.occurredAt, input.category?.id,
            input.destination?.id, payee = input.payee?.takeIf(String::isNotBlank),
            note = input.note?.takeIf(String::isNotBlank), sourceKind = FinanceSourceKind.MANUAL))
    }

    suspend fun createAccount(id: Uuid, workspaceId: Uuid, name: String, kind: AccountKind,
        currency: CurrencyCode, openingText: String, openingAsOf: Instant): Outcome<Account> {
        val opening = try { Money.parse(openingText, currency, locale) }
            catch (_: IllegalArgumentException) { return invalid("openingBalanceMinor") }
            catch (_: ArithmeticException) { return invalid("openingBalanceMinor") }
        return repository.createAccount(Account(id, workspaceId, name, kind, currency, opening.amountMinor, openingAsOf))
    }

    suspend fun createCategory(id: Uuid, workspaceId: Uuid, name: String,
        direction: CategoryDirection): Outcome<Category> =
        repository.createCategory(Category(id, workspaceId, name, direction))

    private fun invalid(field: String): Outcome.Failure = Outcome.Failure(AppError.InvalidInput(field))
}
