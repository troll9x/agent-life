package vn.lifeos.data.finance.scheduled

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ScheduledMigration {
    val from7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `scheduled_transactions` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `title` TEXT NOT NULL, `recurrence` TEXT NOT NULL, `anchorDate` TEXT NOT NULL, `nextDueDate` TEXT, `amountMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `kind` TEXT NOT NULL, `accountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `version` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `accountId`) REFERENCES `accounts`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `categoryId`) REFERENCES `categories`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scheduled_transactions_workspaceId_id` ON `scheduled_transactions` (`workspaceId`, `id`)")
            listOf("accountId", "categoryId").forEach { db.execSQL("CREATE INDEX IF NOT EXISTS `index_scheduled_transactions_workspaceId_$it` ON `scheduled_transactions` (`workspaceId`, `$it`)") }
            db.execSQL("CREATE TABLE IF NOT EXISTS `planned_transactions` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `ruleId` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `title` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `kind` TEXT NOT NULL, `accountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `state` TEXT NOT NULL, `transactionId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `ruleId`) REFERENCES `scheduled_transactions`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `transactionId`) REFERENCES `financial_transactions`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_transactions_workspaceId_ruleId_dueDate` ON `planned_transactions` (`workspaceId`, `ruleId`, `dueDate`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_transactions_workspaceId_transactionId` ON `planned_transactions` (`workspaceId`, `transactionId`)")
        }
    }
}
