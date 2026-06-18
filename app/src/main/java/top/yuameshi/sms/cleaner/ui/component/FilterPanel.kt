package top.yuameshi.sms.cleaner.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yuameshi.sms.cleaner.data.model.FilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    filterState: FilterState,
    filterHistory: List<String>,
    onFilterChange: (FilterState) -> Unit,
    onClearFilters: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAdvancedMode by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf(filterState.keyword) }
    var number by remember { mutableStateOf(filterState.number) }
    var regex by remember { mutableStateOf(filterState.regex) }
    var isRegexMode by remember { mutableStateOf(filterState.isRegexMode) }
    var regexError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Search input
        OutlinedTextField(
            value = if (isRegexMode) regex else keyword,
            onValueChange = { value ->
                if (isRegexMode) {
                    regex = value
                    regexError = validateRegex(value)
                } else {
                    keyword = value
                }
            },
            label = { Text(if (isRegexMode) "正则表达式" else "关键词/号码") },
            leadingIcon = {
                Icon(
                    imageVector = if (isRegexMode) Icons.Default.Code else Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                Row {
                    // Regex toggle
                    IconButton(onClick = {
                        isRegexMode = !isRegexMode
                        if (isRegexMode) {
                            regex = keyword
                        } else {
                            keyword = regex
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "正则模式",
                            tint = if (isRegexMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Clear button
                    if (keyword.isNotEmpty() || regex.isNotEmpty()) {
                        IconButton(onClick = {
                            keyword = ""
                            regex = ""
                            number = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除"
                            )
                        }
                    }
                }
            },
            isError = regexError != null,
            supportingText = regexError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mode toggle and search button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Advanced mode toggle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isAdvancedMode,
                    onCheckedChange = { isAdvancedMode = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("高级模式", style = MaterialTheme.typography.bodySmall)
            }

            // Search button
            Button(
                onClick = {
                    onFilterChange(
                        filterState.copy(
                            keyword = keyword,
                            number = number,
                            regex = regex,
                            isRegexMode = isRegexMode
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("搜索")
            }
        }

        // Advanced filters
        AnimatedVisibility(visible = isAdvancedMode) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                // Date range
                Text("日期范围", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FilterState.DateRange.entries.toList()) { dateRange ->
                        FilterChip(
                            selected = filterState.dateRange == dateRange,
                            onClick = {
                                onFilterChange(filterState.copy(dateRange = dateRange))
                            },
                            label = { Text(getDateRangeName(dateRange)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Read status
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

                // Lock status
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

                // Message type
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

                // SIM card
                Text("SIM卡", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FilterState.SimId.entries.toList()) { simId ->
                        FilterChip(
                            selected = filterState.simId == simId,
                            onClick = {
                                onFilterChange(filterState.copy(simId = simId))
                            },
                            label = { Text(getSimIdName(simId)) }
                        )
                    }
                }
            }
        }

        // Filter history
        if (filterHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最近使用", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onClearHistory) {
                    Text("清除全部")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterHistory) { history ->
                    SuggestionChip(
                        onClick = {
                            keyword = history
                            onFilterChange(filterState.copy(keyword = history))
                        },
                        label = { Text(history) }
                    )
                }
            }
        }

        // Clear all filters button
        if (filterState.hasFilters()) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClearFilters,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ClearAll, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("清除全部筛选")
            }
        }
    }
}

private fun validateRegex(regex: String): String? {
    if (regex.isEmpty()) return null
    return try {
        Regex(regex)
        null
    } catch (e: Exception) {
        "正则表达式格式不正确"
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

private fun getSimIdName(simId: FilterState.SimId): String {
    return when (simId) {
        FilterState.SimId.ALL -> "全部"
        FilterState.SimId.SIM1 -> "SIM1"
        FilterState.SimId.SIM2 -> "SIM2"
    }
}
