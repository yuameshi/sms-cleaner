package top.yuameshi.sms.cleaner.data.repository

import android.net.Uri
import top.yuameshi.sms.cleaner.data.datasource.SmsDataSource
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor(
    private val smsDataSource: SmsDataSource
) {
    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage> {
        val messages = smsDataSource.getSmsMessages(filterState, page, pageSize)

        // Apply regex filter in memory if needed
        return if (filterState.regex.isNotEmpty()) {
            try {
                val regex = Regex(filterState.regex)
                messages.filter { regex.containsMatchIn(it.body) }
            } catch (e: Exception) {
                messages
            }
        } else {
            messages
        }
    }

    suspend fun getTotalCount(filterState: FilterState): Int {
        return smsDataSource.getTotalCount(filterState)
    }

    suspend fun deleteMessages(ids: List<Long>): Int {
        return smsDataSource.deleteMessages(ids)
    }

    suspend fun deleteMessagesByFilter(filterState: FilterState): Int {
        // If regex filter is active, we need to get matching messages first
        // then delete by IDs, since SQL doesn't support regex natively
        return if (filterState.regex.isNotEmpty()) {
            try {
                val regex = Regex(filterState.regex)
                // Get all messages matching the filter (without regex)
                val allMessages = mutableListOf<SmsMessage>()
                var page = 0
                val pageSize = 100
                while (true) {
                    val messages = smsDataSource.getSmsMessages(filterState.copy(regex = ""), page, pageSize)
                    if (messages.isEmpty()) break
                    allMessages.addAll(messages)
                    page++
                }
                // Filter by regex in memory
                val matchingIds = allMessages.filter { regex.containsMatchIn(it.body) }.map { it.id }
                if (matchingIds.isNotEmpty()) {
                    smsDataSource.deleteMessages(matchingIds)
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        } else {
            smsDataSource.deleteMessagesByFilter(filterState)
        }
    }

    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri? {
        return smsDataSource.insertMessage(address, body, date, type, read, subId)
    }

    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean {
        return smsDataSource.checkDuplicate(address, body, date)
    }
}
