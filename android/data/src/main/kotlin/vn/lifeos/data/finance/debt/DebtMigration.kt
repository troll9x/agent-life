package vn.lifeos.data.finance.debt

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DebtMigration {
    val from6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `debt_agreements` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `role` TEXT NOT NULL, `counterpartyId` TEXT NOT NULL, `counterpartyLabel` TEXT NOT NULL, `currency` TEXT NOT NULL, `openingPrincipalMinor` INTEGER NOT NULL, `dueDate` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_agreements_workspaceId` ON `debt_agreements` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_debt_agreements_workspaceId_id` ON `debt_agreements` (`workspaceId`, `id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `debt_movements` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `debtId` TEXT NOT NULL, `operationId` TEXT NOT NULL, `transactionId` TEXT, `principalDeltaMinor` INTEGER NOT NULL, `interestTransactionId` TEXT, `feeTransactionId` TEXT, `interestMinor` INTEGER NOT NULL, `feeMinor` INTEGER NOT NULL, `occurredAt` INTEGER NOT NULL, `reversesMovementId` TEXT, `note` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `debtId`) REFERENCES `debt_agreements`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `transactionId`) REFERENCES `financial_transactions`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `interestTransactionId`) REFERENCES `financial_transactions`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `feeTransactionId`) REFERENCES `financial_transactions`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_movements_workspaceId_debtId` ON `debt_movements` (`workspaceId`, `debtId`)")
            listOf("operationId", "transactionId", "interestTransactionId", "feeTransactionId").forEach {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_debt_movements_workspaceId_$it` ON `debt_movements` (`workspaceId`, `$it`)")
            }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_debt_movements_reversesMovementId` ON `debt_movements` (`reversesMovementId`)")
        }
    }
}
