package vn.lifeos.app.finance.history

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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.planner.PlannerDateTimePicker
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.Category
import vn.lifeos.domain.finance.PostedFinanceRow
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val display = java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

/** Recent immutable rows remain visible as an original -> reversal -> replacement chain. */
@Composable
internal fun FinanceHistorySection(
    workspaceId: Uuid, rows: List<PostedFinanceRow>, accounts: List<Account>, categories: List<Category>,
    zone: ZoneId, now: Instant, busy: Boolean, completedReplacementId: Uuid?,
    onReplace: (ReplacePostedTransaction) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf("") }
    var sourceId by rememberSaveable { mutableStateOf("") }
    var destinationId by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var correctionAt by rememberSaveable { mutableStateOf(now.atZone(zone).toLocalDateTime().format(dateTimeFormat)) }
    var reversalId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var reversalOperationId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var replacementId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var replacementOperationId by rememberSaveable { mutableStateOf(RandomIdGenerator.newId().value) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(completedReplacementId) {
        if (completedReplacementId?.value == replacementId) {
            editingId = ""
            reversalId = RandomIdGenerator.newId().value
            reversalOperationId = RandomIdGenerator.newId().value
            replacementId = RandomIdGenerator.newId().value
            replacementOperationId = RandomIdGenerator.newId().value
        }
    }
    Text("Lịch sử giao dịch", style = MaterialTheme.typography.titleLarge)
    if (rows.isEmpty()) Text("Chưa có giao dịch đã ghi.")
    rows.forEach { row ->
        val transaction = row.transaction
        val reversed = rows.any { it.transaction.reversesId == transaction.id }
        val refunded = rows.any { it.transaction.refundOfId == transaction.id }
        val amountMinor = row.entries.firstOrNull()?.deltaMinor?.let { if (it == Long.MIN_VALUE) 0 else kotlin.math.abs(it) } ?: 0L
        val reversesId = transaction.reversesId
        val replacementOfId = transaction.replacementOfId
        val chain = when {
            reversesId != null -> "Đảo giao dịch ${reversesId.value.takeLast(8)}"
            replacementOfId != null -> "Thay cho ${replacementOfId.value.takeLast(8)}"
            reversed -> "Đã sửa"
            else -> "Gốc"
        }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${transaction.kind.name} · ${display.format(amountMinor)} ${row.entries.firstOrNull()?.currency?.value.orEmpty()}")
            Text("${transaction.occurredAt.atZone(zone).toLocalDate()} · $chain · ${transaction.id.value.takeLast(8)}")
            Text("Tác động số dư: ${row.entries.joinToString { entry ->
                val account = accounts.firstOrNull { it.id == entry.accountId }?.name ?: entry.accountId.value.takeLast(8)
                "$account ${if (entry.deltaMinor > 0) "+" else ""}${display.format(entry.deltaMinor)}"
            }}")
            if (transaction.kind in setOf(TransactionKind.INCOME, TransactionKind.EXPENSE, TransactionKind.TRANSFER) &&
                !reversed && !refunded) {
                TextButton(onClick = {
                    editingId = transaction.id.value
                    val sourceEntry = row.entries.firstOrNull { it.deltaMinor < 0 } ?: row.entries.firstOrNull()
                    sourceId = sourceEntry?.accountId?.value.orEmpty()
                    destinationId = row.entries.firstOrNull { it.accountId.value != sourceId }?.accountId?.value.orEmpty()
                    categoryId = transaction.categoryId?.value.orEmpty()
                    amount = amountMinor.toString()
                    correctionAt = now.atZone(zone).toLocalDateTime().format(dateTimeFormat)
                    localError = null
                }, modifier = Modifier.testTag("finance_edit_${transaction.id.value}")) { Text("Sửa bằng giao dịch mới") }
            }
        } }
    }
    val original = rows.firstOrNull { it.transaction.id.value == editingId } ?: return
    val kind = original.transaction.kind
    val selectedSource = accounts.firstOrNull { it.id.value == sourceId }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sửa ${original.transaction.id.value.takeLast(8)}", style = MaterialTheme.typography.titleMedium)
        Text("Giao dịch gốc sẽ được đảo, rồi ghi khoản thay thế cùng lúc. Ngày sửa quyết định kỳ báo cáo của hai khoản mới.")
        OutlinedTextField(amount, { amount = it }, label = { Text("Số tiền thay thế") },
            modifier = Modifier.fillMaxWidth().testTag("finance_edit_amount"))
        Text("Tài khoản thay thế")
        accounts.forEach { account ->
            Row(Modifier.fillMaxWidth().clickable { sourceId = account.id.value }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(sourceId == account.id.value, onClick = null)
                Text(account.name, Modifier.padding(top = 12.dp))
            }
        }
        if (kind == TransactionKind.TRANSFER) {
            Text("Tài khoản nhận")
            accounts.filter { it.id.value != sourceId && it.currency == selectedSource?.currency }.forEach { account ->
                Row(Modifier.fillMaxWidth().clickable { destinationId = account.id.value }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(destinationId == account.id.value, onClick = null)
                    Text(account.name, Modifier.padding(top = 12.dp))
                }
            }
        } else {
            Text("Nhóm ${if (kind == TransactionKind.INCOME) "thu" else "chi"}")
            categories.filter { it.direction.name == kind.name && it.archivedAt == null }.forEach { category ->
                Row(Modifier.fillMaxWidth().clickable { categoryId = category.id.value }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(categoryId == category.id.value, onClick = null)
                    Text(category.name, Modifier.padding(top = 12.dp))
                }
            }
        }
        PlannerDateTimePicker("Ngày và giờ sửa", correctionAt, now.atZone(zone).toLocalDateTime()) { correctionAt = it }
        val parsed = selectedSource?.let { runCatching { Money.parse(amount, it.currency, Locale.forLanguageTag("vi-VN")) }.getOrNull() }
        if (parsed != null && parsed.amountMinor > 0 && selectedSource != null) {
            val old = original.entries.filter { it.accountId == selectedSource.id }.sumOf { it.deltaMinor }
            val replacementDelta = if (kind == TransactionKind.EXPENSE || kind == TransactionKind.TRANSFER) -parsed.amountMinor else parsed.amountMinor
            Text("Dự kiến thay đổi số dư ${selectedSource.name}: ${display.format(replacementDelta - old)} ${selectedSource.currency.value}")
        }
        if (localError != null) Text(localError!!, color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { editingId = "" }) { Text("Hủy") }
            Button(enabled = !busy && selectedSource != null, modifier = Modifier.testTag("finance_replace"), onClick = {
                val occurred = runCatching { LocalDateTime.parse(correctionAt, dateTimeFormat).atZone(zone).toInstant() }.getOrNull()
                val source = accounts.firstOrNull { it.id.value == sourceId }
                val destination = accounts.firstOrNull { it.id.value == destinationId }
                val category = categories.firstOrNull { it.id.value == categoryId }
                if (occurred == null || source == null) localError = "Chọn tài khoản và ngày sửa hợp lệ."
                else {
                    localError = null
                    onReplace(ReplacePostedTransaction(original.transaction.id, original.transaction.postedAt,
                        Uuid.parse(reversalId), Uuid.parse(reversalOperationId),
                        ManualEntryInput(Uuid.parse(replacementId), Uuid.parse(replacementOperationId), workspaceId,
                            kind, source, if (kind == TransactionKind.TRANSFER) destination else null,
                            if (kind == TransactionKind.TRANSFER) null else category,
                            amount, occurred, original.transaction.payee, original.transaction.note)))
                }
            }) { Text("Lưu bản sửa") }
        }
    } }
}
