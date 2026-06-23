package top.yuameshi.sms.cleaner.domain.usecase

import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import javax.inject.Inject

/**
 * 删除类型
 */
sealed class DeleteType {
    /** 删除单条消息 */
    data object Single : DeleteType()
    /** 删除多条消息（用户手动选择） */
    data object Multiple : DeleteType()
    /** 按筛选条件删除全部匹配消息 */
    data object AllByFilter : DeleteType()
}

/**
 * 删除短信用例
 * 封装删除逻辑，依赖 SmsRepository 接口实现依赖反转
 */
class DeleteSmsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    /**
     * 执行删除操作
     * @param deleteType 删除类型
     * @param ids 消息ID列表，Single 和 Multiple 类型时必传
     * @param filterState 筛选条件，AllByFilter 类型时必传
     * @return Result 包含实际删除的消息数量
     */
    suspend operator fun invoke(
        deleteType: DeleteType,
        ids: List<Long>? = null,
        filterState: FilterState? = null
    ): Result<Int> {
        return try {
            val deletedCount = when (deleteType) {
                DeleteType.Single -> {
                    requireNotNull(ids) { "Single delete requires ids" }
                    smsRepository.deleteMessages(ids)
                }
                DeleteType.Multiple -> {
                    requireNotNull(ids) { "Multiple delete requires ids" }
                    smsRepository.deleteMessages(ids)
                }
                DeleteType.AllByFilter -> {
                    requireNotNull(filterState) { "AllByFilter delete requires filterState" }
                    smsRepository.deleteMessagesByFilter(filterState)
                }
            }
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
