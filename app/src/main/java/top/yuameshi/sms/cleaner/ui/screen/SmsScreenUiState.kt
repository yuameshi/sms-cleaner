package top.yuameshi.sms.cleaner.ui.screen

import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SelectionState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.model.SmsMessage

/**
 * Consolidated UI state for the SMS screen.
 * Replaces 12 independent StateFlows with a single state object.
 */
data class SmsScreenUiState(
    val smsState: SmsUiState = SmsUiState.Loading,
    val filterState: FilterState = FilterState(),
    val selectionState: SelectionState = SelectionState(),
    val operationState: OperationState = OperationState.Idle,
    val isDefaultSmsApp: Boolean = false,
    val hasPermissions: Boolean = false,
    val filterHistory: List<String> = emptyList(),
    val simCards: List<SimCardInfo> = emptyList(),
    val previewMessages: List<SmsMessage> = emptyList(),
    val isRefreshing: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showDefaultSmsDialog: Boolean = false
)
