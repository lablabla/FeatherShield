package com.lablabla.feathershield.ui.provisioning

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningScreen(
    navController: NavController,
    viewModel: ProvisioningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val provisioningState by viewModel.provisioningState.collectAsState()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    when (val state = provisioningState) {
        ProvisioningState.Scanning -> {
            when (cameraPermissionState.status) {
                PermissionStatus.Granted -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { context ->
                                val previewView = PreviewView(context)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .build()
                                        .also {
                                            it.setAnalyzer(
                                                ContextCompat.getMainExecutor(context),
                                                QrCodeAnalyzer { qrCodeData ->
                                                    viewModel.onQrCodeScanned(qrCodeData)
                                                }
                                            )
                                        }
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        // handle exception
                                    }
                                }, ContextCompat.getMainExecutor(context))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is PermissionStatus.Denied -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                            "The camera is important for this app. Please grant the permission."
                        } else {
                            "Camera permission is required to scan the QR code. Please grant the permission in settings."
                        }
                        Text(text = textToShow)
                        Button(onClick = {
                            if (cameraPermissionState.status.shouldShowRationale) {
                                cameraPermissionState.launchPermissionRequest()
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", context.packageName, null)
                                intent.data = uri
                                context.startActivity(intent)
                            }
                        }) {
                            Text("Request permission")
                        }
                    }
                }
            }
        }
        is ProvisioningState.NetworkSelection -> {
            var ssid by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Select a Wi-Fi Network")
                LazyColumn {
                    items(state.networks) { network ->
                        TextButton(onClick = { ssid = network }) {
                            Text(network)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.onNetworkSelected(ssid, password) }) {
                    Text("Connect")
                }
            }
        }
        is ProvisioningState.Provisioning -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ProvisioningStep(
                    text = "Sending Credentials",
                    status = state.sendingCredentialsStatus
                )
                ProvisioningStep(
                    text = "Applying Wifi connection",
                    status = state.applyingWifiConnectionStatus
                )
                ProvisioningStep(
                    text = "Checking Provisioning status",
                    status = state.checkingProvisioningStatus
                )
            }
        }
        ProvisioningState.Success -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Provisioning Successful!")
                Button(onClick = { navController.popBackStack() }) {
                    Text("Done")
                }
            }
        }
        is ProvisioningState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.message)
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ProvisioningStep(text: String, status: StepStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        when {
            status.isDone -> Icon(Icons.Default.Check, contentDescription = "Done")
            status.inProgress -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else -> Icon(Icons.Default.Refresh, contentDescription = "Not Done")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}

class QrCodeAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let {
                            onQrCodeScanned(it)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}
