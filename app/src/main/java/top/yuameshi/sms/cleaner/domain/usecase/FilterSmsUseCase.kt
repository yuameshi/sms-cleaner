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

    fun getDateRangeMillis(dateRange: FilterState.DateRange): Pair<Long, Long>? {
        val now = System.currentTimeMillis()
        return when (dateRange) {
            FilterState.DateRange.TODAY -> {
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            FilterState.DateRange.LAST_7_DAYS -> {
                Pair(now - 7 * 24 * 60 * 60 * 1000, now)
            }
            FilterState.DateRange.LAST_30_DAYS -> {
                Pair(now - 30 * 24 * 60 * 60 * 1000, now)
            }
            FilterState.DateRange.LAST_90_DAYS -> {
                Pair(now - 90 * 24 * 60 * 60 * 1000, now)
            }
            FilterState.DateRange.CUSTOM -> null
            FilterState.DateRange.ALL -> null
        }
    }
}
