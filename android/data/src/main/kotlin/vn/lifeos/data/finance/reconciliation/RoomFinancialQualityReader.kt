package vn.lifeos.data.finance.reconciliation

import java.time.Clock
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.domain.common.SourceState
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.finance.LedgerRules
import vn.lifeos.domain.finance.reconciliation.FinancialDataQuality
import vn.lifeos.domain.finance.reconciliation.FinanceSourceQuality
import vn.lifeos.domain.finance.reports.ReportFilter

/** Called inside the same Room read transaction as the report facts. Unknown coverage stays unknown. */
class RoomFinancialQualityReader(private val database: AgentLifeDatabase, private val clock: Clock) {
    suspend fun load(filter: ReportFilter): FinancialDataQuality {
        val dao = database.reconciliationDao()
        val ledger = database.financeDao()
        val cutoff = minOf(clock.millis(), Math.subtractExact(filter.endExclusive.toEpochMilli(), 1))
        val accounts = dao.accounts(filter.workspaceId.value, filter.currency.value, filter.accountId?.value, cutoff)
        val snapshots = dao.snapshots(filter.workspaceId.value).filter { it.asOfInstant <= cutoff }
        val unreconciled = accounts.filter { account ->
            val last = snapshots.firstOrNull { it.accountId == account.id }
            if (last == null || last.state != "RESOLVED") true
            else {
                val atSnapshot = LedgerRules.balance(account.openingBalanceMinor, ledger.entriesForBalance(filter.workspaceId.value,
                    account.id, account.openingAsOf, last.asOfInstant).map { it.deltaMinor })
                atSnapshot != last.observedBalanceMinor || ledger.entriesForBalance(filter.workspaceId.value,
                    account.id, last.asOfInstant, cutoff).isNotEmpty()
            }
        }.map { Uuid.parse(it.id) }.toSet()
        val pending = dao.periodCandidates(filter.workspaceId.value, filter.currency.value, filter.accountId?.value,
            filter.start.toEpochMilli(), filter.endExclusive.toEpochMilli()).size
        val sources = dao.sources(filter.workspaceId.value).map {
            FinanceSourceQuality(it.provider, runCatching { SourceState.valueOf(it.state) }.getOrDefault(SourceState.ERROR))
        }
        // A connected mailbox/notification listener alone cannot establish bank-account/period coverage.
        return FinancialDataQuality(unreconciled, pending, sources, sourceCoverageKnown = false)
    }
}
