package vn.lifeos.app.planner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
internal fun PlannerDatePicker(label: String, value: String, fallback: LocalDate, optional: Boolean = false,
    onChange: (String) -> Unit) {
    val context = LocalContext.current
    Row {
        OutlinedButton(onClick = {
            val initial = runCatching { LocalDate.parse(value) }.getOrNull() ?: fallback
            DatePickerDialog(context, { _, year, month, day ->
                onChange(LocalDate.of(year, month + 1, day).toString())
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }, modifier = Modifier.testTag("date_picker")) { Text("$label: ${value.ifBlank { "Chọn ngày" }}") }
        if (optional && value.isNotBlank()) TextButton(onClick = { onChange("") }) { Text("Bỏ chọn") }
    }
}

@Composable
internal fun PlannerDateTimePicker(label: String, value: String, fallback: LocalDateTime, optional: Boolean = false,
    onChange: (String) -> Unit) {
    val context = LocalContext.current
    Row {
        OutlinedButton(onClick = {
            val initial = runCatching { LocalDateTime.parse(value, dateTimeFormat) }.getOrNull() ?: fallback
            DatePickerDialog(context, { _, year, month, day ->
                val date = LocalDate.of(year, month + 1, day)
                TimePickerDialog(context, { _, hour, minute ->
                    onChange(date.atTime(hour, minute).format(dateTimeFormat))
                }, initial.hour, initial.minute, true).show()
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }, modifier = Modifier.testTag("date_time_picker")) { Text("$label: ${value.ifBlank { "Chọn ngày và giờ" }}") }
        if (optional && value.isNotBlank()) TextButton(onClick = { onChange("") }) { Text("Bỏ chọn") }
    }
}
