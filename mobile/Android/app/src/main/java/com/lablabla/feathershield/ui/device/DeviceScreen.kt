package com.lablabla.feathershield.ui.device

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lablabla.feathershield.ui.theme.FeatherShieldTheme

// --- STATE AND ACTIONS DEFINITIONS ---

data class DeviceUiState(
    val name: String? = null,
    val batteryLevel: Int? = null,
    val lastImageUrl: String? = null,
    val isUpdateAvailable: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface DeviceAction {
    object OnBackClick : DeviceAction
    object OnStartStreamClick : DeviceAction
    object OnStopStreamClick : DeviceAction
    object OnSprayClick : DeviceAction
    object OnUpdateFirmwareClick : DeviceAction
}

// --- STATEFUL CONTAINER (THE ROUTE) ---

@Composable
fun DeviceRoute(
    navController: NavController,
    viewModel: DeviceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DeviceScreen(
        state = uiState,
        onAction = { action ->
            if (action is DeviceAction.OnBackClick) {
                navController.popBackStack()
            } else {
                viewModel.handleAction(action)
            }
        }
    )
}

// --- STATELESS UI COMPONENT ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    state: DeviceUiState,
    onAction: (DeviceAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { onAction(DeviceAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Media display area (Image or Video Stream)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes up available vertical space
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.lastImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Latest image from device",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Device Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Battery: ${state.batteryLevel ?: "N/A"}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    Button(onClick = { onAction(DeviceAction.OnStartStreamClick) }) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Start Stream")
                    }

                    Button(onClick = { onAction(DeviceAction.OnSprayClick) }) {
                        Icon(Icons.Default.SignalWifi4Bar, contentDescription = null) // Placeholder Icon
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Spray")
                    }
                }
                if (state.isUpdateAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAction(DeviceAction.OnUpdateFirmwareClick) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Update Firmware")
                    }
                }
            }
        }
    }
}
// --- PREVIEW SECTION ---

@Preview(name = "Light Mode - Default", showBackground = true, widthDp = 360)
@Composable
fun DeviceScreenPreview_Light() {
    FeatherShieldTheme {
        Surface {
            DeviceScreen(
                state = DeviceUiState(
                    isLoading = false,
                    name = "N3ST-001",
                    batteryLevel = 88,
                    lastImageUrl = "https://placehold.co/600x400/EEE/31343C?text=Last+Capture",
                    isUpdateAvailable = false
                ),
                onAction = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun DeviceScreenPreview_Dark_Streaming() {
    FeatherShieldTheme {
        Surface {
            DeviceScreen(
                state = DeviceUiState(
                    isLoading = false,
                    name = "N3ST-002",
                    batteryLevel = 45,
                ),
                onAction = {}
            )
        }
    }
}

@Preview(name = "Dark Mode - Update", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun DeviceScreenPreview_Dark_Update() {
    FeatherShieldTheme {
        Surface {
            DeviceScreen(
                state = DeviceUiState(
                    isLoading = false,
                    name = "N3ST-002",
                    batteryLevel = 45,
                    isUpdateAvailable = true
                ),
                onAction = {}
            )
        }
    }
}

@Preview(name = "Loading State", showBackground = true, widthDp = 360)
@Composable
fun DeviceScreenPreview_Loading() {
    FeatherShieldTheme {
        Surface {
            DeviceScreen(
                state = DeviceUiState(isLoading = true),
                onAction = {}
            )
        }
    }
}
