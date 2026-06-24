package top.yuameshi.sms.cleaner.data.repository

import android.net.Uri
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.model.SmsMessage

interface SmsRepository {
    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    suspend fun getTotalCount(filterState: FilterState): Int

    suspend fun deleteMessages(ids: List<Long>): Int

    suspend fun deleteMessagesByFilter(filterState: FilterState): Int

    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri?

    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean

    suspend fun getSmsMessagesByIds(ids: List<Long>): List<SmsMessage>

    fun getSimCards(): List<SimCardInfo>
}
