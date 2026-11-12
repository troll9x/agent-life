package vn.lifeos.app.finance.reconciliation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import vn.lifeos.domain.finance.reconciliation.FinancialDataQuality
import vn.lifeos.domain.finance.reconciliation.FinanceQualityFlag

@Composable
internal fun FinanceQualityNotice(quality: FinancialDataQuality) {
    if (quality.canClaimComplete) return
    Text("Chất lượng dữ liệu: chưa thể coi thu/chi là đầy đủ", color = MaterialTheme.colorScheme.error)
    if (FinanceQualityFlag.UNRECONCILED in quality.flags) Text("Chưa đối soát hoặc có ledger mới sau mốc kiểm: ${quality.unreconciledAccountIds.size} tài khoản")
    if (FinanceQualityFlag.PENDING_CANDIDATE in quality.flags) Text("Còn ${quality.pendingCandidateCount} khoản chờ nhập cần kiểm tra; chưa tính vào thu/chi đã ghi.")
    if (FinanceQualityFlag.MISSING_SOURCE in quality.flags) Text("Thiếu nguồn hoặc nguồn chưa sẵn sàng; nhập tay vẫn hoạt động.")
    if (FinanceQualityFlag.UNKNOWN_SOURCE_COVERAGE in quality.flags) Text("Chưa xác minh độ phủ nguồn ngân hàng cho tài khoản/kỳ này.")
    Text("Báo cáo chỉ phản ánh các giao dịch đã ghi; còn dữ liệu cần kiểm tra.")
}
