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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.CalendarEvent
import vn.lifeos.domain.planner.EventOccurrence
import vn.lifeos.domain.planner.EventTimeKind
import vn.lifeos.domain.planner.RecurrenceSpec
import vn.lifeos.domain.planner.eventOccurrences
import vn.lifeos.domain.planner.overlappingEvents

private val dateTimePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val displayPattern = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val temporaryId = Uuid.parse("00000000-0000-4000-8000-000000000999")

private enum class EditScope { SERIES, ONE }
private data class EditorTarget(val event: CalendarEvent?, val occurrence: EventOccurrence?, val scope: EditScope)

@Composable
internal fun CalendarPanel(state: PlannerUiState, now: Instant, profileZone: ZoneId, actions: PlannerActions?) {
    var selected by rememberSaveable { mutableStateOf(now.atZone(profileZone).toLocalDate().toString()) }
    var week by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var choosing by remember { mutableStateOf<Pair<CalendarEvent, EventOccurrence>?>(null) }
    val day = runCatching { LocalDate.parse(selected) }.getOrNull() ?: now.atZone(profileZone).toLocalDate()
    val from = if (week) day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else day
    val end = from.plusDays(if (week) 7 else 1)
    val occurrences = runCatching { eventOccurrences(state.events, state.exceptions, from, end, profileZone) }.getOrDefault(emptyList())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!week, onClick = { week = false }, label = { Text("Ngày") })
            FilterChip(week, onClick = { week = true }, label = { Text("Tuần") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { selected = day.minusDays(if (week) 7 else 1).toString() }) { Text("‹ Trước") }
            TextButton(onClick = { selected = day.plusDays(if (week) 7 else 1).toString() }) { Text("Sau ›") }
        }
        PlannerDatePicker("Ngày", selected, day) { selected = it }
        Text("$from → ${end.minusDays(1)} · ${profileZone.id}")
        if (state.events.any { event -> event.recurrence?.let { RecurrenceSpec.parse(it) == null } == true }) {
            Text("Có lịch từ nguồn ngoài dùng quy tắc lặp chưa hỗ trợ; app không tự tạo các lần lặp đó.",
                color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { editor = EditorTarget(null, null, EditScope.SERIES) }, enabled = actions != null) { Text("Thêm lịch") }
        if (occurrences.isEmpty()) Text("Không có lịch trong khoảng này.")
        occurrences.forEach { item ->
            val source = state.events.firstOrNull { it.id == item.seriesId }
            val conflicts = overlappingEvents(item, occurrences, profileZone)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (item.timeKind == EventTimeKind.ALL_DAY) "Cả ngày · ${item.startDate} → trước ${item.endDateExclusive}"
                        else "${item.startAt?.atZone(profileZone)?.format(displayPattern)} – ${item.endAt?.atZone(profileZone)?.format(displayPattern)}")
                    if (conflicts.isNotEmpty()) Text("Trùng lịch với ${conflicts.joinToString { it.title }}", color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("calendar_conflict"))
                    if (item.readonly) Text("Nguồn ngoài · chỉ đọc")
                    else if (source != null) TextButton(onClick = {
                        if (item.recurring) choosing = source to item else editor = EditorTarget(source, item, EditScope.SERIES)
                    }, enabled = actions != null) { Text("Sửa") }
                }
            }
        }
    }
    choosing?.let { (event, occurrence) ->
        AlertDialog(onDismissRequest = { choosing = null }, title = { Text("Phạm vi sửa lịch lặp") },
            text = { Text("Chỉ lần này sẽ tạo ngoại lệ. Toàn chuỗi sẽ cập nhật mọi lần theo quy tắc mới.") },
            confirmButton = { TextButton(onClick = { editor = EditorTarget(event, occurrence, EditScope.ONE); choosing = null }) { Text("Chỉ lần này") } },
            dismissButton = { TextButton(onClick = { editor = EditorTarget(event, occurrence, EditScope.SERIES); choosing = null }) { Text("Toàn chuỗi") } })
    }
    editor?.let { target -> EventEditor(state, target, day, profileZone, actions) { editor = null } }
}

@Composable
private fun EventEditor(state: PlannerUiState, target: EditorTarget, selectedDate: LocalDate, profileZone: ZoneId,
    actions: PlannerActions?, onDismiss: () -> Unit) {
    val event = target.event
    val occurrence = target.occurrence
    val editOne = target.scope == EditScope.ONE
    val initialDate = if (editOne) occurrence?.startDate ?: occurrence?.startAt?.atZone(event?.eventZoneId ?: profileZone)?.toLocalDate()
        else event?.startDate ?: event?.startAt?.atZone(event.eventZoneId)?.toLocalDate()
    var title by remember(target) { mutableStateOf(event?.title ?: "") }
    var allDay by remember(target) { mutableStateOf(event?.timeKind != EventTimeKind.TIMED) }
    var startDate by remember(target) { mutableStateOf((initialDate ?: selectedDate).toString()) }
    var endDate by remember(target) { mutableStateOf((if (editOne) occurrence?.endDateExclusive else event?.endDateExclusive)?.toString() ?: (initialDate ?: selectedDate).plusDays(1).toString()) }
    val initialStart = if (editOne) occurrence?.startAt else event?.startAt
    val initialEnd = if (editOne) occurrence?.endAt else event?.endAt
    var startTime by remember(target) { mutableStateOf(initialStart?.atZone(event?.eventZoneId ?: profileZone)?.format(dateTimePattern) ?: "${initialDate ?: selectedDate} 09:00") }
    var endTime by remember(target) { mutableStateOf(initialEnd?.atZone(event?.eventZoneId ?: profileZone)?.format(dateTimePattern) ?: "${initialDate ?: selectedDate} 10:00") }
    var zoneText by remember(target) { mutableStateOf(event?.eventZoneId?.id ?: profileZone.id) }
    val initialRule = if (editOne) null else event?.recurrence?.let(RecurrenceSpec::parse)
    var frequency by remember(target) { mutableStateOf(initialRule?.frequency?.name ?: "NONE") }
    var interval by remember(target) { mutableStateOf(initialRule?.interval?.toString() ?: "1") }
    var until by remember(target) { mutableStateOf(initialRule?.untilInclusive?.toString() ?: "") }
    var count by remember(target) { mutableStateOf(initialRule?.count?.toString() ?: "") }
    var confirmOverlap by remember(target) { mutableStateOf(false) }
    val zone = runCatching { ZoneId.of(zoneText) }.getOrNull()
    val firstDate = runCatching { LocalDate.parse(startDate) }.getOrNull()
    val lastDate = runCatching { LocalDate.parse(endDate) }.getOrNull()
    val start = if (zone == null) null else runCatching { LocalDateTime.parse(startTime, dateTimePattern).atZone(zone).toInstant() }.getOrNull()
    val end = if (zone == null) null else runCatching { LocalDateTime.parse(endTime, dateTimePattern).atZone(zone).toInstant() }.getOrNull()
    val rule = if (editOne) null else parseRule(frequency, interval, until, count, if (allDay) firstDate else start?.atZone(zone)?.toLocalDate())
    val validTime = if (allDay) firstDate != null && lastDate != null && lastDate.isAfter(firstDate)
        else start != null && end != null && end.isAfter(start)
    val valid = title.isNotBlank() && zone != null && validTime && (frequency == "NONE" || rule != null)
    val draft = if (!valid) null else EventDraft(title.trim(), if (allDay) EventTimeKind.ALL_DAY else EventTimeKind.TIMED,
        zone!!, startAt = if (allDay) null else start, endAt = if (allDay) null else end,
        startDate = if (allDay) firstDate else null, endDateExclusive = if (allDay) lastDate else null, recurrence = rule)
    val candidate = draft?.let { input -> CalendarEvent(event?.id ?: temporaryId, event?.workspaceId ?: temporaryId,
        null, null, input.title, input.timeKind, input.startAt, input.endAt, input.startDate,
        input.endDateExclusive, input.zoneId, input.recurrence, null, false, 1) }
    val candidateDate = draft?.startDate ?: draft?.startAt?.atZone(draft.zoneId)?.toLocalDate()
    val overlaps = if (candidate == null || candidateDate == null) emptyList() else runCatching {
        val original = eventOccurrences(state.events, state.exceptions, candidateDate, candidateDate.plusDays(7), profileZone)
            .filterNot { it.key == occurrence?.key || (event != null && !editOne && it.seriesId == event.id) }
        val proposed = eventOccurrences(listOf(candidate), emptyList(), candidateDate, candidateDate.plusDays(7), profileZone)
        proposed.flatMap { overlappingEvents(it, original, profileZone) }.distinctBy(EventOccurrence::key)
    }.getOrDefault(emptyList())
    val opened = remember { state.actionVersion }
    LaunchedEffect(state.actionVersion) { if (state.actionVersion > opened) onDismiss() }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(when { event == null -> "Thêm lịch"; editOne -> "Sửa một lần lặp"; else -> "Sửa toàn chuỗi" }) },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it; confirmOverlap = false }, label = { Text("Tên sự kiện") }, modifier = Modifier.testTag("event_title"))
            Row { Text("Cả ngày", modifier = Modifier.padding(top = 12.dp)); Switch(allDay, { allDay = it; confirmOverlap = false }) }
            OutlinedTextField(zoneText, { zoneText = it; confirmOverlap = false }, label = { Text("Múi giờ") })
            if (allDay) {
                PlannerDatePicker("Từ ngày", startDate, selectedDate) { startDate = it; confirmOverlap = false }
                PlannerDatePicker("Đến trước ngày", endDate, selectedDate.plusDays(1)) { endDate = it; confirmOverlap = false }
            } else {
                PlannerDateTimePicker("Bắt đầu", startTime, selectedDate.atTime(9, 0)) { startTime = it; confirmOverlap = false }
                PlannerDateTimePicker("Kết thúc", endTime, selectedDate.atTime(10, 0)) { endTime = it; confirmOverlap = false }
            }
            if (!editOne) RecurrenceFields(frequency, { frequency = it; confirmOverlap = false }, interval, { interval = it; confirmOverlap = false },
                until, { until = it; confirmOverlap = false }, count, { count = it; confirmOverlap = false },
                firstDate ?: start?.atZone(zone)?.toLocalDate() ?: selectedDate)
            if (!valid) Text("Kiểm tra tên, khoảng thời gian, múi giờ và quy tắc lặp. Rule khác DAILY/WEEKLY/MONTHLY chưa hỗ trợ.", color = MaterialTheme.colorScheme.error)
            if (overlaps.isNotEmpty()) Text("Trùng lịch với ${overlaps.joinToString { it.title }}. Có thể sửa giờ hoặc chọn vẫn lưu; app không tự dời lịch.",
                color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("overlap_warning"))
        } },
        confirmButton = { TextButton(onClick = {
            if (overlaps.isNotEmpty() && !confirmOverlap) confirmOverlap = true
            else if (draft != null) when {
                event == null -> actions?.createEvent(draft)
                editOne -> actions?.replaceOccurrence(event, occurrence!!.startDate ?: occurrence.startAt!!.atZone(event.eventZoneId).toLocalDate(), draft)
                else -> actions?.updateEvent(event, draft)
            }
        }, enabled = draft != null && !state.busy) { Text(if (overlaps.isNotEmpty() && confirmOverlap) "Vẫn lưu" else "Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}
