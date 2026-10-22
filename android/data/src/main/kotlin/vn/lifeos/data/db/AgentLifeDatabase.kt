package vn.lifeos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkspaceEntity::class, ProfileEntity::class, DeviceEntity::class,
        IntegrationConnectionEntity::class, SyncCheckpointEntity::class, AuditEventEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AgentLifeDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao
}
