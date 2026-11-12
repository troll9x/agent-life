package vn.lifeos.app.finance.debt

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.Money
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.debt.*
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

internal data class DebtUiState(val loading: Boolean = true, val busy: Boolean = false,
    val positions: List<DebtPosition> = emptyList(), val error: String? = null,
    val confirmation: String? = null, val savedOperationId: Uuid? = null)

internal data class DebtEntryInput(val id: Uuid, val operationId: Uuid, val agreement: DebtAgreement,
    val accountId: Uuid, val action: DebtAction, val principal: String, val interest: String, val fee: String,
    val interestCategoryId: Uuid?, val feeCategoryId: Uuid?, val occurredAt: Instant?)

internal interface DebtActions {
    fun retry()
    fun create(id: Uuid, counterpartyId: Uuid, label: String, role: DebtRole, opening: String, dueDate: LocalDate?)
    fun post(input: DebtEntryInput)
    fun reverse(id: Uuid, operationId: Uuid, debtId: Uuid, movementId: Uuid, reason: String)
}

internal class DebtFeature(private val repository: DebtRepository, private val workspace: Uuid,
    private val currency: CurrencyCode, private val clock: Clock, private val scope: CoroutineScope) : DebtActions {
    private val mutable = MutableStateFlow(DebtUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var action: Job? = null
    init { retry() }
    override fun retry() {
        observer?.cancel()
        mutable.value = mutable.value.copy(loading = true, error = null)
        observer = scope.launch {
            repository.observe(workspace).collect {
                mutable.value = when (it) {
                    is Outcome.Success -> mutable.value.copy(loading = false, positions = it.value)
                    is Outcome.Failure -> mutable.value.copy(loading = false, error = "Không đọc được khoản vay. Thử lại.")
                }
            }
        }
    }
    override fun create(id: Uuid, counterpartyId: Uuid, label: String, role: DebtRole, opening: String, dueDate: LocalDate?) {
        val amount = parse(opening, currency) ?: return invalidAmount()
        execute("Đã tạo khoản vay", null) {
            repository.create(DebtAgreement(id, workspace, role, counterpartyId, label, currency, amount, dueDate))
        }
    }
    override fun post(input: DebtEntryInput) {
        val principal = parse(input.principal, input.agreement.currency) ?: return invalidAmount()
        val interest = parse(input.interest, input.agreement.currency) ?: return invalidAmount()
        val fee = parse(input.fee, input.agreement.currency) ?: return invalidAmount()
        execute("Đã ghi gốc, lãi và phí theo từng thành phần", input.operationId) {
            repository.post(PostDebtMovement(input.id, workspace, input.agreement.id, input.agreement.counterpartyId,
                input.operationId, input.accountId, input.agreement.currency, input.action, principal, interest, fee,
                input.interestCategoryId.takeIf { interest > 0 }, input.feeCategoryId.takeIf { fee > 0 }, input.occurredAt ?: clock.instant()))
        }
    }
    override fun reverse(id: Uuid, operationId: Uuid, debtId: Uuid, movementId: Uuid, reason: String) {
        execute("Đã ghi bút toán đảo; lịch sử gốc được giữ lại", operationId) {
            repository.reverse(ReverseDebtMovement(id, workspace, debtId, movementId, operationId, clock.instant(), reason))
        }
    }
    private fun parse(text: String, code: CurrencyCode): Long? = runCatching {
        Money.parse(text.ifBlank { "0" }, code, Locale.forLanguageTag("vi-VN")).amountMinor.takeIf { it >= 0 }
    }.getOrNull()
    private fun invalidAmount() { mutable.value = mutable.value.copy(error = "Số tiền phải không âm và đúng định dạng.") }
    private fun execute(message: String, operation: Uuid?, block: suspend () -> Outcome<*>) {
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null, confirmation = null)
            mutable.value = when (val result = block()) {
                is Outcome.Success -> mutable.value.copy(busy = false, confirmation = message, savedOperationId = operation)
                is Outcome.Failure -> mutable.value.copy(busy = false, error = when (val error = result.error) {
                    AppError.Conflict -> "Lệnh đã ghi hoặc dữ liệu đã thay đổi. Kiểm tra lịch sử trước khi nhập lại."
                    AppError.NotFound -> "Không tìm thấy khoản vay trong hồ sơ này."
                    AppError.Storage -> "Không lưu được khoản vay. Có thể thử lại khi ngoại tuyến."
                    is AppError.InvalidInput -> when (error.field) {
                        "occurredAt" -> "Chọn thời gian không ở tương lai và không trước biến động đã ghi gần nhất."
                        "overpayment" -> "Khoản trả hoặc điều chỉnh vượt gốc còn lại."
                        "categoryId", "feeCategoryId" -> "Chọn nhóm lãi và phí riêng, đúng chiều thu/chi."
                        else -> "Dữ liệu chưa hợp lệ: ${error.field}. Kiểm tra số tiền, đối tác và tài khoản."
                    }
                })
            }
        }
    }
}
