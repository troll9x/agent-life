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
import vn.lifeos.app.finance.reconciliation.*
import vn.lifeos.data.finance.reconciliation.RoomReconciliationRepository
import vn.lifeos.data.finance.imports.RoomFinanceInboxRepository
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.imports.InboundFinanceEvidence
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class FinanceReconciliationUiTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun snapshotDoesNotPostAndAdjustmentRequiresSeparateReasonAndConfirmation() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val fixture = RandomIdGenerator.newId().value
        val provider = EncryptedDatabaseProvider(app, "reconciliation-$fixture.db", "reconciliation_$fixture", "reconciliation-$fixture.wrap")
        val db = (provider.open() as DatabaseAccess.Ready).database
        val zone = ZoneId.systemDefault()
        val clock = app.appContainer.clock
        val now = clock.instant()
        val profileStore = app.appContainer.profileStore(db)
        profileStore.complete(ProfileInput("Fixture reconciliation", zone.id, "VND", 1))
        val workspace = (profileStore.observe().first() as Outcome.Success).value!!.profile.workspaceId
        val ledger = app.appContainer.financeRepository(db)
        val currency = CurrencyCode.of("VND")
        val bank = Account(RandomIdGenerator.newId(), workspace, "Fixture bank reconcile", AccountKind.BANK,
            currency, 10_000_000, now.minusSeconds(86400))
        val category = Category(RandomIdGenerator.newId(), workspace, "Fixture expense reconcile", CategoryDirection.EXPENSE)
        ledger.createAccount(bank); ledger.createCategory(category)
        ledger.post(PostFinanceCommand(RandomIdGenerator.newId(), workspace, RandomIdGenerator.newId(), TransactionKind.EXPENSE,
            bank.id, Money(600_000, currency), now.minusSeconds(60), categoryId = category.id))
        val inbox = RoomFinanceInboxRepository(db, clock, RandomIdGenerator)
        repeat(3) { n -> inbox.ingest(InboundFinanceEvidence(RandomIdGenerator.newId(), RandomIdGenerator.newId(), workspace,
            null, FinanceSourceKind.NOTIFICATION, "fixture-$n", null, null, null, now, 1, 50_000, currency,
            CategoryDirection.EXPENSE, now, bank.id, category.id, "", 9_000)) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val feature = app.appContainer.financeFeature(db, workspace, currency, zone, 1, scope)
        composeRule.setContent {
            val state by feature.state.collectAsState()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                state.report?.let { FinanceQualityNotice(it.quality) }
                ReconciliationSection(state.reconciliation, state.accounts, workspace, zone, now,
                    feature.reconciliationActions, state.busy, state.error, feature::postAdjustment)
            }
        }
        composeRule.waitUntil(30_000) { composeRule.onAllNodesWithText("Kiểm: ${bank.name} · VND").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Kiểm: ${bank.name} · VND").performScrollTo().performClick()
        composeRule.onNodeWithTag("reconciliation_observed").performScrollTo().performTextInput("9300000")
        composeRule.onNodeWithText("Lưu snapshot đối soát").performScrollTo().performClick()
        composeRule.waitUntil(30_000) { feature.state.value.reconciliation.views.size == 1 }
        composeRule.onNodeWithText("Chênh lệch lúc lưu: -100.000 VND").performScrollTo().assertIsDisplayed()
        assertEquals(9_400_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        composeRule.onNodeWithText("Còn 3 khoản chờ nhập cần kiểm tra; chưa tính vào thu/chi đã ghi.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Xem giao dịch và khoản chờ nhập quanh mốc").performScrollTo().performClick()
        composeRule.waitUntil(30_000) { feature.state.value.reconciliation.review != null }
        composeRule.onNodeWithText("Mở điều chỉnh có lý do").performScrollTo().performClick()
        composeRule.onNodeWithText("Xem lại điều chỉnh riêng").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("reconciliation_adjustment_reason").performScrollTo().performTextInput("Fixture correction")
        composeRule.onNodeWithText("Xem lại điều chỉnh riêng").performScrollTo().performClick()
        composeRule.onNodeWithText("Xác nhận điều chỉnh số dư").assertIsDisplayed()
        assertEquals(9_400_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        composeRule.onNodeWithText("Xác nhận ghi điều chỉnh").performClick()
        composeRule.waitUntil(30_000) { feature.state.value.reconciliation.views.single().currentDifferenceMinor == 0L }
        composeRule.onNodeWithText("Xác nhận đã đối soát").performScrollTo().performClick()
        composeRule.waitUntil(30_000) { feature.state.value.reconciliation.views.single().resolved }
        assertEquals(9_300_000L, (ledger.balance(workspace, bank.id, clock.instant()) as Outcome.Success).value.amountMinor)
        assertEquals(600_000L, (ledger.report(FinanceReportQuery(workspace, currency, now.minusSeconds(86400), clock.instant().plusSeconds(1))) as Outcome.Success).value.netExpenseMinor)
        scope.cancel(); provider.close()
        val reopened = (provider.open() as DatabaseAccess.Ready).database
        val stored = (RoomReconciliationRepository(reopened, clock, RandomIdGenerator).observe(workspace).first() as Outcome.Success).value.single()
        assertTrue(stored.resolved); assertEquals(-100_000L, stored.snapshot.differenceMinor)
        provider.close()
    }
}
