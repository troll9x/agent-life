package vn.lifeos.app.finance.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceUiState
import vn.lifeos.app.planner.PlannerDatePicker
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.AccountKind
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.goals.GoalProgress
import vn.lifeos.domain.common.CurrencyCode

private fun amount(minor: Long, currency: CurrencyCode): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
        minimumFractionDigits = currency.exponent
        maximumFractionDigits = currency.exponent
    }.format(BigDecimal.valueOf(minor).movePointLeft(currency.exponent))

private fun percentage(basisPoints: Int): String =
    if (basisPoints % 100 == 0) "${basisPoints / 100}%"
    else "${basisPoints / 100},${(basisPoints % 100).toString().padStart(2, '0').trimEnd('0')}%"

@Composable
internal fun SavingsGoalsSection(state: FinanceUiState, workspaceId: Uuid, zone: ZoneId, now: Instant,
    actions: FinanceActions?) {
    var name by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var targetDate by rememberSaveable { mutableStateOf("") }
    var selectedAccounts by remember { mutableStateOf(setOf<String>()) }
    val savingsAccounts = state.accounts.filter { it.kind == AccountKind.SAVINGS && it.archivedAt == null }
    val selectedCurrency = savingsAccounts.firstOrNull { it.id.value in selectedAccounts }?.currency
    Card(Modifier.fillMaxWidth().testTag("finance_goals")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mục tiêu tiết kiệm", style = MaterialTheme.typography.titleLarge)
            Text("Tiến độ lấy từ số dư account; cùng một account có thể hiện ở nhiều mục tiêu nhưng không tạo thêm tiền.")
            if (state.goalsLoading) Text("Đang tải mục tiêu…")
            if (state.goalsError != null) {
                Text(state.goalsError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { actions?.retrySavingsGoals() }) { Text("Thử lại") }
            }
            state.goalOverview?.totalAssetsByCurrency?.forEach { (currency, amount) ->
                Text("Tổng số dư tài khoản (không cộng lặp mục tiêu): ${amount(amount, currency)} ${currency.value}")
            }
            if (!state.goalsLoading && state.goalsError == null && state.savingsGoals.isEmpty())
                Text("Chưa có mục tiêu. Tạo tài khoản tiết kiệm trước khi đặt mục tiêu.")
            state.goalOverview?.goals?.forEach { progress -> GoalCard(progress, state, workspaceId, zone, now, actions) }
            if (savingsAccounts.isNotEmpty()) {
                Text("Tạo mục tiêu", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Tên mục tiêu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(target, { target = it }, label = { Text("Số tiền mục tiêu") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_target"))
                PlannerDatePicker("Ngày mong muốn", targetDate, now.atZone(zone).toLocalDate(), optional = true) {
                    targetDate = it
                }
                Text("Liên kết tài khoản tiết kiệm cùng tiền tệ")
                savingsAccounts.filter { selectedCurrency == null || it.currency == selectedCurrency || it.id.value in selectedAccounts }
                    .forEach { account ->
                        FilterChip(account.id.value in selectedAccounts, onClick = {
                            selectedAccounts = if (account.id.value in selectedAccounts) selectedAccounts - account.id.value
                                else selectedAccounts + account.id.value
                        }, label = { Text("${account.name} · ${account.currency.value}") })
                    }
                Button(enabled = !state.goalsBusy && name.isNotBlank() && selectedAccounts.isNotEmpty(), onClick = {
                    actions?.createSavingsGoal(name, target, targetDate.takeIf(String::isNotBlank)?.let(LocalDate::parse),
                        selectedAccounts.map(Uuid::parse).toSet())
                }) { Text("Lưu mục tiêu") }
            }
        }
    }
}

@Composable
private fun GoalCard(progress: GoalProgress, state: FinanceUiState, workspaceId: Uuid, zone: ZoneId,
    now: Instant, actions: FinanceActions?) {
    val goal = progress.goal
    var editTarget by rememberSaveable(goal.id.value) { mutableStateOf(
        BigDecimal.valueOf(goal.targetMinor).movePointLeft(goal.currency.exponent).stripTrailingZeros().toPlainString()) }
    var editDate by rememberSaveable(goal.id.value) { mutableStateOf(goal.targetDate?.toString().orEmpty()) }
    var transferAmount by rememberSaveable(goal.id.value) { mutableStateOf("") }
    var sourceId by rememberSaveable(goal.id.value) { mutableStateOf("") }
    var destinationId by rememberSaveable(goal.id.value) { mutableStateOf(goal.linkedAccountIds.firstOrNull()?.value.orEmpty()) }
    var transactionId by rememberSaveable(goal.id.value) { mutableStateOf(RandomIdGenerator.newId().value) }
    var operationId by rememberSaveable(goal.id.value) { mutableStateOf(RandomIdGenerator.newId().value) }
    LaunchedEffect(state.actionVersion) {
        if (state.confirmation == "Đã ghi giao dịch") {
            transferAmount = ""; transactionId = RandomIdGenerator.newId().value
            operationId = RandomIdGenerator.newId().value
        }
    }
    Column(Modifier.fillMaxWidth().testTag("goal_${goal.id.value}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${goal.name}${if (goal.archivedAt != null) " · Đã lưu trữ" else ""}", style = MaterialTheme.typography.titleMedium)
        Text("${amount(progress.currentMinor, goal.currency)} / ${amount(goal.targetMinor, goal.currency)} ${goal.currency.value} · " +
            percentage(progress.progressBasisPoints))
        goal.targetDate?.let { Text("Ngày mong muốn: $it") }
        Text("Liên kết: " + state.accounts.filter { it.id in goal.linkedAccountIds }.joinToString { it.name })
        if (goal.archivedAt != null) return@Column
        OutlinedTextField(editTarget, { editTarget = it }, label = { Text("Đổi số tiền mục tiêu") }, modifier = Modifier.fillMaxWidth())
        PlannerDatePicker("Đổi ngày mong muốn", editDate, now.atZone(zone).toLocalDate(), optional = true) { editDate = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !state.goalsBusy, onClick = {
                actions?.changeSavingsGoalTarget(goal.id, editTarget, editDate.takeIf(String::isNotBlank)?.let(LocalDate::parse))
            }) { Text("Cập nhật") }
            TextButton(enabled = !state.goalsBusy, onClick = { actions?.archiveSavingsGoal(goal.id) }) { Text("Lưu trữ") }
        }
        Text("Chuyển tiền vào tài khoản tiết kiệm (không tính là chi phí)")
        state.accounts.filter { it.currency == goal.currency && it.archivedAt == null &&
            it.kind in setOf(AccountKind.BANK, AccountKind.CASH) }.forEach { account ->
            FilterChip(sourceId == account.id.value, onClick = { sourceId = account.id.value },
                label = { Text("Từ ${account.name}") })
        }
        state.accounts.filter { it.id in goal.linkedAccountIds && it.archivedAt == null }.forEach { account ->
            FilterChip(destinationId == account.id.value, onClick = { destinationId = account.id.value },
                label = { Text("Đến ${account.name}") })
        }
        OutlinedTextField(transferAmount, { transferAmount = it }, label = { Text("Số tiền chuyển") },
            modifier = Modifier.fillMaxWidth().testTag("goal_transfer_amount"))
        Button(enabled = !state.busy && sourceId.isNotBlank() && destinationId.isNotBlank() && transferAmount.isNotBlank(),
            onClick = {
                val source = state.accounts.firstOrNull { it.id.value == sourceId }
                val destination = state.accounts.firstOrNull { it.id.value == destinationId }
                if (source != null && destination != null) actions?.postManual(ManualEntryInput(
                    Uuid.parse(transactionId), Uuid.parse(operationId), workspaceId, TransactionKind.TRANSFER,
                    source, destination, null, transferAmount, now, null, "Chuyển vào tài khoản tiết kiệm"))
            }) { Text("Chuyển vào tiết kiệm") }
    }
}
