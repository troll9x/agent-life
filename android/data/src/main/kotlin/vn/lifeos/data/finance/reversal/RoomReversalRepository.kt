package vn.lifeos.data.finance.reversal

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.util.Locale
import kotlinx.coroutines.CancellationException
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.PostFinanceCommand
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.commands.ManualFinanceCommands
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction
import vn.lifeos.domain.finance.reversal.ReplacementResult
import vn.lifeos.domain.finance.reversal.ReversalRepository
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.data.finance.RoomFinanceRepository

/** Both immutable ledger rows commit together, including their entries and evidence. */
class RoomReversalRepository(database: AgentLifeDatabase, clock: Clock, ids: IdGenerator) : ReversalRepository {
    private val dao = database.financeDao()
    private val ledger = RoomFinanceRepository(database, clock, ids)
    private val manual = ManualFinanceCommands(ledger, Locale.forLanguageTag("vi-VN"))
    private val database = database

    override suspend fun replace(command: ReplacePostedTransaction): Outcome<ReplacementResult> {
        val prepared = when (val result = manual.prepare(command.replacement)) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        if (prepared.kind !in setOf(TransactionKind.INCOME, TransactionKind.EXPENSE, TransactionKind.TRANSFER))
            return invalid("kind")
        val workspace = prepared.workspaceId.value
        if (command.originalId == prepared.id || command.reversalId == prepared.id ||
            command.reversalId == command.originalId || command.reversalOperationId == prepared.operationId)
            return invalid("id")
        return try {
            database.withTransaction {
                val original = dao.transaction(workspace, command.originalId.value)
                    ?: throw Abort(Outcome.Failure(AppError.NotFound))
                if (original.postedAt != command.expectedPostedAt.toEpochMilli() ||
                    original.kind !in setOf("INCOME", "EXPENSE", "TRANSFER") ||
                    dao.reversal(workspace, original.id) != null || dao.replacement(workspace, original.id) != null ||
                    dao.refunds(workspace, original.id).isNotEmpty())
                    throw Abort(Outcome.Failure(AppError.Conflict))
                val entries = dao.entries(workspace, original.id)
                if (entries.isEmpty()) throw Abort(invalid("originalId"))
                val first = entries.first()
                val amount = try { Math.absExact(first.deltaMinor) } catch (_: ArithmeticException) {
                    throw Abort(invalid("amount"))
                }
                // The correction date is the accounting date for both rows. An old period stays intact.
                val reverse = PostFinanceCommand(command.reversalId, prepared.workspaceId,
                    command.reversalOperationId, TransactionKind.REVERSAL, Uuid.parse(first.accountId),
                    Money(amount, CurrencyCode.of(first.currency)), prepared.occurredAt,
                    relatedTransactionId = command.originalId, note = "Sửa giao dịch đã ghi")
                val reversed = when (val result = ledger.post(reverse)) {
                    is Outcome.Success -> result.value
                    is Outcome.Failure -> throw Abort(result)
                }
                val replacement = when (val result = ledger.post(prepared.copy(replacementOfId = command.originalId))) {
                    is Outcome.Success -> result.value
                    is Outcome.Failure -> throw Abort(result)
                }
                Outcome.Success(ReplacementResult(reversed, replacement))
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (aborted: Abort) { aborted.failure }
        catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
        catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
        catch (_: ArithmeticException) { invalid("amount") }
        catch (_: IllegalArgumentException) { invalid("command") }
    }

    private fun invalid(field: String) = Outcome.Failure(AppError.InvalidInput(field))
    private class Abort(val failure: Outcome.Failure) : RuntimeException()
}
