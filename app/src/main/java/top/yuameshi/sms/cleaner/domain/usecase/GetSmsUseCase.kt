package top.yuameshi.sms.cleaner.domain.usecase

import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import javax.inject.Inject

class GetSmsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage> {
        return smsRepository.getSmsMessages(filterState, page, pageSize)
    }

    suspend fun getTotalCount(filterState: FilterState): Int {
        return smsRepository.getTotalCount(filterState)
    }
}
