package vn.lifeos.app.finance.reconciliation

import java.time.Clock
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.reconciliation.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

internal data class ReconciliationUiState(val loading: Boolean = true, val busy: Boolean = false,
    val views: List<ReconciliationView> = emptyList(), val review: ReconciliationReview? = null,
    val error: String? = null, val confirmation: String? = null)
internal interface ReconciliationActions {
    fun retry()
    fun capture(id: Uuid, account: Account, observed: String, asOf: Instant?, note: String)
    fun resolve(id: Uuid)
    fun review(id: Uuid)
}
internal class ReconciliationFeature(private val repository: ReconciliationRepository, private val workspace: Uuid,
    private val clock: Clock, private val scope: CoroutineScope, private val refreshReports: () -> Unit) : ReconciliationActions {
    private val mutable = MutableStateFlow(ReconciliationUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var action: Job? = null
    init { retry() }
    override fun retry() {
        observer?.cancel()
        mutable.value = mutable.value.copy(loading = true, error = null)
        observer = scope.launch {
            repository.observe(workspace).collect { result ->
                mutable.value = when (result) {
                    is Outcome.Success -> mutable.value.copy(loading = false, views = result.value,
                        review = mutable.value.review?.let { review ->
                            result.value.firstOrNull { it.snapshot.id == review.view.snapshot.id }?.let { review.copy(view = it) }
                        })
                    is Outcome.Failure -> mutable.value.copy(loading = false, error = "Không đọc được snapshot đối soát. Thử lại.")
                }
                refreshReports()
            }
        }
    }
    override fun capture(id: Uuid, account: Account, observed: String, asOf: Instant?, note: String) {
        val amount = runCatching { Money.parse(observed.removePrefix("+"), account.currency, Locale.forLanguageTag("vi-VN")) }.getOrNull()
        if (amount == null || account.workspaceId != workspace) {
            mutable.value = mutable.value.copy(error = "Chọn tài khoản và nhập số dư hợp lệ."); return
        }
        execute("Đã lưu snapshot; ledger không thay đổi") {
            repository.capture(ObserveBalance(id, workspace, account.id, amount.currency, asOf ?: clock.instant(), amount.amountMinor, note))
        }
    }
    override fun resolve(id: Uuid) = execute("Đã xác nhận đối soát tại mốc này") { repository.resolve(workspace, id) }
    override fun review(id: Uuid) {
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null)
            when (val result = repository.review(workspace, id)) {
                is Outcome.Success -> mutable.value = mutable.value.copy(busy = false, review = result.value)
                is Outcome.Failure -> mutable.value = mutable.value.copy(busy = false, error = message(result.error))
            }
        }
    }
    private fun execute(confirmation: String, block: suspend () -> Outcome<*>) {
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null, confirmation = null)
            mutable.value = when (val result = block()) {
                is Outcome.Success -> mutable.value.copy(busy = false, confirmation = confirmation)
                is Outcome.Failure -> mutable.value.copy(busy = false, error = message(result.error))
            }
            refreshReports()
        }
    }
    private fun message(error: AppError): String = when (error) {
        AppError.Conflict -> "Snapshot đã có hoặc đã được xử lý. Tải lại để kiểm tra."
        AppError.NotFound -> "Không tìm thấy snapshot hoặc tài khoản trong hồ sơ này."
        AppError.Storage -> "Không lưu/đọc được dữ liệu trên thiết bị. Thử lại."
        is AppError.InvalidInput -> if (error.field == "difference") "Ledger chưa khớp observed tại mốc này. Kiểm tra giao dịch trước khi xác nhận."
            else "Dữ liệu chưa hợp lệ: ${error.field}; mốc kiểm không được trước mở tài khoản hoặc ở tương lai."
    }
}
