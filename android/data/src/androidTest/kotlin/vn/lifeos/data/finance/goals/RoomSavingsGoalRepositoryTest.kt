package vn.lifeos.data.finance.goals

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.finance.RoomFinanceRepository
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.finance.FinanceReportQuery
import vn.lifeos.domain.finance.PostFinanceCommand
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.goals.SavingsGoal
import vn.lifeos.domain.finance.goals.SavingsGoalProgress
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class RoomSavingsGoalRepositoryTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val now = Instant.parse("2026-10-29T12:00:00Z")
    private val start = Instant.parse("2026-10-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val vnd = CurrencyCode.of("VND")
    private val workspace = id(801)
    private val bank = Account(id(805), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, start)
    private val savings = Account(id(806), workspace, "Fixture savings", AccountKind.SAVINGS, vnd, 0, start)

    @Test fun transferProgressArchiveAndReopenStayLedgerBacked() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "savings-goal-repository.db"
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        assertTrue(RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(802), id(803), id(804), "Personal", "Fixture",
                "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val finance = RoomFinanceRepository(db, clock, RandomIdGenerator)
        val goals = RoomSavingsGoalRepository(db, clock)
        assertTrue(finance.createAccount(bank) is Outcome.Success)
        assertTrue(finance.createAccount(savings) is Outcome.Success)
        val goal = SavingsGoal(id(810), workspace, "Reserve", vnd, 10_000_000, null, setOf(savings.id))
        assertTrue(goals.create(goal) is Outcome.Success)
        assertTrue(goals.create(goal.copy(id = id(811), targetMinor = 4_000_000)) is Outcome.Success)
        assertTrue(finance.post(PostFinanceCommand(id(812), workspace, id(813), TransactionKind.TRANSFER,
            bank.id, Money(2_000_000, vnd), now, destinationAccountId = savings.id)) is Outcome.Success)
        val balances = mapOf(bank.id to (finance.balance(workspace, bank.id, now) as Outcome.Success).value.amountMinor,
            savings.id to (finance.balance(workspace, savings.id, now) as Outcome.Success).value.amountMinor)
        val progress = SavingsGoalProgress.calculate((goals.observe(workspace).first() as Outcome.Success).value,
            listOf(bank, savings), balances)
        assertEquals(2_000_000L, progress.goals.first { it.goal.id == goal.id }.currentMinor)
        assertEquals(2_000, progress.goals.first { it.goal.id == goal.id }.progressBasisPoints)
        assertEquals(5_000, progress.goals.first { it.goal.id == id(811) }.progressBasisPoints)
        assertEquals(10_000_000L, progress.totalAssetsByCurrency[vnd])
        val report = (finance.report(FinanceReportQuery(workspace, vnd, start, now.plusSeconds(1))) as Outcome.Success).value
        assertEquals(0L, report.incomeMinor)
        assertEquals(0L, report.netExpenseMinor)
        assertTrue(goals.updateTarget(workspace, goal.id, 12_000_000, null) is Outcome.Success)
        assertTrue(goals.archive(workspace, goal.id) is Outcome.Success)
        assertNotNull(db.financeDao().account(workspace.value, savings.id.value))
        assertEquals(2, db.financeDao().entries(workspace.value, id(812).value).size)
        db.close()
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val stored = (RoomSavingsGoalRepository(reopened, clock).observe(workspace).first() as Outcome.Success).value
        assertEquals(2, stored.size)
        assertEquals(12_000_000L, stored.first { it.id == goal.id }.targetMinor)
        assertNotNull(stored.first { it.id == goal.id }.archivedAt)
        assertNotNull(reopened.financeDao().account(workspace.value, savings.id.value))
        reopened.close()
    }
}
