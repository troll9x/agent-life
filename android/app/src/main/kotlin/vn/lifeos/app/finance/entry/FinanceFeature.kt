package vn.lifeos.app.finance.entry

import android.content.Context
import java.time.Clock
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import vn.lifeos.domain.common.CurrencyCode
import vn.lifeos.domain.common.IdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.*
import vn.lifeos.domain.finance.commands.ManualEntryInput
import vn.lifeos.domain.finance.commands.ManualFinanceCommands
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

internal data class FinanceUiState(
    val loading: Boolean = true, val loadFailed: Boolean = false, val busy: Boolean = false,
    val error: String? = null, val confirmation: String? = null, val actionVersion: Long = 0,
    val accounts: List<Account> = emptyList(), val categories: List<Category> = emptyList(),
    val transactions: List<PostedFinanceRow> = emptyList(), val balances: Map<Uuid, Long> = emptyMap(),
    val defaultWalletId: Uuid? = null,
)

internal interface FinanceActions {
    fun createAccount(name: String, kind: AccountKind, openingText: String, openingAsOf: Instant)
    fun createCategory(name: String, direction: CategoryDirection)
    fun postManual(input: ManualEntryInput)
    fun setDefaultWallet(accountId: Uuid)
    fun clearFinanceError()
    fun retryFinance()
}

internal class DefaultWalletPreference(context: Context) {
    private val preferences = context.getSharedPreferences("finance_wallet", Context.MODE_PRIVATE)
    fun get(workspace: Uuid): Uuid? = preferences.getString(workspace.value, null)?.let { runCatching { Uuid.parse(it) }.getOrNull() }
    fun set(workspace: Uuid, account: Uuid) { preferences.edit().putString(workspace.value, account.value).apply() }
}

/** ViewModel-owned coordinator. Compose only sees state and commands. */
internal class FinanceFeature(private val repository: FinanceRepository, private val workspaceId: Uuid,
    private val currency: CurrencyCode, private val ids: IdGenerator, private val clock: Clock,
    private val walletPreference: DefaultWalletPreference, private val scope: CoroutineScope,
) : FinanceActions {
    private val commands = ManualFinanceCommands(repository, Locale.forLanguageTag("vi-VN"))
    private val mutable = MutableStateFlow(FinanceUiState())
    val state: StateFlow<FinanceUiState> = mutable.asStateFlow()
    private var observeJob: Job? = null
    private var action: Job? = null

    init { retryFinance() }

    override fun retryFinance() {
        observeJob?.cancel()
        mutable.value = mutable.value.copy(loading = true, loadFailed = false, error = null)
        observeJob = scope.launch {
            combine(repository.observeAccounts(workspaceId), repository.observeCategories(workspaceId),
                repository.observeRecentTransactions(workspaceId)) { accounts, categories, transactions ->
                Triple(accounts, categories, transactions)
            }.collect { (accountsResult, categoriesResult, transactionsResult) ->
                val accounts = (accountsResult as? Outcome.Success)?.value
                val categories = (categoriesResult as? Outcome.Success)?.value
                val transactions = (transactionsResult as? Outcome.Success)?.value
                if (accounts == null || categories == null || transactions == null) {
                    mutable.value = mutable.value.copy(loading = false, loadFailed = true,
                        error = "Không đọc được sổ tài chính.")
                } else {
                    val balances = mutableMapOf<Uuid, Long>()
                    var failed = false
                    accounts.forEach { account ->
                        when (val balance = repository.balance(workspaceId, account.id, clock.instant())) {
                            is Outcome.Success -> balances[account.id] = balance.value.amountMinor
                            is Outcome.Failure -> failed = true
                        }
                    }
                    val selected = walletPreference.get(workspaceId)?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull { it.kind == AccountKind.CASH }?.id
                    mutable.value = mutable.value.copy(loading = false, loadFailed = failed,
                        error = if (failed) "Không tính được số dư tài khoản." else null,
                        accounts = accounts, categories = categories, transactions = transactions,
                        balances = balances, defaultWalletId = selected)
                }
            }
        }
    }

    override fun createAccount(name: String, kind: AccountKind, openingText: String, openingAsOf: Instant) = submit("Đã tạo tài khoản") {
        commands.createAccount(ids.newId(), workspaceId, name, kind, currency, openingText, openingAsOf)
    }

    override fun createCategory(name: String, direction: CategoryDirection) = submit("Đã tạo nhóm") {
        commands.createCategory(ids.newId(), workspaceId, name, direction)
    }

    override fun postManual(input: ManualEntryInput) {
        if (input.workspaceId != workspaceId) { mutable.value = mutable.value.copy(error = "Workspace không khớp."); return }
        submit("Đã ghi giao dịch", input.transactionId) { commands.post(input) }
    }

    override fun setDefaultWallet(accountId: Uuid) {
        if (mutable.value.accounts.none { it.id == accountId && it.kind == AccountKind.CASH }) return
        walletPreference.set(workspaceId, accountId)
        mutable.value = mutable.value.copy(defaultWalletId = accountId)
    }

    override fun clearFinanceError() { mutable.value = mutable.value.copy(error = null, confirmation = null) }

    private fun submit(message: String, transactionId: Uuid? = null, operation: suspend () -> Outcome<*>) {
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null, confirmation = null)
            val result = try { operation() } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { Outcome.Failure(AppError.Storage) }
            mutable.value = when (result) {
                is Outcome.Success -> mutable.value.copy(busy = false, confirmation = message,
                    actionVersion = mutable.value.actionVersion + 1)
                is Outcome.Failure -> mutable.value.copy(busy = false,
                    error = when (val error = result.error) {
                        is AppError.InvalidInput -> "Dữ liệu chưa hợp lệ: ${error.field}."
                        AppError.Conflict -> if (transactionId != null && mutable.value.transactions.any { it.transaction.id == transactionId })
                            "Giao dịch này đã được lưu." else "Dữ liệu đã thay đổi hoặc thao tác đã được gửi. Kiểm tra danh sách trước khi thử lại."
                        AppError.NotFound -> "Tài khoản hoặc nhóm không còn tồn tại."
                        AppError.Storage -> "Không lưu được dữ liệu. Thử lại."
                    })
            }
        }
    }
}
