package top.yuameshi.sms.cleaner.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportDialog(
    onImport: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            showConfirmDialog = true
        }
    }

    // File picker dialog
    if (!showConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("导入短信") },
            text = {
                Column {
                    Text("选择CSV文件导入短信。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "注意：仅支持本App导出的CSV格式文件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { launcher.launch("*/*") }
                ) {
                    Text("选择文件")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            modifier = modifier
        )
    }

    // Confirmation dialog
    if (showConfirmDialog && selectedUri != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedUri = null
                onDismiss()
            },
            title = { Text("确认导入") },
            text = {
                Column {
                    Text("即将导入选中的CSV文件。")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "导入规则：",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 具有相同号码、内容、时间的行将自动跳过")
                    Text("• 将按照 SIM 卡槽为目标进行导入")
                    Text("• 文件格式不正确的行将被忽略")
                    Text("• 导入过程中请勿关闭应用")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "是否继续导入？",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedUri?.let { onImport(it) }
                        selectedUri = null
                    }
                ) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedUri = null
                    }
                ) {
                    Text("取消")
                }
            },
            modifier = modifier
        )
    }
}
