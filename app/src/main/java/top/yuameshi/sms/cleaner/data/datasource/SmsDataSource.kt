package top.yuameshi.sms.cleaner.data.datasource

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SubscriptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yuameshi.sms.cleaner.data.model.FilterState
import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.model.SmsMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private var cachedTotalCount: Int? = null

    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage> = withContext(Dispatchers.IO) {
        val (selection, selectionArgs) = buildSelection(filterState)
        val offset = page * pageSize

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.LOCKED,
                Telephony.Sms.SUBSCRIPTION_ID
            ),
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC LIMIT $pageSize OFFSET $offset"
        )

        cursor?.use {
            val messages = mutableListOf<SmsMessage>()
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val locked = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.LOCKED)) == 1
                val subId = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))

                val contactName = getContactName(address)

                messages.add(
                    SmsMessage(
                        id = id,
                        address = address,
                        body = body,
                        date = date,
                        type = type,
                        read = read,
                        locked = locked,
                        subId = subId,
                        contactName = contactName
                    )
                )
            }
            messages
        } ?: emptyList()
    }

    suspend fun getTotalCount(filterState: FilterState): Int = withContext(Dispatchers.IO) {
        if (!filterState.hasFilters()) {
            cachedTotalCount?.let { return@withContext it }
        }

        val (selection, selectionArgs) = buildSelection(filterState)

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            selection,
            selectionArgs,
            null
        )

        val count = cursor?.use {
            it.count
        } ?: 0

        if (!filterState.hasFilters()) {
            cachedTotalCount = count
        }

        count
    }

    suspend fun deleteMessages(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0

        var totalDeleted = 0
        ids.chunked(100).forEach { chunk ->
            val idList = chunk.joinToString(",")
            val deleted = contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} IN ($idList)",
                null
            )
            totalDeleted += deleted
        }
        invalidateTotalCountCache()
        totalDeleted
    }

    suspend fun deleteMessagesByFilter(filterState: FilterState): Int = withContext(Dispatchers.IO) {
        val (selection, selectionArgs) = buildSelection(filterState)
        val result = contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            selection,
            selectionArgs
        )
        invalidateTotalCountCache()
        result
    }

    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, date)
            put(Telephony.Sms.TYPE, type)
            put(Telephony.Sms.READ, if (read) 1 else 0)
            put(Telephony.Sms.SUBSCRIPTION_ID, subId)
        }

        val result = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        invalidateTotalCountCache()
        result
    }

    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} = ?",
            arrayOf(address, body, date.toString()),
            null
        )

        cursor?.use {
            it.count > 0
        } ?: false
    }

    fun invalidateTotalCountCache() {
        cachedTotalCount = null
    }

    private fun buildSelection(filterState: FilterState): Pair<String?, Array<String>?> {
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
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                calendar.set(java.util.Calendar.MINUTE, 59)
                calendar.set(java.util.Calendar.SECOND, 59)
                calendar.set(java.util.Calendar.MILLISECOND, 999)
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
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = it
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
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

    private fun getTodayStart(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getSimCards(): List<SimCardInfo> {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptions = subscriptionManager.activeSubscriptionInfoList
            subscriptions?.map { info ->
                SimCardInfo(
                    subscriptionId = info.subscriptionId,
                    displayName = info.displayName?.toString() ?: "",
                    carrierName = info.carrierName?.toString() ?: "",
                    phoneNumber = info.number ?: "",
                    slotIndex = info.simSlotIndex
                )
            }?.sortedBy { it.slotIndex } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            } else {
                null
            }
        }
    }
}
