package top.yuameshi.sms.cleaner.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MultiSelectBottomBar(
    hasSelection: Boolean,
    onDeleteClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onInvertSelectionClick: () -> Unit,
    onDeselectAllClick: () -> Unit,
    onExportClick: () -> Unit
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Delete
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDeleteClick,
                    enabled = hasSelection
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
                Text("删除", style = MaterialTheme.typography.labelSmall)
            }

            // Select All
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSelectAllClick) {
                    Icon(Icons.Default.SelectAll, contentDescription = "全选")
                }
                Text("全选", style = MaterialTheme.typography.labelSmall)
            }

            // Invert Selection
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onInvertSelectionClick) {
                    Icon(Icons.Default.Flip, contentDescription = "反选")
                }
                Text("反选", style = MaterialTheme.typography.labelSmall)
            }

            // Deselect All
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDeselectAllClick,
                    enabled = hasSelection
                ) {
                    Icon(Icons.Default.Deselect, contentDescription = "取消全选")
                }
                Text("取消全选", style = MaterialTheme.typography.labelSmall)
            }

            // Export
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onExportClick,
                    enabled = hasSelection
                ) {
                    Icon(Icons.Default.GetApp, contentDescription = "导出")
                }
                Text("导出", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
