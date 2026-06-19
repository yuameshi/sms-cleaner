package top.yuameshi.sms.cleaner.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yuameshi.sms.cleaner.ui.component.DeleteConfirmDialog
import top.yuameshi.sms.cleaner.ui.component.ExportDialog
import top.yuameshi.sms.cleaner.ui.component.FilterPanel
import top.yuameshi.sms.cleaner.ui.component.ImportDialog
import top.yuameshi.sms.cleaner.ui.component.SmsListItem
import top.yuameshi.sms.cleaner.util.DefaultSmsManager

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hasPermissions: Boolean = false,
    viewModel: SmsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val selectionState by viewModel.selectionState.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsStateWithLifecycle()
    val filterHistory by viewModel.filterHistory.collectAsStateWithLifecycle()
    val previewMessages by viewModel.previewMessages.collectAsStateWithLifecycle()

    var showFilterPanel by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDefaultSmsDialog by remember { mutableStateOf(false) }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.checkPermissionsAndDefaultSms()
        }
    }

    val listState = rememberLazyListState()

    // Load messages when permissions are granted
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.checkPermissionsAndDefaultSms()
            viewModel.loadMessages()
        }
    }

    // Handle operation state
    LaunchedEffect(operationState) {
        when (operationState) {
            is OperationState.Success -> {
                // Show snackbar or dialog
            }
            is OperationState.Error -> {
                // Show error dialog
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionState.isMultiSelectMode) {
                            "已选择 ${selectionState.selectedCount} 条"
                        } else {
                            "SMS Cleaner"
                        }
                    )
                },
                navigationIcon = {
                    if (selectionState.isMultiSelectMode) {
                        IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    }
                },
                actions = {
                    if (!selectionState.isMultiSelectMode) {
                        IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "筛选",
                                tint = if (filterState.hasFilters()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "导出")
                        }
                        IconButton(onClick = { showImportDialog = true }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "导入")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectionState.isMultiSelectMode) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                                Text("删除", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选")
                                Text("全选", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { showExportDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FileDownload, contentDescription = "导出")
                                Text("导出", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter panel
            if (showFilterPanel) {
                FilterPanel(
                    filterState = filterState,
                    filterHistory = filterHistory,
                    onFilterChange = { viewModel.updateFilter(it) },
                    onClearFilters = { viewModel.clearFilters() },
                    onClearHistory = { viewModel.clearFilterHistory() }
                )
            }

            // Statistics
            when (val state = uiState) {
                is SmsUiState.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "共 ${state.totalCount} 条短信",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (filterState.hasFilters()) {
                            Text(
                                text = "筛选 ${state.filteredCount} 条",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                else -> {}
            }

            // Content
            if (!hasPermissions) {
                // Permission not granted state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "需要短信和通讯录权限",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请在系统设置中授予应用权限",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else when (val state = uiState) {
                is SmsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SmsUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "没有找到短信",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.messages,
                                key = { it.id }
                            ) { message ->
                                SmsListItem(
                                    message = message,
                                    isSelected = selectionState.isSelected(message.id),
                                    isMultiSelectMode = selectionState.isMultiSelectMode,
                                    keyword = filterState.keyword,
                                    onItemClick = {
                                        if (selectionState.isMultiSelectMode) {
                                            viewModel.toggleSelection(message.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionState.isMultiSelectMode) {
                                            viewModel.enterMultiSelectMode(message.id)
                                        }
                                    },
                                    onDeleteClick = {
                                        viewModel.enterMultiSelectMode(message.id)
                                        showDeleteDialog = true
                                    }
                                )
                            }

                            // Load more trigger
                            if (state.hasMore) {
                                item {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMore()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
                is SmsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadMessages() }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        LaunchedEffect(Unit) {
            viewModel.loadPreviewMessages()
        }
        DeleteConfirmDialog(
            count = selectionState.selectedCount,
            previewMessages = previewMessages,
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            filteredCount = (uiState as? SmsUiState.Success)?.filteredCount ?: 0,
            totalCount = (uiState as? SmsUiState.Success)?.totalCount ?: 0,
            hasFilters = filterState.hasFilters(),
            onExport = { exportAll, fileName ->
                viewModel.exportMessages(exportAll, fileName)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // Import dialog
    if (showImportDialog) {
        ImportDialog(
            onImport = { uri ->
                viewModel.importMessages(uri)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    // Default SMS app dialog
    if (showDefaultSmsDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultSmsDialog = false },
            title = { Text("需要设置默认短信App") },
            text = { Text("为了删除短信，需要将本App设为默认短信App。操作完成后可随时恢复原来的短信App。") },
            confirmButton = {
                TextButton(onClick = {
                    showDefaultSmsDialog = false
                    context.findActivity()?.let { activity ->
                        DefaultSmsManager.requestDefaultSmsRole(activity, defaultSmsLauncher)
                    }
                }) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDefaultSmsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Operation state dialog
    when (val state = operationState) {
        is OperationState.Progress -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("处理中...") },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = if (state.total > 0) state.current.toFloat() / state.total else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("已处理 ${state.current}/${state.total} 条")
                    }
                },
                confirmButton = {}
            )
        }
        is OperationState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetOperationState() },
                title = { Text("完成") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetOperationState() }) {
                        Text("确定")
                    }
                }
            )
        }
        is OperationState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetOperationState() },
                title = { Text("错误") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetOperationState() }) {
                        Text("确定")
                    }
                }
            )
        }
        else -> {}
    }
}
