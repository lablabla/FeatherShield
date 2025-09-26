package com.lablabla.feathershield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lablabla.feathershield.ui.auth.AuthViewModel
import com.lablabla.feathershield.ui.auth.LoginRoute
import com.lablabla.feathershield.ui.dashboard.DashboardRoute
import com.lablabla.feathershield.ui.dashboard.DashboardViewModel
import com.lablabla.feathershield.ui.device.DeviceRoute
import com.lablabla.feathershield.ui.device.DeviceViewModel
import com.lablabla.feathershield.ui.provisioning.ProvisioningRoute
import com.lablabla.feathershield.ui.provisioning.ProvisioningViewModel

@Composable
fun AppNavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser = authViewModel.getCurrentUser()

    // The start destination depends on the user's authentication state
    val startDestination = if (currentUser != null) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginRoute(navController = navController)
        }
        composable("dashboard") {
            DashboardRoute(navController = navController)
        }
        composable("add_device") {
            ProvisioningRoute(navController = navController)
        }
        composable(
            route = "device/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("deviceId")?.let { deviceId ->
                val viewModel = hiltViewModel<DeviceViewModel>()
                DeviceRoute(navController = navController)
            }
        }
        // Other screens will be added here
    }
}
