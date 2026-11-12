package vn.lifeos.data.finance.scheduled

import java.time.LocalDate
import java.time.ZoneId
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.reminders.ReminderCandidate
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.ReminderTarget

/** Same durable reminder store, alarm fallback, permissions and reboot reconciliation as planner. */
suspend fun scheduledReminderCandidates(database: AgentLifeDatabase, workspace: Uuid, zone: ZoneId): List<ReminderCandidate> {
    val dao = database.scheduledDao()
    val dates = dao.rules(workspace.value).filter { it.enabled && it.nextDueDate != null }.map { it.id to it.nextDueDate!! } +
        dao.plans(workspace.value).filter { it.state == "PENDING" }.map { it.ruleId to it.dueDate }
    return dates.distinct().map { (rule, date) ->
        ReminderCandidate(ReminderTarget.FINANCE, Uuid.parse(rule), date, LocalDate.parse(date).atTime(9, 0).atZone(zone).toInstant())
    }
}
