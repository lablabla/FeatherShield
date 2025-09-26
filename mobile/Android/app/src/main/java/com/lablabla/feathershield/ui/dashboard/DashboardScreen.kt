package com.lablabla.feathershield.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.lablabla.feathershield.data.model.Device
import com.lablabla.feathershield.ui.dashboard.components.DeviceCard
import com.lablabla.feathershield.ui.dashboard.components.DeviceListItem
import com.lablabla.feathershield.ui.theme.FeatherShieldTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow



// --- STATE AND ACTIONS DEFINITIONS ---

data class DashboardUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface DashboardAction {
    data class OnDeviceClick(val deviceId: String) : DashboardAction
    object OnAddDeviceClick : DashboardAction
    object OnSignOutClick : DashboardAction
}

// --- STATEFUL CONTAINER (THE ROUTE) ---

@Composable
fun DashboardRoute(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The Dashboard doesn't have complex side-effects like Login,
    // so we can handle navigation directly in the action handler.
    DashboardScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                is DashboardAction.OnAddDeviceClick -> navController.navigate("add_device")
                is DashboardAction.OnDeviceClick -> navController.navigate("device/${action.deviceId}")
                is DashboardAction.OnSignOutClick -> {
                    viewModel.signOut() // Perform the sign-out logic
                    navController.navigate("login") {
                        // Clear the back stack up to the dashboard
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            }
        }
    )
}


// --- STATELESS UI COMPONENT ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("My Nestboxes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onAction(DashboardAction.OnAddDeviceClick) }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add a new device"
                        )
                    }
                    IconButton(onClick = { onAction(DashboardAction.OnSignOutClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.devices.isEmpty()) {
                EmptyState(onAction)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.devices) { deviceState ->
                        DeviceCard(device = deviceState, onAction = onAction)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(onAction: (DashboardAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Nestboxes Found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Get started by adding your first device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(DashboardAction.OnAddDeviceClick) }) {
            Icon(Icons.Default.Add, contentDescription = "Add Icon", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Add a Device")
        }
    }
}

@Preview(name = "Dark Mode - Empty State", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun DashboardScreenPreview_Dark_Empty() {
    FeatherShieldTheme {
        Surface {
            DashboardScreen(state = DashboardUiState(devices = emptyList()), onAction = {})
        }
    }
}

@Preview(name = "Dark Mode - Full", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun DashboardScreenPreview_Dark_Full() {
    FeatherShieldTheme {
        val devices = listOf(
            Device(name = "Nestbox 001", batteryLevel = 80),
            Device(name = "Nestbox 002", batteryLevel = 15)
        )
        Surface {
            DashboardScreen(state = DashboardUiState(devices = devices), onAction = {})
        }
    }
}

@Preview(name = "Light Mode - Empty State", showBackground = true, widthDp = 360)
@Composable
fun DashboardScreenPreview_Light_Empty() {
    FeatherShieldTheme {
        Surface {
            DashboardScreen(state = DashboardUiState(devices = emptyList()), onAction = {})
        }
    }
}

@Preview(name = "Light Mode - Full", showBackground = true, widthDp = 360)
@Composable
fun DashboardScreenPreview_Light_Full() {
    FeatherShieldTheme {
        Surface {
            val devices = listOf(
                Device(name = "Nestbox 001", batteryLevel = 80),
                Device(name = "Nestbox 002", batteryLevel = 15)
            )
            DashboardScreen(state = DashboardUiState(devices = devices), onAction = {})
        }
    }
}