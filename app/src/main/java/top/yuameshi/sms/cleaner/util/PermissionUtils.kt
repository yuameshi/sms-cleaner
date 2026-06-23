package top.yuameshi.sms.cleaner.util

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.ActivityCompat
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

    /**
     * 获取被拒绝的权限列表
     */
    fun getDeniedPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 检查是否应该显示权限说明（用户拒绝过一次但没有选择"不再询问"）
     * 需要Activity上下文
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return REQUIRED_PERMISSIONS.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    /**
     * 检查是否有永久拒绝的权限（用户选择了"不再询问"）
     * 需要Activity上下文
     */
    fun hasPermanentlyDeniedPermissions(activity: Activity): Boolean {
        val deniedPermissions = getDeniedPermissions(activity)
        return deniedPermissions.any { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * 检查特定权限是否被永久拒绝
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
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
