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

@Database(
    entities = [
        WorkspaceEntity::class, ProfileEntity::class, DeviceEntity::class,
        IntegrationConnectionEntity::class, SyncCheckpointEntity::class, AuditEventEntity::class,
        PrivacyPreferenceEntity::class, SourceCaptureEntity::class,
        ProjectEntity::class, TaskEntity::class, TaskChecklistItemEntity::class,
        CalendarEventEntity::class, RecurrenceExceptionEntity::class, ReminderEntity::class,
        AccountEntity::class, CategoryEntity::class, FinancialTransactionEntity::class, LedgerEntryEntity::class,
        SourceEvidenceEntity::class, ImportCandidateEntity::class, BudgetEntity::class, ReconciliationEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AgentLifeDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao
    abstract fun privacyDao(): PrivacyDao
    abstract fun plannerDao(): PlannerDao
    abstract fun reminderDao(): ReminderDao
    abstract fun financeDao(): FinanceDao
}
