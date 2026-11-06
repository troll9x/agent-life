package vn.lifeos.data.finance.goals

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.finance.goals.SavingsGoal
import vn.lifeos.domain.finance.goals.SavingsGoalRepository
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

class RoomSavingsGoalRepository(private val database: AgentLifeDatabase, private val clock: Clock) : SavingsGoalRepository {
    private val goals = database.savingsGoalDao()
    private val finance = database.financeDao()

    override fun observe(workspaceId: Uuid): Flow<Outcome<List<SavingsGoal>>> =
        goals.observeGoals(workspaceId.value).map<List<SavingsGoalEntity>, Outcome<List<SavingsGoal>>> { rows ->
            database.withTransaction {
                Outcome.Success(rows.map { row -> row.domain(goals.links(workspaceId.value, row.id)) })
            }
        }.catch { if (it is CancellationException) throw it else emit(Outcome.Failure(AppError.Storage)) }

    override suspend fun create(goal: SavingsGoal): Outcome<SavingsGoal> = safe {
        if (goal.name.isBlank() || goal.targetMinor <= 0 || goal.linkedAccountIds.isEmpty() || goal.archivedAt != null)
            return@safe invalid("goal")
        database.withTransaction {
            if (finance.activeWorkspace(goal.workspaceId.value) == null) return@withTransaction Outcome.Failure(AppError.NotFound)
            for (accountId in goal.linkedAccountIds) {
                val account = finance.account(goal.workspaceId.value, accountId.value)
                    ?: return@withTransaction invalid("linkedAccountIds")
                if (account.currency != goal.currency.value || account.kind != AccountKind.SAVINGS.name ||
                    account.archivedAt != null) return@withTransaction invalid("linkedAccountIds")
            }
            val normalized = goal.copy(name = goal.name.trim())
            goals.insertGoal(SavingsGoalEntity(goal.id.value, goal.workspaceId.value, normalized.name,
                goal.currency.value, goal.targetMinor, goal.targetDate?.toString(), null))
            goal.linkedAccountIds.forEach { goals.insertLink(SavingsGoalAccountEntity(goal.id.value, it.value, goal.workspaceId.value)) }
            Outcome.Success(normalized)
        }
    }

    override suspend fun updateTarget(workspaceId: Uuid, goalId: Uuid, targetMinor: Long,
        targetDate: LocalDate?): Outcome<SavingsGoal> = safe {
        if (targetMinor <= 0) return@safe invalid("targetMinor")
        database.withTransaction {
            val before = goals.goal(workspaceId.value, goalId.value) ?: return@withTransaction Outcome.Failure(AppError.NotFound)
            if (before.archivedAt != null) return@withTransaction Outcome.Failure(AppError.Conflict)
            if (goals.updateTarget(workspaceId.value, goalId.value, targetMinor, targetDate?.toString()) != 1)
                return@withTransaction Outcome.Failure(AppError.Conflict)
            Outcome.Success(before.copy(targetMinor = targetMinor, targetDate = targetDate?.toString())
                .domain(goals.links(workspaceId.value, goalId.value)))
        }
    }

    override suspend fun archive(workspaceId: Uuid, goalId: Uuid): Outcome<SavingsGoal> = safe {
        database.withTransaction {
            val before = goals.goal(workspaceId.value, goalId.value) ?: return@withTransaction Outcome.Failure(AppError.NotFound)
            if (before.archivedAt != null) return@withTransaction Outcome.Failure(AppError.Conflict)
            val archivedAt = clock.instant().toEpochMilli()
            if (goals.archive(workspaceId.value, goalId.value, archivedAt) != 1)
                return@withTransaction Outcome.Failure(AppError.Conflict)
            Outcome.Success(before.copy(archivedAt = archivedAt).domain(goals.links(workspaceId.value, goalId.value)))
        }
    }

    private suspend fun <T> safe(block: suspend () -> Outcome<T>): Outcome<T> = try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SQLiteConstraintException) { Outcome.Failure(AppError.Conflict) }
        catch (_: IllegalArgumentException) { invalid("goal") }
        catch (_: ArithmeticException) { invalid("amount") }
        catch (_: SQLiteException) { Outcome.Failure(AppError.Storage) }
    private fun invalid(field: String): Outcome.Failure = Outcome.Failure(AppError.InvalidInput(field))
}

private fun SavingsGoalEntity.domain(links: List<SavingsGoalAccountEntity>) = SavingsGoal(
    Uuid.parse(id), Uuid.parse(workspaceId), name, CurrencyCode.of(currency), targetMinor,
    targetDate?.let(LocalDate::parse), links.map { Uuid.parse(it.accountId) }.toSet(),
    archivedAt?.let(Instant::ofEpochMilli))
