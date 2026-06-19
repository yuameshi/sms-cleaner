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
        // If regex is active, we need to fetch all messages and filter in memory
        // because SQL doesn't support regex natively
        return if (filterState.regex.isNotEmpty()) {
            try {
                val regex = Regex(filterState.regex)
                // Get all messages matching the filter (without regex)
                val allMessages = mutableListOf<SmsMessage>()
                var currentPage = 0
                val fetchPageSize = 100
                while (true) {
                    val messages = smsDataSource.getSmsMessages(filterState.copy(regex = ""), currentPage, fetchPageSize)
                    if (messages.isEmpty()) break
                    allMessages.addAll(messages)
                    currentPage++
                }
                // Filter by regex in memory and apply pagination
                val filteredMessages = allMessages.filter { regex.containsMatchIn(it.body) }
                val startIndex = page * pageSize
                val endIndex = minOf(startIndex + pageSize, filteredMessages.size)
                if (startIndex < filteredMessages.size) {
                    filteredMessages.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                // Fallback to non-regex query
                smsDataSource.getSmsMessages(filterState.copy(regex = ""), page, pageSize)
            }
        } else {
            smsDataSource.getSmsMessages(filterState, page, pageSize)
        }
    }

    suspend fun getTotalCount(filterState: FilterState): Int {
        // If regex is active, we need to count all matching messages in memory
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
                // Count messages matching regex
                allMessages.count { regex.containsMatchIn(it.body) }
            } catch (e: Exception) {
                // Fallback to non-regex count
                smsDataSource.getTotalCount(filterState.copy(regex = ""))
            }
        } else {
            smsDataSource.getTotalCount(filterState)
        }
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
