package top.yuameshi.sms.cleaner.ui.screen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.yuameshi.sms.cleaner.data.model.SelectionState

class SelectionManager {
    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    fun enterMultiSelectMode(id: Long) {
        _selectionState.value = _selectionState.value.enterMultiSelectMode(id)
    }

    fun exitMultiSelectMode() {
        _selectionState.value = _selectionState.value.exitMultiSelectMode()
    }

    fun toggleSelection(id: Long) {
        _selectionState.value = _selectionState.value.toggleSelection(id)
    }

    fun selectAll(totalCount: Int) {
        _selectionState.value = _selectionState.value.selectAll(totalCount)
    }

    fun invertSelection(allIds: List<Long>) {
        _selectionState.value = _selectionState.value.invertSelection(allIds)
    }

    fun deselectAll() {
        _selectionState.value = _selectionState.value.deselectAll()
    }

    fun clearSelection() {
        _selectionState.value = _selectionState.value.clearSelection()
    }
}
