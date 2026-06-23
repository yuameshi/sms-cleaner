package top.yuameshi.sms.cleaner.ui.screen

import top.yuameshi.sms.cleaner.data.model.SmsMessage

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
