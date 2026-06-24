package top.yuameshi.sms.cleaner.ui.screen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SelectionState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.domain.usecase.CheckDefaultSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.CheckPermissionsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.DeleteSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.DeleteType
import top.yuameshi.sms.cleaner.domain.usecase.ExportSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.GetSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.ImportSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.LoadSimCardsUseCase
import top.yuameshi.sms.cleaner.data.repository.FilterHistoryRepository
import javax.inject.Inject

/**
 * 导出范围
 */
enum class ExportScope {
    ALL,       // 导出全部
    FILTERED,  // 导出当前筛选结果
    SELECTED   // 导出已选中
}

@HiltViewModel
class SmsViewModel @Inject constructor(
    private val getSmsUseCase: GetSmsUseCase,
    private val deleteSmsUseCase: DeleteSmsUseCase,
    private val exportSmsUseCase: ExportSmsUseCase,
    private val importSmsUseCase: ImportSmsUseCase,
    private val checkDefaultSmsUseCase: CheckDefaultSmsUseCase,
    private val checkPermissionsUseCase: CheckPermissionsUseCase,
    private val filterHistoryRepository: FilterHistoryRepository,
    private val loadSimCardsUseCase: LoadSimCardsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsScreenUiState())
    val uiState: StateFlow<SmsScreenUiState> = _uiState.asStateFlow()

    private val selectionManager = SelectionManager()

    // 缓存：短名称是否唯一（true=用短名称，false=用长名称）
    var useShortSimName = true
        private set

    // 待删除的消息ID（单条删除时使用）
    private var pendingDeleteMessageId: Long? = null

    private var currentPage = 0
    private var allMessages = mutableListOf<SmsMessage>()
    private var totalCount = 0
    private var filteredCount = 0

    init {
        checkPermissionsAndDefaultSms()
        loadFilterHistory()
        loadSimCards()

        // Observe SelectionManager state and mirror in consolidated _uiState
        viewModelScope.launch {
            selectionManager.selectionState.collect { selectionState ->
                _uiState.update { it.copy(selectionState = selectionState) }
            }
        }
    }

    fun checkPermissionsAndDefaultSms() {
        _uiState.update {
            it.copy(
                hasPermissions = checkPermissionsUseCase(),
                isDefaultSmsApp = checkDefaultSmsUseCase()
            )
        }
    }

    private fun loadFilterHistory() {
        _uiState.update { it.copy(filterHistory = filterHistoryRepository.getHistory()) }
    }

    fun loadSimCards() {
        val result = loadSimCardsUseCase()
        _uiState.update { it.copy(simCards = result.simCards) }
        // 判断短名称是否唯一：如果有重复的短名称，则使用长名称
        useShortSimName = result.useShortSimName
    }

    fun getSimDisplayName(subscriptionId: Int): String {
        val card = _uiState.value.simCards.find { it.subscriptionId == subscriptionId }
            ?: return "SIM $subscriptionId"
        return if (useShortSimName) card.getShortName() else card.getFormattedName()
    }

    fun addFilterHistory(keyword: String) {
        filterHistoryRepository.addHistory(keyword)
        loadFilterHistory()
    }

    fun clearFilterHistory() {
        filterHistoryRepository.clearHistory()
        loadFilterHistory()
    }

    fun loadMessages() {
        viewModelScope.launch {
            // If we already have Success state, set isLoading to preserve subtitle
            val currentState = _uiState.value.smsState
            if (currentState is SmsUiState.Success) {
                _uiState.update { it.copy(smsState = currentState.copy(isLoading = true)) }
            } else {
                _uiState.update { it.copy(smsState = SmsUiState.Loading) }
            }
            try {
                currentPage = 0
                allMessages.clear()

                // Parallel execution: filteredCount + messages at the same time
                // totalCount uses SmsDataSource internal cache (instant on cache hit)
                val filterState = _uiState.value.filterState
                val filteredCountDeferred = async { getSmsUseCase.getTotalCount(filterState) }
                val messagesDeferred = async { getSmsUseCase(filterState, currentPage) }

                totalCount = getSmsUseCase.getTotalCount(FilterState())  // Cache hit, instant return
                filteredCount = filteredCountDeferred.await()
                val messages = messagesDeferred.await()
                allMessages.addAll(messages)

                _uiState.update {
                    it.copy(
                        smsState = SmsUiState.Success(
                            messages = allMessages.toList(),
                            totalCount = totalCount,
                            filteredCount = filteredCount,
                            hasMore = allMessages.size < filteredCount,
                            isLoading = false
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(smsState = SmsUiState.Error(e.message ?: "加载失败")) }
            }
        }
    }

    /**
     * 下拉刷新：重新加载短信列表
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                currentPage = 0
                allMessages.clear()

                val filterState = _uiState.value.filterState
                val filteredCountDeferred = async { getSmsUseCase.getTotalCount(filterState) }
                val messagesDeferred = async { getSmsUseCase(filterState, currentPage) }

                totalCount = getSmsUseCase.getTotalCount(FilterState())
                filteredCount = filteredCountDeferred.await()
                val messages = messagesDeferred.await()
                allMessages.addAll(messages)

                _uiState.update {
                    it.copy(
                        smsState = SmsUiState.Success(
                            messages = allMessages.toList(),
                            totalCount = totalCount,
                            filteredCount = filteredCount,
                            hasMore = allMessages.size < filteredCount,
                            isLoading = false
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(smsState = SmsUiState.Error(e.message ?: "刷新失败")) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value.smsState
        if (currentState is SmsUiState.Success && currentState.hasMore) {
            viewModelScope.launch {
                try {
                    currentPage++
                    val messages = getSmsUseCase(_uiState.value.filterState, currentPage)
                    allMessages.addAll(messages)

                    _uiState.update {
                        it.copy(
                            smsState = SmsUiState.Success(
                                messages = allMessages.toList(),
                                totalCount = totalCount,
                                filteredCount = filteredCount,
                                hasMore = allMessages.size < filteredCount
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Keep current state on error
                }
            }
        }
    }

    fun updateFilter(filterState: FilterState) {
        _uiState.update { it.copy(filterState = filterState) }
        if (filterState.keyword.isNotEmpty()) {
            addFilterHistory(filterState.keyword)
        }
        loadMessages()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filterState = FilterState()) }
        loadMessages()
    }

    fun enterMultiSelectMode(id: Long) = selectionManager.enterMultiSelectMode(id)

    fun exitMultiSelectMode() = selectionManager.exitMultiSelectMode()

    fun toggleSelection(id: Long) = selectionManager.toggleSelection(id)

    fun selectAll() = selectionManager.selectAll(filteredCount)

    fun invertSelection() = selectionManager.invertSelection(allMessages.map { it.id })

    fun deselectAll() = selectionManager.deselectAll()

    fun clearSelection() = selectionManager.clearSelection()

    /**
     * 请求删除单条消息（侧滑删除调用）
     * 会先检查是否需要设置默认短信App，然后显示确认对话框
     */
    fun requestDelete(messageId: Long) {
        // 检查是否需要设置默认短信App
        if (!checkDefaultSmsUseCase()) {
            _uiState.update { it.copy(showDefaultSmsDialog = true) }
            return
        }
        // 保存待删除的消息ID并显示确认对话框
        pendingDeleteMessageId = messageId
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    /**
     * 请求删除选中的消息（多选删除调用）
     * 会先检查是否需要设置默认短信App，然后显示确认对话框
     */
    fun requestDeleteSelected() {
        // 检查是否需要设置默认短信App
        if (!checkDefaultSmsUseCase()) {
            _uiState.update { it.copy(showDefaultSmsDialog = true) }
            return
        }
        // 显示确认对话框
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    /**
     * 确认删除（用户点击确认后调用）
     */
    fun confirmDelete() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(operationState = OperationState.Progress(0, 0)) }
            val currentSelection = _uiState.value.selectionState
            val deleteResult = if (pendingDeleteMessageId != null) {
                // 单条删除
                deleteSmsUseCase(
                    deleteType = DeleteType.Single,
                    ids = listOf(pendingDeleteMessageId!!)
                )
            } else if (currentSelection.isSelectAll) {
                // 全选删除
                deleteSmsUseCase(
                    deleteType = DeleteType.AllByFilter,
                    filterState = _uiState.value.filterState
                )
            } else {
                // 多选删除
                deleteSmsUseCase(
                    deleteType = DeleteType.Multiple,
                    ids = currentSelection.selectedIds.toList()
                )
            }

            deleteResult.fold(
                onSuccess = { deletedCount ->
                    _uiState.update {
                        it.copy(operationState = OperationState.Success("成功删除 $deletedCount 条短信"))
                    }
                    exitMultiSelectMode()
                    pendingDeleteMessageId = null
                    loadMessages()
                },
                onFailure = { e ->
                    if (e is IllegalStateException) {
                        // 不是默认短信App，显示设置对话框
                        _uiState.update {
                            it.copy(
                                operationState = OperationState.Idle,
                                showDefaultSmsDialog = true,
                                isDefaultSmsApp = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(operationState = OperationState.Error(e.message ?: "删除失败"))
                        }
                    }
                    pendingDeleteMessageId = null
                }
            )
        }
    }

    /**
     * 取消删除（用户点击取消后调用）
     */
    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
        pendingDeleteMessageId = null
    }

    /**
     * 关闭默认短信App对话框
     */
    fun dismissDefaultSmsDialog() {
        _uiState.update { it.copy(showDefaultSmsDialog = false) }
    }

    /**
     * 获取删除确认对话框中显示的消息数量
     */
    fun getDeleteCount(): Int {
        return if (pendingDeleteMessageId != null) {
            1
        } else {
            _uiState.value.selectionState.selectedCount
        }
    }

    fun exportMessages(scope: ExportScope, selectedIds: Set<Long>, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(operationState = OperationState.Progress(0, 0)) }

            try {
                val result = when (scope) {
                    ExportScope.SELECTED -> {
                        exportSmsUseCase(
                            selectedIds = selectedIds,
                            uri = uri,
                            onProgress = { exported, total ->
                                _uiState.update {
                                    it.copy(operationState = OperationState.Progress(exported, total))
                                }
                            }
                        )
                    }
                    ExportScope.FILTERED -> {
                        exportSmsUseCase(
                            filterState = _uiState.value.filterState,
                            exportAll = false,
                            uri = uri,
                            onProgress = { exported, total ->
                                _uiState.update {
                                    it.copy(operationState = OperationState.Progress(exported, total))
                                }
                            }
                        )
                    }
                    ExportScope.ALL -> {
                        exportSmsUseCase(
                            filterState = _uiState.value.filterState,
                            exportAll = true,
                            uri = uri,
                            onProgress = { exported, total ->
                                _uiState.update {
                                    it.copy(operationState = OperationState.Progress(exported, total))
                                }
                            }
                        )
                    }
                }

                result.fold(
                    onSuccess = { exportedCount ->
                        _uiState.update {
                            it.copy(
                                operationState = OperationState.Success(
                                    "导出完成！共导出 $exportedCount 条短信"
                                )
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(operationState = OperationState.Error(e.message ?: "导出失败"))
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(operationState = OperationState.Error(e.message ?: "导出失败"))
                }
            }
        }
    }

    fun importMessages(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(operationState = OperationState.Progress(0, 0)) }

            try {
                val result = importSmsUseCase(
                    uri = uri,
                    onProgress = { imported, skipped ->
                        _uiState.update {
                            it.copy(operationState = OperationState.Progress(imported, imported + skipped))
                        }
                    }
                )

                result.fold(
                    onSuccess = { importResult ->
                        _uiState.update {
                            it.copy(
                                operationState = OperationState.Success(
                                    "导入完成！成功导入 ${importResult.imported} 条，跳过重复 ${importResult.skipped} 条"
                                )
                            )
                        }
                        loadMessages()
                    },
                    onFailure = { e ->
                        if (e is IllegalStateException) {
                            // 不是默认短信App，显示设置对话框
                            _uiState.update {
                                it.copy(
                                    operationState = OperationState.Idle,
                                    showDefaultSmsDialog = true,
                                    isDefaultSmsApp = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(operationState = OperationState.Error(e.message ?: "导入失败"))
                            }
                        }
                    }
                )
            } catch (e: IllegalStateException) {
                // 不是默认短信App，显示设置对话框
                _uiState.update {
                    it.copy(
                        operationState = OperationState.Idle,
                        showDefaultSmsDialog = true,
                        isDefaultSmsApp = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(operationState = OperationState.Error(e.message ?: "导入失败"))
                }
            }
        }
    }

    /**
     * 请求导入短信
     * 会先检查是否需要设置默认短信App
     * @return true如果可以导入，false如果需要先设置默认短信App
     */
    fun requestImport(): Boolean {
        if (!checkDefaultSmsUseCase()) {
            _uiState.update { it.copy(showDefaultSmsDialog = true) }
            return false
        }
        return true
    }

    fun loadPreviewMessages() {
        viewModelScope.launch {
            try {
                val currentSelection = _uiState.value.selectionState
                val messages = if (pendingDeleteMessageId != null) {
                    // 单条删除预览
                    allMessages.filter { it.id == pendingDeleteMessageId }.take(5)
                } else if (currentSelection.isSelectAll) {
                    getSmsUseCase(_uiState.value.filterState, 0, 5)
                } else {
                    allMessages.filter { it.id in currentSelection.selectedIds }.take(5)
                }
                _uiState.update { it.copy(previewMessages = messages) }
            } catch (e: Exception) {
                _uiState.update { it.copy(previewMessages = emptyList()) }
            }
        }
    }

    fun resetOperationState() {
        _uiState.update { it.copy(operationState = OperationState.Idle) }
    }
}
