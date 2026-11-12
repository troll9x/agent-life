package vn.lifeos.app.finance.entry

import android.content.Context
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
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
import vn.lifeos.domain.finance.reversal.ReplacePostedTransaction
import vn.lifeos.domain.finance.reversal.ReversalRepository
import vn.lifeos.domain.finance.refund.ManualRefundAdjustmentCommands
import vn.lifeos.domain.finance.refund.ManualRefundInput
import vn.lifeos.domain.finance.refund.ManualAdjustmentInput
import vn.lifeos.domain.finance.reports.*
import vn.lifeos.domain.finance.imports.*
import vn.lifeos.domain.finance.goals.*
import vn.lifeos.domain.finance.debt.DebtRepository
import vn.lifeos.app.finance.debt.DebtActions
import vn.lifeos.app.finance.debt.DebtFeature
import vn.lifeos.app.finance.debt.DebtUiState
import java.time.LocalDate
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.Outcome

internal data class FinanceUiState(
    val loading: Boolean = true, val loadFailed: Boolean = false, val busy: Boolean = false,
    val error: String? = null, val confirmation: String? = null, val actionVersion: Long = 0,
    val accounts: List<Account> = emptyList(), val categories: List<Category> = emptyList(),
    val transactions: List<PostedFinanceRow> = emptyList(), val balances: Map<Uuid, Long> = emptyMap(),
    val defaultWalletId: Uuid? = null, val completedReplacementId: Uuid? = null,
    val monthlyReports: Map<YearMonth, FinanceReport> = emptyMap(),
    val report: ReportSnapshot? = null, val previousReport: ReportSnapshot? = null,
    val reportLoading: Boolean = false, val reportError: String? = null,
    val reportPeriod: ReportPeriod? = null, val reportAccountId: Uuid? = null,
    val inboxLoading: Boolean = false, val inboxBusy: Boolean = false,
    val inboxError: String? = null, val inboxItems: List<InboxItem> = emptyList(),
    val goalsLoading: Boolean = false, val goalsBusy: Boolean = false, val goalsError: String? = null,
    val savingsGoals: List<SavingsGoal> = emptyList(), val goalOverview: GoalOverview? = null,
    val debt: DebtUiState = DebtUiState(),
    val scheduled: vn.lifeos.app.finance.scheduled.ScheduledUiState = vn.lifeos.app.finance.scheduled.ScheduledUiState(),
)

internal interface FinanceActions {
    val scheduledActions: vn.lifeos.app.finance.scheduled.ScheduledActions? get() = null
    val debtActions: DebtActions? get() = null
    fun createAccount(name: String, kind: AccountKind, openingText: String, openingAsOf: Instant)
    fun createCategory(name: String, direction: CategoryDirection)
    fun postManual(input: ManualEntryInput)
    fun replacePosted(command: ReplacePostedTransaction)
    fun postRefund(input: ManualRefundInput)
    fun postAdjustment(input: ManualAdjustmentInput)
    fun setDefaultWallet(accountId: Uuid)
    fun clearFinanceError()
    fun retryFinance()
    fun selectReportPeriod(monthOffset: Long)
    fun selectReportAccount(accountId: Uuid?)
    fun saveReportBudget(categoryId: Uuid, amountText: String, thresholdEnabled: Boolean)
    fun retryReport()
    fun retryInbox()
    fun approveInbox(candidateId: Uuid, accountId: Uuid, categoryId: Uuid)
    fun linkInbox(candidateId: Uuid, transactionId: Uuid)
    fun ignoreInbox(candidateId: Uuid)
    fun retrySavingsGoals()
    fun createSavingsGoal(name: String, targetText: String, targetDate: LocalDate?, accountIds: Set<Uuid>)
    fun changeSavingsGoalTarget(goalId: Uuid, targetText: String, targetDate: LocalDate?)
    fun archiveSavingsGoal(goalId: Uuid)
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
    private val reversals: ReversalRepository? = null, private var zone: ZoneId = ZoneId.systemDefault(),
    private val reports: FinanceReportsRepository? = null, private var budgetStartDay: Int = 1,
    private val inbox: FinanceInboxRepository? = null,
    private val goals: SavingsGoalRepository? = null,
    debtRepository: DebtRepository? = null,
    scheduledRepository: vn.lifeos.domain.finance.scheduled.ScheduledFinanceRepository? = null,
    rescheduleFinance: () -> Unit = {},
) : FinanceActions {
    private val commands = ManualFinanceCommands(repository, Locale.forLanguageTag("vi-VN"))
    private val corrections = ManualRefundAdjustmentCommands(repository, Locale.forLanguageTag("vi-VN"))
    private val mutable = MutableStateFlow(FinanceUiState())
    val state: StateFlow<FinanceUiState> = mutable.asStateFlow()
    private var observeJob: Job? = null
    private var action: Job? = null
    private var reportJob: Job? = null
    private var inboxJob: Job? = null
    private var inboxAction: Job? = null
    private var goalsJob: Job? = null
    private var goalsAction: Job? = null
    override val debtActions = debtRepository?.let { DebtFeature(it, workspaceId, currency, clock, scope) }
    override val scheduledActions = scheduledRepository?.let {
        vn.lifeos.app.finance.scheduled.ScheduledFeature(it, workspaceId, zone, clock, scope, rescheduleFinance)
    }

    init {
        scheduledActions?.let { feature -> scope.launch {
            feature.state.collect { mutable.value = mutable.value.copy(scheduled = it) }
        } }
        retryFinance(); retryInbox(); retrySavingsGoals()
        debtActions?.let { feature -> scope.launch {
            feature.state.collect { mutable.value = mutable.value.copy(debt = it) }
        } }
    }

    override fun retrySavingsGoals() {
        val source = goals ?: return
        goalsJob?.cancel()
        mutable.value = mutable.value.copy(goalsLoading = true, goalsError = null)
        goalsJob = scope.launch {
            source.observe(workspaceId).collect { result ->
                mutable.value = when (result) {
                    is Outcome.Success -> mutable.value.copy(goalsLoading = false, savingsGoals = result.value, goalsError = null)
                    is Outcome.Failure -> mutable.value.copy(goalsLoading = false, goalsError = "Không đọc được mục tiêu tiết kiệm.")
                }
                refreshGoalOverview()
            }
        }
    }

    private fun refreshGoalOverview() {
        val current = mutable.value
        if (current.loading || current.loadFailed) return
        val overview = runCatching { SavingsGoalProgress.calculate(current.savingsGoals, current.accounts, current.balances) }
        mutable.value = if (overview.isSuccess) current.copy(goalOverview = overview.getOrThrow())
            else current.copy(goalOverview = null, goalsError = "Không tính được tiến độ từ số dư tài khoản.")
    }

    override fun createSavingsGoal(name: String, targetText: String, targetDate: LocalDate?, accountIds: Set<Uuid>) {
        val accounts = mutable.value.accounts.filter { it.id in accountIds }
        if (accounts.size != accountIds.size || accounts.isEmpty() || accounts.any { it.kind != AccountKind.SAVINGS } ||
            accounts.map { it.currency }.distinct().size != 1) return goalError("Chọn tài khoản tiết kiệm cùng tiền tệ.")
        val currency = accounts.first().currency
        val amount = parseGoalAmount(targetText, currency) ?: return goalError("Mục tiêu phải là số tiền dương.")
        goalAction("Đã tạo mục tiêu") { goals!!.create(SavingsGoal(ids.newId(), workspaceId, name,
            currency, amount, targetDate, accountIds)) }
    }

    override fun changeSavingsGoalTarget(goalId: Uuid, targetText: String, targetDate: LocalDate?) {
        val goal = mutable.value.savingsGoals.firstOrNull { it.id == goalId } ?: return
        val amount = parseGoalAmount(targetText, goal.currency) ?: return goalError("Mục tiêu phải là số tiền dương.")
        goalAction("Đã đổi mục tiêu") { goals!!.updateTarget(workspaceId, goalId, amount, targetDate) }
    }

    override fun archiveSavingsGoal(goalId: Uuid) {
        goalAction("Đã lưu trữ mục tiêu") { goals!!.archive(workspaceId, goalId) }
    }

    private fun parseGoalAmount(text: String, currency: CurrencyCode): Long? = runCatching {
        vn.lifeos.domain.common.Money.parse(text, currency, Locale.forLanguageTag("vi-VN"))
            .amountMinor.takeIf { it > 0 }
    }.getOrNull()
    private fun goalError(message: String) { mutable.value = mutable.value.copy(goalsError = message) }
    private fun goalAction(message: String, operation: suspend () -> Outcome<SavingsGoal>) {
        if (goals == null || goalsAction?.isActive == true) return
        goalsAction = scope.launch {
            mutable.value = mutable.value.copy(goalsBusy = true, goalsError = null)
            val result = operation()
            mutable.value = when (result) {
                is Outcome.Success -> mutable.value.copy(goalsBusy = false, confirmation = message)
                is Outcome.Failure -> mutable.value.copy(goalsBusy = false, goalsError = when (val error = result.error) {
                    is AppError.InvalidInput -> "Mục tiêu chưa hợp lệ: ${error.field}."
                    AppError.Conflict -> "Mục tiêu đã thay đổi hoặc đã lưu trữ."
                    AppError.NotFound -> "Không tìm thấy tài khoản hoặc mục tiêu."
                    AppError.Storage -> "Không lưu được mục tiêu. Thử lại."
                })
            }
        }
    }

    override fun retryInbox() {
        val source = inbox ?: return
        inboxJob?.cancel()
        mutable.value = mutable.value.copy(inboxLoading = true, inboxError = null)
        inboxJob = scope.launch {
            source.observe(workspaceId).collect { result ->
                mutable.value = when (result) {
                    is Outcome.Success -> mutable.value.copy(inboxLoading = false, inboxError = null, inboxItems = result.value)
                    is Outcome.Failure -> mutable.value.copy(inboxLoading = false, inboxError = "Không đọc được hàng chờ nhập.")
                }
            }
        }
    }

    override fun approveInbox(candidateId: Uuid, accountId: Uuid, categoryId: Uuid) {
        val item = mutable.value.inboxItems.firstOrNull { it.candidate.id == candidateId } ?: return
        val candidate = item.candidate
        val amount = candidate.amountMinor ?: return inboxError("Thiếu số tiền cần kiểm tra.")
        val currency = candidate.currency ?: return inboxError("Thiếu tiền tệ cần kiểm tra.")
        val occurred = candidate.occurredAt ?: return inboxError("Thiếu thời gian giao dịch.")
        val kind = when (candidate.direction) {
            CategoryDirection.INCOME -> TransactionKind.INCOME
            CategoryDirection.EXPENSE -> TransactionKind.EXPENSE
            null -> return inboxError("Thiếu chiều thu/chi.")
        }
        val command = PostFinanceCommand(ids.newId(), workspaceId, ids.newId(), kind, accountId,
            vn.lifeos.domain.common.Money(amount, currency), occurred, categoryId = categoryId)
        decideInbox(candidateId, InboxDecision.Approve(command))
    }

    override fun linkInbox(candidateId: Uuid, transactionId: Uuid) =
        decideInbox(candidateId, InboxDecision.Duplicate(transactionId))
    override fun ignoreInbox(candidateId: Uuid) = decideInbox(candidateId, InboxDecision.Ignore)

    private fun inboxError(message: String) { mutable.value = mutable.value.copy(inboxError = message) }

    private fun decideInbox(candidateId: Uuid, decision: InboxDecision) {
        val source = inbox ?: return
        if (inboxAction?.isActive == true) return
        inboxAction = scope.launch {
            mutable.value = mutable.value.copy(inboxBusy = true, inboxError = null)
            val result = source.decide(workspaceId, candidateId, decision)
            mutable.value = when (result) {
                is Outcome.Success -> mutable.value.copy(inboxBusy = false)
                is Outcome.Failure -> mutable.value.copy(inboxBusy = false, inboxError = when (val error = result.error) {
                    AppError.Conflict -> "Ứng viên đã đổi hoặc reference đã ghi sổ. Tải lại và chọn ghép."
                    is AppError.InvalidInput -> "Dữ liệu hoặc giao dịch ghép không khớp: ${error.field}."
                    AppError.NotFound -> "Không tìm thấy ứng viên hoặc giao dịch."
                    AppError.Storage -> "Không lưu được quyết định. Thử lại."
                })
            }
        }
    }

    fun updateZone(value: ZoneId) {
        scheduledActions?.updateZone(value)
        if (zone == value) return
        zone = value
        retryFinance()
    }

    fun updateProfile(value: ZoneId, startDay: Int) {
        scheduledActions?.updateZone(value)
        if (zone == value && budgetStartDay == startDay) return
        zone = value; budgetStartDay = startDay
        mutable.value = mutable.value.copy(reportPeriod = null)
        retryFinance()
    }

    override fun selectReportPeriod(monthOffset: Long) {
        val period = mutable.value.reportPeriod ?: reportPeriodContaining(clock.instant().atZone(zone).toLocalDate(), budgetStartDay)
        mutable.value = mutable.value.copy(reportPeriod = ReportPeriod(period.start.plusMonths(monthOffset), period.endExclusive.plusMonths(monthOffset)))
        retryReport()
    }

    override fun selectReportAccount(accountId: Uuid?) {
        mutable.value = mutable.value.copy(reportAccountId = accountId)
        retryReport()
    }

    override fun retryReport() {
        val source = reports ?: return
        reportJob?.cancel()
        val period = mutable.value.reportPeriod ?: reportPeriodContaining(clock.instant().atZone(zone).toLocalDate(), budgetStartDay)
        val filter = ReportFilter(workspaceId, currency, period, zone, mutable.value.reportAccountId)
        val previousFilter = filter.copy(period = period.previousComparable(clock.instant().atZone(zone).toLocalDate()))
        mutable.value = mutable.value.copy(reportLoading = true, reportError = null, reportPeriod = period,
            report = null, previousReport = null)
        reportJob = scope.launch {
            val current = source.load(filter)
            val previous = source.load(previousFilter)
            mutable.value = if (current is Outcome.Success && previous is Outcome.Success) mutable.value.copy(
                reportLoading = false, report = FinanceReportUseCase.calculate(filter, current.value),
                previousReport = FinanceReportUseCase.calculate(previousFilter, previous.value))
            else mutable.value.copy(reportLoading = false, reportError = "Không đọc được báo cáo. Thử lại.")
        }
    }

    override fun saveReportBudget(categoryId: Uuid, amountText: String, thresholdEnabled: Boolean) {
        val source = reports ?: return
        val period = mutable.value.reportPeriod ?: return
        val amount = runCatching { vn.lifeos.domain.common.Money.parse(amountText, currency, Locale.forLanguageTag("vi-VN")) }.getOrNull()
        if (amount == null || amount.amountMinor < 0) {
            mutable.value = mutable.value.copy(reportError = "Ngân sách phải là số tiền không âm."); return
        }
        scope.launch {
            val result = source.saveBudget(Budget(ids.newId(), workspaceId, categoryId, period.start,
                currency, amount.amountMinor, if (thresholdEnabled) 8_000 else null))
            if (result is Outcome.Success) retryReport()
            else mutable.value = mutable.value.copy(reportError = "Không lưu được ngân sách. Thử lại.")
        }
    }

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
                    val months = (transactions.map { YearMonth.from(it.transaction.occurredAt.atZone(zone)) } +
                        YearMonth.from(clock.instant().atZone(zone))).distinct().sortedDescending()
                    val monthly = mutableMapOf<YearMonth, FinanceReport>()
                    months.forEach { month ->
                        val start = month.atDay(1).atStartOfDay(zone).toInstant()
                        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
                        when (val report = repository.report(FinanceReportQuery(workspaceId, currency, start, end))) {
                            is Outcome.Success -> monthly[month] = report.value
                            is Outcome.Failure -> failed = true
                        }
                    }
                    val selected = walletPreference.get(workspaceId)?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull { it.kind == AccountKind.CASH }?.id
                    mutable.value = mutable.value.copy(loading = false, loadFailed = failed,
                        error = if (failed) "Không tính được số dư hoặc báo cáo tài chính." else null,
                        accounts = accounts, categories = categories, transactions = transactions,
                        balances = balances, defaultWalletId = selected, monthlyReports = monthly)
                    refreshGoalOverview()
                    retryReport()
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

    override fun postRefund(input: ManualRefundInput) {
        if (input.workspaceId != workspaceId) { mutable.value = mutable.value.copy(error = "Workspace không khớp."); return }
        submit("Đã ghi hoàn tiền", input.id) { corrections.refund(input) }
    }

    override fun postAdjustment(input: ManualAdjustmentInput) {
        if (input.workspaceId != workspaceId) { mutable.value = mutable.value.copy(error = "Workspace không khớp."); return }
        submit("Đã ghi điều chỉnh", input.id) { corrections.adjust(input) }
    }

    override fun replacePosted(command: ReplacePostedTransaction) {
        if (command.replacement.workspaceId != workspaceId) {
            mutable.value = mutable.value.copy(error = "Workspace không khớp."); return
        }
        val service = reversals ?: run {
            mutable.value = mutable.value.copy(error = "Không mở được lịch sử sửa giao dịch."); return
        }
        if (action?.isActive == true) return
        action = scope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null, confirmation = null)
            val result = try { service.replace(command) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { Outcome.Failure(AppError.Storage) }
            mutable.value = when (result) {
                is Outcome.Success -> mutable.value.copy(busy = false, confirmation = "Đã lưu bản sửa",
                    completedReplacementId = result.value.replacement.id,
                    actionVersion = mutable.value.actionVersion + 1)
                is Outcome.Failure -> mutable.value.copy(busy = false,
                    error = when (val error = result.error) {
                        AppError.Conflict -> "Giao dịch đã được sửa, có hoàn tiền hoặc dữ liệu đã đổi. Kiểm tra lịch sử trước khi thử lại."
                        is AppError.InvalidInput -> "Bản sửa chưa hợp lệ: ${error.field}."
                        AppError.NotFound -> "Không tìm thấy giao dịch gốc."
                        AppError.Storage -> "Không lưu được bản sửa. Thử lại."
                    })
            }
        }
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
