package top.yuameshi.sms.cleaner.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.ui.theme.HighlightYellow
import top.yuameshi.sms.cleaner.ui.theme.TypeTagInbox
import top.yuameshi.sms.cleaner.ui.theme.TypeTagSent
import top.yuameshi.sms.cleaner.ui.theme.TypeTagDraft
import top.yuameshi.sms.cleaner.ui.theme.TypeTagOutbox
import top.yuameshi.sms.cleaner.ui.theme.TypeTagDefault
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SmsListItem(
    message: SmsMessage,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    keyword: String,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxLength = 100

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                if (isMultiSelectMode) {
                    // In multi-select mode, select the item instead of deleting
                    onItemClick()
                    false // Don't dismiss
                } else {
                    onDeleteClick()
                    false
                }
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (isMultiSelectMode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFFF44336) // DeleteRed
                        }
                    }
                    else -> Color.LightGray
                },
                label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (isMultiSelectMode) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Delete
                    },
                    contentDescription = if (isMultiSelectMode) "选中" else "删除",
                    tint = Color.White
                )
            }
        }
    ) {
        CardContent(
            message = message,
            isSelected = isSelected,
            isMultiSelectMode = isMultiSelectMode,
            keyword = keyword,
            isExpanded = isExpanded,
            maxLength = maxLength,
            onItemClick = onItemClick,
            onLongClick = onLongClick,
            onToggleExpand = { isExpanded = !isExpanded }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardContent(
    message: SmsMessage,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    keyword: String,
    isExpanded: Boolean,
    maxLength: Int,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection checkbox or avatar
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                )
            } else {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitial(message),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contact name or number
                    Text(
                        text = message.contactName ?: message.address,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Date
                    Text(
                        text = formatDate(message.date),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message body
                val bodyText = if (message.body.length > maxLength && !isExpanded) {
                    message.body.take(maxLength) + "..."
                } else {
                    message.body
                }

                if (keyword.isNotEmpty()) {
                    Text(
                        text = highlightKeyword(bodyText, keyword),
                        fontSize = 14.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = bodyText,
                        fontSize = 14.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (message.body.length > maxLength) {
                    TextButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "收起" else "展开",
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = getTypeColor(message.type),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = SmsMessage.getTypeName(message.type),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // SIM card
                    Text(
                        text = "SIM ${message.subId}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Read status indicator
                    if (!message.read) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun getInitial(message: SmsMessage): String {
    val name = message.contactName ?: message.address
    return if (name.isNotEmpty()) {
        name.first().toString()
    } else {
        "?"
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 24 * 60 * 60 * 1000 -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

private fun getTypeColor(type: Int): Color {
    return when (type) {
        SmsMessage.TYPE_INBOX -> TypeTagInbox
        SmsMessage.TYPE_SENT -> TypeTagSent
        SmsMessage.TYPE_DRAFT -> TypeTagDraft
        SmsMessage.TYPE_OUTBOX -> TypeTagOutbox
        else -> TypeTagDefault
    }
}

private fun highlightKeyword(text: String, keyword: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var startIndex = 0
        val lowerText = text.lowercase()
        val lowerKeyword = keyword.lowercase()

        while (startIndex < text.length) {
            val index = lowerText.indexOf(lowerKeyword, startIndex)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }

            append(text.substring(startIndex, index))
            withStyle(style = SpanStyle(background = HighlightYellow)) {
                append(text.substring(index, index + keyword.length))
            }
            startIndex = index + keyword.length
        }
    }
}
