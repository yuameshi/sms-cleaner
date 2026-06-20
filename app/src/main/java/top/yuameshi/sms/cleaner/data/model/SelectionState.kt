package top.yuameshi.sms.cleaner.data.model

data class SelectionState(
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectAll: Boolean = false,
    val totalFilteredCount: Int = 0
) {
    val selectedCount: Int
        get() = if (isSelectAll) totalFilteredCount else selectedIds.size

    fun toggleSelection(id: Long): SelectionState {
        val newSelectedIds = if (id in selectedIds) {
            selectedIds - id
        } else {
            selectedIds + id
        }
        return copy(
            selectedIds = newSelectedIds,
            isSelectAll = false
        )
    }

    fun selectAll(totalCount: Int): SelectionState {
        return copy(
            isSelectAll = true,
            totalFilteredCount = totalCount
        )
    }

    fun clearSelection(): SelectionState {
        return copy(
            selectedIds = emptySet(),
            isSelectAll = false,
            totalFilteredCount = 0
        )
    }

    fun deselectAll(): SelectionState {
        return copy(
            selectedIds = emptySet(),
            isSelectAll = false,
            totalFilteredCount = 0
        )
    }

    fun invertSelection(allIds: List<Long>): SelectionState {
        val newSelected = allIds.toSet() - selectedIds
        return copy(
            selectedIds = newSelected,
            isSelectAll = false
        )
    }

    fun enterMultiSelectMode(id: Long): SelectionState {
        return copy(
            isMultiSelectMode = true,
            selectedIds = setOf(id)
        )
    }

    fun exitMultiSelectMode(): SelectionState {
        return copy(
            isMultiSelectMode = false,
            selectedIds = emptySet(),
            isSelectAll = false,
            totalFilteredCount = 0
        )
    }

    fun isSelected(id: Long): Boolean {
        return isSelectAll || id in selectedIds
    }
}
