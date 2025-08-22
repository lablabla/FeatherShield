package com.lablabla.feathershield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lablabla.feathershield.ui.auth.AuthViewModel
import com.lablabla.feathershield.ui.auth.LoginScreen
import com.lablabla.feathershield.ui.dashboard.DashboardScreen
import com.lablabla.feathershield.ui.dashboard.DashboardViewModel
import com.lablabla.feathershield.ui.device.DeviceScreen
import com.lablabla.feathershield.ui.device.DeviceViewModel
import com.lablabla.feathershield.ui.provisioning.ProvisioningScreen
import com.lablabla.feathershield.ui.provisioning.ProvisioningViewModel
import com.lablabla.feathershield.ui.provisioning.WifiCredentialsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser = authViewModel.getCurrentUser()

    // The start destination depends on the user's authentication state
    val startDestination = if (currentUser != null) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("dashboard") {
            val viewModel = hiltViewModel<DashboardViewModel>()
            DashboardScreen(navController = navController, viewModel = viewModel)
        }
        composable("add_device") {
            val viewModel = hiltViewModel<ProvisioningViewModel>()
            ProvisioningScreen(navController = navController, viewModel = viewModel)
        }
        composable("wifi_credentials/{deviceId}") { backStackEntry ->
            backStackEntry.arguments?.getString("deviceId")?.let { deviceId ->
                WifiCredentialsScreen(navController = navController, deviceId = deviceId)
            }
        }
        composable(
            route = "livefeed/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("deviceId")?.let { deviceId ->
                val viewModel = hiltViewModel<DeviceViewModel>()
                DeviceScreen(navController = navController, viewModel = viewModel)
            }
        }
        // Other screens will be added here
    }
}
