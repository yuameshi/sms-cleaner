package top.yuameshi.sms.cleaner.ui.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsDatePickerDialog(
    onDateSelected: (startDateMillis: Long, endDateMillis: Long) -> Unit,
    onDismiss: () -> Unit,
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    modifier: Modifier = Modifier
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate,
        initialSelectedEndDateMillis = initialEndDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startDateMillis = dateRangePickerState.selectedStartDateMillis
                    val endDateMillis = dateRangePickerState.selectedEndDateMillis
                    if (startDateMillis != null && endDateMillis != null) {
                        onDateSelected(startDateMillis, endDateMillis)
                    }
                    onDismiss()
                },
                enabled = dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        modifier = modifier
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.heightIn(max = 420.dp),
            title = {
                Text(
                    text = "选择日期",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp, bottom = 0.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            },
            headline = {
                val startDate = dateRangePickerState.selectedStartDateMillis
                val endDate = dateRangePickerState.selectedEndDateMillis
                val formatter = DateTimeFormatter.ofPattern("M月d日")

                val headlineText = when {
                    startDate != null && endDate != null -> {
                        val start = Instant.ofEpochMilli(startDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val end = Instant.ofEpochMilli(endDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        "${start.format(formatter)} - ${end.format(formatter)}"
                    }
                    startDate != null -> {
                        val start = Instant.ofEpochMilli(startDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        start.format(formatter)
                    }
                    endDate != null -> {
                        val end = Instant.ofEpochMilli(endDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        end.format(formatter)
                    }
                    else -> "选择日期范围"
                }

                Text(
                    text = headlineText,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1
                )
            }
        )
    }
}
