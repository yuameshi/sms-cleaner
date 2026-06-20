package top.yuameshi.sms.cleaner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * MMS receiver required for default SMS app eligibility.
 * Android requires this component to be present for an app to qualify as the default SMS app.
 */
class MmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received MMS: ${intent.action}")
        // MMS handling is not implemented as this is primarily a SMS cleaner app.
        // This receiver exists to satisfy Android's default SMS app requirements.
    }
}
