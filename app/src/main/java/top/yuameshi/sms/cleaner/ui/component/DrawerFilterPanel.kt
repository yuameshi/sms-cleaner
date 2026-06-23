package top.yuameshi.sms.cleaner.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.util.DeviceUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerFilterPanel(
    filterState: FilterState,
    simCards: List<SimCardInfo>,
    useShortSimName: Boolean = true,
    onFilterChange: (FilterState) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf(filterState.customStartDate) }
    var customEndDate by remember { mutableStateOf(filterState.customEndDate) }

    // 监听SIM卡选择变化，在MIUI/澎湃OS上显示警告
    LaunchedEffect(filterState.simSubscriptionId) {
        if (filterState.simSubscriptionId != null && DeviceUtils.isMiuiOrHyperOS()) {
            Toast.makeText(
                context,
                "MIUI/澎湃OS可能无法正确按SIM卡筛选",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title with Reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "筛选",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(
                onClick = {
                    customStartDate = null
                    customEndDate = null
                    onClearFilters()
                }
            ) {
                Text("重置")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date range section
        Text("日期范围", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterState.DateRange.entries.toList()) { dateRange ->
                FilterChip(
                    selected = if (dateRange == FilterState.DateRange.CUSTOM) {
                        false // "自定义" chip is never selected
                    } else if (customStartDate != null && customEndDate != null) {
                        false // No preset chip selected when custom dates are set
                    } else {
                        filterState.dateRange == dateRange
                    },
                    onClick = {
                        if (dateRange == FilterState.DateRange.CUSTOM) {
                            showDatePicker = true
                        } else {
                            // Clear custom dates when a preset is selected
                            customStartDate = null
                            customEndDate = null
                            onFilterChange(
                                filterState.copy(
                                    dateRange = dateRange,
                                    customStartDate = null,
                                    customEndDate = null
                                )
                            )
                        }
                    },
                    label = { Text(getDateRangeName(dateRange)) }
                )
            }
        }

        // Custom date chip row
        if (customStartDate != null && customEndDate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val startDate = Instant.ofEpochMilli(customStartDate!!)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val endDate = Instant.ofEpochMilli(customEndDate!!)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                    val dateRangeText = "${startDate.format(formatter)} - ${endDate.format(formatter)}"
                    FilterChip(
                        selected = true,
                        onClick = { showDatePicker = true },
                        label = { Text(dateRangeText) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    customStartDate = null
                                    customEndDate = null
                                    onFilterChange(
                                        filterState.copy(
                                            dateRange = FilterState.DateRange.ALL,
                                            customStartDate = null,
                                            customEndDate = null
                                        )
                                    )
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除自定义日期",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Read status section
        Text("已读状态", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterState.ReadStatus.entries.toList()) { status ->
                FilterChip(
                    selected = filterState.readStatus == status,
                    onClick = {
                        onFilterChange(filterState.copy(readStatus = status))
                    },
                    label = { Text(getReadStatusName(status)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lock status section
        Text("锁定状态", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterState.LockStatus.entries.toList()) { status ->
                FilterChip(
                    selected = filterState.lockStatus == status,
                    onClick = {
                        onFilterChange(filterState.copy(lockStatus = status))
                    },
                    label = { Text(getLockStatusName(status)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message type section
        Text("消息类型", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterState.MessageType.entries.toList()) { type ->
                FilterChip(
                    selected = filterState.messageType == type,
                    onClick = {
                        onFilterChange(filterState.copy(messageType = type))
                    },
                    label = { Text(getMessageTypeName(type)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SIM card section
        Text("SIM卡", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" chip
            item {
                FilterChip(
                    selected = filterState.simSubscriptionId == null,
                    onClick = {
                        onFilterChange(filterState.copy(simSubscriptionId = null))
                    },
                    label = { Text("全部") }
                )
            }
            // Dynamic SIM chips
            items(simCards) { sim ->
                FilterChip(
                    selected = filterState.simSubscriptionId == sim.subscriptionId,
                    onClick = {
                        onFilterChange(filterState.copy(simSubscriptionId = sim.subscriptionId))
                    },
                    label = { Text(if (useShortSimName) sim.getShortName() else sim.getFormattedName()) }
                )
            }
        }
    }

    // Date Range Picker Dialog
    if (showDatePicker) {
        SmsDatePickerDialog(
            onDateSelected = { startDateMillis, endDateMillis ->
                customStartDate = startDateMillis
                customEndDate = endDateMillis
                // 立即应用自定义日期筛选
                onFilterChange(
                    filterState.copy(
                        dateRange = FilterState.DateRange.CUSTOM,
                        customStartDate = startDateMillis,
                        customEndDate = endDateMillis
                    )
                )
            },
            onDismiss = { showDatePicker = false },
            initialStartDate = customStartDate,
            initialEndDate = customEndDate
        )
    }
}

private fun getDateRangeName(dateRange: FilterState.DateRange): String {
    return when (dateRange) {
        FilterState.DateRange.ALL -> "全部"
        FilterState.DateRange.TODAY -> "今天"
        FilterState.DateRange.LAST_7_DAYS -> "最近7天"
        FilterState.DateRange.LAST_30_DAYS -> "最近30天"
        FilterState.DateRange.LAST_90_DAYS -> "最近90天"
        FilterState.DateRange.CUSTOM -> "自定义"
    }
}

private fun getReadStatusName(status: FilterState.ReadStatus): String {
    return when (status) {
        FilterState.ReadStatus.ALL -> "全部"
        FilterState.ReadStatus.READ -> "已读"
        FilterState.ReadStatus.UNREAD -> "未读"
    }
}

private fun getLockStatusName(status: FilterState.LockStatus): String {
    return when (status) {
        FilterState.LockStatus.ALL -> "全部"
        FilterState.LockStatus.LOCKED -> "锁定"
        FilterState.LockStatus.UNLOCKED -> "未锁定"
    }
}

private fun getMessageTypeName(type: FilterState.MessageType): String {
    return when (type) {
        FilterState.MessageType.ALL -> "全部"
        FilterState.MessageType.INBOX -> "收件箱"
        FilterState.MessageType.SENT -> "已发送"
        FilterState.MessageType.DRAFT -> "草稿"
        FilterState.MessageType.OUTBOX -> "发件箱"
    }
}

