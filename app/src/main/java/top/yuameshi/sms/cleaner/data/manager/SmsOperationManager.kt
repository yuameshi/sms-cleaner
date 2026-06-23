package top.yuameshi.sms.cleaner.data.manager

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import top.yuameshi.sms.cleaner.util.DefaultSmsManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一的短信数据库操作管理器
 * 所有数据库操作（导入、删除）都通过此接口调用
 * 负责判断是否需要展示设置默认短信App的提示
 */
@Singleton
class SmsOperationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
) {
    /**
     * 判断当前操作是否需要设置为默认短信App
     * 删除和写入操作都需要默认短信App权限
     */
    fun needsDefaultSmsApp(): Boolean {
        return !DefaultSmsManager.isDefaultSmsApp(context)
    }

    /**
     * 检查是否为默认短信App，如果不是则抛出异常
     * 用于在实际执行写入操作前进行检查
     * @throws IllegalStateException 如果不是默认短信App
     */
    fun checkDefaultSmsAppOrThrow() {
        if (needsDefaultSmsApp()) {
            throw IllegalStateException("需要设置为默认短信App才能执行此操作")
        }
    }

    /**
     * 删除指定ID的短信
     * @param ids 要删除的短信ID列表
     * @return 实际删除的短信数量
     * @throws IllegalStateException 如果不是默认短信App
     */
    suspend fun deleteMessages(ids: List<Long>): Int {
        checkDefaultSmsAppOrThrow()
        return smsRepository.deleteMessages(ids)
    }

    /**
     * 根据筛选条件删除短信
     * @param filterState 筛选条件
     * @return 实际删除的短信数量
     * @throws IllegalStateException 如果不是默认短信App
     */
    suspend fun deleteMessagesByFilter(filterState: FilterState): Int {
        checkDefaultSmsAppOrThrow()
        return smsRepository.deleteMessagesByFilter(filterState)
    }

    /**
     * 插入一条短信
     * @param address 号码
     * @param body 内容
     * @param date 时间戳
     * @param type 类型
     * @param read 是否已读
     * @param subId SIM卡ID
     * @return 插入的URI，失败返回null
     * @throws IllegalStateException 如果不是默认短信App
     */
    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri? {
        checkDefaultSmsAppOrThrow()
        return smsRepository.insertMessage(address, body, date, type, read, subId)
    }

    /**
     * 检查短信是否重复
     * @param address 号码
     * @param body 内容
     * @param date 时间戳
     * @return 是否重复
     */
    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean {
        return smsRepository.checkDuplicate(address, body, date)
    }

    fun getSimCards(): List<SimCardInfo> {
        return smsRepository.getSimCards()
    }
}
