package vn.lifeos.app.finance.scheduled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.planner.PlannerDatePicker
import vn.lifeos.app.planner.PlannerDateTimePicker
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.Category
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.scheduled.*

private fun FinanceRecurrence.label() = when (this) {
    FinanceRecurrence.ONCE -> "Một lần"; FinanceRecurrence.DAILY -> "Hằng ngày"
    FinanceRecurrence.WEEKLY -> "Hằng tuần"; FinanceRecurrence.MONTHLY -> "Hằng tháng"; FinanceRecurrence.YEARLY -> "Hằng năm"
}
private fun PlannedTransaction.amount(): String {
    val scale = currency.exponent
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
        minimumFractionDigits = scale; maximumFractionDigits = scale
    }.format(BigDecimal.valueOf(amountMinor).movePointLeft(scale)) + " " + currency.value
}

@Composable
internal fun ScheduledSection(state: ScheduledUiState, accounts: List<Account>, categories: List<Category>,
    zone: ZoneId, now: Instant, actions: ScheduledActions?) {
    if (actions == null) return
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScheduledTransaction?>(null) }
    Card(Modifier.fillMaxWidth().testTag("finance_scheduled")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Định kỳ và khoản dự kiến", style = MaterialTheme.typography.titleLarge)
            Text("Khoản dự kiến không đổi số dư hoặc thực chi. Chỉ ghi sổ sau khi bạn xác nhận; dùng được ngoại tuyến.")
            Text("Nhắc lúc 09:00 ngày đến hạn theo múi giờ thiết bị, dùng quyền nhắc chung. Khi quyền thông báo bị tắt hoặc máy không chạy nền, vẫn xem các khoản đến hạn ở đây.")
            Text("Tạm dừng chỉ ngừng sinh lần mới; các lần đã sinh được xử lý riêng bên dưới.")
            vn.lifeos.app.reminders.ReminderStatusPanel(false, actions::retry)
            if (state.loading) Text("Đang tải khoản dự kiến…")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.confirmation?.let { Text(it) }
            TextButton(onClick = actions::retry, enabled = !state.busy) { Text("Tải lại khoản dự kiến") }
            if (!state.loading && state.error == null && state.snapshot.rules.isEmpty()) Text("Chưa có quy tắc hoặc khoản dự kiến.")
            state.snapshot.rules.forEach { rule ->
                Text("${rule.title} · ${rule.recurrence.label()} · ${if (rule.enabled) "Đang bật" else "Đã tạm dừng"}")
                Text("Lần tiếp theo: ${rule.nextDueDate ?: "Đã sinh lần cuối"}")
                Row {
                    TextButton(enabled = !state.busy, onClick = { actions.setEnabled(rule, !rule.enabled) }) {
                        Text(if (rule.enabled) "Tạm dừng" else "Bật lại")
                    }
                    TextButton(enabled = !state.busy, onClick = { editing = rule; showForm = true }) { Text("Sửa lần tương lai") }
                }
            }
            TextButton(onClick = { editing = null; showForm = !showForm }) { Text("Tạo khoản dự kiến") }
            if (showForm) key(editing?.id) {
                ScheduleForm(editing, accounts, categories, zone, now, state.busy, actions)
            }
            if (!state.loading && state.snapshot.rules.isNotEmpty() && state.snapshot.items.isEmpty()) Text("Chưa có lần đến hạn. Hệ thống chỉ sinh dự kiến khi tới ngày.")
            state.snapshot.items.forEach { PlannedRow(it, zone, now, state.busy, actions) }
        }
    }
}

@Composable
private fun ScheduleForm(editing: ScheduledTransaction?, accounts: List<Account>, categories: List<Category>, zone: ZoneId,
    now: Instant, busy: Boolean, actions: ScheduledActions) {
    val id = rememberSaveable { editing?.id?.value ?: RandomIdGenerator.newId().value }
    var title by rememberSaveable { mutableStateOf(editing?.title.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(editing?.let {
        BigDecimal.valueOf(it.amountMinor).movePointLeft(it.currency.exponent).toPlainString()
    }.orEmpty()) }
    var due by rememberSaveable { mutableStateOf(editing?.nextDueDate?.toString().orEmpty()) }
    var recurrence by rememberSaveable { mutableStateOf(editing?.recurrence?.name ?: FinanceRecurrence.MONTHLY.name) }
    var kind by rememberSaveable { mutableStateOf(editing?.kind?.name ?: TransactionKind.EXPENSE.name) }
    var account by rememberSaveable { mutableStateOf(editing?.accountId?.value.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(editing?.categoryId?.value.orEmpty()) }
    Text(if (editing == null) "Quy tắc mới" else "Chỉ sửa lần chưa sinh trong tương lai", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(title, { title = it }, label = { Text("Tên khoản dự kiến") }, modifier = Modifier.fillMaxWidth().testTag("schedule_title"))
    OutlinedTextField(amount, { amount = it }, label = { Text("Số tiền dự kiến") }, modifier = Modifier.fillMaxWidth().testTag("schedule_amount"))
    PlannerDatePicker("Ngày đến hạn tiếp theo", due, now.atZone(zone).toLocalDate()) { due = it }
    FinanceRecurrence.entries.forEach { frequency ->
        FilterChip(recurrence == frequency.name, { recurrence = frequency.name }, label = { Text(frequency.label()) })
    }
    Row {
        FilterChip(kind == "EXPENSE", { kind = "EXPENSE"; category = "" }, label = { Text("Chi dự kiến") })
        FilterChip(kind == "INCOME", { kind = "INCOME"; category = "" }, label = { Text("Thu dự kiến") })
    }
    accounts.filter { it.archivedAt == null }.forEach { item ->
        FilterChip(account == item.id.value, { account = item.id.value }, label = { Text("Ví: ${item.name} · ${item.currency.value}") })
    }
    categories.filter { it.archivedAt == null && it.direction.name == kind }.forEach { item ->
        FilterChip(category == item.id.value, { category = item.id.value }, label = { Text("Nhóm: ${item.name}") })
    }
    Button(enabled = !busy && title.isNotBlank() && due.isNotBlank() && account.isNotBlank() && category.isNotBlank(), onClick = {
        accounts.firstOrNull { it.id.value == account }?.let {
            actions.save(ScheduleInput(Uuid.parse(id), editing?.version, title, FinanceRecurrence.valueOf(recurrence), LocalDate.parse(due),
                amount, it, TransactionKind.valueOf(kind), Uuid.parse(category)))
        }
    }) { Text("Lưu quy tắc") }
}

@Composable
private fun PlannedRow(item: PlannedItem, zone: ZoneId, now: Instant, busy: Boolean, actions: ScheduledActions) {
    val plan = item.plan
    val operation = rememberSaveable(plan.id.value) { RandomIdGenerator.newId().value }
    var at by rememberSaveable(plan.id.value) { mutableStateOf("") }
    var separate by rememberSaveable(plan.id.value) { mutableStateOf(false) }
    var review by remember { mutableStateOf<PlannedDecision?>(null) }
    Column(Modifier.fillMaxWidth().testTag("planned_${plan.id.value}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${plan.title} · ${plan.dueDate} · ${plan.amount()}", style = MaterialTheme.typography.titleMedium)
        Text(when (plan.state) { PlannedState.PENDING -> "Dự kiến — chưa ghi sổ"; PlannedState.POSTED -> "Đã ghi/ghép vào sổ"; PlannedState.SKIPPED -> "Đã bỏ qua — số dư không đổi" })
        plan.transactionId?.let { Text("Giao dịch: ${it.value}") }
        if (plan.state == PlannedState.SKIPPED) return@Column
        if (plan.state == PlannedState.PENDING) {
            PlannerDateTimePicker("Ngày thực hiện", at, plan.dueDate.atTime(9, 0), optional = true) { at = it }
            Button(enabled = !busy, onClick = {
                val date = if (at.isBlank()) plan.dueDate.atTime(9, 0).atZone(zone).toInstant().let { if (it > now) now else it }
                    else LocalDateTime.parse(at, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(zone).toInstant()
                review = PlannedDecision.Post(Uuid.parse(operation), date, separatePayment = separate)
            }) { Text("Xác nhận khoản dự kiến") }
            TextButton(enabled = !busy, onClick = { review = PlannedDecision.Skip }) { Text("Bỏ qua lần này") }
            if (item.evidence.isNotEmpty() || item.postedTransactionIds.isNotEmpty()) {
                Text("Có khoản tương tự. Kiểm tra và chọn ghép; gợi ý không phải xác nhận cùng hóa đơn.")
                Row { Checkbox(separate, { separate = it }); Text("Tôi đã kiểm tra: đây là khoản thanh toán riêng") }
            }
            item.postedTransactionIds.forEach { transaction ->
                TextButton(enabled = !busy, onClick = { review = PlannedDecision.Link(transaction) }) {
                    Text("Ghép giao dịch ${transaction.value}")
                }
            }
        }
        item.evidence.forEach { evidence ->
            TextButton(enabled = !busy, onClick = {
                val transactionId = plan.transactionId
                review = if (transactionId != null) PlannedDecision.Link(transactionId, evidence.candidateId)
                else PlannedDecision.Post(Uuid.parse(operation), evidence.occurredAt, evidence.candidateId)
            }) { Text("Ghép ${evidence.sourceKind} · ${evidence.occurredAt.atZone(zone).toLocalDate()} · ${evidence.candidateId.value}") }
        }
    }
    review?.let { decision ->
        AlertDialog(onDismissRequest = { review = null }, title = { Text("Xem lại khoản dự kiến") },
            text = { Column {
                Text("${plan.title} · ${plan.dueDate} · ${plan.amount()}")
                if (decision is PlannedDecision.Post) Text("Thời điểm ghi nhận: ${decision.occurredAt.atZone(zone).toLocalDateTime()}")
                Text(when (decision) {
                    PlannedDecision.Skip -> "Bỏ qua lần này; không ghi sổ."
                    is PlannedDecision.Link -> "Ghép vào giao dịch đã có, không tạo dòng tiền mới."
                    is PlannedDecision.Post -> if (decision.candidateId != null) "Duyệt bằng chứng ngân hàng và ghi một giao dịch."
                        else if (decision.separatePayment) "Ghi khoản thanh toán riêng sau khi đã kiểm tra gợi ý trùng."
                        else "Ghi sổ một lần và thay đổi số dư theo khoản này."
                })
            } }, confirmButton = { Button(enabled = !busy, onClick = { actions.decide(plan.id, decision); review = null }) { Text("Xác nhận thực hiện") } },
            dismissButton = { TextButton(onClick = { review = null }) { Text("Quay lại") } })
    }
}
