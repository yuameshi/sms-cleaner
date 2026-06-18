package top.yuameshi.sms.cleaner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import top.yuameshi.sms.cleaner.ui.screen.MainScreen

@Composable
fun SmsCleanerApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen()
        }
    }
}
