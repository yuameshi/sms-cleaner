package top.yuameshi.sms.cleaner.service

import android.app.IntentService
import android.content.Intent
import android.net.Uri
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

            // TODO: Implement respond via message functionality
            // This is a stub implementation for default SMS app eligibility
        }
    }
}
