package vn.lifeos.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
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
import vn.lifeos.app.finance.entry.DefaultWalletPreference
import vn.lifeos.app.finance.entry.FinanceEntryScreen
import vn.lifeos.app.finance.entry.FinanceFeature
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.security.EncryptedDatabaseProvider
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.finance.Category
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.foundation.Outcome

@RunWith(AndroidJUnit4::class)
class FinanceEntryEndToEndTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun cashIncomePostedFromComposeSurvivesEncryptedDatabaseReopen() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val fixtureId = RandomIdGenerator.newId().value
        val provider = EncryptedDatabaseProvider(app, "finance-entry-$fixtureId.db",
            "finance_entry_$fixtureId", "finance-entry-$fixtureId.wrap")
        val database = (provider.open() as DatabaseAccess.Ready).database
        val zone = ZoneId.systemDefault()
        val profileStore = app.appContainer.profileStore(database)
        assertTrue(profileStore.complete(ProfileInput("Fixture finance", zone.id, "VND", 1)) is Outcome.Success)
        val workspaceId = (profileStore.observe().first() as Outcome.Success).value!!.profile.workspaceId
        val repository = app.appContainer.financeRepository(database)
        val cash = Account(RandomIdGenerator.newId(), workspaceId, "Fixture cash UI", AccountKind.CASH,
            CurrencyCode.of("VND"), 0, LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toInstant())
        val category = Category(RandomIdGenerator.newId(), workspaceId, "Fixture income UI", CategoryDirection.INCOME)
        assertTrue(repository.createAccount(cash) is Outcome.Success)
        assertTrue(repository.createCategory(category) is Outcome.Success)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val feature = FinanceFeature(repository, workspaceId, cash.currency, RandomIdGenerator,
            app.appContainer.clock, DefaultWalletPreference(app), scope)
        val now = app.appContainer.clock.instant()
        composeRule.setContent {
            val state by feature.state.collectAsState()
            FinanceEntryScreen(state, workspaceId, zone, now, feature)
        }
        composeRule.waitUntil(30_000) {
            composeRule.onAllNodesWithText("Fixture cash UI", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("finance_kind_income").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_source_${cash.id.value}").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_category_${category.id.value}").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_amount").performScrollTo().performTextInput("500000")
        composeRule.onNodeWithTag("finance_save").performScrollTo().performClick()
        composeRule.waitUntil(30_000) {
            composeRule.onAllNodesWithText("Đã ghi giao dịch").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(500_000L, (repository.balance(workspaceId, cash.id, now.plusSeconds(60)) as Outcome.Success).value.amountMinor)
        scope.cancel()
        provider.close()
        val reopened = (provider.open() as DatabaseAccess.Ready).database
        val persisted = app.appContainer.financeRepository(reopened)
        assertEquals(500_000L, (persisted.balance(workspaceId, cash.id, now.plusSeconds(60)) as Outcome.Success).value.amountMinor)
        assertEquals(1, (persisted.observeRecentTransactions(workspaceId).first() as Outcome.Success).value.count {
            row -> row.entries.any { it.accountId == cash.id }
        })
        provider.close()
    }
}
