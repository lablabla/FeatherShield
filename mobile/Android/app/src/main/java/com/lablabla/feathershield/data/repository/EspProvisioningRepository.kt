package com.lablabla.feathershield.data.repository

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.util.Log
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.WiFiAccessPoint
import com.espressif.provisioning.listeners.BleScanListener
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.provisioning.listeners.WiFiScanListener
import com.lablabla.feathershield.ui.provisioning.ProvisioningFlowState
import com.lablabla.feathershield.ui.provisioning.ProvisioningState
import com.lablabla.feathershield.ui.provisioning.StepStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.ranges.contains


@Singleton
@SuppressLint("MissingPermission")
class EspProvisioningRepository @Inject constructor(application: Application) {

    private val _provisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Scanning)
    val provisioningState = _provisioningState.asStateFlow()

    private val espProvisionManager: ESPProvisionManager = ESPProvisionManager.getInstance(application)
    private var espDevice: ESPDevice? = null

    init {
        EventBus.getDefault().register(this)
    }
    fun startBleScan(deviceName: String, pop: String) {
        _provisioningState.value = ProvisioningState.ConnectingToDevice
        espProvisionManager.searchBleEspDevices(deviceName, bleScanListener(pop))
    }

    fun stopBleScan() {
        espProvisionManager.stopBleScan()
        _provisioningState.value = ProvisioningState.Error
    }

    val CAPABILITY_WIFI_SCAN: String = "wifi_scan"
    val CAPABILITY_THREAD_SCAN: String = "thread_scan"
    val CAPABILITY_THREAD_PROV: String = "thread_prov"

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: DeviceConnectionEvent) {
        Log.d("", "ON Device Prov Event RECEIVED : " + event.eventType)
        val deviceCaps = espProvisionManager.espDevice.deviceCapabilities
        when (event.eventType) {
            ESPConstants.EVENT_DEVICE_CONNECTED -> {
                if (deviceCaps.contains(CAPABILITY_WIFI_SCAN)) {
                    scanNetworks()
                }
            }
            ESPConstants.EVENT_DEVICE_DISCONNECTED -> {
                _provisioningState.value = ProvisioningState.Scanning
            }
        }
    }
    private fun bleScanListener(pop: String) = object : BleScanListener {
        override fun scanStartFailed() {
            _provisioningState.value = ProvisioningState.Error("BLE scan start failed")
        }

        override fun onPeripheralFound(device: BluetoothDevice, scanResult: ScanResult) {

            espProvisionManager.stopBleScan()
            espDevice = espProvisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1)
            espDevice?.proofOfPossession = pop

            var serviceUuid: String? = ""

            if (scanResult.scanRecord!!.serviceUuids != null
            ) {
                serviceUuid =
                    scanResult.scanRecord!!.serviceUuids[0].toString()
            }

            espDevice?.bluetoothDevice = device
            espDevice?.primaryServiceUuid = serviceUuid
            espDevice?.connectBLEDevice(device, serviceUuid!!)
//            }


        }

        override fun scanCompleted() {}

        override fun onFailure(e: Exception?) {
            _provisioningState.value = ProvisioningState.Error("BLE scan failed: ${e?.message}")
        }
    }

    fun scanNetworks() {
        _provisioningState.value = ProvisioningState.NetworkScan
        espDevice?.scanNetworks(wiFiScanListener)
    }

    private val wiFiScanListener = object : WiFiScanListener {
        override fun onWifiListReceived(wifiList: ArrayList<WiFiAccessPoint>?) {
            val networks = wifiList?.map { it.wifiName } ?: emptyList()
            _provisioningState.value = ProvisioningState.NetworkSelection(networks)
        }

        override fun onWiFiScanFailed(e: Exception?) {
            _provisioningState.value = ProvisioningState.Error("Wi-Fi scan failed: ${e?.message}")
        }
    }

    fun provisionDevice(ssid: String, password: String) {
        _provisioningState.value = ProvisioningState.Provisioning(
            StepStatus(inProgress = true),
            StepStatus(),
            StepStatus()
        )
        espDevice?.provision(ssid, password, provisionListener)
    }

    private val provisionListener = object : ProvisionListener {
        override fun createSessionFailed(e: Exception?) {
            _provisioningState.value = ProvisioningState.Error("Failed to create session: ${e?.message}")
        }

        override fun wifiConfigSent() {
            _provisioningState.value = ProvisioningState.Provisioning(
                StepStatus(isDone = true),
                StepStatus(inProgress = true),
                StepStatus()
            )
        }

        override fun wifiConfigFailed(e: Exception?) {
            _provisioningState.value = ProvisioningState.Error("Wi-Fi config failed: ${e?.message}")
        }

        override fun wifiConfigApplied() {
            _provisioningState.value = ProvisioningState.Provisioning(
                StepStatus(isDone = true),
                StepStatus(isDone = true),
                StepStatus(inProgress = true)
            )
        }

        override fun wifiConfigApplyFailed(e: Exception?) {
            _provisioningState.value = ProvisioningState.Error("Wi-Fi config apply failed: ${e?.message}")
        }

        override fun provisioningFailedFromDevice(failureReason: ESPConstants.ProvisionFailureReason?) {
            _provisioningState.value = ProvisioningState.Error("Provisioning failed from device: $failureReason")
        }

        override fun deviceProvisioningSuccess() {
            _provisioningState.value = ProvisioningState.Success
        }

        override fun onProvisioningFailed(e: Exception?) {
            _provisioningState.value = ProvisioningState.Error("Provisioning failed: ${e?.message}")
        }
    }
}
