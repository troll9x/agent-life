package vn.lifeos.domain.finance.goals

import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.foundation.Outcome

data class SavingsGoal(val id: Uuid, val workspaceId: Uuid, val name: String,
    val currency: CurrencyCode, val targetMinor: Long, val targetDate: LocalDate?,
    val linkedAccountIds: Set<Uuid>, val archivedAt: Instant? = null)

interface SavingsGoalRepository {
    fun observe(workspaceId: Uuid): Flow<Outcome<List<SavingsGoal>>>
    suspend fun create(goal: SavingsGoal): Outcome<SavingsGoal>
    suspend fun updateTarget(workspaceId: Uuid, goalId: Uuid, targetMinor: Long, targetDate: LocalDate?): Outcome<SavingsGoal>
    suspend fun archive(workspaceId: Uuid, goalId: Uuid): Outcome<SavingsGoal>
}

data class GoalProgress(val goal: SavingsGoal, val currentMinor: Long, val progressBasisPoints: Int)
data class GoalOverview(val goals: List<GoalProgress>, val totalAssetsByCurrency: Map<CurrencyCode, Long>)

object SavingsGoalProgress {
    fun calculate(goals: List<SavingsGoal>, accounts: List<Account>, balances: Map<Uuid, Long>): GoalOverview {
        val accountsById = accounts.associateBy { it.id }
        val progress = goals.map { goal ->
            require(goal.targetMinor > 0)
            val current = goal.linkedAccountIds.fold(0L) { sum, id ->
                val account = requireNotNull(accountsById[id])
                require(account.currency == goal.currency && account.kind == AccountKind.SAVINGS)
                Math.addExact(sum, requireNotNull(balances[id]))
            }
            val basisPoints = BigInteger.valueOf(current.coerceAtLeast(0L))
                .multiply(BigInteger.valueOf(10_000L)).divide(BigInteger.valueOf(goal.targetMinor))
                .min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
            GoalProgress(goal, current, basisPoints)
        }
        val assets = accounts.filter { it.kind != AccountKind.CREDIT }
            .groupBy { it.currency }.mapValues { (_, sameCurrency) ->
                sameCurrency.distinctBy { it.id }.fold(0L) { sum, account ->
                    Math.addExact(sum, requireNotNull(balances[account.id]))
                }
            }
        return GoalOverview(progress, assets)
    }
}
