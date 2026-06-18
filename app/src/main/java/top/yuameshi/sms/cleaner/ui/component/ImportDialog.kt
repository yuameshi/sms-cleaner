package top.yuameshi.sms.cleaner.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportDialog(
    onImport: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImport(it) }
    }

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
                onClick = { launcher.launch("text/csv") }
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
