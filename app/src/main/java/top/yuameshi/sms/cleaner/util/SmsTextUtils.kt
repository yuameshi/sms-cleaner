package top.yuameshi.sms.cleaner.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getInitial(message: SmsMessage): String {
    val name = message.contactName ?: message.address
    return if (name.isNotEmpty()) {
        name.first().toString()
    } else {
        "?"
    }
}

fun formatDate(timestamp: Long): String {
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

@Composable
fun highlightKeyword(text: String, keyword: String): AnnotatedString {
    val highlightBackground = MaterialTheme.colorScheme.tertiaryContainer
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
            withStyle(style = SpanStyle(background = highlightBackground)) {
                append(text.substring(index, index + keyword.length))
            }
            startIndex = index + keyword.length
        }
    }
}
