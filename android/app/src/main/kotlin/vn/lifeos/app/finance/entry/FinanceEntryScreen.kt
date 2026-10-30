package vn.lifeos.app.finance.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.planner.PlannerDatePicker
import vn.lifeos.app.planner.PlannerDateTimePicker
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.commands.ManualEntryInput

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val displayNumbers = java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

@Composable
internal fun FinanceEntryScreen(state: FinanceUiState, workspaceId: Uuid, zone: ZoneId, now: Instant,
    actions: FinanceActions?) {
    val today = now.atZone(zone).toLocalDate()
    var accountName by rememberSaveable { mutableStateOf("") }
    var accountKind by rememberSaveable { mutableStateOf(AccountKind.CASH.name) }
    var openingAmount by rememberSaveable { mutableStateOf("0") }
    var openingDate by rememberSaveable { mutableStateOf(today.toString()) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryDirection by rememberSaveable { mutableStateOf(CategoryDirection.EXPENSE.name) }
    var kind by rememberSaveable { mutableStateOf(TransactionKind.EXPENSE.name) }
    var sourceId by rememberSaveable { mutableStateOf("") }
    var destinationId by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var occurred by rememberSaveable { mutableStateOf(now.atZone(zone).toLocalDateTime().format(dateTimeFormat)) }
    var payee by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var transactionId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var operationId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.defaultWalletId, state.accounts.size) {
        sourceId = state.defaultWalletId?.value ?: sourceId.ifBlank { state.accounts.firstOrNull()?.id?.value.orEmpty() }
    }
    LaunchedEffect(state.actionVersion) {
        if (state.confirmation == "Đã ghi giao dịch") {
            amount = ""; payee = ""; note = ""; transactionId = RandomIdGenerator.newId().value
            operationId = RandomIdGenerator.newId().value; localError = null
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.loading) { Text("Đang mở sổ tài chính…"); return@Column }
        if (state.loadFailed) {
            Text(state.error ?: "Không đọc được sổ tài chính.", color = MaterialTheme.colorScheme.error)
            Button(onClick = { actions?.retryFinance() }) { Text("Thử lại") }
            return@Column
        }
        Text("Tài khoản và số dư", style = MaterialTheme.typography.titleLarge)
        if (state.accounts.isEmpty()) Text("Chưa có tài khoản. Tạo ví tiền mặt hoặc tài khoản ngân hàng để bắt đầu.")
        state.accounts.forEach { account ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("${account.name} · ${account.kind.name}", style = MaterialTheme.typography.titleMedium)
                Text("${state.balances[account.id]?.let(displayNumbers::format) ?: "—"} ${account.currency.value}")
                if (account.kind == AccountKind.CASH) TextButton(onClick = { actions?.setDefaultWallet(account.id) }) {
                    Text(if (state.defaultWalletId == account.id) "Ví mặc định ✓" else "Đặt ví mặc định")
                }
            } }
        }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Thêm tài khoản", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(accountName, { accountName = it }, label = { Text("Tên tài khoản") },
                modifier = Modifier.fillMaxWidth().testTag("finance_account_name"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AccountKind.CASH, AccountKind.BANK, AccountKind.SAVINGS).forEach { option ->
                    FilterChip(selected = accountKind == option.name, onClick = { accountKind = option.name },
                        label = { Text(when(option) { AccountKind.CASH -> "Tiền mặt"; AccountKind.BANK -> "Ngân hàng"; else -> "Tiết kiệm" }) })
                }
            }
            OutlinedTextField(openingAmount, { openingAmount = it }, label = { Text("Số dư đầu kỳ") },
                modifier = Modifier.fillMaxWidth().testTag("finance_opening"))
            PlannerDatePicker("Ngày chốt số dư", openingDate, today) { openingDate = it }
            Button(enabled = !state.busy && accountName.isNotBlank(), onClick = {
                val date = runCatching { LocalDate.parse(openingDate) }.getOrNull()
                if (date == null) localError = "Chọn ngày chốt số dư hợp lệ."
                else actions?.createAccount(accountName, AccountKind.valueOf(accountKind), openingAmount,
                    date.atStartOfDay(zone).toInstant())
            }) { Text("Tạo tài khoản") }
        } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Thêm nhóm thu/chi", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(categoryName, { categoryName = it }, label = { Text("Tên nhóm") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CategoryDirection.EXPENSE, CategoryDirection.INCOME).forEach { option ->
                    FilterChip(selected = categoryDirection == option.name, onClick = { categoryDirection = option.name },
                        label = { Text(if (option == CategoryDirection.EXPENSE) "Chi" else "Thu") })
                }
            }
            Button(enabled = !state.busy && categoryName.isNotBlank(), onClick = {
                actions?.createCategory(categoryName, CategoryDirection.valueOf(categoryDirection))
            }) { Text("Tạo nhóm") }
        } }
        Text("Ghi giao dịch", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(TransactionKind.EXPENSE, TransactionKind.INCOME, TransactionKind.TRANSFER).forEach { option ->
                FilterChip(selected = kind == option.name, onClick = { kind = option.name; localError = null },
                    label = { Text(when(option) { TransactionKind.EXPENSE -> "Chi"; TransactionKind.INCOME -> "Thu"; else -> "Chuyển" }) },
                    modifier = Modifier.testTag("finance_kind_${option.name.lowercase()}"))
            }
        }
        Text(if (kind == TransactionKind.TRANSFER.name) "Từ tài khoản" else "Tài khoản")
        state.accounts.forEach { account ->
            SelectionRow(account.name, sourceId == account.id.value, "finance_source_${account.id.value}") { sourceId = account.id.value }
        }
        if (kind == TransactionKind.TRANSFER.name) {
            Text("Đến tài khoản cùng tiền tệ")
            val source = state.accounts.firstOrNull { it.id.value == sourceId }
            state.accounts.filter { it.id != source?.id && it.currency == source?.currency }.forEach { account ->
                SelectionRow(account.name, destinationId == account.id.value, "finance_destination_${account.id.value}") { destinationId = account.id.value }
            }
        } else {
            Text("Nhóm ${if (kind == TransactionKind.INCOME.name) "thu" else "chi"}")
            val direction = if (kind == TransactionKind.INCOME.name) CategoryDirection.INCOME else CategoryDirection.EXPENSE
            state.categories.filter { it.direction == direction && it.archivedAt == null }.forEach { category ->
                SelectionRow(category.name, categoryId == category.id.value, "finance_category_${category.id.value}") { categoryId = category.id.value }
            }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("Số tiền") },
            modifier = Modifier.fillMaxWidth().testTag("finance_amount"))
        PlannerDateTimePicker("Ngày và giờ", occurred, now.atZone(zone).toLocalDateTime()) { occurred = it }
        OutlinedTextField(payee, { payee = it }, label = { Text("Người nhận / nguồn thu (tùy chọn)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Ghi chú (tùy chọn)") }, modifier = Modifier.fillMaxWidth())
        if (localError != null) Text(localError!!, color = MaterialTheme.colorScheme.error)
        if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
        if (state.confirmation != null) Text(state.confirmation, color = MaterialTheme.colorScheme.primary)
        Button(enabled = !state.busy && state.accounts.isNotEmpty(), modifier = Modifier.testTag("finance_save"), onClick = {
            val source = state.accounts.firstOrNull { it.id.value == sourceId }
            val destination = state.accounts.firstOrNull { it.id.value == destinationId }
            val category = state.categories.firstOrNull { it.id.value == categoryId }
            val instant = runCatching { LocalDateTime.parse(occurred, dateTimeFormat).atZone(zone).toInstant() }.getOrNull()
            if (source == null || instant == null) localError = "Chọn tài khoản và ngày giờ hợp lệ."
            else {
                localError = null
                actions?.postManual(ManualEntryInput(Uuid.parse(transactionId), Uuid.parse(operationId), workspaceId,
                    TransactionKind.valueOf(kind), source, if (kind == TransactionKind.TRANSFER.name) destination else null,
                    if (kind == TransactionKind.TRANSFER.name) null else category, amount, instant, payee, note))
            }
        }) { Text(if (state.busy) "Đang lưu…" else "Lưu giao dịch") }
        Text("Giao dịch gần đây", style = MaterialTheme.typography.titleLarge)
        if (state.transactions.isEmpty()) Text("Chưa có giao dịch đã ghi.")
        state.transactions.forEach { row ->
            val amountMinor = row.entries.firstOrNull()?.deltaMinor?.let { if (it < 0) -it else it } ?: 0L
            Text("${row.transaction.kind.name} · ${displayNumbers.format(amountMinor)} ${row.entries.firstOrNull()?.currency?.value.orEmpty()} · ${row.transaction.occurredAt.atZone(zone).toLocalDate()}")
        }
    }
}

@Composable private fun SelectionRow(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(top = 12.dp))
    }
}
