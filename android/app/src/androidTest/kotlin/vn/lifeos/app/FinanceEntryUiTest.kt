package vn.lifeos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceEntryScreen
import vn.lifeos.app.finance.entry.FinanceUiState
import vn.lifeos.app.finance.history.FinanceHistorySection
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.finance.Category
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.FinancialTransaction
import vn.lifeos.domain.finance.LedgerEntry
import vn.lifeos.domain.finance.PostedFinanceRow
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier

@RunWith(AndroidJUnit4::class)
class FinanceEntryUiTest {
    @get:Rule val composeRule = createComposeRule()
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    private val workspace = id(501)
    private val bank = Account(id(502), workspace, "Fixture bank", AccountKind.BANK,
        CurrencyCode.of("VND"), 10_000_000, Instant.parse("2026-10-01T00:00:00Z"))
    private val cash = Account(id(503), workspace, "Fixture cash", AccountKind.CASH,
        CurrencyCode.of("VND"), 0, Instant.parse("2026-10-01T00:00:00Z"))

    @Test fun transferFormKeepsOperationIdAcrossRepeatedSaveAndShowsBalances() {
        val submitted = mutableListOf<ManualEntryInput>()
        val actions = object : FinanceActions {
            override fun createAccount(name: String, kind: AccountKind, openingText: String, openingAsOf: Instant) = Unit
            override fun createCategory(name: String, direction: CategoryDirection) = Unit
            override fun postManual(input: ManualEntryInput) { submitted += input }
            override fun replacePosted(command: ReplacePostedTransaction) = Unit
            override fun setDefaultWallet(accountId: Uuid) = Unit
            override fun clearFinanceError() = Unit
            override fun retryFinance() = Unit
        }
        composeRule.setContent {
            FinanceEntryScreen(FinanceUiState(loading = false, accounts = listOf(bank, cash),
                balances = mapOf(bank.id to 10_000_000, cash.id to 0), defaultWalletId = bank.id),
                workspace, ZoneId.of("Asia/Ho_Chi_Minh"), Instant.parse("2026-10-30T02:00:00Z"), actions)
        }
        composeRule.onNodeWithText("10.000.000 VND").assertIsDisplayed()
        composeRule.onNodeWithTag("finance_kind_transfer").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_destination_${cash.id.value}").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_amount").performScrollTo().performTextInput("1000000")
        composeRule.onNodeWithTag("finance_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_save").performClick()
        assertEquals(2, submitted.size)
        assertEquals(TransactionKind.TRANSFER, submitted[0].kind)
        assertEquals(bank.id, submitted[0].source.id)
        assertEquals(cash.id, submitted[0].destination?.id)
        assertEquals("1000000", submitted[0].amountText)
        assertEquals(submitted[0].operationId, submitted[1].operationId)
        assertEquals(submitted[0].transactionId, submitted[1].transactionId)
    }

    @Test fun historyEditShowsBalanceEffectAndKeepsOperationIdsForRetry() {
        val submitted = mutableListOf<ReplacePostedTransaction>()
        val occurred = Instant.parse("2026-10-29T12:00:00Z")
        val category = Category(id(504), workspace, "Fixture expense", CategoryDirection.EXPENSE)
        val original = FinancialTransaction(id(505), workspace, id(506), TransactionKind.EXPENSE,
            occurred, occurred, category.id, null, null, null, null, null, null)
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FinanceHistorySection(workspace, listOf(PostedFinanceRow(original,
                    listOf(LedgerEntry(id(507), original.id, bank.id, -500_000, bank.currency)))),
                    listOf(bank, cash), listOf(category), ZoneId.of("Asia/Ho_Chi_Minh"),
                    Instant.parse("2026-10-30T02:00:00Z"), false, null) { submitted += it }
            }
        }
        composeRule.onNodeWithTag("finance_edit_${original.id.value}").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_edit_amount").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("finance_edit_amount").performTextInput("600000")
        composeRule.onNodeWithText("Dự kiến thay đổi số dư Fixture bank: -100.000 VND").assertIsDisplayed()
        composeRule.onNodeWithTag("finance_replace").performScrollTo().performClick()
        composeRule.onNodeWithTag("finance_replace").performClick()
        assertEquals(2, submitted.size)
        assertEquals(original.id, submitted.first().originalId)
        assertEquals(submitted[0].reversalOperationId, submitted[1].reversalOperationId)
        assertEquals(submitted[0].replacement.operationId, submitted[1].replacement.operationId)
    }
}
