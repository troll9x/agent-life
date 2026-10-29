package vn.lifeos.data.reminders

import androidx.room.withTransaction
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.planner.ReminderEntity
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.ReminderTarget

data class ReminderCandidate(val target: ReminderTarget, val targetId: Uuid, val occurrenceKey: String,
    val triggerAt: Instant, val slot: String = "DUE")
data class ReminderPlanResult(val schedule: List<ReminderEntity>, val cancelIds: List<String>)
data class ReminderCompletion(val targetId: String, val cancelledIds: List<String>)

/** All transitions are conditional on the encrypted Room row, so duplicate broadcasts are harmless. */
class RoomReminderStore(private val database: AgentLifeDatabase) {
    private val dao = database.reminderDao()

    suspend fun reconcile(workspaceId: Uuid, candidates: List<ReminderCandidate>, now: Instant): ReminderPlanResult =
        database.withTransaction {
            val existing = dao.all(workspaceId.value)
            val byKey = existing.associateBy { key(it.targetType, it.targetId, it.occurrenceKey, it.slot) }
            val cutoff = now.minusSeconds(300).toEpochMilli()
            val desired = candidates.filter { it.triggerAt.toEpochMilli() > cutoff }.associateBy {
                key(it.target.name, it.targetId.value, it.occurrenceKey, it.slot)
            }
            val cancelled = mutableListOf<String>()
            val deviceId = dao.deviceId(workspaceId.value) ?: return@withTransaction ReminderPlanResult(emptyList(), emptyList())
            val time = now.toEpochMilli()
            existing.filter { it.state == "PENDING" && it.slot == "DUE" &&
                key(it.targetType, it.targetId, it.occurrenceKey, it.slot) !in desired }.forEach { row ->
                dao.update(row.copy(state = "CANCELLED", updatedAt = time, version = row.version + 1))
                cancelled += row.id
            }
            desired.forEach { (identity, candidate) ->
                val before = byKey[identity]
                if (before == null) {
                    val id = stableId(workspaceId.value, identity)
                    dao.insert(ReminderEntity(id, workspaceId.value, candidate.target.name, candidate.targetId.value,
                        candidate.occurrenceKey, candidate.slot, candidate.triggerAt.toEpochMilli(), "PENDING",
                        id, time, time, 1, deviceId))
                } else if (before.triggerAt != candidate.triggerAt.toEpochMilli() || before.state == "CANCELLED") {
                    dao.update(before.copy(triggerAt = candidate.triggerAt.toEpochMilli(), state = "PENDING",
                        updatedAt = time, version = before.version + 1))
                }
            }
            existing.filter { it.state == "PENDING" && it.slot == "SNOOZE" }.forEach { row ->
                val active = if (row.targetType == "TASK") dao.task(workspaceId.value, row.targetId)?.state !in listOf(null, "DONE", "CANCELLED")
                    else dao.eventTitle(workspaceId.value, row.targetId) != null
                if (!active || row.triggerAt <= cutoff) {
                    dao.update(row.copy(state = "CANCELLED", updatedAt = time, version = row.version + 1))
                    cancelled += row.id
                }
            }
            ReminderPlanResult(dao.all(workspaceId.value).filter { it.state == "PENDING" && it.triggerAt > cutoff }, cancelled)
        }

    suspend fun claim(id: String, expectedTrigger: Long, now: Instant): ReminderEntity? = database.withTransaction {
        val row = dao.byId(id) ?: return@withTransaction null
        if (row.state != "PENDING" || row.triggerAt != expectedTrigger || row.triggerAt > now.toEpochMilli()) return@withTransaction null
        val active = if (row.targetType == "TASK") dao.task(row.workspaceId, row.targetId)?.state !in listOf(null, "DONE", "CANCELLED")
            else dao.eventTitle(row.workspaceId, row.targetId) != null
        dao.update(row.copy(state = if (active) "FIRED" else "CANCELLED", updatedAt = now.toEpochMilli(), version = row.version + 1))
        row.takeIf { active }
    }

    suspend fun snooze(id: String, now: Instant): ReminderEntity? = database.withTransaction {
        val fired = dao.byId(id) ?: return@withTransaction null
        if (fired.slot != "DUE" || fired.state != "FIRED") return@withTransaction null
        if (fired.targetType == "TASK" && dao.task(fired.workspaceId, fired.targetId)?.state in listOf(null, "DONE", "CANCELLED")) return@withTransaction null
        val identity = key(fired.targetType, fired.targetId, fired.occurrenceKey, "SNOOZE")
        val before = dao.all(fired.workspaceId).firstOrNull { key(it.targetType, it.targetId, it.occurrenceKey, it.slot) == identity }
        if (before != null) return@withTransaction before.takeIf { it.state == "PENDING" }
        val newId = stableId(fired.workspaceId, identity)
        val trigger = now.plusSeconds(600).toEpochMilli()
        val row = fired.copy(id = newId, slot = "SNOOZE", triggerAt = trigger, state = "PENDING",
            platformRequestId = newId, createdAt = now.toEpochMilli(), updatedAt = now.toEpochMilli(), version = 1)
        dao.insert(row)
        row
    }

    suspend fun complete(id: String, now: Instant): ReminderCompletion? = database.withTransaction {
        val reminder = dao.byId(id) ?: return@withTransaction null
        if (reminder.targetType != "TASK") return@withTransaction null
        val task = dao.task(reminder.workspaceId, reminder.targetId) ?: return@withTransaction null
        if (task.state == "CANCELLED") return@withTransaction null
        if (task.state != "DONE" && dao.completeTask(reminder.workspaceId, task.id, task.version, now.toEpochMilli()) != 1) return@withTransaction null
        val pending = dao.all(reminder.workspaceId).filter { it.targetType == "TASK" && it.targetId == task.id && it.state == "PENDING" }
        pending.forEach { dao.update(it.copy(state = "CANCELLED", updatedAt = now.toEpochMilli(), version = it.version + 1)) }
        ReminderCompletion(task.id, pending.map(ReminderEntity::id))
    }

    suspend fun title(row: ReminderEntity): String? = if (row.targetType == "TASK") dao.task(row.workspaceId, row.targetId)?.title
        else dao.eventTitle(row.workspaceId, row.targetId)

    private fun key(type: String, id: String, occurrence: String, slot: String) = "$type|$id|$occurrence|$slot"
    private fun stableId(workspace: String, key: String) = UUID.nameUUIDFromBytes("$workspace|$key".toByteArray(StandardCharsets.UTF_8)).toString()
}
