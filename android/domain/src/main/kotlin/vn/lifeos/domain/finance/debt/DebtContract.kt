package vn.lifeos.domain.finance.debt

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.foundation.Outcome

enum class DebtRole { LENDER, BORROWER }
enum class DebtAction { ADVANCE, REPAY }

data class DebtAgreement(val id: Uuid, val workspaceId: Uuid, val role: DebtRole,
    val counterpartyId: Uuid, val counterpartyLabel: String, val currency: CurrencyCode,
    val openingPrincipalMinor: Long, val dueDate: LocalDate? = null)

data class DebtMovement(val id: Uuid, val debtId: Uuid, val transactionId: Uuid?,
    val principalDeltaMinor: Long, val interestTransactionId: Uuid?, val feeTransactionId: Uuid?,
    val interestMinor: Long, val feeMinor: Long, val occurredAt: Instant,
    val reversesMovementId: Uuid? = null, val note: String? = null)

data class DebtPosition(val agreement: DebtAgreement, val movements: List<DebtMovement>) {
    val outstandingMinor: Long get() = movements.fold(agreement.openingPrincipalMinor) { sum, movement ->
        Math.addExact(sum, movement.principalDeltaMinor)
    }
    val interestMinor: Long get() = movements.fold(0L) { sum, movement -> Math.addExact(sum, movement.interestMinor) }
    val feeMinor: Long get() = movements.fold(0L) { sum, movement -> Math.addExact(sum, movement.feeMinor) }
}

data class PostDebtMovement(val id: Uuid, val workspaceId: Uuid, val debtId: Uuid,
    val counterpartyId: Uuid, val operationId: Uuid, val accountId: Uuid, val currency: CurrencyCode,
    val action: DebtAction, val principalMinor: Long, val interestMinor: Long, val feeMinor: Long,
    val interestCategoryId: Uuid?, val feeCategoryId: Uuid?, val occurredAt: Instant, val note: String? = null)

data class ReverseDebtMovement(val id: Uuid, val workspaceId: Uuid, val debtId: Uuid,
    val movementId: Uuid, val operationId: Uuid, val occurredAt: Instant, val reason: String)

interface DebtRepository {
    fun observe(workspaceId: Uuid): Flow<Outcome<List<DebtPosition>>>
    suspend fun create(agreement: DebtAgreement): Outcome<DebtAgreement>
    suspend fun post(command: PostDebtMovement): Outcome<DebtMovement>
    suspend fun reverse(command: ReverseDebtMovement): Outcome<DebtMovement>
}

object DebtRules {
    fun principalDelta(action: DebtAction, principal: Long): Long {
        require(principal >= 0)
        return if (action == DebtAction.ADVANCE) principal else Math.negateExact(principal)
    }
    fun principalKind(role: DebtRole, action: DebtAction): TransactionKind =
        if ((role == DebtRole.BORROWER) == (action == DebtAction.ADVANCE)) TransactionKind.LOAN_PRINCIPAL_IN
        else TransactionKind.LOAN_PRINCIPAL_OUT
    fun validateAmounts(outstanding: Long, action: DebtAction, principal: Long, interest: Long, fee: Long): Long {
        require(principal >= 0 && interest >= 0 && fee >= 0) { "amount" }
        require(Math.addExact(Math.addExact(principal, interest), fee) > 0) { "amount" }
        val result = Math.addExact(outstanding, principalDelta(action, principal))
        require(result >= 0) { "overpayment" }
        return result
    }
}
