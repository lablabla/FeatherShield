@file:OptIn(ExperimentalPermissionsApi::class)

package com.lablabla.feathershield.ui.provisioning

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lablabla.feathershield.R
import com.lablabla.feathershield.ui.theme.FeatherShieldTheme


// Data for a single step in the progress indicator UI
data class ProvisioningStep(val text: String, val status: UiStepStatus)


// Represents the major states of the provisioning flow
sealed interface ProvisioningFlowState {
    object CameraPermission : ProvisioningFlowState
    object Scanning : ProvisioningFlowState
    data class NetworkSelection(val availableNetworks: List<String>) : ProvisioningFlowState
    data class Provisioning(val steps: List<ProvisioningStep>) : ProvisioningFlowState
    object Success : ProvisioningFlowState
    data class Error(val message: String) : ProvisioningFlowState
}

// The single state object for the entire screen
data class ProvisioningUiState constructor(
    val flowState: ProvisioningFlowState = ProvisioningFlowState.CameraPermission,
    val cameraPermissionStatus: PermissionStatus? = null,
    val selectedSsid: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false
)

// All possible user actions
sealed interface ProvisioningAction {
    object OnRequestPermissionClick : ProvisioningAction
    object OnGoToSettingsClick : ProvisioningAction
    data class OnQrCodeScanned(val data: String) : ProvisioningAction
    data class OnNetworkSelected(val ssid: String) : ProvisioningAction
    data class OnPasswordChange(val password: String) : ProvisioningAction
    object OnTogglePasswordVisibility : ProvisioningAction
    object OnConnectClick : ProvisioningAction
    object OnDoneClick : ProvisioningAction
    object OnBackClick : ProvisioningAction
    object OnTryAgainClick : ProvisioningAction
}


// --- STATEFUL CONTAINER (THE ROUTE) ---

@Composable
fun ProvisioningRoute(
    navController: NavController,
    viewModel: ProvisioningViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    ) { isGranted ->
        // This can be used to trigger a refresh in the ViewModel if needed
    }

    // Update the ViewModel with the latest permission status
    LaunchedEffect(cameraPermissionState.status) {
        // This can be replaced by a ViewModel action if more complex logic is needed
    }

    ProvisioningScreen(
        state = uiState.copy(cameraPermissionStatus = cameraPermissionState.status),
        onAction = { action ->
            when (action) {
                is ProvisioningAction.OnBackClick -> {
                    viewModel.handleAction(action)
                    navController.popBackStack()
                }
                is ProvisioningAction.OnDoneClick -> navController.popBackStack()
                is ProvisioningAction.OnRequestPermissionClick -> cameraPermissionState.launchPermissionRequest()
                is ProvisioningAction.OnGoToSettingsClick -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
                else -> viewModel.handleAction(action)
            }
        }
    )
}

// --- STATELESS UI COMPONENT ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProvisioningScreen(
    state: ProvisioningUiState,
    onAction: (ProvisioningAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Nestbox") },
                navigationIcon = {
                    IconButton(onClick = { onAction(ProvisioningAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val flowState = state.flowState) {
                is ProvisioningFlowState.CameraPermission -> {
                    CameraPermissionStep(
                        status = state.cameraPermissionStatus,
                        onAction = onAction
                    )
                }
                is ProvisioningFlowState.Scanning -> {
                    if (state.cameraPermissionStatus is PermissionStatus.Granted) {
                        ScanningStep(onQrCodeScanned = { onAction(ProvisioningAction.OnQrCodeScanned(it)) })
                    } else {
                        // Show permission UI if status changes while scanning
                        CameraPermissionStep(status = state.cameraPermissionStatus, onAction = onAction)
                    }
                }
                is ProvisioningFlowState.NetworkSelection -> {
                    NetworkSelectionStep(
                        networks = flowState.availableNetworks,
                        state = state,
                        onAction = onAction
                    )
                }
                is ProvisioningFlowState.Provisioning -> {
                    ProvisioningProgressStep(steps = flowState.steps)
                }
                is ProvisioningFlowState.Success -> {
                    SuccessStep(onAction = onAction)
                }
                is ProvisioningFlowState.Error -> {
                    ErrorStep(message = flowState.message, onAction = onAction)
                }
            }
        }
    }
}

// --- UI for each step ---

@Composable
fun CameraPermissionStep(
    status: PermissionStatus?,
    onAction: (ProvisioningAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Camera Access Required", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        val text = if (status?.shouldShowRationale == true) {
            "To add your Nestbox, this app needs to scan a QR code. Please grant camera permission to continue."
        } else {
            "Camera permission is required to scan the QR code. Please grant the permission to continue."
        }
        Text(text, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (status?.shouldShowRationale == true) {
                onAction(ProvisioningAction.OnRequestPermissionClick)
            } else {
                onAction(ProvisioningAction.OnGoToSettingsClick)
            }
        }) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun ScanningStep(onQrCodeScanned: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx),
                            QrCodeAnalyzer(onQrCodeScanned)
                        )
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    // Log error
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NetworkSelectionStep(
    networks: List<String>,
    state: ProvisioningUiState,
    onAction: (ProvisioningAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select a Wi-Fi Network", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(networks) { network ->
                ListItem(
                    headlineContent = { Text(network) },
                    modifier = Modifier.clickable { onAction(ProvisioningAction.OnNetworkSelected(network)) },
                    trailingContent = {
                        if (state.selectedSsid == network) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(ProvisioningAction.OnPasswordChange(it)) },
            label = { Text("Password") },
            trailingIcon = {
                IconButton(onClick = { onAction(ProvisioningAction.OnTogglePasswordVisibility) }) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onAction(ProvisioningAction.OnConnectClick) },
            enabled = state.selectedSsid.isNotBlank()
        ) {
            Text("Connect")
        }
    }
}

@Composable
fun ProvisioningProgressStep(steps: List<ProvisioningStep>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Provisioning Your Device...", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(32.dp))
        steps.forEach { step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(0.8f)
            ) {
                when (step.status) {
                    UiStepStatus.DONE -> Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
                    UiStepStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    UiStepStatus.PENDING -> Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Pending")
                    UiStepStatus.ERROR -> Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(step.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun SuccessStep(onAction: (ProvisioningAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Provisioning Successful!", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Your Nestbox is now connected.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(ProvisioningAction.OnDoneClick) }) {
            Text("Done")
        }
    }
}

@Composable
fun ErrorStep(message: String, onAction: (ProvisioningAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("Provisioning Failed", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(ProvisioningAction.OnTryAgainClick) }) {
            Text("Try Again")
        }
    }
}

class QrCodeAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
            scanner.process(image)
                .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let(onQrCodeScanned) }
                .addOnCompleteListener { imageProxy.close() }
        }
    }
}

// --- HELPER COMPOSABLES ---

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000)
        ), label = "shimmer offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                Color.Transparent,
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned {
            size = it.size
        }
}

// --- PREVIEW SECTION ---


@androidx.compose.ui.tooling.preview.Preview(name = "Permission Denied", showBackground = true, widthDp = 360)
@Composable
fun Preview_PermissionDenied() {
    FeatherShieldTheme {
        Surface {
            ProvisioningScreen(
                state = ProvisioningUiState(flowState = ProvisioningFlowState.CameraPermission, cameraPermissionStatus = PermissionStatus.Denied(false)),
                onAction = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Network Selection", showBackground = true, widthDp = 360)
@Composable
fun Preview_NetworkSelection() {
    FeatherShieldTheme {
        Surface {
            ProvisioningScreen(
                state = ProvisioningUiState(
                    flowState = ProvisioningFlowState.NetworkSelection(listOf("Home Wi-Fi", "Guest_Network", "OfficeLAN")),
                    selectedSsid = "Home Wi-Fi"
                ),
                onAction = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Provisioning In Progress", showBackground = true, widthDp = 360)
@Composable
fun Preview_ProvisioningProgress() {
    FeatherShieldTheme {
        Surface {
            ProvisioningScreen(
                state = ProvisioningUiState(
                    flowState = ProvisioningFlowState.Provisioning(
                        steps = listOf(
                            ProvisioningStep("Sending Credentials", UiStepStatus.DONE),
                            ProvisioningStep("Applying Wi-Fi connection", UiStepStatus.IN_PROGRESS),
                            ProvisioningStep("Verifying status", UiStepStatus.PENDING)
                        )
                    )
                ),
                onAction = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Dark - Provisioning In Progress", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Preview_ProvisioningProgress_Dark() {
    FeatherShieldTheme {
        Surface {
            ProvisioningScreen(
                state = ProvisioningUiState(
                    flowState = ProvisioningFlowState.Provisioning(
                        steps = listOf(
                            ProvisioningStep("Sending Credentials", UiStepStatus.DONE),
                            ProvisioningStep("Applying Wi-Fi connection", UiStepStatus.IN_PROGRESS),
                            ProvisioningStep("Verifying status", UiStepStatus.PENDING)
                        )
                    )
                ),
                onAction = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Success", showBackground = true, widthDp = 360)
@Composable
fun Preview_Success() {
    FeatherShieldTheme {
        Surface {
            ProvisioningScreen(
                state = ProvisioningUiState(flowState = ProvisioningFlowState.Success),
                onAction = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Error", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Preview_Error() {
    FeatherShieldTheme {
        Surface {
            ProvisioningScreen(
                state = ProvisioningUiState(flowState = ProvisioningFlowState.Error("Failed to connect to device. Please ensure it's nearby and in pairing mode.")),
                onAction = {}
            )
        }
    }
}