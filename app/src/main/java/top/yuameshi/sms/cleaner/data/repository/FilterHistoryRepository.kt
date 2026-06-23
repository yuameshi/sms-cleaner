package top.yuameshi.sms.cleaner.data.repository

interface FilterHistoryRepository {
    fun getHistory(): List<String>
    fun addHistory(keyword: String)
    fun clearHistory()
}
