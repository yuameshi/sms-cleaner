package top.yuameshi.sms.cleaner.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SelectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenTopBar(
    selectionState: SelectionState,
    filterState: FilterState,
    uiState: SmsUiState,
    isDefaultSmsApp: Boolean,
    onFilterClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onRestoreDefaultSmsClick: () -> Unit,
    onExitMultiSelect: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = if (selectionState.isMultiSelectMode) {
                        "已选择 ${selectionState.selectedCount} 条"
                    } else {
                        "SMS Cleaner"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!selectionState.isMultiSelectMode) {
                    when (val state = uiState) {
                        is SmsUiState.Success -> {
                            Text(
                                text = if (state.isLoading) {
                                    "正在加载..."
                                } else if (filterState.hasFilters()) {
                                    "共 ${state.totalCount} 条 | 筛选 ${state.filteredCount} 条"
                                } else {
                                    "共 ${state.totalCount} 条短信"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {}
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (selectionState.isMultiSelectMode) {
                IconButton(onClick = onExitMultiSelect) {
                    Icon(Icons.Default.Close, contentDescription = "取消")
                }
            }
        },
        actions = {
            if (!selectionState.isMultiSelectMode) {
                // 筛选按钮
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("筛选短信") } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "筛选短信",
                            tint = if (filterState.hasFilters()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // 更多选项菜单
                var menuExpanded by remember { mutableStateOf(false) }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("更多选项") } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出短信") },
                            onClick = {
                                menuExpanded = false
                                onExportClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.GetApp, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入短信") },
                            onClick = {
                                menuExpanded = false
                                onImportClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Publish, contentDescription = null)
                            }
                        )
                        if (isDefaultSmsApp) {
                            DropdownMenuItem(
                                text = { Text("恢复默认短信App") },
                                onClick = {
                                    menuExpanded = false
                                    onRestoreDefaultSmsClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (selectionState.isMultiSelectMode) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}
