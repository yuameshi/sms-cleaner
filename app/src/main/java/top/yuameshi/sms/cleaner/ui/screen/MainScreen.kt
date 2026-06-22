package top.yuameshi.sms.cleaner.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yuameshi.sms.cleaner.ui.component.DeleteConfirmDialog
import top.yuameshi.sms.cleaner.ui.component.DrawerFilterPanel
import top.yuameshi.sms.cleaner.ui.component.ExportDialog
import top.yuameshi.sms.cleaner.ui.component.ImportDialog
import top.yuameshi.sms.cleaner.ui.component.SmsListItem
import top.yuameshi.sms.cleaner.util.DefaultSmsManager
import kotlinx.coroutines.launch

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
    val previewMessages by viewModel.previewMessages.collectAsStateWithLifecycle()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsStateWithLifecycle()
    val showDefaultSmsDialog by viewModel.showDefaultSmsDialog.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                DrawerFilterPanel(
                    filterState = filterState,
                    onFilterChange = { viewModel.updateFilter(it) },
                    onClearFilters = { viewModel.clearFilters() }
                )
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
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
                                            text = if (filterState.hasFilters()) {
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
                            IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
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
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                                            showExportDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.GetApp, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("导入短信") },
                                        onClick = {
                                            menuExpanded = false
                                            if (viewModel.requestImport()) {
                                                showImportDialog = true
                                            }
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
                                                context.findActivity()?.let { activity ->
                                                    DefaultSmsManager.openDefaultAppsSettings(activity)
                                                }
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
         bottomBar = {
            if (selectionState.isMultiSelectMode) {
                val hasSelection = selectionState.selectedCount > 0 || selectionState.isSelectAll
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Delete
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.requestDeleteSelected() },
                                enabled = hasSelection
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                            Text("删除", style = MaterialTheme.typography.labelSmall)
                        }

                        // Select All
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选")
                            }
                            Text("全选", style = MaterialTheme.typography.labelSmall)
                        }

                        // Invert Selection
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.invertSelection() }) {
                                Icon(Icons.Default.Flip, contentDescription = "反选")
                            }
                            Text("反选", style = MaterialTheme.typography.labelSmall)
                        }

                        // Deselect All
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.deselectAll() },
                                enabled = hasSelection
                            ) {
                                Icon(Icons.Default.Deselect, contentDescription = "取消全选")
                            }
                            Text("取消全选", style = MaterialTheme.typography.labelSmall)
                        }

                        // Export
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { showExportDialog = true },
                                enabled = hasSelection
                            ) {
                                Icon(Icons.Default.GetApp, contentDescription = "导出")
                            }
                            Text("导出", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
            } else {
                // Search Bar with debounce
                var searchText by remember { mutableStateOf(filterState.keyword) }

                // Debounce effect: update filter after 500ms of no typing
                LaunchedEffect(searchText) {
                    if (searchText != filterState.keyword) {
                        kotlinx.coroutines.delay(500)
                        viewModel.updateFilter(filterState.copy(keyword = searchText))
                    }
                }

                // Sync external filter state changes to local state
                LaunchedEffect(filterState.keyword) {
                    if (filterState.keyword != searchText) {
                        searchText = filterState.keyword
                    }
                }

                when (val state = uiState) {
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Search box as first item
                            item(key = "search") {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { keyword ->
                                        searchText = keyword
                                    },
                                    label = { Text("搜索短信") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchText.isNotEmpty()) {
                                            IconButton(onClick = {
                                                searchText = ""
                                                viewModel.updateFilter(filterState.copy(keyword = ""))
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "清空")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

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
                                        viewModel.requestDelete(message.id)
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
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        LaunchedEffect(Unit) {
            viewModel.loadPreviewMessages()
        }
        DeleteConfirmDialog(
            count = viewModel.getDeleteCount(),
            previewMessages = previewMessages,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            filteredCount = (uiState as? SmsUiState.Success)?.filteredCount ?: 0,
            totalCount = (uiState as? SmsUiState.Success)?.totalCount ?: 0,
            hasFilters = filterState.hasFilters(),
            onExport = { exportAll, uri ->
                viewModel.exportMessages(exportAll, uri)
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
            onDismissRequest = { viewModel.dismissDefaultSmsDialog() },
            title = { Text("需要设置默认短信App") },
            text = { Text("为了删除或导入短信，需要将本App设为默认短信App。操作完成后可随时恢复原来的短信App。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDefaultSmsDialog()
                    context.findActivity()?.let { activity ->
                        DefaultSmsManager.requestDefaultSmsRole(activity, defaultSmsLauncher)
                    }
                }) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDefaultSmsDialog() }) {
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
}
