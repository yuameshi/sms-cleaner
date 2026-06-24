package top.yuameshi.sms.cleaner.ui.screen

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yuameshi.sms.cleaner.ui.component.DrawerFilterPanel
import top.yuameshi.sms.cleaner.ui.component.SmsListItem
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.util.DefaultSmsManager
import top.yuameshi.sms.cleaner.util.findActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hasPermissions: Boolean = false,
    showPermissionPermanentlyDenied: Boolean = false,
    onRetryPermissionRequest: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    viewModel: SmsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            viewModel.loadSimCards()
        }
    }

    // Refresh SIM cards when drawer opens
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            viewModel.loadSimCards()
        }
    }

    // 拦截返回事件：多选模式下退出多选，而不是返回上一页
    BackHandler(enabled = uiState.selectionState.isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                DrawerFilterPanel(
                    filterState = uiState.filterState,
                    simCards = uiState.simCards,
                    useShortSimName = viewModel.useShortSimName,
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
                MainScreenTopBar(
                    selectionState = uiState.selectionState,
                    filterState = uiState.filterState,
                    uiState = uiState.smsState,
                    isDefaultSmsApp = uiState.isDefaultSmsApp,
                    onFilterClick = { scope.launch { drawerState.open() } },
                    onExportClick = { showExportDialog = true },
                    onImportClick = {
                        if (viewModel.requestImport()) {
                            showImportDialog = true
                        }
                    },
                    onRestoreDefaultSmsClick = {
                        context.findActivity()?.let { activity ->
                            DefaultSmsManager.openDefaultAppsSettings(activity)
                        }
                    },
                    onExitMultiSelect = { viewModel.exitMultiSelectMode() },
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.clickable {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                )
            },
         bottomBar = {
            if (uiState.selectionState.isMultiSelectMode) {
                MultiSelectBottomBar(
                    hasSelection = uiState.selectionState.selectedCount > 0 || uiState.selectionState.isSelectAll,
                    onDeleteClick = { viewModel.requestDeleteSelected() },
                    onSelectAllClick = { viewModel.selectAll() },
                    onInvertSelectionClick = { viewModel.invertSelection() },
                    onDeselectAllClick = { viewModel.deselectAll() },
                    onExportClick = { showExportDialog = true }
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Content
            if (!hasPermissions) {
                PermissionDeniedContent(
                    showPermissionPermanentlyDenied = showPermissionPermanentlyDenied,
                    onRetryPermissionRequest = onRetryPermissionRequest,
                    onOpenAppSettings = onOpenAppSettings
                )
            } else {
                // Search Bar with debounce
                var searchText by remember { mutableStateOf(uiState.filterState.keyword) }

                // Debounce effect: update filter after 500ms of no typing
                LaunchedEffect(searchText) {
                    if (searchText != uiState.filterState.keyword) {
                        kotlinx.coroutines.delay(500)
                        viewModel.updateFilter(uiState.filterState.copy(keyword = searchText))
                    }
                }

                // Sync external filter state changes to local state
                LaunchedEffect(uiState.filterState.keyword) {
                    if (uiState.filterState.keyword != searchText) {
                        searchText = uiState.filterState.keyword
                    }
                }

                when (val state = uiState.smsState) {
                is SmsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SmsUiState.Success -> {
                    val pullRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = state.isLoading || uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                        state = pullRefreshState,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                modifier = Modifier.align(Alignment.TopCenter),
                                isRefreshing = state.isLoading || uiState.isRefreshing,
                                state = pullRefreshState,
                                color = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            )
                        }
                    ) {
                        if (state.isLoading && state.messages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (state.messages.isEmpty()) {
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
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Search box as first item
                                item(key = "search") {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                                                        viewModel.updateFilter(uiState.filterState.copy(keyword = ""))
                                                    }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "清空")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }

                                items(
                                    items = state.messages,
                                    key = { it.id }
                                ) { message ->
                                    SmsListItem(
                                        message = message,
                                        isSelected = uiState.selectionState.isSelected(message.id),
                                        isMultiSelectMode = uiState.selectionState.isMultiSelectMode,
                                        keyword = uiState.filterState.keyword,
                                        simDisplayName = viewModel.getSimDisplayName(message.subId),
                                        onItemClick = {
                                            if (uiState.selectionState.isMultiSelectMode) {
                                                viewModel.toggleSelection(message.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!uiState.selectionState.isMultiSelectMode) {
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

    MainScreenDialogs(
        showDeleteConfirmDialog = uiState.showDeleteConfirmDialog,
        previewMessages = uiState.previewMessages,
        deleteCount = viewModel.getDeleteCount(),
        onLoadPreviewMessages = { viewModel.loadPreviewMessages() },
        onConfirmDelete = { viewModel.confirmDelete() },
        onCancelDelete = { viewModel.cancelDelete() },
        showExportDialog = showExportDialog,
        isMultiSelectMode = uiState.selectionState.isMultiSelectMode,
        selectedCount = uiState.selectionState.selectedCount,
        filteredCount = (uiState.smsState as? SmsUiState.Success)?.filteredCount ?: 0,
        totalCount = (uiState.smsState as? SmsUiState.Success)?.totalCount ?: 0,
        hasFilters = uiState.filterState.hasFilters(),
        onExport = { scope, uri ->
            viewModel.exportMessages(scope, uiState.selectionState.selectedIds, uri)
            showExportDialog = false
        },
        onDismissExport = { showExportDialog = false },
        showImportDialog = showImportDialog,
        onImport = { uri ->
            viewModel.importMessages(uri)
            showImportDialog = false
        },
        onDismissImport = { showImportDialog = false },
        showDefaultSmsDialog = uiState.showDefaultSmsDialog,
        onDismissDefaultSmsDialog = { viewModel.dismissDefaultSmsDialog() },
        onConfirmDefaultSms = {
            viewModel.dismissDefaultSmsDialog()
            context.findActivity()?.let { activity ->
                DefaultSmsManager.requestDefaultSmsRole(activity, defaultSmsLauncher)
            }
        },
        operationState = uiState.operationState,
        onResetOperationState = { viewModel.resetOperationState() }
    )
    }
}
