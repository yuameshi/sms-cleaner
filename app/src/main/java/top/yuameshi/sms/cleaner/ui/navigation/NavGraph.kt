package top.yuameshi.sms.cleaner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import top.yuameshi.sms.cleaner.ui.screen.MainScreen

@Composable
fun SmsCleanerApp(
    hasPermissions: Boolean = false,
    showPermissionPermanentlyDenied: Boolean = false,
    onRetryPermissionRequest: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                hasPermissions = hasPermissions,
                showPermissionPermanentlyDenied = showPermissionPermanentlyDenied,
                onRetryPermissionRequest = onRetryPermissionRequest,
                onOpenAppSettings = onOpenAppSettings
            )
        }
    }
}
