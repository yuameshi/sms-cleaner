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
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SelectionState
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.domain.usecase.DeleteSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.ExportSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.FilterSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.GetSmsUseCase
import top.yuameshi.sms.cleaner.domain.usecase.ImportSmsUseCase
import top.yuameshi.sms.cleaner.util.DefaultSmsManager
import top.yuameshi.sms.cleaner.util.PermissionUtils
import javax.inject.Inject

sealed class SmsUiState {
    object Loading : SmsUiState()
    data class Success(
        val messages: List<SmsMessage>,
        val totalCount: Int,
        val filteredCount: Int,
        val hasMore: Boolean
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
    private val deleteSmsUseCase: DeleteSmsUseCase,
    private val exportSmsUseCase: ExportSmsUseCase,
    private val importSmsUseCase: ImportSmsUseCase
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

    private var currentPage = 0
    private var allMessages = mutableListOf<SmsMessage>()
    private var totalCount = 0
    private var filteredCount = 0

    init {
        checkPermissionsAndDefaultSms()
    }

    fun checkPermissionsAndDefaultSms() {
        _hasPermissions.value = PermissionUtils.hasAllPermissions(context)
        _isDefaultSmsApp.value = DefaultSmsManager.isDefaultSmsApp(context)
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = SmsUiState.Loading
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
                    hasMore = allMessages.size < filteredCount
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

    fun clearSelection() {
        _selectionState.value = _selectionState.value.clearSelection()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _operationState.value = OperationState.Progress(0, _selectionState.value.selectedCount)

            try {
                val deletedCount = if (_selectionState.value.isSelectAll) {
                    deleteSmsUseCase.deleteByFilter(_filterState.value)
                } else {
                    deleteSmsUseCase(_selectionState.value.selectedIds.toList())
                }

                _operationState.value = OperationState.Success("成功删除 $deletedCount 条短信")
                _selectionState.value = SelectionState()
                loadMessages()
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "删除失败")
            }
        }
    }

    fun exportMessages(exportAll: Boolean, fileName: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Progress(0, 0)

            try {
                val result = exportSmsUseCase(
                    filterState = _filterState.value,
                    exportAll = exportAll,
                    fileName = fileName,
                    onProgress = { exported, total ->
                        _operationState.value = OperationState.Progress(exported, total)
                    }
                )

                result.fold(
                    onSuccess = { file ->
                        _operationState.value = OperationState.Success(
                            "导出完成！文件：${file.absolutePath}，大小：${file.length() / 1024} KB"
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

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
