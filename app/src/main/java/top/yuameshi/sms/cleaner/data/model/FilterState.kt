package top.yuameshi.sms.cleaner.data.model

data class FilterState(
    val keyword: String = "",
    val number: String = "",
    val dateRange: DateRange = DateRange.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val readStatus: ReadStatus = ReadStatus.ALL,
    val lockStatus: LockStatus = LockStatus.ALL,
    val messageType: MessageType = MessageType.ALL,
    val simSubscriptionId: Int? = null,
    val contactId: Long? = null
) {
    enum class DateRange {
        ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS, CUSTOM
    }

    enum class ReadStatus {
        ALL, READ, UNREAD
    }

    enum class LockStatus {
        ALL, LOCKED, UNLOCKED
    }

    enum class MessageType {
        ALL, INBOX, SENT, DRAFT, OUTBOX
    }

    fun hasFilters(): Boolean {
        return keyword.isNotEmpty() ||
                number.isNotEmpty() ||
                dateRange != DateRange.ALL ||
                readStatus != ReadStatus.ALL ||
                lockStatus != LockStatus.ALL ||
                messageType != MessageType.ALL ||
                simSubscriptionId != null ||
                contactId != null
    }
}
