package vn.lifeos.data.finance.goals

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SavingsGoalMigration {
    val from5To6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `savings_goals` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `name` TEXT NOT NULL, `currency` TEXT NOT NULL, `targetMinor` INTEGER NOT NULL, `targetDate` TEXT, `archivedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_workspaceId` ON `savings_goals` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_savings_goals_workspaceId_id` ON `savings_goals` (`workspaceId`, `id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `savings_goal_accounts` (`goalId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, PRIMARY KEY(`goalId`, `accountId`), FOREIGN KEY(`workspaceId`, `goalId`) REFERENCES `savings_goals`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `accountId`) REFERENCES `accounts`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goal_accounts_workspaceId_goalId` ON `savings_goal_accounts` (`workspaceId`, `goalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goal_accounts_workspaceId_accountId` ON `savings_goal_accounts` (`workspaceId`, `accountId`)")
        }
    }
}
