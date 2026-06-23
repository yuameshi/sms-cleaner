package top.yuameshi.sms.cleaner.data.repository

import android.net.Uri
import top.yuameshi.sms.cleaner.data.datasource.SmsDataSource
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val smsDataSource: SmsDataSource
) : SmsRepository {
    override suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int
    ): List<SmsMessage> {
        return smsDataSource.getSmsMessages(filterState, page, pageSize)
    }

    override suspend fun getTotalCount(filterState: FilterState): Int {
        return smsDataSource.getTotalCount(filterState)
    }

    override suspend fun deleteMessages(ids: List<Long>): Int {
        return smsDataSource.deleteMessages(ids)
    }

    override suspend fun deleteMessagesByFilter(filterState: FilterState): Int {
        return smsDataSource.deleteMessagesByFilter(filterState)
    }

    override suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri? {
        return smsDataSource.insertMessage(address, body, date, type, read, subId)
    }

    override suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean {
        return smsDataSource.checkDuplicate(address, body, date)
    }

    override fun getSimCards(): List<SimCardInfo> {
        return smsDataSource.getSimCards()
    }
}
