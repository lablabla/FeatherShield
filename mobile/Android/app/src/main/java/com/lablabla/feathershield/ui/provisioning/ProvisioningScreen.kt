package com.lablabla.feathershield.ui.provisioning

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningScreen(
    navController: NavController,
    viewModel: ProvisioningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val provisioningState by viewModel.provisioningState.collectAsState()

    // Request for BLE and location permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(provisioningState) {
        when (provisioningState) {
            is ProvisioningState.ProvisioningSuccess -> {
                Toast.makeText(context, "Device provisioned successfully!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            is ProvisioningState.Error -> {
                val errorMessage = (provisioningState as ProvisioningState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provision New Nestbox") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = provisioningState) {
                is ProvisioningState.Idle -> {
                    Text("Ready to start provisioning")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.startScan() }) {
                        Text("Start Scan for Devices")
                    }
                }
                is ProvisioningState.Scanning -> {
                    Text("Scanning for devices...")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                is ProvisioningState.DevicesFound -> {
                    LazyColumn {
                        items(state.devices) { device ->
                            Text(
                                text = device.name ?: "Unknown Device",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.connectDevice(device) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
                is ProvisioningState.Connecting -> {
                    Text("Connecting to device: ${state.device.name ?: "Unknown Device"}")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                else -> {
                    // Handle other states
                }
            }
        }
    }
}
