package vn.lifeos.app.finance.scheduled

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.Account
import vn.lifeos.domain.finance.TransactionKind
import vn.lifeos.domain.finance.scheduled.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

internal data class ScheduledUiState(val loading: Boolean = true, val busy: Boolean = false,
    val snapshot: ScheduledSnapshot = ScheduledSnapshot(emptyList(), emptyList()), val error: String? = null,
    val confirmation: String? = null)
internal data class ScheduleInput(val id: Uuid, val expectedVersion: Long?, val title: String,
    val recurrence: FinanceRecurrence, val nextDueDate: LocalDate, val amount: String,
    val account: Account, val kind: TransactionKind, val categoryId: Uuid)
internal interface ScheduledActions {
    fun retry()
    fun save(input: ScheduleInput)
    fun setEnabled(rule: ScheduledTransaction, enabled: Boolean)
    fun decide(occurrenceId: Uuid, decision: PlannedDecision)
}
internal class ScheduledFeature(private val repository: ScheduledFinanceRepository, private val workspace: Uuid,
    private var zone: ZoneId, private val clock: Clock, private val scope: CoroutineScope,
    private val reschedule: () -> Unit) : ScheduledActions {
    private val mutable = MutableStateFlow(ScheduledUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var action: Job? = null
    init { retry() }
    fun updateZone(value: ZoneId) { if (zone != value) { zone = value; retry() } }
    override fun retry() {
        observer?.cancel()
        mutable.value = mutable.value.copy(loading = true, error = null)
        observer = scope.launch {
            val generated = repository.generateDue(workspace, zone)
            if (generated is Outcome.Failure) {
                mutable.value = mutable.value.copy(loading = false, error = "Không tạo được khoản dự kiến. Thử lại.")
                return@launch
            }
            reschedule()
            repository.observe(workspace, zone).collect {
                mutable.value = when (it) {
                    is Outcome.Success -> mutable.value.copy(loading = false, snapshot = it.value)
                    is Outcome.Failure -> mutable.value.copy(loading = false, error = "Không tải được khoản dự kiến. Thử lại.")
                }
            }
        }
    }
    override fun save(input: ScheduleInput) {
        val amount = runCatching { Money.parse(input.amount, input.account.currency, Locale.forLanguageTag("vi-VN")) }.getOrNull()
        if (amount == null || amount.amountMinor <= 0) {
            mutable.value = mutable.value.copy(error = "Số tiền phải dương và đúng định dạng."); return
        }
        execute("Đã lưu quy tắc; lần dự kiến đã sinh giữ nguyên") {
            repository.save(ScheduledTransaction(input.id, workspace, input.title, input.recurrence, input.nextDueDate,
                input.nextDueDate, amount.amountMinor, amount.currency, input.kind, input.account.id, input.categoryId,
                mutable.value.snapshot.rules.firstOrNull { it.id == input.id }?.enabled ?: true), zone, input.expectedVersion)
        }
    }
    override fun setEnabled(rule: ScheduledTransaction, enabled: Boolean) = execute("Đã đổi trạng thái quy tắc") {
        repository.setEnabled(workspace, rule.id, rule.version, enabled)
    }
    override fun decide(occurrenceId: Uuid, decision: PlannedDecision) = execute(
        if (decision == PlannedDecision.Skip) "Đã bỏ qua; số dư không đổi" else "Đã xác nhận và liên kết lần dự kiến") {
        repository.decide(workspace, occurrenceId, zone, decision)
    }
    private fun execute(message: String, block: suspend () -> Outcome<*>) {
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null, confirmation = null)
            mutable.value = when (val result = block()) {
                is Outcome.Success -> mutable.value.copy(busy = false, confirmation = message)
                is Outcome.Failure -> mutable.value.copy(busy = false, error = when (val error = result.error) {
                    AppError.Conflict -> "Dữ liệu đã đổi hoặc có giao dịch/bằng chứng tương tự. Tải lại và chọn ghép, tránh ghi trùng."
                    AppError.NotFound -> "Không tìm thấy khoản dự kiến trong hồ sơ này."
                    AppError.Storage -> "Không lưu được dữ liệu trên thiết bị. Thử lại."
                    is AppError.InvalidInput -> when (error.field) {
                        "nextDueDate" -> "Khi sửa quy tắc, chọn ngày tương lai; lần đã sinh không bị sửa."
                        "occurredAt" -> "Ngày thực hiện không được ở tương lai."
                        else -> "Dữ liệu chưa hợp lệ: ${error.field}."
                    }
                })
            }
            if (mutable.value.error == null) retry()
        }
    }
}
