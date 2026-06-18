package top.yuameshi.sms.cleaner.domain.usecase

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(
        filterState: FilterState,
        exportAll: Boolean,
        fileName: String,
        onProgress: (exported: Int, total: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val totalCount = if (exportAll) {
                smsRepository.getTotalCount(FilterState())
            } else {
                smsRepository.getTotalCount(filterState)
            }

            val file = createExportFile(fileName)
            val outputStream = FileOutputStream(file)

            // Write BOM for UTF-8
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            // Write header
            val header = "ID,号码,内容,时间,类型,已读状态,锁定状态,SIM卡,发送状态"
            outputStream.write("$header\n".toByteArray(Charsets.UTF_8))

            var exported = 0
            var page = 0
            val pageSize = 100

            while (exported < totalCount) {
                val messages = if (exportAll) {
                    smsRepository.getSmsMessages(FilterState(), page, pageSize)
                } else {
                    smsRepository.getSmsMessages(filterState, page, pageSize)
                }

                if (messages.isEmpty()) break

                messages.forEach { message ->
                    val line = formatCsvLine(message)
                    outputStream.write("$line\n".toByteArray(Charsets.UTF_8))
                    exported++
                    onProgress(exported, totalCount)
                }

                page++
            }

            outputStream.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createExportFile(fileName: String): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        return File(documentsDir, "$fileName.csv")
    }

    private fun formatCsvLine(message: SmsMessage): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(message.date))
        val typeName = SmsMessage.getTypeName(message.type)
        val readStatus = if (message.read) "已读" else "未读"
        val lockStatus = if (message.locked) "锁定" else "未锁定"
        val simCard = "SIM ${message.subId}"

        return listOf(
            message.id.toString(),
            escapeCsvField(message.address),
            escapeCsvField(message.body),
            dateStr,
            typeName,
            readStatus,
            lockStatus,
            simCard,
            "成功"
        ).joinToString(",")
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
