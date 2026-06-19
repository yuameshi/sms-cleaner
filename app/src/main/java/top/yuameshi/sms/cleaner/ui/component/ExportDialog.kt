package top.yuameshi.sms.cleaner.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportDialog(
    filteredCount: Int,
    totalCount: Int,
    hasFilters: Boolean,
    onExport: (exportAll: Boolean, uri: Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var exportAll by remember { mutableStateOf(!hasFilters) }
    var fileName by remember {
        mutableStateOf("sms_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}")
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { onExport(exportAll, it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出短信") },
        text = {
            Column {
                // Export scope
                Text("导出范围：", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                if (hasFilters) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !exportAll,
                            onClick = { exportAll = false }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出当前筛选结果 ($filteredCount 条)")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = exportAll,
                        onClick = { exportAll = true }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出全部短信 ($totalCount 条)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File name
                Text("文件名：", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text(".csv") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    createDocumentLauncher.launch("$fileName.csv")
                },
                enabled = fileName.isNotEmpty()
            ) {
                Text("开始导出")
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
