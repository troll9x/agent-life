package vn.lifeos.data.finance.debt

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.AuditEventEntity
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.PostFinanceCommand
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.debt.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

class RoomDebtRepository(private val database: AgentLifeDatabase, private val clock: Clock,
    private val ids: IdGenerator) : DebtRepository {
    private val dao = database.debtDao()
    private val finance = database.financeDao()
    private val ledger = RoomFinanceRepository(database, clock, ids)

    override fun observe(workspaceId: Uuid): Flow<Outcome<List<DebtPosition>>> =
        combine(dao.observeAgreements(workspaceId.value), dao.observeMovements(workspaceId.value)) { _, _ ->
            database.withTransaction {
                Outcome.Success(dao.agreements(workspaceId.value).map {
                    DebtPosition(it.domain(), dao.movements(workspaceId.value, it.id).map(DebtMovementEntity::domain))
                }) as Outcome<List<DebtPosition>>
            }
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun create(agreement: DebtAgreement): Outcome<DebtAgreement> = write {
        if (agreement.counterpartyLabel.isBlank() || agreement.counterpartyLabel.length > 120 || agreement.openingPrincipalMinor < 0)
            abort("agreement")
        if (finance.activeWorkspace(agreement.workspaceId.value) == null) throw Abort(Outcome.Failure(AppError.NotFound))
        val normalized = agreement.copy(counterpartyLabel = agreement.counterpartyLabel.trim())
        dao.insertAgreement(DebtAgreementEntity(agreement.id.value, agreement.workspaceId.value, agreement.role.name,
            agreement.counterpartyId.value, normalized.counterpartyLabel, agreement.currency.value,
            agreement.openingPrincipalMinor, agreement.dueDate?.toString(), clock.millis()))
        audit(agreement.workspaceId, agreement.id, "CREATE_DEBT", null)
        normalized
    }

    override suspend fun post(command: PostDebtMovement): Outcome<DebtMovement> = write {
        val position = position(command.workspaceId, command.debtId)
        val agreement = position.agreement
        if (agreement.counterpartyId != command.counterpartyId) abort("counterpartyId")
        if (agreement.currency != command.currency) abort("currency")
        validateTime(position, command.occurredAt)
        if (dao.byOperation(command.workspaceId.value, command.operationId.value) != null) conflict()
        if (command.action == DebtAction.REPAY && command.principalMinor > position.outstandingMinor) abort("overpayment")
        DebtRules.validateAmounts(position.outstandingMinor, command.action,
            command.principalMinor, command.interestMinor, command.feeMinor)
        val costKind = if (agreement.role == DebtRole.BORROWER) TransactionKind.EXPENSE else TransactionKind.INCOME
        if (command.interestMinor == 0L && command.interestCategoryId != null ||
            command.feeMinor == 0L && command.feeCategoryId != null) abort("categoryId")
        if (command.interestMinor > 0 && command.feeMinor > 0 && command.interestCategoryId == command.feeCategoryId)
            abort("feeCategoryId")
        var first = true
        suspend fun component(amount: Long, kind: TransactionKind, categoryId: Uuid?): Uuid? {
            if (amount == 0L) return null
            val operationId = if (first) command.operationId else ids.newId()
            first = false
            val posted = ledger.postDebtComponent(PostFinanceCommand(ids.newId(), command.workspaceId, operationId,
                kind, command.accountId, Money(amount, command.currency), command.occurredAt,
                categoryId = categoryId, debtId = command.debtId, note = command.note))
            return success(posted).id
        }
        val principalId = component(command.principalMinor, DebtRules.principalKind(agreement.role, command.action), null)
        val interestId = component(command.interestMinor, costKind, command.interestCategoryId)
        val feeId = component(command.feeMinor, costKind, command.feeCategoryId)
        // Detect account balance overflow before committing any component.
        success(ledger.balance(command.workspaceId, command.accountId, clock.instant()))
        val row = DebtMovementEntity(command.id.value, command.workspaceId.value, command.debtId.value,
            command.operationId.value, principalId?.value, DebtRules.principalDelta(command.action, command.principalMinor),
            interestId?.value, feeId?.value, command.interestMinor, command.feeMinor,
            command.occurredAt.toEpochMilli(), null, command.note?.trim())
        dao.insertMovement(row)
        audit(command.workspaceId, command.id, "POST_DEBT_MOVEMENT", command.operationId)
        row.domain()
    }

    override suspend fun reverse(command: ReverseDebtMovement): Outcome<DebtMovement> = write {
        if (command.reason.isBlank()) abort("reason")
        val position = position(command.workspaceId, command.debtId)
        validateTime(position, command.occurredAt)
        if (dao.byOperation(command.workspaceId.value, command.operationId.value) != null) conflict()
        val original = position.movements.firstOrNull { it.id == command.movementId } ?: abort("movementId")
        if (original.reversesMovementId != null || position.movements.any { it.reversesMovementId == original.id }) conflict()
        val delta = Math.negateExact(original.principalDeltaMinor)
        if (Math.addExact(position.outstandingMinor, delta) < 0) abort("overpayment")
        var first = true
        suspend fun reverseComponent(id: Uuid?): Uuid? {
            if (id == null) return null
            val entries = finance.entries(command.workspaceId.value, id.value)
            val entry = entries.singleOrNull() ?: abort("transactionId")
            val operation = if (first) command.operationId else ids.newId()
            first = false
            val result = ledger.postDebtComponent(PostFinanceCommand(ids.newId(), command.workspaceId, operation,
                TransactionKind.REVERSAL, Uuid.parse(entry.accountId),
                Money(Math.absExact(entry.deltaMinor), position.agreement.currency), command.occurredAt,
                relatedTransactionId = id, debtId = command.debtId, note = command.reason))
            val reversed = success(result)
            success(ledger.balance(command.workspaceId, Uuid.parse(entry.accountId), clock.instant()))
            return reversed.id
        }
        val principal = reverseComponent(original.transactionId)
        val interest = reverseComponent(original.interestTransactionId)
        val fee = reverseComponent(original.feeTransactionId)
        val row = DebtMovementEntity(command.id.value, command.workspaceId.value, command.debtId.value,
            command.operationId.value, principal?.value, delta, interest?.value, fee?.value,
            Math.negateExact(original.interestMinor), Math.negateExact(original.feeMinor),
            command.occurredAt.toEpochMilli(), original.id.value, command.reason.trim())
        dao.insertMovement(row)
        audit(command.workspaceId, command.id, "REVERSE_DEBT_MOVEMENT", command.operationId)
        row.domain()
    }

    private suspend fun position(workspace: Uuid, id: Uuid): DebtPosition {
        if (finance.activeWorkspace(workspace.value) == null) throw Abort(Outcome.Failure(AppError.NotFound))
        val agreement = dao.agreement(workspace.value, id.value) ?: throw Abort(Outcome.Failure(AppError.NotFound))
        return DebtPosition(agreement.domain(), dao.movements(workspace.value, id.value).map(DebtMovementEntity::domain))
    }
    private fun validateTime(position: DebtPosition, at: Instant) {
        if (at.isAfter(clock.instant()) || position.movements.any { it.occurredAt.isAfter(at) }) abort("occurredAt")
    }
    private suspend fun audit(workspace: Uuid, entity: Uuid, action: String, operation: Uuid?) {
        database.foundationDao().insertAudit(AuditEventEntity(ids.newId().value, workspace.value, "USER", action,
            "DEBT", entity.value, clock.millis(), operation?.value))
    }
    private fun <T> success(result: Outcome<T>): T = when (result) {
        is Outcome.Success -> result.value
        is Outcome.Failure -> throw Abort(result)
    }
    private fun abort(field: String): Nothing = throw Abort(Outcome.Failure(AppError.InvalidInput(field)))
    private fun conflict(): Nothing = throw Abort(Outcome.Failure(AppError.Conflict))
    private class Abort(val failure: Outcome.Failure) : RuntimeException()
    private suspend fun <T> write(block: suspend () -> T): Outcome<T> = try {
        Outcome.Success(database.withTransaction { block() })
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Abort) { error.failure }
    catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
    catch (_: ArithmeticException) { Outcome.Failure(AppError.InvalidInput("amount")) }
    catch (_: IllegalArgumentException) { Outcome.Failure(AppError.InvalidInput("amount")) }
    catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
}

private fun DebtAgreementEntity.domain() = DebtAgreement(Uuid.parse(id), Uuid.parse(workspaceId), DebtRole.valueOf(role),
    Uuid.parse(counterpartyId), counterpartyLabel, CurrencyCode.of(currency), openingPrincipalMinor, dueDate?.let(LocalDate::parse))
private fun DebtMovementEntity.domain() = DebtMovement(Uuid.parse(id), Uuid.parse(debtId), transactionId?.let(Uuid::parse),
    principalDeltaMinor, interestTransactionId?.let(Uuid::parse), feeTransactionId?.let(Uuid::parse), interestMinor, feeMinor,
    Instant.ofEpochMilli(occurredAt), reversesMovementId?.let(Uuid::parse), note)
