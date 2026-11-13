package vn.lifeos.data.notifications

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import vn.lifeos.data.db.DeviceEntity
import vn.lifeos.data.db.IntegrationConnectionEntity

@Entity(tableName = "notification_apps", primaryKeys = ["workspaceId", "deviceId", "packageName"], foreignKeys = [
    ForeignKey(entity = DeviceEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "deviceId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = IntegrationConnectionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "connectionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "connectionId"])])
data class NotificationAppEntity(val workspaceId: String, val deviceId: String, val packageName: String,
    val connectionId: String, val enabled: Boolean)

@Entity(tableName = "notification_captures", foreignKeys = [
    ForeignKey(entity = IntegrationConnectionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "connectionId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = DeviceEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "deviceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "connectionId"]), Index(value = ["workspaceId", "deviceId"]),
        Index(value = ["workspaceId", "deviceId", "notificationKey", "postTime", "contentHash"], unique = true), Index("payloadExpiresAt")])
data class NotificationCaptureEntity(@PrimaryKey val id: String, val workspaceId: String, val connectionId: String,
    val deviceId: String, val packageName: String, val notificationKey: String, val postTime: Long,
    val contentHash: String, val revision: Int, val processingState: String, val receivedAt: Long,
    val title: String?, val body: String?, val contentUnavailable: Boolean, val payloadExpiresAt: Long?)

data class NotificationGateRow(val workspaceId: String, val deviceId: String, val packageName: String,
    val connectionId: String, val state: String)
