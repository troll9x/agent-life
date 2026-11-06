package vn.lifeos.app.finance.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.ZoneId
import java.util.Locale
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceUiState
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.imports.InboxItem
import vn.lifeos.domain.finance.imports.MatchReason

private val numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

@Composable
internal fun FinanceInboxSection(state: FinanceUiState, zone: ZoneId, actions: FinanceActions?) {
    Card(Modifier.fillMaxWidth().testTag("finance_inbox")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hàng chờ nhập", style = MaterialTheme.typography.titleLarge)
            Text("Khoản chờ duyệt chưa thay đổi số dư.")
            if (state.inboxLoading) Text("Đang tải hàng chờ…")
            if (state.inboxError != null) {
                Text(state.inboxError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { actions?.retryInbox() }) { Text("Thử lại") }
            }
            if (!state.inboxLoading && state.inboxError == null && state.inboxItems.isEmpty())
                Text("Chưa có khoản chờ nhập.")
            state.inboxItems.forEach { item -> InboxCandidateCard(item, state, zone, actions) }
        }
    }
}

@Composable
private fun InboxCandidateCard(item: InboxItem, state: FinanceUiState, zone: ZoneId, actions: FinanceActions?) {
    val candidate = item.candidate
    var accountId by rememberSaveable(candidate.id.value) { mutableStateOf(candidate.proposedAccountId?.value) }
    var categoryId by rememberSaveable(candidate.id.value) { mutableStateOf(candidate.proposedCategoryId?.value) }
    val direction = candidate.direction
    val eligibleAccounts = state.accounts.filter { it.archivedAt == null && it.currency == candidate.currency }
    val eligibleCategories = state.categories.filter { it.archivedAt == null && it.direction == direction }
    Column(Modifier.fillMaxWidth().testTag("finance_candidate_${candidate.id.value}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${item.evidence.sourceKind} · ${candidate.state.name}", style = MaterialTheme.typography.titleMedium)
        Text("${direction?.name ?: "Chưa rõ chiều"} · ${candidate.amountMinor?.let(numbers::format) ?: "Chưa rõ số tiền"} ${candidate.currency?.value.orEmpty()}")
        Text("${candidate.occurredAt?.atZone(zone)?.toLocalDateTime() ?: "Chưa rõ thời gian"} · parser v${candidate.parserVersion}")
        item.evidence.bankReference?.let { Text("Reference: $it") }
        if (candidate.missingFields.isNotBlank()) Text("Cần kiểm tra: ${candidate.missingFields}")
        if (item.suggestions.size > 1) Text("Có nhiều giao dịch có thể khớp. Chọn sau khi đối chiếu.",
            color = MaterialTheme.colorScheme.error)
        item.suggestions.forEach { suggestion ->
            val recent = state.transactions.firstOrNull { it.transaction.id == suggestion.transactionId }
            val label = if (recent == null) "${suggestion.occurredAt.atZone(zone).toLocalDate()} · " +
                "${numbers.format(suggestion.amountMinor)} ${candidate.currency?.value.orEmpty()} · " +
                suggestion.transactionId.value.take(8)
                else "${recent.transaction.occurredAt.atZone(zone).toLocalDate()} · ${recent.transaction.payee ?: recent.transaction.id.value.take(8)}"
            Text("Gợi ý ${if (suggestion.reason == MatchReason.VERIFIED_REFERENCE) "theo reference" else "theo số tiền/thời gian"}: $label")
            TextButton(enabled = !state.inboxBusy, onClick = { actions?.linkInbox(candidate.id, suggestion.transactionId) }) {
                Text("Ghép vào giao dịch này")
            }
        }
        if (candidate.amountMinor != null && candidate.currency != null && candidate.direction != null && candidate.occurredAt != null) {
            Text("Tài khoản")
            eligibleAccounts.forEach { account ->
                FilterChip(accountId == account.id.value, onClick = { accountId = account.id.value },
                    label = { Text(account.name) })
            }
            Text("Nhóm ${if (direction == CategoryDirection.INCOME) "thu" else "chi"}")
            eligibleCategories.forEach { category ->
                FilterChip(categoryId == category.id.value, onClick = { categoryId = category.id.value },
                    label = { Text(category.name) })
            }
            Button(enabled = !state.inboxBusy && accountId != null && categoryId != null,
                onClick = {
                    val account = accountId?.let(Uuid::parse)
                    val category = categoryId?.let(Uuid::parse)
                    if (account != null && category != null) actions?.approveInbox(candidate.id, account, category)
                }) { Text("Duyệt và ghi sổ") }
        }
        TextButton(enabled = !state.inboxBusy, onClick = { actions?.ignoreInbox(candidate.id) }) { Text("Bỏ qua") }
    }
}
