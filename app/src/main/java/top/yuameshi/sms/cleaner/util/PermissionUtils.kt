package top.yuameshi.sms.cleaner.util

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat

object PermissionUtils {
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS
    )

    fun hasAllPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 RoleManager
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) ?: false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4-9 使用 Telephony.Sms
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
            defaultSmsPackage == context.packageName
        } else {
            true
        }
    }
}
