package vn.lifeos.domain.finance.goals

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.AccountKind

class SavingsGoalProgressTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(1)
    private val vnd = CurrencyCode.of("VND")
    private val before = Instant.parse("2026-10-01T00:00:00Z")
    private val bank = Account(id(2), workspace, "bank", AccountKind.BANK, vnd, 10_000_000, before)
    private val savings = Account(id(3), workspace, "savings", AccountKind.SAVINGS, vnd, 0, before)

    @Test fun sharedSavingsAccountAppearsInGoalsButAssetsCountOnce() {
        val goals = listOf(
            SavingsGoal(id(4), workspace, "Home", vnd, 10_000_000, null, setOf(savings.id)),
            SavingsGoal(id(5), workspace, "Emergency", vnd, 4_000_000, null, setOf(savings.id)))
        val overview = SavingsGoalProgress.calculate(goals, listOf(bank, savings),
            mapOf(bank.id to 8_000_000, savings.id to 2_000_000))
        assertEquals(listOf(2_000, 5_000), overview.goals.map { it.progressBasisPoints })
        assertEquals(listOf(2_000_000L, 2_000_000L), overview.goals.map { it.currentMinor })
        assertEquals(10_000_000L, overview.totalAssetsByCurrency[vnd])
    }

    @Test fun archivedGoalDoesNotEraseAccountBalance() {
        val archived = SavingsGoal(id(4), workspace, "Home", vnd, 10_000_000, null,
            setOf(savings.id), Instant.parse("2026-11-01T00:00:00Z"))
        val overview = SavingsGoalProgress.calculate(listOf(archived), listOf(bank, savings),
            mapOf(bank.id to 8_000_000, savings.id to 2_000_000))
        assertEquals(2_000_000L, overview.goals.single().currentMinor)
        assertEquals(10_000_000L, overview.totalAssetsByCurrency[vnd])
    }
}
