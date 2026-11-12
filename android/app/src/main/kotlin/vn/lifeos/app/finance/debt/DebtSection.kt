package vn.lifeos.app.finance.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.planner.PlannerDatePicker
import vn.lifeos.app.planner.PlannerDateTimePicker
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.Category
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.debt.*

private fun money(amount: Long, currency: CurrencyCode): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
        minimumFractionDigits = currency.exponent; maximumFractionDigits = currency.exponent
    }.format(BigDecimal.valueOf(amount).movePointLeft(currency.exponent)) + " " + currency.value

@Composable
internal fun DebtSection(state: DebtUiState, accounts: List<Account>, categories: List<Category>,
    zone: ZoneId, now: Instant, actions: DebtActions?) {
    if (actions == null) return
    var label by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(DebtRole.BORROWER.name) }
    var opening by rememberSaveable { mutableStateOf("0") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var newId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var partyId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("finance_debt")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cho vay và đi vay", style = MaterialTheme.typography.titleLarge)
            Text("Gốc vay thay đổi số dư và dư nợ. Lãi, phí được ghi thu/chi riêng. Dữ liệu lưu trên thiết bị và dùng được ngoại tuyến.")
            if (state.loading) Text("Đang tải khoản vay…")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = actions::retry) { Text("Thử lại khoản vay") } }
            state.confirmation?.let { Text(it) }
            if (!state.loading && state.error == null && state.positions.isEmpty()) Text("Chưa có khoản vay.")
            state.positions.forEach { DebtCard(it, state, accounts, categories, zone, now, actions) }
            TextButton(onClick = { showCreate = !showCreate }) { Text("Tạo khoản vay") }
            if (showCreate) {
                FilterChip(role == DebtRole.BORROWER.name, { role = DebtRole.BORROWER.name }, label = { Text("Tôi đi vay") })
                FilterChip(role == DebtRole.LENDER.name, { role = DebtRole.LENDER.name }, label = { Text("Tôi cho vay") })
                OutlinedTextField(label, { label = it }, label = { Text("Tên gợi nhớ đối tác") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(opening, { opening = it }, label = { Text("Gốc còn lại khi bắt đầu theo dõi") }, modifier = Modifier.fillMaxWidth())
                Text("Gốc ban đầu chỉ ghi dư nợ đã có; không tạo dòng tiền. Để 0 nếu sẽ ghi giải ngân mới.")
                PlannerDatePicker("Hạn trả", dueDate, now.atZone(zone).toLocalDate(), optional = true) { dueDate = it }
                Button(enabled = !state.busy && label.isNotBlank(), onClick = {
                    actions.create(Uuid.parse(newId), Uuid.parse(partyId), label, DebtRole.valueOf(role), opening,
                        dueDate.takeIf(String::isNotBlank)?.let(LocalDate::parse))
                }) { Text("Lưu khoản vay") }
                TextButton(onClick = {
                    label = ""; opening = "0"; dueDate = ""
                    newId = RandomIdGenerator.newId().value; partyId = RandomIdGenerator.newId().value
                }) { Text("Nhập khoản vay khác") }
            }
        }
    }
}

@Composable
private fun DebtCard(position: DebtPosition, state: DebtUiState, accounts: List<Account>, categories: List<Category>,
    zone: ZoneId, now: Instant, actions: DebtActions) {
    val debt = position.agreement
    var expanded by rememberSaveable(debt.id.value) { mutableStateOf(false) }
    var action by rememberSaveable(debt.id.value) { mutableStateOf(DebtAction.REPAY.name) }
    var accountId by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var principal by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var interest by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var fee by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var interestCategory by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var feeCategory by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var at by rememberSaveable(debt.id.value) { mutableStateOf("") }
    var id by rememberSaveable(debt.id.value) { mutableStateOf(RandomIdGenerator.newId().value) }
    var operationId by rememberSaveable(debt.id.value) { mutableStateOf(RandomIdGenerator.newId().value) }
    var review by remember { mutableStateOf<DebtEntryInput?>(null) }
    val saved = state.savedOperationId?.value == operationId
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${debt.counterpartyLabel} · ${if (debt.role == DebtRole.BORROWER) "Tôi đi vay" else "Tôi cho vay"}",
            style = MaterialTheme.typography.titleMedium)
        Text("Gốc còn lại: ${money(position.outstandingMinor, debt.currency)}", Modifier.testTag("debt_outstanding"))
        Text("Lãi đã ghi: ${money(position.interestMinor, debt.currency)} · Phí đã ghi: ${money(position.feeMinor, debt.currency)}")
        Text("Hạn trả: ${debt.dueDate?.toString() ?: "Chưa nhập"}")
        TextButton(onClick = { expanded = !expanded }) { Text("Ghi biến động / xem lịch sử") }
        if (expanded) {
            FilterChip(action == DebtAction.ADVANCE.name, { action = DebtAction.ADVANCE.name }, label = { Text("Giải ngân thêm") })
            FilterChip(action == DebtAction.REPAY.name, { action = DebtAction.REPAY.name }, label = { Text("Hoàn gốc / lãi / phí") })
            accounts.filter { it.archivedAt == null && it.currency == debt.currency }.forEach { account ->
                FilterChip(accountId == account.id.value, { accountId = account.id.value }, label = { Text(account.name) })
            }
            OutlinedTextField(principal, { principal = it }, label = { Text("Gốc") }, modifier = Modifier.fillMaxWidth().testTag("debt_principal"))
            OutlinedTextField(interest, { interest = it }, label = { Text("Lãi") }, modifier = Modifier.fillMaxWidth().testTag("debt_interest"))
            OutlinedTextField(fee, { fee = it }, label = { Text("Phí") }, modifier = Modifier.fillMaxWidth().testTag("debt_fee"))
            val direction = if (debt.role == DebtRole.BORROWER) CategoryDirection.EXPENSE else CategoryDirection.INCOME
            Text("Nhóm lãi (${if (direction == CategoryDirection.EXPENSE) "chi" else "thu"})")
            categories.filter { it.archivedAt == null && it.direction == direction }.forEach { category ->
                FilterChip(interestCategory == category.id.value, { interestCategory = category.id.value }, label = { Text("Lãi: ${category.name}") })
            }
            Text("Nhóm phí (chọn khác nhóm lãi)")
            categories.filter { it.archivedAt == null && it.direction == direction }.forEach { category ->
                FilterChip(feeCategory == category.id.value, { feeCategory = category.id.value }, label = { Text("Phí: ${category.name}") })
            }
            PlannerDateTimePicker("Thời điểm (bỏ chọn để dùng hiện tại)", at, now.atZone(zone).toLocalDateTime(), optional = true) { at = it }
            Button(enabled = !state.busy && accountId.isNotBlank() && !saved, onClick = {
                review = DebtEntryInput(Uuid.parse(id), Uuid.parse(operationId), debt, Uuid.parse(accountId), DebtAction.valueOf(action),
                    principal, interest, fee, interestCategory.takeIf(String::isNotBlank)?.let(Uuid::parse),
                    feeCategory.takeIf(String::isNotBlank)?.let(Uuid::parse),
                    at.takeIf(String::isNotBlank)?.let { LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(zone).toInstant() })
            }) { Text("Xem lại thành phần") }
            if (saved) TextButton(onClick = {
                principal = ""; interest = ""; fee = ""; at = ""
                id = RandomIdGenerator.newId().value; operationId = RandomIdGenerator.newId().value
            }) { Text("Ghi biến động mới") }
            position.movements.forEach { MovementRow(it, position, state.busy, zone, actions) }
        }
    }
    review?.let { input ->
        AlertDialog(onDismissRequest = { review = null }, title = { Text("Xác nhận từng thành phần") },
            text = { Column {
                Text("${debt.counterpartyLabel} · ${if (input.action == DebtAction.ADVANCE) "Giải ngân" else "Hoàn trả"}")
                Text("Gốc: ${input.principal.ifBlank { "0" }} ${debt.currency.value} (không tính thu/chi)")
                Text("Lãi: ${input.interest.ifBlank { "0" }} ${debt.currency.value}")
                Text("Phí: ${input.fee.ifBlank { "0" }} ${debt.currency.value}")
                Text(if (debt.role == DebtRole.BORROWER) "Lãi và phí ghi chi phí." else "Lãi và phí ghi thu nhập.")
                Text("Tài khoản: ${accounts.firstOrNull { it.id == input.accountId }?.name.orEmpty()}")
            } }, confirmButton = { Button(enabled = !state.busy, onClick = { actions.post(input); review = null }) { Text("Xác nhận ghi sổ") } },
            dismissButton = { TextButton(onClick = { review = null }) { Text("Quay lại") } })
    }
}

@Composable
private fun MovementRow(movement: DebtMovement, position: DebtPosition, busy: Boolean, zone: ZoneId, actions: DebtActions) {
    var reason by rememberSaveable(movement.id.value) { mutableStateOf("") }
    val correctionId = rememberSaveable(movement.id.value) { RandomIdGenerator.newId().value }
    val operation = rememberSaveable(movement.id.value) { RandomIdGenerator.newId().value }
    Text("${movement.occurredAt.atZone(zone).toLocalDateTime()} · Gốc ${money(movement.principalDeltaMinor, position.agreement.currency)}")
    Text("Lãi ${money(movement.interestMinor, position.agreement.currency)} · Phí ${money(movement.feeMinor, position.agreement.currency)}")
    Text("Mã biến động: ${movement.id.value}")
    movement.note?.let { Text(it) }
    if (movement.reversesMovementId == null && position.movements.none { it.reversesMovementId == movement.id }) {
        OutlinedTextField(reason, { reason = it }, label = { Text("Lý do sửa sai") }, modifier = Modifier.fillMaxWidth())
        TextButton(enabled = !busy && reason.isNotBlank(), onClick = {
            actions.reverse(Uuid.parse(correctionId), Uuid.parse(operation), position.agreement.id, movement.id, reason)
        }) { Text("Đảo toàn bộ biến động") }
    } else Text("Có bút toán đảo; giữ nguyên lịch sử")
}
