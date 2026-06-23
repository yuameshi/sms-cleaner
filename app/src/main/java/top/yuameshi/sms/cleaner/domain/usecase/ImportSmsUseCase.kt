package top.yuameshi.sms.cleaner.domain.usecase

import android.content.Context
import android.net.Uri
import android.telephony.SubscriptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yuameshi.sms.cleaner.data.manager.SmsOperationManager
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class ImportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsOperationManager: SmsOperationManager
) {
    suspend operator fun invoke(
        uri: Uri,
        onProgress: (imported: Int, skipped: Int) -> Unit
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("无法打开文件"))

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            // Skip BOM if present (UTF-8 BOM is single char \uFEFF after decoding)
            reader.mark(1)
            val firstChar = reader.read()
            if (firstChar != '\uFEFF'.code) {
                reader.reset()
            }

            // Read and validate header (handle multi-line header if needed)
            val headerLine = readCsvLine(reader)
            if (headerLine == null || !isValidHeader(headerLine.joinToString(","))) {
                return@withContext Result.failure(Exception("文件格式不正确"))
            }

            var imported = 0
            var skipped = 0

            // Read records (each record may span multiple lines due to quoted fields)
            var fields = readCsvLine(reader)
            while (fields != null) {
                try {
                    if (fields.size >= 9) {
                        val address = fields[1]
                        val body = fields[2]
                        val dateStr = fields[3]
                        val typeName = fields[4]
                        val readStatus = fields[5]

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date = dateFormat.parse(dateStr)?.time
                        if (date != null) {
                            val type = getTypeFromName(typeName)
                            val read = readStatus == "已读"

                            // Check for duplicate (same address, body, and date)
                            val isDuplicate = smsOperationManager.checkDuplicate(address, body, date)
                            if (isDuplicate) {
                                skipped++
                            } else {
                                // Use default subscription ID (first active SIM)
                                val defaultSubId = getDefaultSubscriptionId()
                                smsOperationManager.insertMessage(address, body, date, type, read, defaultSubId)
                                imported++
                            }

                            onProgress(imported, skipped)
                        } else {
                            skipped++
                        }
                    } else {
                        skipped++
                    }
                } catch (e: IllegalStateException) {
                    // Not default SMS app, throw to caller
                    throw e
                } catch (e: Exception) {
                    // Skip invalid lines
                    skipped++
                }
                fields = readCsvLine(reader)
            }

            reader.close()
            Result.success(ImportResult(imported, skipped))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read a single CSV record from the reader, handling multi-line quoted fields.
     * Returns a list of field values, or null if end of stream.
     * Implements RFC 4180 parsing:
     * - Fields may be enclosed in double quotes
     * - Quoted fields may contain line breaks (CRLF), commas, and escaped quotes
     * - Escaped quotes are represented as ""
     */
    private fun readCsvLine(reader: BufferedReader): List<String>? {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var prevChar: Char? = null

        // Read until we get a complete record (not in the middle of a quoted field)
        while (true) {
            val c = reader.read()
            if (c == -1) {
                // End of stream
                if (current.isNotEmpty() || fields.isNotEmpty()) {
                    // Add the last field
                    fields.add(current.toString())
                    return if (fields.isNotEmpty()) fields else null
                }
                return null
            }

            val char = c.toChar()

            when {
                char == '"' -> {
                    if (inQuotes) {
                        // Inside quotes: check for escaped quote ("")
                        val nextC = reader.read()
                        if (nextC != -1) {
                            val nextChar = nextC.toChar()
                            if (nextChar == '"') {
                                // Escaped quote - add a single quote
                                current.append('"')
                            } else {
                                // End of quoted field
                                inQuotes = false
                                // Process the next character
                                when {
                                    nextChar == ',' -> {
                                        fields.add(current.toString())
                                        current.clear()
                                    }
                                    nextChar == '\r' || nextChar == '\n' -> {
                                        // End of record
                                        fields.add(current.toString())
                                        // Consume LF if CRLF
                                        if (nextChar == '\r') {
                                            reader.mark(1)
                                            val peek = reader.read()
                                            if (peek != -1 && peek.toChar() != '\n') {
                                                reader.reset()
                                            }
                                        }
                                        return fields
                                    }
                                    else -> {
                                        // Unexpected character after closing quote
                                        current.append(nextChar)
                                    }
                                }
                            }
                        } else {
                            // End of stream after closing quote
                            inQuotes = false
                            fields.add(current.toString())
                            return fields
                        }
                    } else {
                        // Start of quoted field
                        inQuotes = true
                    }
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                (char == '\r' || char == '\n') && !inQuotes -> {
                    // End of record (outside quotes)
                    fields.add(current.toString())
                    // Consume LF if CRLF
                    if (char == '\r') {
                        reader.mark(1)
                        val peek = reader.read()
                        if (peek != -1 && peek.toChar() != '\n') {
                            reader.reset()
                        }
                    }
                    return fields
                }
                else -> {
                    current.append(char)
                }
            }

            prevChar = char
        }
    }

    private fun isValidHeader(header: String): Boolean {
        val expectedColumns = listOf("ID", "号码", "内容", "时间", "类型", "已读状态", "锁定状态", "SIM卡", "发送状态")
        return expectedColumns.all { header.contains(it) }
    }

    private fun getTypeFromName(typeName: String): Int {
        return when (typeName) {
            "收件箱" -> SmsMessage.TYPE_INBOX
            "已发送" -> SmsMessage.TYPE_SENT
            "草稿" -> SmsMessage.TYPE_DRAFT
            "发件箱" -> SmsMessage.TYPE_OUTBOX
            else -> SmsMessage.TYPE_INBOX
        }
    }

    /**
     * Get the default subscription ID (first active SIM card)
     */
    private fun getDefaultSubscriptionId(): Int {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptions = subscriptionManager.activeSubscriptionInfoList
            subscriptions?.firstOrNull()?.subscriptionId ?: 0
        } catch (e: Exception) {
            0
        }
    }

    data class ImportResult(
        val imported: Int,
        val skipped: Int
    )
}
