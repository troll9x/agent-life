package vn.lifeos.app.finance.reconciliation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.planner.PlannerDateTimePicker
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.ReconciliationState
import vn.lifeos.domain.finance.reconciliation.ReconciliationView
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput

private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private fun formatMoney(value: Long, code: CurrencyCode): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
    minimumFractionDigits = code.exponent; maximumFractionDigits = code.exponent
}.format(BigDecimal.valueOf(value).movePointLeft(code.exponent)) + " " + code.value

@Composable
internal fun ReconciliationSection(state: ReconciliationUiState, accounts: List<Account>, workspace: Uuid,
    zone: ZoneId, now: Instant, actions: ReconciliationActions?, adjustmentBusy: Boolean, adjustmentError: String?,
    onAdjustment: (ManualAdjustmentInput) -> Unit) {
    if (actions == null) return
    var accountId by rememberSaveable { mutableStateOf("") }
    var observed by rememberSaveable { mutableStateOf("") }
    var asOf by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var id by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    Card(Modifier.fillMaxWidth().testTag("finance_reconciliation")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Đối soát tài khoản", style = MaterialTheme.typography.titleLarge)
            Text("Nhập số dư bạn quan sát tại đúng thời điểm. Lưu snapshot không thay đổi ledger; dữ liệu được lưu ngoại tuyến.")
            if (state.loading) Text("Đang tải đối soát…")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.confirmation?.let { Text(it) }
            TextButton(onClick = actions::retry, enabled = !state.busy) { Text("Tải lại đối soát") }
            accounts.forEach { account ->
                FilterChip(accountId == account.id.value, { accountId = account.id.value }, label = { Text("Kiểm: ${account.name} · ${account.currency.value}") })
            }
            val account = accounts.firstOrNull { it.id.value == accountId }
            OutlinedTextField(observed, { observed = it }, label = { Text("Số dư quan sát") }, modifier = Modifier.fillMaxWidth().testTag("reconciliation_observed"))
            PlannerDateTimePicker("Mốc kiểm (bỏ chọn để dùng hiện tại)", asOf, now.atZone(zone).toLocalDateTime(), optional = true) { asOf = it }
            OutlinedTextField(note, { note = it }, label = { Text("Ghi chú bằng chứng (tùy chọn)") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = !state.busy && account != null && observed.isNotBlank(), onClick = {
                account?.let { actions.capture(Uuid.parse(id), it, observed,
                    asOf.takeIf(String::isNotBlank)?.let { text -> LocalDateTime.parse(text, dateTime).atZone(zone).toInstant() }, note) }
            }) { Text("Lưu snapshot đối soát") }
            TextButton(onClick = { id = RandomIdGenerator.newId().value; observed = ""; asOf = ""; note = "" }) { Text("Nhập mốc kiểm mới") }
            if (!state.loading && state.error == null && state.views.isEmpty()) Text("Chưa có snapshot đối soát.")
            state.views.forEach { view ->
                Text(accounts.firstOrNull { it.id == view.snapshot.accountId }?.name ?: view.snapshot.accountId.value, style = MaterialTheme.typography.titleMedium)
                Text("Mốc: ${view.snapshot.asOfInstant.atZone(zone).toLocalDateTime()}")
                Text("Expected lúc lưu: ${formatMoney(view.snapshot.expectedBalanceMinor, view.currency)}")
                Text("Observed: ${formatMoney(view.snapshot.observedBalanceMinor, view.currency)}")
                Text("Chênh lệch lúc lưu: ${formatMoney(view.snapshot.differenceMinor, view.currency)}", Modifier.testTag("reconciliation_difference"))
                Text(if (view.resolved) "Đã đối soát tại mốc này" else "Chưa đối soát — cần kiểm tra")
                if (view.ledgerChanged) Text("Ledger tại mốc này đã thay đổi: expected hiện tại ${formatMoney(view.currentExpectedMinor, view.currency)}; snapshot gốc giữ nguyên.")
                view.snapshot.note?.takeIf(String::isNotBlank)?.let { Text(it) }
                TextButton(enabled = !state.busy, onClick = { actions.review(view.snapshot.id) }) { Text("Xem giao dịch và khoản chờ nhập quanh mốc") }
                if (view.snapshot.state == ReconciliationState.OPEN) Button(enabled = !state.busy && view.currentDifferenceMinor == 0L,
                    onClick = { actions.resolve(view.snapshot.id) }) { Text("Xác nhận đã đối soát") }
            }
            state.review?.let { review ->
                Text("Khoảng kiểm tra: ${review.from.atZone(zone).toLocalDateTime()} đến trước ${review.endExclusive.atZone(zone).toLocalDateTime()}")
                Text("Ledger quanh mốc", style = MaterialTheme.typography.titleMedium)
                if (review.ledger.isEmpty()) Text("Không có ledger trong khoảng kiểm tra của tài khoản này.")
                review.ledger.forEach { row ->
                    Text("${row.transaction.occurredAt.atZone(zone).toLocalDateTime()} · ${row.transaction.kind} · ${row.transaction.id.value}")
                    row.entries.filter { it.accountId == review.view.snapshot.accountId }.forEach { Text(formatMoney(it.deltaMinor, it.currency)) }
                }
                Text("Candidates chưa xử lý: ${review.candidates.size}")
                review.candidates.forEach { candidate ->
                    Text("${candidate.id.value} · ${candidate.state} · ${candidate.occurredAt?.atZone(zone)?.toLocalDateTime() ?: "Thiếu thời gian"}")
                    Text(candidate.amountMinor?.let { amount -> candidate.currency?.let { formatMoney(amount, it) } } ?: "Thiếu amount/currency")
                }
                Text("Kiểm tra/ghép/bỏ qua candidate ở hàng chờ nhập; không tự thêm khoản để khớp số dư.")
                accounts.firstOrNull { it.id == review.view.snapshot.accountId && it.archivedAt == null }?.let {
                    ReconciliationAdjustment(review.view, it, workspace, zone, adjustmentBusy, adjustmentError, onAdjustment)
                }
            }
        }
    }
}

@Composable
private fun ReconciliationAdjustment(view: ReconciliationView, account: Account, workspace: Uuid, zone: ZoneId,
    busy: Boolean, error: String?, onAdjustment: (ManualAdjustmentInput) -> Unit) {
    var expanded by rememberSaveable(view.snapshot.id.value) { mutableStateOf(false) }
    var delta by rememberSaveable(view.snapshot.id.value) { mutableStateOf(BigDecimal.valueOf(view.currentDifferenceMinor)
        .movePointLeft(account.currency.exponent).toPlainString()) }
    var reason by rememberSaveable(view.snapshot.id.value) { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val id = rememberSaveable(view.snapshot.id.value) { RandomIdGenerator.newId().value }
    val operation = rememberSaveable(view.snapshot.id.value) { RandomIdGenerator.newId().value }
    TextButton(enabled = view.currentDifferenceMinor != 0L, onClick = { expanded = !expanded }) { Text("Mở điều chỉnh có lý do") }
    if (expanded && view.currentDifferenceMinor != 0L) {
        Text("Đây là giao dịch điều chỉnh riêng. Không tự đánh dấu đã đối soát.")
        OutlinedTextField(delta, { delta = it }, label = { Text("Điều chỉnh có dấu") }, modifier = Modifier.fillMaxWidth().testTag("reconciliation_adjustment_amount"))
        OutlinedTextField(reason, { reason = it }, label = { Text("Lý do điều chỉnh bắt buộc") }, modifier = Modifier.fillMaxWidth().testTag("reconciliation_adjustment_reason"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !busy && reason.isNotBlank(), onClick = { confirm = true }) { Text("Xem lại điều chỉnh riêng") }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Xác nhận điều chỉnh số dư") }, text = { Column {
        Text("${account.name} · tại ${view.snapshot.asOfInstant.atZone(zone).toLocalDateTime()}")
        Text("Expected hiện tại: ${formatMoney(view.currentExpectedMinor, view.currency)}")
        Text("Thay đổi đã nhập: $delta ${account.currency.value}")
        Text("Lý do: $reason")
        Text("Điều chỉnh chỉ đổi số dư, không tính vào thu/chi.")
    } }, confirmButton = { Button(enabled = !busy && reason.isNotBlank(), onClick = {
        onAdjustment(ManualAdjustmentInput(Uuid.parse(id), Uuid.parse(operation), workspace, account, delta.removePrefix("+"), view.snapshot.asOfInstant, reason))
        confirm = false
    }) { Text("Xác nhận ghi điều chỉnh") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Quay lại") } })
}
