package vn.lifeos.app.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.CalendarEvent
import vn.lifeos.domain.planner.EventOccurrence
import vn.lifeos.domain.planner.EventTimeKind
import vn.lifeos.domain.planner.Frequency
import vn.lifeos.domain.planner.PlannedTask
import vn.lifeos.domain.planner.Project
import vn.lifeos.domain.planner.RecurrenceSpec
import vn.lifeos.domain.planner.TaskState
import vn.lifeos.domain.planner.eventOccurrences
import vn.lifeos.domain.planner.overlappingEvents

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun PlannerDashboard(state: PlannerUiState, now: Instant, zone: ZoneId, actions: PlannerActions? = null, onOpenWork: () -> Unit) {
    val today = now.atZone(zone).toLocalDate()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hôm nay · $today", style = MaterialTheme.typography.headlineMedium)
        PlannerStatus(state, actions)
        if (!state.loading && !state.loadFailed) {
            val openTasks = state.tasks.filter { it.state != TaskState.DONE && it.state != TaskState.CANCELLED }
            val dueTasks = openTasks.filter {
                it.dueAt?.atZone(zone)?.toLocalDate()?.let { day -> !day.isAfter(today) } == true }
            val events = runCatching { eventOccurrences(state.events, state.exceptions, today, today.plusDays(1), zone) }.getOrDefault(emptyList())
            Text("Việc đang mở: ${openTasks.size} · Đến hạn: ${dueTasks.size} · Lịch: ${events.size}", modifier = Modifier.testTag("planner_dashboard_count"))
            openTasks.take(5).forEach { Text("• ${it.title}") }
            events.forEach { Text("• ${it.title}") }
            Button(onClick = onOpenWork) { Text("Mở Công việc và lịch") }
        }
    }
}

@Composable
internal fun PlannerScreen(state: PlannerUiState, now: Instant, zone: ZoneId, actions: PlannerActions?,
    reminderTargetType: String? = null, reminderTargetId: String? = null, reminderTriggerAt: Long? = null) {
    var showCalendar by rememberSaveable(reminderTargetId) { mutableStateOf(reminderTargetType == "EVENT") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!showCalendar, onClick = { showCalendar = false }, label = { Text("Việc") })
            FilterChip(showCalendar, onClick = { showCalendar = true }, label = { Text("Lịch") })
        }
        PlannerStatus(state, actions)
        if (!state.loading && !state.loadFailed) {
            if (reminderTargetId != null) {
                val title = if (reminderTargetType == "TASK") state.tasks.firstOrNull { it.id.value == reminderTargetId }?.title
                    else state.events.firstOrNull { it.id.value == reminderTargetId }?.title
                Text("Lời nhắc: ${title ?: "Mục đã xóa hoặc không còn khả dụng"}", modifier = Modifier.testTag("reminder_target"))
            }
            if (showCalendar) CalendarPanel(state, now, zone, actions, if (reminderTargetType == "EVENT") reminderTargetId else null,
                reminderTriggerAt)
            else TaskPanel(state, now, zone, actions, if (reminderTargetType == "TASK") reminderTargetId else null)
        }
    }
}

@Composable
private fun PlannerStatus(state: PlannerUiState, actions: PlannerActions?) {
    if (state.loading) Text("Đang tải công việc và lịch…")
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { if (state.loadFailed) actions?.retryPlanner() else actions?.clearPlannerError() }, enabled = actions != null) {
            Text(if (state.loadFailed) "Thử lại" else "Đóng thông báo")
        }
    }
}

@Composable
private fun TaskPanel(state: PlannerUiState, now: Instant, zone: ZoneId, actions: PlannerActions?, focusTaskId: String?) {
    var done by rememberSaveable(focusTaskId) { mutableStateOf(state.tasks.firstOrNull { it.id.value == focusTaskId }?.state == TaskState.DONE) }
    var date by rememberSaveable { mutableStateOf("") }
    var projectIndex by rememberSaveable { mutableStateOf(0) }
    var edit by remember { mutableStateOf<PlannedTask?>(null) }
    var creating by remember { mutableStateOf(false) }
    var checklistTask by remember { mutableStateOf<PlannedTask?>(null) }
    var showProject by remember { mutableStateOf(false) }
    val projects = state.projects.filter { it.archivedAt == null }
    val selectedProject = if (projectIndex == 0) null else projects.getOrNull(projectIndex - 1)
    val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val shown = state.tasks.filter { task ->
        (if (done) task.state == TaskState.DONE else task.state != TaskState.DONE && task.state != TaskState.CANCELLED) &&
            (selectedProject == null || task.projectId == selectedProject.id) &&
            (selectedDate == null || task.dueAt?.atZone(zone)?.toLocalDate() == selectedDate)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!done, onClick = { done = false }, label = { Text("Đang làm") })
            FilterChip(done, onClick = { done = true }, label = { Text("Hoàn tất") })
        }
        PlannerDatePicker("Lọc ngày", date, now.atZone(zone).toLocalDate(), optional = true) { date = it }
        TextButton(onClick = { projectIndex = (projectIndex + 1) % (projects.size + 1) }) {
            Text("Dự án: ${selectedProject?.title ?: "Tất cả"}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { creating = true }, enabled = actions != null) { Text("Thêm việc") }
            TextButton(onClick = { showProject = true }, enabled = actions != null) { Text("Thêm dự án") }
        }
        if (shown.isEmpty()) Text("Không có việc trong bộ lọc này.")
        shown.forEach { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Text("${task.state.name} · ${task.dueAt?.atZone(zone)?.format(dateTimeFormat) ?: "Không hạn"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { edit = task }, enabled = actions != null) { Text("Sửa") }
                        TextButton(onClick = { checklistTask = task; actions?.selectChecklist(task.id) }, enabled = actions != null) { Text("Checklist") }
                        if (task.state != TaskState.DONE) TextButton(onClick = { actions?.setTaskState(task, TaskState.DONE) }, enabled = actions != null && !state.busy) { Text("Hoàn tất") }
                    }
                }
            }
        }
    }
    if (creating || edit != null) TaskEditor(state, edit, projects, now, zone, actions,
        onDismiss = { creating = false; edit = null })
    if (showProject) ProjectEditor(state, actions) { showProject = false }
    checklistTask?.let { task -> ChecklistEditor(state, task, actions) {
        checklistTask = null; actions?.selectChecklist(null)
    } }
}

@Composable
private fun ProjectEditor(state: PlannerUiState, actions: PlannerActions?, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    val opened = remember { state.actionVersion }
    LaunchedEffect(state.actionVersion) { if (state.actionVersion > opened) onDismiss() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Dự án mới") },
        text = { OutlinedTextField(title, { title = it }, label = { Text("Tên dự án") }) },
        confirmButton = { TextButton(onClick = { actions?.createProject(title.trim()) }, enabled = title.isNotBlank() && !state.busy) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
private fun TaskEditor(state: PlannerUiState, task: PlannedTask?, projects: List<Project>, now: Instant, zone: ZoneId,
    actions: PlannerActions?, onDismiss: () -> Unit) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var note by remember(task?.id) { mutableStateOf(task?.note ?: "") }
    var due by remember(task?.id) { mutableStateOf(task?.dueAt?.atZone(zone)?.format(dateTimeFormat) ?: "") }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: 3) }
    var projectIndex by remember(task?.id) { mutableStateOf(projects.indexOfFirst { it.id == task?.projectId } + 1) }
    val initialSpec = task?.recurrence?.let(RecurrenceSpec::parse)
    var frequency by remember(task?.id) { mutableStateOf(initialSpec?.frequency?.name ?: "NONE") }
    var interval by remember(task?.id) { mutableStateOf(initialSpec?.interval?.toString() ?: "1") }
    var until by remember(task?.id) { mutableStateOf(initialSpec?.untilInclusive?.toString() ?: "") }
    var count by remember(task?.id) { mutableStateOf(initialSpec?.count?.toString() ?: "") }
    val dueAt = if (due.isBlank()) null else runCatching { LocalDateTime.parse(due, dateTimeFormat).atZone(zone).toInstant() }.getOrNull()
    val rule = parseRule(frequency, interval, until, count, dueAt?.atZone(zone)?.toLocalDate())
    val valid = title.isNotBlank() && (due.isBlank() || dueAt != null) && (frequency == "NONE" || rule != null)
    val opened = remember { state.actionVersion }
    LaunchedEffect(state.actionVersion) { if (state.actionVersion > opened) onDismiss() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (task == null) "Thêm việc" else "Sửa việc") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Tên việc") }, modifier = Modifier.testTag("task_title"))
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") })
                PlannerDateTimePicker("Hạn", due, now.atZone(zone).toLocalDateTime(), optional = true) { due = it }
                if (due.isNotBlank() && dueAt == null) Text("Ngày/giờ không hợp lệ.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { projectIndex = (projectIndex + 1) % (projects.size + 1) }) {
                    Text("Dự án: ${projects.getOrNull(projectIndex - 1)?.title ?: "Không có"}")
                }
                TextButton(onClick = { priority = if (priority == 4) 1 else priority + 1 }) { Text("Ưu tiên: $priority") }
                RecurrenceFields(frequency, { frequency = it }, interval, { interval = it }, until, { until = it }, count, { count = it },
                    dueAt?.atZone(zone)?.toLocalDate() ?: now.atZone(zone).toLocalDate())
                if (frequency != "NONE" && rule == null) Text("Quy tắc lặp chưa hợp lệ hoặc chưa hỗ trợ.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            val selectedProject = projects.getOrNull(projectIndex - 1)?.id
            if (task == null) actions?.createTask(title.trim(), note.ifBlank { null }, selectedProject, priority, dueAt, rule)
            else actions?.updateTask(task, title.trim(), note.ifBlank { null }, selectedProject, priority, dueAt, rule)
        }, enabled = valid && !state.busy) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
private fun ChecklistEditor(state: PlannerUiState, task: PlannedTask, actions: PlannerActions?, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Checklist · ${task.title}") },
        text = { Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
            state.checklist.forEach { item -> Row {
                Checkbox(item.done, onCheckedChange = { actions?.setChecklistDone(item, it) }, enabled = !state.busy)
                Text(item.title, modifier = Modifier.padding(top = 12.dp))
            } }
            OutlinedTextField(title, { title = it }, label = { Text("Bước mới") })
        } },
        confirmButton = { TextButton(onClick = { actions?.addChecklist(task.id, title.trim(), state.checklist.size); title = "" }, enabled = title.isNotBlank() && !state.busy) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } })
}

internal fun parseRule(frequency: String, interval: String, until: String, count: String, first: LocalDate?): String? {
    if (frequency == "NONE") return null
    if (first == null || (until.isBlank() == count.isBlank())) return null
    val parsed = runCatching { RecurrenceSpec(Frequency.valueOf(frequency), interval.toInt(),
        until.takeIf(String::isNotBlank)?.let(LocalDate::parse), count.takeIf(String::isNotBlank)?.toInt()) }.getOrNull() ?: return null
    if (parsed.untilInclusive?.let { it.isBefore(first) || it.isAfter(first.plusYears(5)) } == true) return null
    return parsed.encode()
}

@Composable
internal fun RecurrenceFields(frequency: String, onFrequency: (String) -> Unit, interval: String, onInterval: (String) -> Unit,
    until: String, onUntil: (String) -> Unit, count: String, onCount: (String) -> Unit, fallback: LocalDate) {
    TextButton(onClick = {
        val choices = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")
        onFrequency(choices[(choices.indexOf(frequency) + 1) % choices.size])
    }) { Text("Lặp: ${mapOf("NONE" to "Không", "DAILY" to "Hằng ngày", "WEEKLY" to "Hằng tuần", "MONTHLY" to "Hằng tháng")[frequency]}") }
    if (frequency != "NONE") {
        OutlinedTextField(interval, onInterval, label = { Text("Mỗi bao nhiêu chu kỳ (1–30)") }, singleLine = true)
        PlannerDatePicker("Đến ngày", until, fallback, optional = true, onChange = onUntil)
        OutlinedTextField(count, onCount, label = { Text("Hoặc số lần (1–366)") }, singleLine = true)
        Text("Chọn một cách kết thúc: đến ngày (trong 5 năm) hoặc số lần.")
        if (frequency == "MONTHLY") Text("Ngày 31: bỏ tháng không có ngày 31; không dời sang ngày khác.")
    }
}
