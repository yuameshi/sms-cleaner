package top.yuameshi.sms.cleaner.service

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log

class RespondService : IntentService("RespondService") {
    companion object {
        private const val TAG = "RespondService"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_RESPOND_VIA_MESSAGE) {
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
