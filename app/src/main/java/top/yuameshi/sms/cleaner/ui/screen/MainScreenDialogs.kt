package top.yuameshi.sms.cleaner.ui.screen

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.ui.component.DeleteConfirmDialog
import top.yuameshi.sms.cleaner.ui.component.ExportDialog
import top.yuameshi.sms.cleaner.ui.component.ImportDialog

@Composable
fun MainScreenDialogs(
    // Delete confirmation dialog
    showDeleteConfirmDialog: Boolean,
    previewMessages: List<SmsMessage>,
    deleteCount: Int,
    onLoadPreviewMessages: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    // Export dialog
    showExportDialog: Boolean,
    isMultiSelectMode: Boolean,
    selectedCount: Int,
    filteredCount: Int,
    totalCount: Int,
    hasFilters: Boolean,
    onExport: (ExportScope, Uri) -> Unit,
    onDismissExport: () -> Unit,
    // Import dialog
    showImportDialog: Boolean,
    onImport: (Uri) -> Unit,
    onDismissImport: () -> Unit,
    // Default SMS app dialog
    showDefaultSmsDialog: Boolean,
    onDismissDefaultSmsDialog: () -> Unit,
    onConfirmDefaultSms: () -> Unit,
    // Operation state dialog
    operationState: OperationState,
    onResetOperationState: () -> Unit
) {
    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        LaunchedEffect(Unit) {
            onLoadPreviewMessages()
        }
        DeleteConfirmDialog(
            count = deleteCount,
            previewMessages = previewMessages,
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            isMultiSelectMode = isMultiSelectMode,
            selectedCount = selectedCount,
            filteredCount = filteredCount,
            totalCount = totalCount,
            hasFilters = hasFilters,
            onExport = onExport,
            onDismiss = onDismissExport
        )
    }

    // Import dialog
    if (showImportDialog) {
        ImportDialog(
            onImport = onImport,
            onDismiss = onDismissImport
        )
    }

    // Default SMS app dialog
    if (showDefaultSmsDialog) {
        AlertDialog(
            onDismissRequest = onDismissDefaultSmsDialog,
            title = { Text("需要设置默认短信App") },
            text = { Text("为了删除或导入短信，需要将本App设为默认短信App。操作完成后可随时恢复原来的短信App。") },
            confirmButton = {
                TextButton(onClick = onConfirmDefaultSms) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDefaultSmsDialog) {
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
                onDismissRequest = onResetOperationState,
                title = { Text("完成") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onResetOperationState) {
                        Text("确定")
                    }
                }
            )
        }
        is OperationState.Error -> {
            AlertDialog(
                onDismissRequest = onResetOperationState,
                title = { Text("错误") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onResetOperationState) {
                        Text("确定")
                    }
                }
            )
        }
        else -> {}
    }
}
