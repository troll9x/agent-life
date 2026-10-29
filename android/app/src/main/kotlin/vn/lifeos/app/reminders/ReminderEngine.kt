package vn.lifeos.app.reminders

import android.content.Context
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.planner.RoomPlannerRepository
import vn.lifeos.data.reminders.RoomReminderStore
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Outcome

internal class ReminderEngine(context: Context, private val database: AgentLifeDatabase, private val clock: Clock) {
    private val alarms = ReminderAlarms(context.applicationContext)
    private val store = RoomReminderStore(database)
    private val planner = RoomPlannerRepository(database, clock)
    private val mutex = Mutex()

    suspend fun reconcile(workspaceId: Uuid, zone: ZoneId): Boolean = mutex.withLock {
        val tasks = (planner.observeTasks(workspaceId).first() as? Outcome.Success)?.value ?: return@withLock false
        val events = (planner.observeEvents(workspaceId).first() as? Outcome.Success)?.value ?: return@withLock false
        val exceptions = (planner.observeExceptions(workspaceId).first() as? Outcome.Success)?.value ?: return@withLock false
        val plan = store.reconcile(workspaceId, reminderCandidates(tasks, events, exceptions, clock.instant(), zone), clock.instant())
        plan.cancelIds.forEach(alarms::cancel)
        plan.schedule.forEach(alarms::schedule)
        true
    }

    fun alarms(): ReminderAlarms = alarms
    fun store(): RoomReminderStore = store
}
