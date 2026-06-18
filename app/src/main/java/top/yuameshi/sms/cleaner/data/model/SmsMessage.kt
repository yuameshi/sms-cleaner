package top.yuameshi.sms.cleaner.data.model

data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val locked: Boolean,
    val subId: Int,
    val threadId: Long,
    val contactName: String? = null
) {
    companion object {
        const val TYPE_INBOX = 1
        const val TYPE_SENT = 2
        const val TYPE_DRAFT = 3
        const val TYPE_OUTBOX = 4
        const val TYPE_FAILED = 5
        const val TYPE_QUEUED = 6

        fun getTypeName(type: Int): String = when (type) {
            TYPE_INBOX -> "收件箱"
            TYPE_SENT -> "已发送"
            TYPE_DRAFT -> "草稿"
            TYPE_OUTBOX -> "发件箱"
            TYPE_FAILED -> "发送失败"
            TYPE_QUEUED -> "待发送"
            else -> "未知"
        }
    }
}
