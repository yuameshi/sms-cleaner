package top.yuameshi.sms.cleaner.util

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.result.ActivityResultLauncher

object DefaultSmsManager {
    fun isDefaultSmsApp(context: Context): Boolean {
        return PermissionUtils.isDefaultSmsApp(context)
    }

    fun requestDefaultSmsRole(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    launcher.launch(intent)
                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
            launcher.launch(intent)
        }
    }

    fun openDefaultAppsSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        activity.startActivity(intent)
    }
}
