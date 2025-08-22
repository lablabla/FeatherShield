package com.lablabla.feathershield.ui.provisioning

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.lablabla.feathershield.data.model.WifiCredentials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiCredentialsScreen(
    navController: NavController,
    deviceId: String,
    viewModel: ProvisioningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Wi-Fi Credentials") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("Wi-Fi SSID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Wi-Fi Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val credentials = WifiCredentials(ssid, password)
                    viewModel.sendWifiCredentials(credentials)
                }
            ) {
                Text("Send Credentials")
            }
        }
    }
}
