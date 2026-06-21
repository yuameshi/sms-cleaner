package top.yuameshi.sms.cleaner.ui.screen

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.yuameshi.sms.cleaner.data.manager.SmsOperationManager
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SelectionState
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.domain.usecase.ExportSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.FilterSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.GetSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.ImportSmsUseCase
import top.yuameshi.sms.cleaner.data.repository.FilterHistoryRepository
import top.yuameshi.sms.cleaner.util.PermissionUtils
import javax.inject.Inject

sealed class SmsUiState {
    object Loading : SmsUiState()
    data class Success(
        val messages: List<SmsMessage>,
        val totalCount: Int,
        val filteredCount: Int,
        val hasMore: Boolean,
        val isLoading: Boolean = false
    ) : SmsUiState()
    data class Error(val message: String) : SmsUiState()
}

sealed class OperationState {
    object Idle : OperationState()
    data class Progress(val current: Int, val total: Int) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

@HiltViewModel
class SmsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getSmsUseCase: GetSmsUseCase,
    private val filterSmsUseCase: FilterSmsUseCase,
    private val exportSmsUseCase: ExportSmsUseCase,
    private val importSmsUseCase: ImportSmsUseCase,
    private val filterHistoryRepository: FilterHistoryRepository,
    private val smsOperationManager: SmsOperationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmsUiState>(SmsUiState.Loading)
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _filterHistory = MutableStateFlow<List<String>>(emptyList())
    val filterHistory: StateFlow<List<String>> = _filterHistory.asStateFlow()

    private val _previewMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val previewMessages: StateFlow<List<SmsMessage>> = _previewMessages.asStateFlow()

    // 删除确认对话框状态
    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    // 需要设置默认短信App的对话框状态
    private val _showDefaultSmsDialog = MutableStateFlow(false)
    val showDefaultSmsDialog: StateFlow<Boolean> = _showDefaultSmsDialog.asStateFlow()

    // 待删除的消息ID（单条删除时使用）
    private var pendingDeleteMessageId: Long? = null

    private var currentPage = 0
    private var allMessages = mutableListOf<SmsMessage>()
    private var totalCount = 0
    private var filteredCount = 0

    init {
        checkPermissionsAndDefaultSms()
        loadFilterHistory()
    }

    fun checkPermissionsAndDefaultSms() {
        _hasPermissions.value = PermissionUtils.hasAllPermissions(context)
        _isDefaultSmsApp.value = !smsOperationManager.needsDefaultSmsApp()
    }

    private fun loadFilterHistory() {
        _filterHistory.value = filterHistoryRepository.getHistory()
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
            val currentState = _uiState.value
            if (currentState is SmsUiState.Success) {
                _uiState.value = currentState.copy(isLoading = true)
            } else {
                _uiState.value = SmsUiState.Loading
            }
            try {
                currentPage = 0
                allMessages.clear()

                totalCount = getSmsUseCase.getTotalCount(FilterState())
                filteredCount = getSmsUseCase.getTotalCount(_filterState.value)

                val messages = getSmsUseCase(_filterState.value, currentPage)
                allMessages.addAll(messages)

                _uiState.value = SmsUiState.Success(
                    messages = allMessages.toList(),
                    totalCount = totalCount,
                    filteredCount = filteredCount,
                    hasMore = allMessages.size < filteredCount,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = SmsUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState is SmsUiState.Success && currentState.hasMore) {
            viewModelScope.launch {
                try {
                    currentPage++
                    val messages = getSmsUseCase(_filterState.value, currentPage)
                    allMessages.addAll(messages)

                    _uiState.value = SmsUiState.Success(
                        messages = allMessages.toList(),
                        totalCount = totalCount,
                        filteredCount = filteredCount,
                        hasMore = allMessages.size < filteredCount
                    )
                } catch (e: Exception) {
                    // Keep current state on error
                }
            }
        }
    }

    fun updateFilter(filterState: FilterState) {
        _filterState.value = filterState
        if (filterState.keyword.isNotEmpty()) {
            addFilterHistory(filterState.keyword)
        }
        loadMessages()
    }

    fun clearFilters() {
        _filterState.value = FilterState()
        loadMessages()
    }

    fun enterMultiSelectMode(id: Long) {
        _selectionState.value = _selectionState.value.enterMultiSelectMode(id)
    }

    fun exitMultiSelectMode() {
        _selectionState.value = _selectionState.value.exitMultiSelectMode()
    }

    fun toggleSelection(id: Long) {
        _selectionState.value = _selectionState.value.toggleSelection(id)
    }

    fun selectAll() {
        _selectionState.value = _selectionState.value.selectAll(filteredCount)
    }

    fun invertSelection() {
        _selectionState.value = _selectionState.value.invertSelection(allMessages.map { it.id })
    }

    fun deselectAll() {
        _selectionState.value = _selectionState.value.deselectAll()
    }

    fun clearSelection() {
        _selectionState.value = _selectionState.value.clearSelection()
    }

    /**
     * 请求删除单条消息（侧滑删除调用）
     * 会先检查是否需要设置默认短信App，然后显示确认对话框
     */
    fun requestDelete(messageId: Long) {
        // 检查是否需要设置默认短信App
        if (smsOperationManager.needsDefaultSmsApp()) {
            _showDefaultSmsDialog.value = true
            return
        }
        // 保存待删除的消息ID并显示确认对话框
        pendingDeleteMessageId = messageId
        _showDeleteConfirmDialog.value = true
    }

    /**
     * 请求删除选中的消息（多选删除调用）
     * 会先检查是否需要设置默认短信App，然后显示确认对话框
     */
    fun requestDeleteSelected() {
        // 检查是否需要设置默认短信App
        if (smsOperationManager.needsDefaultSmsApp()) {
            _showDefaultSmsDialog.value = true
            return
        }
        // 显示确认对话框
        _showDeleteConfirmDialog.value = true
    }

    /**
     * 确认删除（用户点击确认后调用）
     */
    fun confirmDelete() {
        _showDeleteConfirmDialog.value = false
        viewModelScope.launch {
            _operationState.value = OperationState.Progress(0, 0)
            try {
                val deletedCount = if (pendingDeleteMessageId != null) {
                    // 单条删除
                    smsOperationManager.deleteMessages(listOf(pendingDeleteMessageId!!))
                } else if (_selectionState.value.isSelectAll) {
                    // 全选删除
                    smsOperationManager.deleteMessagesByFilter(_filterState.value)
                } else {
                    // 多选删除
                    smsOperationManager.deleteMessages(_selectionState.value.selectedIds.toList())
                }

                _operationState.value = OperationState.Success("成功删除 $deletedCount 条短信")
                _selectionState.value = SelectionState()
                pendingDeleteMessageId = null
                loadMessages()
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "删除失败")
                pendingDeleteMessageId = null
            }
        }
    }

    /**
     * 取消删除（用户点击取消后调用）
     */
    fun cancelDelete() {
        _showDeleteConfirmDialog.value = false
        pendingDeleteMessageId = null
    }

    /**
     * 关闭默认短信App对话框
     */
    fun dismissDefaultSmsDialog() {
        _showDefaultSmsDialog.value = false
    }

    /**
     * 获取删除确认对话框中显示的消息数量
     */
    fun getDeleteCount(): Int {
        return if (pendingDeleteMessageId != null) {
            1
        } else {
            _selectionState.value.selectedCount
        }
    }

    fun exportMessages(exportAll: Boolean, uri: Uri) {
        viewModelScope.launch {
            _operationState.value = OperationState.Progress(0, 0)

            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw Exception("无法打开输出流")

                val result = exportSmsUseCase(
                    filterState = _filterState.value,
                    exportAll = exportAll,
                    outputStream = outputStream,
                    onProgress = { exported, total ->
                        _operationState.value = OperationState.Progress(exported, total)
                    }
                )

                result.fold(
                    onSuccess = { exportedCount ->
                        _operationState.value = OperationState.Success(
                            "导出完成！共导出 $exportedCount 条短信"
                        )
                    },
                    onFailure = { e ->
                        _operationState.value = OperationState.Error(e.message ?: "导出失败")
                    }
                )
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "导出失败")
            }
        }
    }

    fun importMessages(uri: Uri) {
        viewModelScope.launch {
            _operationState.value = OperationState.Progress(0, 0)

            try {
                val result = importSmsUseCase(
                    uri = uri,
                    onProgress = { imported, skipped ->
                        _operationState.value = OperationState.Progress(imported, imported + skipped)
                    }
                )

                result.fold(
                    onSuccess = { importResult ->
                        _operationState.value = OperationState.Success(
                            "导入完成！成功导入 ${importResult.imported} 条，跳过重复 ${importResult.skipped} 条"
                        )
                        loadMessages()
                    },
                    onFailure = { e ->
                        _operationState.value = OperationState.Error(e.message ?: "导入失败")
                    }
                )
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "导入失败")
            }
        }
    }

    /**
     * 请求导入短信
     * 会先检查是否需要设置默认短信App
     * @return true如果可以导入，false如果需要先设置默认短信App
     */
    fun requestImport(): Boolean {
        if (smsOperationManager.needsDefaultSmsApp()) {
            _showDefaultSmsDialog.value = true
            return false
        }
        return true
    }

    fun loadPreviewMessages() {
        viewModelScope.launch {
            try {
                val messages = if (pendingDeleteMessageId != null) {
                    // 单条删除预览
                    allMessages.filter { it.id == pendingDeleteMessageId }.take(5)
                } else if (_selectionState.value.isSelectAll) {
                    getSmsUseCase(_filterState.value, 0, 5)
                } else {
                    allMessages.filter { it.id in _selectionState.value.selectedIds }.take(5)
                }
                _previewMessages.value = messages
            } catch (e: Exception) {
                _previewMessages.value = emptyList()
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
