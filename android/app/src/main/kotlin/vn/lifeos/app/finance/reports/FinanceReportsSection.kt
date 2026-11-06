package vn.lifeos.app.finance.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.math.BigDecimal
import java.util.Locale
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceUiState
import vn.lifeos.domain.finance.CategoryDirection
import vn.lifeos.domain.finance.reports.toSafeCsv

private val numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

@Composable
internal fun FinanceReportsSection(state: FinanceUiState, actions: FinanceActions?) {
    val context = LocalContext.current
    val report = state.report
    var exportError by rememberSaveable { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && report != null) {
            exportError = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(report.toSafeCsv().toByteArray(Charsets.UTF_8)) }
                    ?: error("stream")
                null
            } catch (_: Exception) { "Không xuất được CSV. Thử lại." }
        }
    }
    var drillCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var drillMetric by rememberSaveable { mutableStateOf<String?>(null) }
    var budgetCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var budgetAmount by rememberSaveable { mutableStateOf("") }
    var alertEnabled by rememberSaveable { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().testTag("finance_report")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Báo cáo và ngân sách", style = MaterialTheme.typography.titleLarge)
            val period = state.reportPeriod
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { actions?.selectReportPeriod(-1) }) { Text("Tháng trước") }
                TextButton(onClick = { actions?.selectReportPeriod(1) }) { Text("Tháng sau") }
            }
            Text(if (period == null) "Đang xác định kỳ…" else "Kỳ ${period.start} đến trước ${period.endExclusive}")
            FilterChip(state.reportAccountId == null, onClick = { actions?.selectReportAccount(null) }, label = { Text("Tất cả tài khoản") })
            state.accounts.filter { it.currency == report?.filter?.currency || report == null }.forEach { account ->
                FilterChip(state.reportAccountId == account.id,
                    onClick = { actions?.selectReportAccount(account.id) }, label = { Text(account.name) })
            }
            if (state.reportLoading) Text("Đang tính báo cáo…")
            if (state.reportError != null) {
                Text(state.reportError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { actions?.retryReport() }) { Text("Thử lại") }
            }
            if (report != null) {
                val currency = report.filter.currency.value
                Text("Thu: ${numbers.format(report.incomeMinor)} $currency")
                TextButton(onClick = { drillCategory = null; drillMetric = "income" }) { Text("Xem khoản thu") }
                Text("Chi ròng: ${numbers.format(report.netExpenseMinor)} $currency")
                TextButton(onClick = { drillCategory = null; drillMetric = "expense" }) { Text("Xem khoản chi và hoàn tiền") }
                Text("Dòng tiền thu trừ chi: ${numbers.format(report.surplusMinor)} $currency")
                state.previousReport?.let { previous ->
                    Text("Kỳ trước (${previous.filter.period.start} đến trước ${previous.filter.period.endExclusive}): " +
                        "thu ${numbers.format(previous.incomeMinor)}, chi ròng ${numbers.format(previous.netExpenseMinor)} $currency")
                }
                Text("Hoàn tiền được trừ trong kỳ nhận hoàn tiền. Chuyển khoản và điều chỉnh không tính vào chi tiêu.")
                Button(onClick = { export.launch("agent-life-${report.filter.period.start}-$currency.csv") }) { Text("Xuất CSV") }
                if (exportError != null) Text(exportError!!, color = MaterialTheme.colorScheme.error)
                if (report.lines.isEmpty()) Text("Chưa có giao dịch trong kỳ và bộ lọc này.")
                report.budgets.forEach { budget ->
                    Column {
                        Text("${budget.categoryName}: đã chi ${numbers.format(budget.spentMinor)} / ${numbers.format(budget.budget.limitMinor)} $currency")
                        Text("Còn ${numbers.format(budget.remainingMinor)} $currency" + if (budget.alert) " · Đã chạm ngưỡng cảnh báo" else "")
                        TextButton(onClick = { drillCategory = budget.budget.categoryId.value; drillMetric = null }) { Text("Xem giao dịch nhóm") }
                    }
                }
                Text("Đặt ngân sách nhóm chi", style = MaterialTheme.typography.titleMedium)
                state.categories.filter { it.direction == CategoryDirection.EXPENSE && it.archivedAt == null }.forEach { category ->
                    FilterChip(budgetCategory == category.id.value, onClick = {
                        budgetCategory = category.id.value
                        val existing = report.budgets.firstOrNull { it.budget.categoryId == category.id }
                        budgetAmount = existing?.budget?.limitMinor?.let {
                            BigDecimal.valueOf(it).movePointLeft(report.filter.currency.exponent).stripTrailingZeros().toPlainString()
                        }.orEmpty()
                        alertEnabled = existing?.budget?.alertThresholdBasisPoints != null
                    }, label = { Text(category.name) })
                }
                OutlinedTextField(budgetAmount, { budgetAmount = it }, label = { Text("Giới hạn ($currency)") }, modifier = Modifier.fillMaxWidth())
                Row { Checkbox(alertEnabled, { alertEnabled = it }); Text("Cảnh báo khi dùng 80%") }
                Button(enabled = budgetCategory != null, onClick = {
                    budgetCategory?.let { actions?.saveReportBudget(vn.lifeos.domain.common.Uuid.parse(it), budgetAmount, alertEnabled) }
                }) { Text("Lưu ngân sách") }
                Text("Giao dịch của báo cáo", style = MaterialTheme.typography.titleMedium)
                if (drillCategory != null || drillMetric != null) TextButton(onClick = { drillCategory = null; drillMetric = null }) { Text("Xem tất cả") }
                report.lines.filter { (drillCategory == null || it.categoryId?.value == drillCategory) &&
                    (drillMetric == null || if (drillMetric == "income") it.incomeMinor != 0L else it.netExpenseMinor != 0L)
                }.forEach { line ->
                    Text("${line.occurredAt.atZone(report.filter.zone).toLocalDate()} · ${line.categoryName} · " +
                        "thu ${numbers.format(line.incomeMinor)}, chi ròng ${numbers.format(line.netExpenseMinor)} $currency")
                }
            }
        }
    }
}
