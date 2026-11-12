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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.finance.debt.DebtSection
import vn.lifeos.data.finance.debt.RoomDebtRepository
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.debt.*
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class FinanceDebtEndToEndTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun repaymentFromComposeUpdatesEncryptedLedgerAndSurvivesReopen() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val fixture = RandomIdGenerator.newId().value
        val provider = EncryptedDatabaseProvider(app, "debt-$fixture.db", "debt_$fixture", "debt-$fixture.wrap")
        val db = (provider.open() as DatabaseAccess.Ready).database
        val zone = ZoneId.systemDefault()
        val profileStore = app.appContainer.profileStore(db)
        assertTrue(profileStore.complete(ProfileInput("Fixture debt", zone.id, "VND", 1)) is Outcome.Success)
        val workspace = (profileStore.observe().first() as Outcome.Success).value!!.profile.workspaceId
        val clock = app.appContainer.clock
        val now = clock.instant()
        val currency = CurrencyCode.of("VND")
        val ledger = app.appContainer.financeRepository(db)
        val bank = Account(RandomIdGenerator.newId(), workspace, "Fixture bank debt", AccountKind.BANK, currency,
            10_000_000, now.minusSeconds(86400))
        val interest = Category(RandomIdGenerator.newId(), workspace, "Fixture interest debt", CategoryDirection.EXPENSE)
        ledger.createAccount(bank); ledger.createCategory(interest)
        val debts = RoomDebtRepository(db, clock, RandomIdGenerator)
        assertTrue(debts.create(DebtAgreement(RandomIdGenerator.newId(), workspace, DebtRole.BORROWER,
            RandomIdGenerator.newId(), "Fixture partner debt", currency, 5_000_000)) is Outcome.Success)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val feature = app.appContainer.financeFeature(db, workspace, currency, zone, 1, scope)
        composeRule.setContent {
            val state by feature.state.collectAsState()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DebtSection(state.debt, state.accounts, state.categories, zone, now, feature.debtActions)
            }
        }
        composeRule.waitUntil(30_000) {
            composeRule.onAllNodesWithText("Gốc còn lại: 5.000.000 VND").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ghi biến động / xem lịch sử").performScrollTo().performClick()
        composeRule.onNodeWithText(bank.name).performScrollTo().performClick()
        composeRule.onNodeWithTag("debt_principal").performScrollTo().performTextInput("1000000")
        composeRule.onNodeWithTag("debt_interest").performScrollTo().performTextInput("100000")
        composeRule.onNodeWithText("Lãi: ${interest.name}").performScrollTo().performClick()
        composeRule.onNodeWithText("Xem lại thành phần").performScrollTo().performClick()
        composeRule.onNodeWithText("Xác nhận ghi sổ").performClick()
        composeRule.waitUntil(30_000) {
            composeRule.onAllNodesWithText("Gốc còn lại: 4.000.000 VND").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(8_900_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        assertEquals(100_000L, (ledger.report(FinanceReportQuery(workspace, currency,
            now.minusSeconds(86400), clock.instant().plusSeconds(1))) as Outcome.Success).value.netExpenseMinor)
        scope.cancel(); provider.close()
        val reopened = (provider.open() as DatabaseAccess.Ready).database
        val persisted = (RoomDebtRepository(reopened, clock, RandomIdGenerator).observe(workspace).first() as Outcome.Success).value.single()
        assertEquals(4_000_000L, persisted.outstandingMinor)
        assertEquals(100_000L, persisted.interestMinor)
        assertEquals(1, persisted.movements.size)
        provider.close()
    }
}
