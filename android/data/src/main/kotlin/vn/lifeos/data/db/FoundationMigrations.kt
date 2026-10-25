package vn.lifeos.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Every production database opener must register this migration; destructive fallback is forbidden. */
object FoundationMigrations {
    val from1To2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_profiles_workspaceId_updatedAt` ON `profiles` (`workspaceId`, `updatedAt`)")
        }
    }
    val from2To3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `privacy_preferences` (`workspaceId` TEXT NOT NULL, `sourceKind` TEXT NOT NULL, `allowRemoteProcessing` INTEGER NOT NULL, `cachePolicy` TEXT NOT NULL, `rawRetentionDays` INTEGER NOT NULL, PRIMARY KEY(`workspaceId`, `sourceKind`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_privacy_preferences_workspaceId` ON `privacy_preferences` (`workspaceId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `source_captures` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `connectionId` TEXT NOT NULL, `sourceKind` TEXT NOT NULL, `sourceEventId` TEXT NOT NULL, `processingState` TEXT NOT NULL, `rawContent` TEXT, `rawExpiresAt` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `connectionId`) REFERENCES `integration_connections`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_captures_workspaceId_connectionId` ON `source_captures` (`workspaceId`, `connectionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_captures_workspaceId_sourceKind` ON `source_captures` (`workspaceId`, `sourceKind`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_source_captures_workspaceId_connectionId_sourceEventId` ON `source_captures` (`workspaceId`, `connectionId`, `sourceEventId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_captures_rawExpiresAt` ON `source_captures` (`rawExpiresAt`)")
        }
    }
}
