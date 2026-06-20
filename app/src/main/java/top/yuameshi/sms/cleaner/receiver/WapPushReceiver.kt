package top.yuameshi.sms.cleaner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * WAP Push receiver required for default SMS app eligibility.
 * Android requires this component to be present for an app to qualify as the default SMS app.
 */
class WapPushReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "WapPushReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received WAP Push: ${intent.action}")
        // WAP Push handling is not implemented as this is primarily a SMS cleaner app.
        // This receiver exists to satisfy Android's default SMS app requirements.
    }
}
