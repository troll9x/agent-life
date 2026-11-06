package vn.lifeos.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceUiState
import vn.lifeos.app.finance.goals.SavingsGoalsSection
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.goals.SavingsGoal
import vn.lifeos.domain.finance.goals.SavingsGoalProgress
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput
import vn.lifeos.domain.finance.refund.ManualRefundInput

@RunWith(AndroidJUnit4::class)
class FinanceGoalsUiTest {
    @get:Rule val composeRule = createComposeRule()
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")

    @Test fun displaysLedgerProgressAndSubmitsTransfer() {
        val workspace = id(501)
        val at = Instant.parse("2026-10-30T02:00:00Z")
        val vnd = CurrencyCode.of("VND")
        val bank = Account(id(502), workspace, "Fixture bank", AccountKind.BANK, vnd, 8_000_000, at.minusSeconds(86400))
        val savings = Account(id(503), workspace, "Fixture savings", AccountKind.SAVINGS, vnd, 0, at.minusSeconds(86400))
        val goal = SavingsGoal(id(504), workspace, "Reserve", vnd, 10_000_000, null, setOf(savings.id))
        val overview = SavingsGoalProgress.calculate(listOf(goal), listOf(bank, savings),
            mapOf(bank.id to 8_000_000, savings.id to 2_000_000))
        val submitted = mutableListOf<ManualEntryInput>()
        val actions = object : FinanceActions {
            override fun createAccount(name: String, kind: AccountKind, openingText: String, openingAsOf: Instant) = Unit
            override fun createCategory(name: String, direction: CategoryDirection) = Unit
            override fun postManual(input: ManualEntryInput) { submitted += input }
            override fun replacePosted(command: ReplacePostedTransaction) = Unit
            override fun postRefund(input: ManualRefundInput) = Unit
            override fun postAdjustment(input: ManualAdjustmentInput) = Unit
            override fun setDefaultWallet(accountId: Uuid) = Unit
            override fun clearFinanceError() = Unit
            override fun retryFinance() = Unit
            override fun selectReportPeriod(monthOffset: Long) = Unit
            override fun selectReportAccount(accountId: Uuid?) = Unit
            override fun saveReportBudget(categoryId: Uuid, amountText: String, thresholdEnabled: Boolean) = Unit
            override fun retryReport() = Unit
            override fun retryInbox() = Unit
            override fun approveInbox(candidateId: Uuid, accountId: Uuid, categoryId: Uuid) = Unit
            override fun linkInbox(candidateId: Uuid, transactionId: Uuid) = Unit
            override fun ignoreInbox(candidateId: Uuid) = Unit
            override fun retrySavingsGoals() = Unit
            override fun createSavingsGoal(name: String, targetText: String, targetDate: LocalDate?, accountIds: Set<Uuid>) = Unit
            override fun changeSavingsGoalTarget(goalId: Uuid, targetText: String, targetDate: LocalDate?) = Unit
            override fun archiveSavingsGoal(goalId: Uuid) = Unit
        }
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SavingsGoalsSection(FinanceUiState(loading = false, accounts = listOf(bank, savings),
                    balances = mapOf(bank.id to 8_000_000, savings.id to 2_000_000),
                    savingsGoals = listOf(goal), goalOverview = overview), workspace, ZoneId.of("Asia/Ho_Chi_Minh"), at, actions)
            }
        }
        composeRule.onNodeWithText("2.000.000 / 10.000.000 VND · 20%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Từ Fixture bank").performScrollTo().performClick()
        composeRule.onNodeWithTag("goal_transfer_amount").performScrollTo().performTextInput("500000")
        composeRule.onNodeWithText("Chuyển vào tiết kiệm").performScrollTo().performClick()
        assertEquals(TransactionKind.TRANSFER, submitted.single().kind)
        assertEquals(bank.id, submitted.single().source.id)
        assertEquals(savings.id, submitted.single().destination?.id)
    }
}
