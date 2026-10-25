package vn.lifeos.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerLocalProfileId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val originDeviceId: String,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "profiles",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true), Index(value = ["workspaceId", "updatedAt"])],
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val displayName: String,
    val zoneId: String,
    val baseCurrency: String,
    val budgetStartDay: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val originDeviceId: String,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "devices",
    foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId"), Index(value = ["workspaceId", "id"], unique = true)],
)
data class DeviceEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val platform: String,
    val appVersion: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val originDeviceId: String,
    val revokedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "integration_connections",
    foreignKeys = [
        ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DeviceEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "notificationDeviceId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("workspaceId"),
        Index(value = ["workspaceId", "id"], unique = true),
        Index(value = ["workspaceId", "provider", "accountKey", "tenantKey"], unique = true),
        Index(value = ["workspaceId", "state"]),
        Index(value = ["workspaceId", "notificationDeviceId"]),
    ],
)
data class IntegrationConnectionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val provider: String,
    val externalAccountId: String? = null,
    val tenantId: String? = null,
    val accountKey: String = externalAccountId ?: "",
    val tenantKey: String = tenantId ?: "",
    val scopes: String,
    val configVersion: Int,
    val state: String,
    val lastAttemptAt: Long? = null,
    val lastSuccessfulAt: Long? = null,
    val notificationDeviceId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val originDeviceId: String,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "sync_checkpoints",
    foreignKeys = [ForeignKey(entity = IntegrationConnectionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "connectionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "connectionId"]), Index(value = ["connectionId", "resourceKey"], unique = true)],
)
data class SyncCheckpointEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val connectionId: String,
    val resourceKey: String,
    val partialCursor: String? = null,
    val completedCursor: String? = null,
    val coveredFrom: Long? = null,
    val coveredTo: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val originDeviceId: String,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "audit_events",
    foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "happenedAt"])],
)
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val actorType: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val happenedAt: Long,
    val operationId: String? = null,
)

@Entity(
    tableName = "privacy_preferences",
    primaryKeys = ["workspaceId", "sourceKind"],
    foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("workspaceId")],
)
data class PrivacyPreferenceEntity(
    val workspaceId: String,
    val sourceKind: String,
    val allowRemoteProcessing: Boolean,
    val cachePolicy: String,
    val rawRetentionDays: Int,
)

/** Raw content is nullable and exists only in the encrypted DB after explicit diagnostic opt-in. */
@Entity(
    tableName = "source_captures",
    foreignKeys = [ForeignKey(entity = IntegrationConnectionEntity::class, parentColumns = ["workspaceId", "id"], childColumns = ["workspaceId", "connectionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["workspaceId", "connectionId"]), Index(value = ["workspaceId", "sourceKind"]), Index(value = ["workspaceId", "connectionId", "sourceEventId"], unique = true), Index("rawExpiresAt")],
)
data class SourceCaptureEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val connectionId: String,
    val sourceKind: String,
    val sourceEventId: String,
    val processingState: String,
    val rawContent: String?,
    val rawExpiresAt: Long?,
    val createdAt: Long,
)
