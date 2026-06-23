package top.yuameshi.sms.cleaner.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import top.yuameshi.sms.cleaner.util.CsvParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class ImportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
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
            val headerLine = CsvParser.readCsvLine(reader)
            if (headerLine == null || !isValidHeader(headerLine.joinToString(","))) {
                return@withContext Result.failure(Exception("文件格式不正确"))
            }

            var imported = 0
            var skipped = 0

            // Read records (each record may span multiple lines due to quoted fields)
            var fields = CsvParser.readCsvLine(reader)
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
                            val isDuplicate = smsRepository.checkDuplicate(address, body, date)
                            if (isDuplicate) {
                                skipped++
                            } else {
                                // Use default subscription ID (first active SIM)
                                val defaultSubId = getDefaultSubscriptionId()
                                smsRepository.insertMessage(address, body, date, type, read, defaultSubId)
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
                fields = CsvParser.readCsvLine(reader)
            }

            reader.close()
            Result.success(ImportResult(imported, skipped))
        } catch (e: Exception) {
            Result.failure(e)
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
            val simCards = smsRepository.getSimCards()
            simCards.firstOrNull()?.subscriptionId ?: 0
        } catch (e: Exception) {
            0
        }
    }

    data class ImportResult(
        val imported: Int,
        val skipped: Int
    )
}
