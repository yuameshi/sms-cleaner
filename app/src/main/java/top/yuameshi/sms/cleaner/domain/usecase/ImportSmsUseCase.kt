package top.yuameshi.sms.cleaner.domain.usecase

import android.content.Context
import android.net.Uri
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

            // Skip BOM if present
            reader.mark(3)
            val bom = CharArray(3)
            reader.read(bom)
            if (bom[0] != '\uFEFF') {
                reader.reset()
            }

            // Skip header
            val header = reader.readLine()
            if (!isValidHeader(header)) {
                return@withContext Result.failure(Exception("文件格式不正确"))
            }

            var imported = 0
            var skipped = 0
            var lineNumber = 1

            var line = reader.readLine()
            while (line != null) {
                lineNumber++
                try {
                    val fields = parseCsvLine(line)
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

                            // Check for duplicate
                            val isDuplicate = smsOperationManager.checkDuplicate(address, body, date)
                            if (isDuplicate) {
                                skipped++
                            } else {
                                smsOperationManager.insertMessage(address, body, date, type, read, 0)
                                imported++
                            }

                            onProgress(imported, skipped)
                        } else {
                            skipped++
                        }
                    }
                } catch (e: IllegalStateException) {
                    // 不是默认短信App，直接抛出异常让调用者处理
                    throw e
                } catch (e: Exception) {
                    // Skip invalid lines
                    skipped++
                }
                line = reader.readLine()
            }

            reader.close()
            Result.success(ImportResult(imported, skipped))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidHeader(header: String?): Boolean {
        if (header == null) return false
        val expectedColumns = listOf("ID", "号码", "内容", "时间", "类型", "已读状态", "锁定状态", "SIM卡", "发送状态")
        return expectedColumns.all { header.contains(it) }
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> {
                    if (inQuotes && current.isNotEmpty() && current.last() == '"') {
                        current.append('"')
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        fields.add(current.toString())

        return fields
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

    data class ImportResult(
        val imported: Int,
        val skipped: Int
    )
}
