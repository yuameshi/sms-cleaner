package top.yuameshi.sms.cleaner.service

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.JobIntentService

class RespondService : JobIntentService() {
    companion object {
        private const val TAG = "RespondService"
        private const val JOB_ID = 1001
        private const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, RespondService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action == ACTION_RESPOND_VIA_MESSAGE) {
            val uri = intent.data
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)

            Log.d(TAG, "Respond via message to $uri: $message")

            if (uri != null && message != null) {
                val address = uri.schemeSpecificPart
                try {
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(address, null, message, null, null)
                    Log.d(TAG, "Reply sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send reply", e)
                }
            }
        }
    }
}
