package top.yuameshi.sms.cleaner.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterHistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FilterHistoryRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "filter_history",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_HISTORY = "filter_history"
        private const val MAX_HISTORY_SIZE = 5
        private const val SEPARATOR = "|||"
    }

    override fun getHistory(): List<String> {
        val historyStr = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (historyStr.isEmpty()) {
            emptyList()
        } else {
            historyStr.split(SEPARATOR).filter { it.isNotEmpty() }
        }
    }

    override fun addHistory(keyword: String) {
        if (keyword.isBlank()) return

        val currentHistory = getHistory().toMutableList()
        currentHistory.remove(keyword)
        currentHistory.add(0, keyword)

        val trimmedHistory = currentHistory.take(MAX_HISTORY_SIZE)
        prefs.edit()
            .putString(KEY_HISTORY, trimmedHistory.joinToString(SEPARATOR))
            .apply()
    }

    override fun clearHistory() {
        prefs.edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}
