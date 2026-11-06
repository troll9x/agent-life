package vn.lifeos.app.finance.refund

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.planner.PlannerDateTimePicker
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.FinanceReport
import vn.lifeos.domain.finance.PostedFinanceRow
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput
import vn.lifeos.domain.finance.refund.ManualRefundInput

private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val numbers = java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

@Composable
internal fun RefundAdjustmentSection(workspaceId: Uuid, accounts: List<Account>, rows: List<PostedFinanceRow>,
    balances: Map<Uuid, Long>, monthlyReports: Map<YearMonth, FinanceReport>, zone: ZoneId, now: Instant,
    busy: Boolean, error: String?, confirmation: String?, actionVersion: Long,
    onRefund: (ManualRefundInput) -> Unit, onAdjustment: (ManualAdjustmentInput) -> Unit) {
    var expenseId by rememberSaveable { mutableStateOf("") }
    var depositId by rememberSaveable { mutableStateOf("") }
    var refundAmount by rememberSaveable { mutableStateOf("") }
    var refundDate by rememberSaveable { mutableStateOf(now.atZone(zone).toLocalDateTime().format(dateTime)) }
    var refundNote by rememberSaveable { mutableStateOf("") }
    var refundId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var refundOperationId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var adjustmentAccountId by rememberSaveable { mutableStateOf("") }
    var signedAmount by rememberSaveable { mutableStateOf("") }
    var adjustmentDate by rememberSaveable { mutableStateOf(now.atZone(zone).toLocalDateTime().format(dateTime)) }
    var reason by rememberSaveable { mutableStateOf("") }
    var adjustmentId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var adjustmentOperationId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(actionVersion) {
        when (confirmation) {
            "Đã ghi hoàn tiền" -> {
                refundAmount = ""; refundNote = ""
                refundId = RandomIdGenerator.newId().value
                refundOperationId = RandomIdGenerator.newId().value
            }
            "Đã ghi điều chỉnh" -> {
                signedAmount = ""; reason = ""
                adjustmentId = RandomIdGenerator.newId().value
                adjustmentOperationId = RandomIdGenerator.newId().value
            }
        }
    }

    Text("Báo cáo theo tháng", style = MaterialTheme.typography.titleLarge)
    Text("Thu, chi và hoàn tiền lấy từ sổ đã ghi; điều chỉnh chỉ tác động số dư.")
    if (monthlyReports.isEmpty()) Text("Chưa tính được báo cáo tháng.")
    monthlyReports.toSortedMap(compareByDescending { it }).forEach { (month, report) ->
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text(month.toString(), style = MaterialTheme.typography.titleMedium)
            Text("Thu: ${numbers.format(report.incomeMinor)} · Chi gộp: ${numbers.format(report.grossExpenseMinor)}")
            Text("Hoàn tiền: ${numbers.format(report.refundMinor)} · Thực chi: ${numbers.format(report.netExpenseMinor)}",
                modifier = Modifier.testTag("finance_report_${month}"))
        } }
    }

    Text("Hoàn tiền", style = MaterialTheme.typography.titleLarge)
    Text("Chọn khoản chi gốc. Có thể hoàn một phần; tổng hoàn không vượt số đã chi.")
    val expenses = rows.filter { it.transaction.kind == TransactionKind.EXPENSE &&
        rows.none { other -> other.transaction.reversesId == it.transaction.id } }
    if (expenses.isEmpty()) Text("Chưa có khoản chi có thể hoàn trong lịch sử gần đây.")
    expenses.forEach { row ->
        val amount = row.entries.singleOrNull()?.deltaMinor?.let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) } ?: 0L
        Row(Modifier.fillMaxWidth().clickable {
            expenseId = row.transaction.id.value
            depositId = row.entries.singleOrNull()?.accountId?.value.orEmpty()
            localError = null
        }.testTag("finance_refund_original_${row.transaction.id.value}"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(expenseId == row.transaction.id.value, onClick = null)
            Text("${numbers.format(amount)} ${row.entries.firstOrNull()?.currency?.value.orEmpty()} · ${row.transaction.occurredAt.atZone(zone).toLocalDate()}",
                Modifier.padding(top = 12.dp))
        }
    }
    val original = expenses.firstOrNull { it.transaction.id.value == expenseId }
    val originalCurrency = original?.entries?.singleOrNull()?.currency
    if (original != null) {
        Text("Tài khoản nhận hoàn tiền")
        accounts.filter { it.currency == originalCurrency && it.archivedAt == null }.forEach { account ->
            Row(Modifier.fillMaxWidth().testTag("finance_refund_deposit_${account.id.value}")
                .clickable { depositId = account.id.value }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(depositId == account.id.value, onClick = null)
                Text(account.name, Modifier.padding(top = 12.dp))
            }
        }
        OutlinedTextField(refundAmount, { refundAmount = it }, label = { Text("Số tiền hoàn") },
            modifier = Modifier.fillMaxWidth().testTag("finance_refund_amount"))
        PlannerDateTimePicker("Ngày nhận hoàn tiền", refundDate, now.atZone(zone).toLocalDateTime()) { refundDate = it }
        OutlinedTextField(refundNote, { refundNote = it }, label = { Text("Ghi chú (tùy chọn)") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy, modifier = Modifier.testTag("finance_refund_save"), onClick = {
            val account = accounts.firstOrNull { it.id.value == depositId }
            val occurred = runCatching { LocalDateTime.parse(refundDate, dateTime).atZone(zone).toInstant() }.getOrNull()
            if (account == null || occurred == null) localError = "Chọn tài khoản và ngày nhận hợp lệ."
            else {
                localError = null
                onRefund(ManualRefundInput(Uuid.parse(refundId), Uuid.parse(refundOperationId), workspaceId,
                    original, account, refundAmount, occurred, refundNote))
            }
        }) { Text(if (busy) "Đang lưu…" else "Ghi hoàn tiền") }
    }

    Text("Điều chỉnh số dư", style = MaterialTheme.typography.titleLarge)
    Text("Chỉ dùng khi cần sửa số dư có lý do. Nhập dấu + để tăng hoặc - để giảm. Không tính là thu/chi.")
    accounts.filter { it.archivedAt == null }.forEach { account ->
        Row(Modifier.fillMaxWidth().testTag("finance_adjustment_account_${account.id.value}")
            .clickable { adjustmentAccountId = account.id.value }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(adjustmentAccountId == account.id.value, onClick = null)
            Text(account.name, Modifier.padding(top = 12.dp))
        }
    }
    OutlinedTextField(signedAmount, { signedAmount = it }, label = { Text("Thay đổi số dư có dấu") },
        modifier = Modifier.fillMaxWidth().testTag("finance_adjustment_amount"))
    val account = accounts.firstOrNull { it.id.value == adjustmentAccountId }
    val delta = account?.let { runCatching { Money.parse(signedAmount.removePrefix("+"), it.currency,
        Locale.forLanguageTag("vi-VN")).amountMinor }.getOrNull() }
    if (account != null && delta != null && delta != 0L) {
        val before = balances[account.id]
        val after = before?.let { runCatching { Math.addExact(it, delta) }.getOrNull() }
        Text("Xem trước: ${account.name} ${before?.let(numbers::format) ?: "—"} → ${after?.let(numbers::format) ?: "—"} ${account.currency.value}",
            modifier = Modifier.testTag("finance_adjustment_preview"))
    }
    PlannerDateTimePicker("Ngày và giờ điều chỉnh", adjustmentDate, now.atZone(zone).toLocalDateTime()) { adjustmentDate = it }
    OutlinedTextField(reason, { reason = it }, label = { Text("Lý do bắt buộc") },
        modifier = Modifier.fillMaxWidth().testTag("finance_adjustment_reason"))
    if (localError != null) Text(localError!!, color = MaterialTheme.colorScheme.error)
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
    if (confirmation != null) Text(confirmation, color = MaterialTheme.colorScheme.primary)
    Button(enabled = !busy && account != null && reason.isNotBlank() && delta != null && delta != 0L,
        modifier = Modifier.testTag("finance_adjustment_save"), onClick = {
            val occurred = runCatching { LocalDateTime.parse(adjustmentDate, dateTime).atZone(zone).toInstant() }.getOrNull()
            if (account == null || occurred == null) localError = "Chọn tài khoản và ngày điều chỉnh hợp lệ."
            else {
                localError = null
                onAdjustment(ManualAdjustmentInput(Uuid.parse(adjustmentId), Uuid.parse(adjustmentOperationId),
                    workspaceId, account, signedAmount.removePrefix("+"), occurred, reason))
            }
        }) { Text(if (busy) "Đang lưu…" else "Ghi điều chỉnh") }
}
