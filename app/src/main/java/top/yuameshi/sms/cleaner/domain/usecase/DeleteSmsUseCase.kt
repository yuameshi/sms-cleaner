package top.yuameshi.sms.cleaner.domain.usecase

import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import javax.inject.Inject

class DeleteSmsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(ids: List<Long>): Int {
        return smsRepository.deleteMessages(ids)
    }

    suspend fun deleteByFilter(filterState: FilterState): Int {
        return smsRepository.deleteMessagesByFilter(filterState)
    }
}
