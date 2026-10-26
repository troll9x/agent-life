package vn.lifeos.data.planner

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PlannerMigration {
    val from3To4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `title` TEXT NOT NULL, `color` TEXT, `archivedAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_workspaceId` ON `projects` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_projects_workspaceId_id` ON `projects` (`workspaceId`, `id`)")

            db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `projectId` TEXT, `title` TEXT NOT NULL, `note` TEXT, `state` TEXT NOT NULL, `priority` INTEGER NOT NULL, `dueAt` INTEGER, `completedAt` INTEGER, `recurrence` TEXT, `sourceProposalId` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `projectId`) REFERENCES `projects`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_workspaceId` ON `tasks` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_workspaceId_id` ON `tasks` (`workspaceId`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_workspaceId_projectId` ON `tasks` (`workspaceId`, `projectId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_workspaceId_state_dueAt` ON `tasks` (`workspaceId`, `state`, `dueAt`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_workspaceId_sourceProposalId` ON `tasks` (`workspaceId`, `sourceProposalId`)")

            db.execSQL("CREATE TABLE IF NOT EXISTS `task_checklist_items` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `taskId` TEXT NOT NULL, `title` TEXT NOT NULL, `done` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`, `taskId`) REFERENCES `tasks`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_checklist_items_workspaceId_taskId_sortOrder` ON `task_checklist_items` (`workspaceId`, `taskId`, `sortOrder`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_checklist_items_workspaceId_taskId_id` ON `task_checklist_items` (`workspaceId`, `taskId`, `id`)")

            db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_events` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `sourceConnectionId` TEXT, `externalId` TEXT, `title` TEXT NOT NULL, `timeKind` TEXT NOT NULL, `startAt` INTEGER, `endAt` INTEGER, `startDate` TEXT, `endDateExclusive` TEXT, `eventZoneId` TEXT NOT NULL, `recurrence` TEXT, `sourceWebLink` TEXT, `readonly` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`workspaceId`, `sourceConnectionId`) REFERENCES `integration_connections`(`workspaceId`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_workspaceId` ON `calendar_events` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calendar_events_workspaceId_id` ON `calendar_events` (`workspaceId`, `id`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calendar_events_workspaceId_sourceConnectionId_externalId` ON `calendar_events` (`workspaceId`, `sourceConnectionId`, `externalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_workspaceId_startDate_endDateExclusive` ON `calendar_events` (`workspaceId`, `startDate`, `endDateExclusive`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_workspaceId_startAt_endAt` ON `calendar_events` (`workspaceId`, `startAt`, `endAt`)")

            db.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_exceptions` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `seriesId` TEXT NOT NULL, `originalOccurrenceKey` TEXT NOT NULL, `overridePayload` TEXT, `cancelled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_exceptions_workspaceId` ON `recurrence_exceptions` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_exceptions_workspaceId_seriesId_originalOccurrenceKey` ON `recurrence_exceptions` (`workspaceId`, `seriesId`, `originalOccurrenceKey`)")

            db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `occurrenceKey` TEXT NOT NULL, `slot` TEXT NOT NULL, `triggerAt` INTEGER NOT NULL, `state` TEXT NOT NULL, `platformRequestId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `originDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_workspaceId` ON `reminders` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminders_workspaceId_targetType_targetId_occurrenceKey_slot` ON `reminders` (`workspaceId`, `targetType`, `targetId`, `occurrenceKey`, `slot`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_workspaceId_state_triggerAt` ON `reminders` (`workspaceId`, `state`, `triggerAt`)")
        }
    }
}
