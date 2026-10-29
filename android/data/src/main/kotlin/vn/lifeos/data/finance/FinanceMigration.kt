package vn.lifeos.data.finance

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive v4→v5 migration; existing planner and encrypted user data stay intact. */
object FinanceMigration {
    val from4To5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `currency` TEXT NOT NULL, `openingBalanceMinor` INTEGER NOT NULL, `openingAsOf` INTEGER NOT NULL, `archivedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_workspaceId` ON `accounts` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_workspaceId_id` ON `accounts` (`workspaceId`, `id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `name` TEXT NOT NULL, `direction` TEXT NOT NULL, `parentId` TEXT, `archivedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_workspaceId` ON `categories` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_workspaceId_id` ON `categories` (`workspaceId`, `id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `financial_transactions` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `operationId` TEXT NOT NULL, `kind` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `postedAt` INTEGER NOT NULL, `categoryId` TEXT, `payee` TEXT, `note` TEXT, `reversesId` TEXT, `refundOfId` TEXT, `replacementOfId` TEXT, `debtId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_transactions_workspaceId` ON `financial_transactions` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_transactions_workspaceId_id` ON `financial_transactions` (`workspaceId`, `id`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_transactions_workspaceId_operationId` ON `financial_transactions` (`workspaceId`, `operationId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_transactions_reversesId` ON `financial_transactions` (`reversesId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_transactions_workspaceId_occurredAt` ON `financial_transactions` (`workspaceId`, `occurredAt`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `ledger_entries` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `transactionId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `deltaMinor` INTEGER NOT NULL, `currency` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `transactionId`) REFERENCES `financial_transactions`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `accountId`) REFERENCES `accounts`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_workspaceId_transactionId` ON `ledger_entries` (`workspaceId`, `transactionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_workspaceId_accountId` ON `ledger_entries` (`workspaceId`, `accountId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `source_evidence` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `connectionId` TEXT, `connectionKey` TEXT NOT NULL, `sourceKind` TEXT NOT NULL, `sourceEventId` TEXT NOT NULL, `revisionHash` TEXT, `revisionKey` TEXT NOT NULL, `bankReference` TEXT, `maskedAccount` TEXT, `observedAt` INTEGER NOT NULL, `transactionId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_evidence_workspaceId` ON `source_evidence` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_source_evidence_workspaceId_connectionKey_sourceEventId_revisionKey` ON `source_evidence` (`workspaceId`, `connectionKey`, `sourceEventId`, `revisionKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_evidence_workspaceId_transactionId` ON `source_evidence` (`workspaceId`, `transactionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `import_candidates` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `sourceEvidenceId` TEXT NOT NULL, `parserVersion` INTEGER NOT NULL, `amountMinor` INTEGER, `currency` TEXT, `direction` TEXT, `occurredAt` INTEGER, `proposedAccountId` TEXT, `proposedCategoryId` TEXT, `missingFields` TEXT NOT NULL, `confidenceBasisPoints` INTEGER NOT NULL, `state` TEXT NOT NULL, `matchedTransactionId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`sourceEvidenceId`) REFERENCES `source_evidence`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_import_candidates_workspaceId` ON `import_candidates` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_import_candidates_sourceEvidenceId` ON `import_candidates` (`sourceEvidenceId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `periodStartDate` TEXT NOT NULL, `currency` TEXT NOT NULL, `limitMinor` INTEGER NOT NULL, `alertThresholdBasisPoints` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `categoryId`) REFERENCES `categories`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_workspaceId_categoryId` ON `budgets` (`workspaceId`, `categoryId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_workspaceId_categoryId_periodStartDate_currency` ON `budgets` (`workspaceId`, `categoryId`, `periodStartDate`, `currency`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `reconciliations` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `asOfInstant` INTEGER NOT NULL, `observedBalanceMinor` INTEGER NOT NULL, `expectedBalanceMinor` INTEGER NOT NULL, `differenceMinor` INTEGER NOT NULL, `note` TEXT, `state` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `accountId`) REFERENCES `accounts`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reconciliations_workspaceId_accountId` ON `reconciliations` (`workspaceId`, `accountId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reconciliations_workspaceId_accountId_asOfInstant` ON `reconciliations` (`workspaceId`, `accountId`, `asOfInstant`)")
        }
    }
}
