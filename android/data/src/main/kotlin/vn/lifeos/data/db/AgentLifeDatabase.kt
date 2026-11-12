package vn.lifeos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import vn.lifeos.data.planner.CalendarEventEntity
import vn.lifeos.data.planner.PlannerDao
import vn.lifeos.data.planner.ProjectEntity
import vn.lifeos.data.planner.RecurrenceExceptionEntity
import vn.lifeos.data.planner.ReminderEntity
import vn.lifeos.data.planner.TaskChecklistItemEntity
import vn.lifeos.data.planner.TaskEntity
import vn.lifeos.data.reminders.ReminderDao
import vn.lifeos.data.finance.*
import vn.lifeos.data.finance.imports.FinanceInboxDao
import vn.lifeos.data.finance.goals.SavingsGoalEntity
import vn.lifeos.data.finance.goals.SavingsGoalAccountEntity
import vn.lifeos.data.finance.goals.SavingsGoalDao
import vn.lifeos.data.finance.debt.DebtAgreementEntity
import vn.lifeos.data.finance.debt.DebtMovementEntity
import vn.lifeos.data.finance.debt.DebtDao

@Database(
    entities = [
        WorkspaceEntity::class, ProfileEntity::class, DeviceEntity::class,
        IntegrationConnectionEntity::class, SyncCheckpointEntity::class, AuditEventEntity::class,
        PrivacyPreferenceEntity::class, SourceCaptureEntity::class,
        ProjectEntity::class, TaskEntity::class, TaskChecklistItemEntity::class,
        CalendarEventEntity::class, RecurrenceExceptionEntity::class, ReminderEntity::class,
        AccountEntity::class, CategoryEntity::class, FinancialTransactionEntity::class, LedgerEntryEntity::class,
        SourceEvidenceEntity::class, ImportCandidateEntity::class, BudgetEntity::class, ReconciliationEntity::class,
        SavingsGoalEntity::class, SavingsGoalAccountEntity::class,
        DebtAgreementEntity::class, DebtMovementEntity::class,
        vn.lifeos.data.finance.scheduled.ScheduledTransactionEntity::class,
        vn.lifeos.data.finance.scheduled.PlannedTransactionEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AgentLifeDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao
    abstract fun privacyDao(): PrivacyDao
    abstract fun plannerDao(): PlannerDao
    abstract fun reminderDao(): ReminderDao
    abstract fun financeDao(): FinanceDao
    abstract fun financeInboxDao(): FinanceInboxDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun debtDao(): DebtDao
    abstract fun scheduledDao(): vn.lifeos.data.finance.scheduled.ScheduledDao
    abstract fun reconciliationDao(): vn.lifeos.data.finance.reconciliation.ReconciliationDao
}
