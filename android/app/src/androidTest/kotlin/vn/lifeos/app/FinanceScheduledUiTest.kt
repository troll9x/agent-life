package vn.lifeos.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.finance.scheduled.*
import vn.lifeos.data.finance.scheduled.RoomScheduledFinanceRepository
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.scheduled.*
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class FinanceScheduledUiTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun plannedRentNeedsConfirmationAndPersistsInEncryptedDatabase() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val fixture = RandomIdGenerator.newId().value
        val provider = EncryptedDatabaseProvider(app, "scheduled-$fixture.db", "scheduled_$fixture", "scheduled-$fixture.wrap")
        val db = (provider.open() as DatabaseAccess.Ready).database
        val zone = ZoneId.systemDefault()
        val clock = app.appContainer.clock
        val now = clock.instant()
        val profileStore = app.appContainer.profileStore(db)
        assertTrue(profileStore.complete(ProfileInput("Fixture scheduled", zone.id, "VND", 1)) is Outcome.Success)
        val workspace = (profileStore.observe().first() as Outcome.Success).value!!.profile.workspaceId
        val ledger = app.appContainer.financeRepository(db)
        val currency = CurrencyCode.of("VND")
        val bank = Account(RandomIdGenerator.newId(), workspace, "Fixture bank scheduled", AccountKind.BANK, currency,
            10_000_000, now.minusSeconds(86400))
        val category = Category(RandomIdGenerator.newId(), workspace, "Fixture rent scheduled", CategoryDirection.EXPENSE)
        ledger.createAccount(bank); ledger.createCategory(category)
        val repo = RoomScheduledFinanceRepository(db, clock, RandomIdGenerator)
        val date = now.atZone(zone).toLocalDate()
        assertTrue(repo.save(ScheduledTransaction(RandomIdGenerator.newId(), workspace, "Fixture rent", FinanceRecurrence.MONTHLY,
            date, date, 1_000_000, currency, TransactionKind.EXPENSE, bank.id, category.id), zone) is Outcome.Success)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val feature = app.appContainer.financeFeature(db, workspace, currency, zone, 1, scope)
        composeRule.setContent {
            val state by feature.state.collectAsState()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ScheduledSection(state.scheduled, state.accounts, state.categories, zone, now, feature.scheduledActions)
            }
        }
        composeRule.waitUntil(30_000) { composeRule.onAllNodesWithText("Dự kiến — chưa ghi sổ").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(10_000_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        composeRule.onNodeWithText("Xác nhận khoản dự kiến").performScrollTo().performClick()
        composeRule.onNodeWithText("Xem lại khoản dự kiến").assertIsDisplayed()
        assertEquals(10_000_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        composeRule.onNodeWithText("Xác nhận thực hiện").performClick()
        composeRule.waitUntil(30_000) { composeRule.onAllNodesWithText("Đã ghi/ghép vào sổ").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(9_000_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        assertEquals(1_000_000L, (ledger.report(FinanceReportQuery(workspace, currency, now.minusSeconds(86400),
            clock.instant().plusSeconds(1))) as Outcome.Success).value.netExpenseMinor)
        scope.cancel(); provider.close()
        val reopened = (provider.open() as DatabaseAccess.Ready).database
        val persisted = RoomScheduledFinanceRepository(reopened, clock, RandomIdGenerator)
        assertEquals(0, (persisted.generateDue(workspace, zone) as Outcome.Success).value)
        val snapshot = (persisted.observe(workspace, zone).first() as Outcome.Success).value
        assertEquals(1, snapshot.items.size); assertEquals(PlannedState.POSTED, snapshot.items.single().plan.state)
        provider.close()
    }
}
