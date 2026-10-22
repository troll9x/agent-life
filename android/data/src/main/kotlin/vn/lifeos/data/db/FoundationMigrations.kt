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
}
