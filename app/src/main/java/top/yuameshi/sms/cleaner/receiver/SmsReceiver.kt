package top.yuameshi.sms.cleaner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { message ->
                Log.d(TAG, "Received SMS from ${message.displayOriginatingAddress}: ${message.displayMessageBody}")
                // Handle incoming SMS - display notification
                showSmsNotification(context, message)
            }
        }
    }

    private fun showSmsNotification(context: Context, message: android.telephony.SmsMessage) {
        // TODO: Implement notification display
        // For now, just log the message
        Log.d(TAG, "SMS received - notification should be shown")
    }
}
