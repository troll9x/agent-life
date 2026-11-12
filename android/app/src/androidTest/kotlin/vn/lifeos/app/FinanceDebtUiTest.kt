package vn.lifeos.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.finance.debt.*
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.debt.*

@RunWith(AndroidJUnit4::class)
class FinanceDebtUiTest {
    @get:Rule val composeRule = createComposeRule()
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")
    @Test fun reviewsPrincipalInterestAndFeeBeforePosting() {
        val at = Instant.parse("2026-11-12T10:00:00Z")
        val vnd = CurrencyCode.of("VND")
        val workspace = id(1201)
        val debt = DebtAgreement(id(1202), workspace, DebtRole.BORROWER, id(1203), "Fixture partner", vnd, 5_000_000,
            LocalDate.parse("2026-12-12"))
        val bank = Account(id(1204), workspace, "Fixture bank", AccountKind.BANK, vnd, 10_000_000, at.minusSeconds(86400))
        val interest = Category(id(1205), workspace, "Fixture interest", CategoryDirection.EXPENSE)
        val fee = Category(id(1206), workspace, "Fixture fee", CategoryDirection.EXPENSE)
        val submitted = mutableListOf<DebtEntryInput>()
        val actions = object : DebtActions {
            override fun retry() = Unit
            override fun create(id: Uuid, counterpartyId: Uuid, label: String, role: DebtRole, opening: String, dueDate: LocalDate?) = Unit
            override fun post(input: DebtEntryInput) { submitted += input }
            override fun reverse(id: Uuid, operationId: Uuid, debtId: Uuid, movementId: Uuid, reason: String) = Unit
        }
        composeRule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DebtSection(DebtUiState(loading = false, positions = listOf(DebtPosition(debt, emptyList()))),
                    listOf(bank), listOf(interest, fee), ZoneId.of("Asia/Ho_Chi_Minh"), at, actions)
            }
        }
        composeRule.onNodeWithText("Gốc còn lại: 5.000.000 VND").assertIsDisplayed()
        composeRule.onNodeWithText("Ghi biến động / xem lịch sử").performScrollTo().performClick()
        composeRule.onNodeWithText("Fixture bank").performScrollTo().performClick()
        composeRule.onNodeWithTag("debt_principal").performScrollTo().performTextInput("1000000")
        composeRule.onNodeWithTag("debt_interest").performScrollTo().performTextInput("100000")
        composeRule.onNodeWithTag("debt_fee").performScrollTo().performTextInput("20000")
        composeRule.onNodeWithText("Lãi: Fixture interest").performScrollTo().performClick()
        composeRule.onNodeWithText("Phí: Fixture fee").performScrollTo().performClick()
        composeRule.onNodeWithText("Xem lại thành phần").performScrollTo().performClick()
        composeRule.onNodeWithText("Gốc: 1000000 VND (không tính thu/chi)").assertIsDisplayed()
        composeRule.onNodeWithText("Lãi: 100000 VND").assertIsDisplayed()
        composeRule.onNodeWithText("Phí: 20000 VND").assertIsDisplayed()
        assertEquals(0, submitted.size)
        composeRule.onNodeWithText("Xác nhận ghi sổ").performClick()
        assertEquals("1000000", submitted.single().principal)
        assertEquals(DebtAction.REPAY, submitted.single().action)
        assertEquals(interest.id, submitted.single().interestCategoryId)
        assertEquals(fee.id, submitted.single().feeCategoryId)
        composeRule.onNodeWithText("Xem lại thành phần").performScrollTo().performClick()
        composeRule.onNodeWithText("Xác nhận ghi sổ").performClick()
        assertEquals(submitted[0].operationId, submitted[1].operationId)
    }
}
