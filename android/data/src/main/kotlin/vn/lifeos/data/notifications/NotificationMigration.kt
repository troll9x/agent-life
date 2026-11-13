package vn.lifeos.data.notifications

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NotificationMigration {
    val from8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `notification_apps` (`workspaceId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `packageName` TEXT NOT NULL, `connectionId` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`workspaceId`, `deviceId`, `packageName`), FOREIGN KEY(`workspaceId`, `deviceId`) REFERENCES `devices`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `connectionId`) REFERENCES `integration_connections`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_apps_workspaceId_connectionId` ON `notification_apps` (`workspaceId`, `connectionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `notification_captures` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `connectionId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `packageName` TEXT NOT NULL, `notificationKey` TEXT NOT NULL, `postTime` INTEGER NOT NULL, `contentHash` TEXT NOT NULL, `revision` INTEGER NOT NULL, `processingState` TEXT NOT NULL, `receivedAt` INTEGER NOT NULL, `title` TEXT, `body` TEXT, `contentUnavailable` INTEGER NOT NULL, `payloadExpiresAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `connectionId`) REFERENCES `integration_connections`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `deviceId`) REFERENCES `devices`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_captures_workspaceId_connectionId` ON `notification_captures` (`workspaceId`, `connectionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_captures_workspaceId_deviceId` ON `notification_captures` (`workspaceId`, `deviceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notification_captures_workspaceId_deviceId_notificationKey_postTime_contentHash` ON `notification_captures` (`workspaceId`, `deviceId`, `notificationKey`, `postTime`, `contentHash`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_captures_payloadExpiresAt` ON `notification_captures` (`payloadExpiresAt`)")
        }
    }
}
