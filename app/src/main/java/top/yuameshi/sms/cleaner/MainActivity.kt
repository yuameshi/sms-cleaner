package top.yuameshi.sms.cleaner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import top.yuameshi.sms.cleaner.ui.navigation.SmsCleanerApp
import top.yuameshi.sms.cleaner.ui.theme.SMSCleanerTheme
import top.yuameshi.sms.cleaner.util.PermissionUtils

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionsGranted = mutableStateOf(false)
    private val showPermissionPermanentlyDenied = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted.value = allGranted
        
        if (!allGranted) {
            // 检查是否有永久拒绝的权限
            showPermissionPermanentlyDenied.value = PermissionUtils.hasPermanentlyDeniedPermissions(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        checkAndRequestPermissions()

        setContent {
            SMSCleanerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmsCleanerApp(
                        hasPermissions = permissionsGranted.value,
                        showPermissionPermanentlyDenied = showPermissionPermanentlyDenied.value,
                        onRetryPermissionRequest = { retryPermissionRequest() },
                        onOpenAppSettings = { openAppSettings() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )

        // Add POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            permissionsGranted.value = true
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    /**
     * 重试权限请求并重启App
     * 如果用户之前拒绝过但没有选择"不再询问"，则重新请求权限
     * 如果用户选择了"不再询问"，则打开应用设置页面
     * 无论哪种情况，都会重启App以重新初始化所有组件
     */
    fun retryPermissionRequest() {
        if (PermissionUtils.hasPermanentlyDeniedPermissions(this)) {
            // 用户选择了"不再询问"，打开应用设置页面
            openAppSettings()
        } else {
            // 重新请求权限
            checkAndRequestPermissions()
        }
        // 重启App以重新初始化所有组件
        restartApp()
    }

    /**
     * 打开应用设置页面
     * 用户可以在设置中手动授予权限
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    /**
     * 重启整个App
     * 使用Intent with FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
     * 然后杀死当前进程，系统会自动重新启动App
     */
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        // 杀死当前进程，系统会自动重新启动App
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    override fun onResume() {
        super.onResume()
        // 从设置页面返回时重新检查权限
        if (showPermissionPermanentlyDenied.value) {
            checkAndRequestPermissions()
            showPermissionPermanentlyDenied.value = false
        }
    }
}
