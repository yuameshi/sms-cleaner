package top.yuameshi.sms.cleaner.domain.usecase

import top.yuameshi.sms.cleaner.data.model.FilterState
import javax.inject.Inject

class FilterSmsUseCase @Inject constructor() {
    fun buildFilterState(
        keyword: String = "",
        number: String = "",
        dateRange: FilterState.DateRange = FilterState.DateRange.ALL,
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        readStatus: FilterState.ReadStatus = FilterState.ReadStatus.ALL,
        lockStatus: FilterState.LockStatus = FilterState.LockStatus.ALL,
        messageType: FilterState.MessageType = FilterState.MessageType.ALL,
        simId: FilterState.SimId = FilterState.SimId.ALL,
        contactId: Long? = null,
        contactName: String? = null
    ): FilterState {
        return FilterState(
            keyword = keyword,
            number = number,
            dateRange = dateRange,
            customStartDate = customStartDate,
            customEndDate = customEndDate,
            readStatus = readStatus,
            lockStatus = lockStatus,
            messageType = messageType,
            simId = simId,
            contactId = contactId,
            contactName = contactName
        )
    }
}
