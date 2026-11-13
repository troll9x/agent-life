package vn.lifeos.app.banknotifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BankNotificationUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun deniedPermissionExplainsAccessAndOnlyExplicitSelectionEnablesPackage() {
        var selected: Set<String> = emptySet()
        var opened = false
        compose.setContent {
            var state by remember { mutableStateOf(BankNotificationState(loading = false,
                apps = listOf(NotificationAppChoice("fixture.bank", "Fixture Bank")))) }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BankNotificationContent(state, { selected = it; state = state.copy(selected = it) }, { opened = true }, {})
            }
        }
        compose.onNodeWithTag("bank_access_state").assertTextContains("Chưa cấp", substring = true)
        compose.onNodeWithText("Mở quyền đọc thông báo").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened); assertTrue(selected.isEmpty()) }
        compose.onNodeWithTag("bank_app_search").performScrollTo().performTextInput("Fixture")
        compose.onNodeWithTag("bank_app_fixture.bank").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(setOf("fixture.bank"), selected) }
        compose.onNodeWithText("Ngừng thu tất cả ứng dụng").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(selected.isEmpty()) }
    }
}
