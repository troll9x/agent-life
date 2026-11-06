package vn.lifeos.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceUiState
import vn.lifeos.app.finance.inbox.FinanceInboxSection
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.imports.*
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput
import vn.lifeos.domain.finance.refund.ManualRefundInput
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction

@RunWith(AndroidJUnit4::class)
class FinanceInboxUiTest {
    @get:Rule val composeRule = createComposeRule()
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")

    @Test fun candidateShowsSuggestionAndSendsExplicitDecisions() {
        val workspace = id(1)
        val bank = Account(id(2), workspace, "Fixture bank", AccountKind.BANK, CurrencyCode.of("VND"),
            0, Instant.parse("2026-10-01T00:00:00Z"))
        val category = Category(id(3), workspace, "Fixture salary", CategoryDirection.INCOME)
        val candidate = ImportCandidate(id(4), workspace, id(5), 2, 200_000, bank.currency,
            CategoryDirection.INCOME, Instant.parse("2026-10-20T10:00:00Z"), bank.id,
            category.id, "", 9000, CandidateState.PENDING, null)
        val evidence = SourceEvidence(id(5), workspace, null, FinanceSourceKind.MAIL.name,
            "fixture-event", null, "SALARY-123", "***0000", Instant.parse("2026-10-20T10:01:00Z"), null)
        val target = id(6)
        var approved: Triple<Uuid, Uuid, Uuid>? = null
        var linked: Pair<Uuid, Uuid>? = null
        var ignored: Uuid? = null
        val actions = object : FinanceActions {
            override fun createAccount(name: String, kind: AccountKind, openingText: String, openingAsOf: Instant) = Unit
            override fun createCategory(name: String, direction: CategoryDirection) = Unit
            override fun postManual(input: ManualEntryInput) = Unit
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
            override fun approveInbox(candidateId: Uuid, accountId: Uuid, categoryId: Uuid) {
                approved = Triple(candidateId, accountId, categoryId)
            }
            override fun linkInbox(candidateId: Uuid, transactionId: Uuid) { linked = candidateId to transactionId }
            override fun ignoreInbox(candidateId: Uuid) { ignored = candidateId }
            override fun retrySavingsGoals() = Unit
            override fun createSavingsGoal(name: String, targetText: String, targetDate: java.time.LocalDate?, accountIds: Set<Uuid>) = Unit
            override fun changeSavingsGoalTarget(goalId: Uuid, targetText: String, targetDate: java.time.LocalDate?) = Unit
            override fun archiveSavingsGoal(goalId: Uuid) = Unit
        }
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FinanceInboxSection(FinanceUiState(loading = false, accounts = listOf(bank), categories = listOf(category),
                    inboxItems = listOf(InboxItem(candidate, evidence,
                        listOf(MatchSuggestion(target, MatchReason.VERIFIED_REFERENCE,
                            bank.id, 200_000, Instant.parse("2026-10-20T10:00:00Z")))))),
                    ZoneId.of("Asia/Ho_Chi_Minh"), actions)
            }
        }
        composeRule.onNodeWithText("Khoản chờ duyệt chưa thay đổi số dư.").assertIsDisplayed()
        composeRule.onNodeWithText("Ghép vào giao dịch này").performScrollTo().performClick()
        composeRule.onNodeWithText("Duyệt và ghi sổ").performScrollTo().performClick()
        composeRule.onNodeWithText("Bỏ qua").performScrollTo().performClick()
        assertEquals(candidate.id to target, linked)
        assertEquals(Triple(candidate.id, bank.id, category.id), approved)
        assertEquals(candidate.id, ignored)
    }
}
