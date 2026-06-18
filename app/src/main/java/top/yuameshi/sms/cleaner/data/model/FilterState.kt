package top.yuameshi.sms.cleaner.data.model

data class FilterState(
    val keyword: String = "",
    val number: String = "",
    val regex: String = "",
    val isRegexMode: Boolean = false,
    val dateRange: DateRange = DateRange.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val readStatus: ReadStatus = ReadStatus.ALL,
    val lockStatus: LockStatus = LockStatus.ALL,
    val messageType: MessageType = MessageType.ALL,
    val simId: SimId = SimId.ALL,
    val contactId: Long? = null,
    val contactName: String? = null
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

    enum class SimId {
        ALL, SIM1, SIM2
    }

    fun hasFilters(): Boolean {
        return keyword.isNotEmpty() ||
                number.isNotEmpty() ||
                regex.isNotEmpty() ||
                dateRange != DateRange.ALL ||
                readStatus != ReadStatus.ALL ||
                lockStatus != LockStatus.ALL ||
                messageType != MessageType.ALL ||
                simId != SimId.ALL ||
                contactId != null
    }

    fun toQueryString(): String {
        val parts = mutableListOf<String>()
        if (keyword.isNotEmpty()) parts.add("关键词=$keyword")
        if (number.isNotEmpty()) parts.add("号码=$number")
        if (regex.isNotEmpty()) parts.add("正则=$regex")
        if (dateRange != DateRange.ALL) parts.add("日期=${dateRange.name}")
        if (readStatus != ReadStatus.ALL) parts.add("已读=${readStatus.name}")
        if (lockStatus != LockStatus.ALL) parts.add("锁定=${lockStatus.name}")
        if (messageType != MessageType.ALL) parts.add("类型=${messageType.name}")
        if (simId != SimId.ALL) parts.add("SIM=${simId.name}")
        if (contactName != null) parts.add("联系人=$contactName")
        return parts.joinToString(", ")
    }
}
