package top.yuameshi.sms.cleaner.data.datasource

import android.provider.Telephony
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import java.util.Calendar

/**
 * Builds SQL selection clauses and arguments for SMS content provider queries.
 *
 * Encapsulates the complex WHERE clause construction logic that was previously
 * embedded in [SmsDataSource], making it independently testable and reusable.
 */
object SmsSelectionBuilder {

    /**
     * Builds a SQL selection clause and corresponding arguments array from the given filter state.
     *
     * @param filterState The filter criteria to translate into SQL WHERE conditions.
     * @return A [Pair] of selection string and selection args, both nullable for unfiltered queries.
     */
    fun buildSelection(filterState: FilterState): Pair<String?, Array<String>?> {
        val selections = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (filterState.keyword.isNotEmpty()) {
            selections.add("${Telephony.Sms.BODY} LIKE ?")
            args.add("%${filterState.keyword}%")
        }

        if (filterState.number.isNotEmpty()) {
            selections.add("${Telephony.Sms.ADDRESS} LIKE ?")
            args.add("%${filterState.number}%")
        }

        when (filterState.dateRange) {
            FilterState.DateRange.TODAY -> {
                val todayStart = getTodayStart()
                selections.add("${Telephony.Sms.DATE} >= ?")
                args.add(todayStart.toString())
                // Add upper bound for consistency (end of today)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                selections.add("${Telephony.Sms.DATE} <= ?")
                args.add(calendar.timeInMillis.toString())
            }
            FilterState.DateRange.LAST_7_DAYS -> {
                val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                selections.add("${Telephony.Sms.DATE} >= ?")
                args.add(sevenDaysAgo.toString())
            }
            FilterState.DateRange.LAST_30_DAYS -> {
                val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                selections.add("${Telephony.Sms.DATE} >= ?")
                args.add(thirtyDaysAgo.toString())
            }
            FilterState.DateRange.LAST_90_DAYS -> {
                val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
                selections.add("${Telephony.Sms.DATE} >= ?")
                args.add(ninetyDaysAgo.toString())
            }
            FilterState.DateRange.CUSTOM -> {
                filterState.customStartDate?.let {
                    selections.add("${Telephony.Sms.DATE} >= ?")
                    args.add(it.toString())
                }
                filterState.customEndDate?.let {
                    // Adjust endDate to end of day (23:59:59.999) to include all messages on that day
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = it
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endOfDay = calendar.timeInMillis
                    selections.add("${Telephony.Sms.DATE} <= ?")
                    args.add(endOfDay.toString())
                }
            }
            FilterState.DateRange.ALL -> {}
        }

        when (filterState.readStatus) {
            FilterState.ReadStatus.READ -> {
                selections.add("${Telephony.Sms.READ} = ?")
                args.add("1")
            }
            FilterState.ReadStatus.UNREAD -> {
                selections.add("${Telephony.Sms.READ} = ?")
                args.add("0")
            }
            FilterState.ReadStatus.ALL -> {}
        }

        when (filterState.lockStatus) {
            FilterState.LockStatus.LOCKED -> {
                selections.add("${Telephony.Sms.LOCKED} = ?")
                args.add("1")
            }
            FilterState.LockStatus.UNLOCKED -> {
                selections.add("${Telephony.Sms.LOCKED} = ?")
                args.add("0")
            }
            FilterState.LockStatus.ALL -> {}
        }

        when (filterState.messageType) {
            FilterState.MessageType.INBOX -> {
                selections.add("${Telephony.Sms.TYPE} = ?")
                args.add(SmsMessage.TYPE_INBOX.toString())
            }
            FilterState.MessageType.SENT -> {
                selections.add("${Telephony.Sms.TYPE} = ?")
                args.add(SmsMessage.TYPE_SENT.toString())
            }
            FilterState.MessageType.DRAFT -> {
                selections.add("${Telephony.Sms.TYPE} = ?")
                args.add(SmsMessage.TYPE_DRAFT.toString())
            }
            FilterState.MessageType.OUTBOX -> {
                selections.add("${Telephony.Sms.TYPE} = ?")
                args.add(SmsMessage.TYPE_OUTBOX.toString())
            }
            FilterState.MessageType.ALL -> {}
        }

        filterState.simSubscriptionId?.let { subId ->
            selections.add("${Telephony.Sms.SUBSCRIPTION_ID} = ?")
            args.add(subId.toString())
        }

        filterState.contactId?.let {
            selections.add("${Telephony.Sms.THREAD_ID} = ?")
            args.add(it.toString())
        }

        val selection = if (selections.isEmpty()) null else selections.joinToString(" AND ")
        val selectionArgs = if (args.isEmpty()) null else args.toTypedArray()

        return Pair(selection, selectionArgs)
    }

    /**
     * Returns the start of today as a Unix timestamp in milliseconds.
     */
    fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
