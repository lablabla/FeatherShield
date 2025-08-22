package com.lablabla.feathershield.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.lablabla.feathershield.data.model.WifiCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BleProvisioningRepository @Inject constructor(
    private val context: Context
) {
    private val bluetoothAdapter : BluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val bleScanner = bluetoothAdapter.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private val provisioningServiceUuid = UUID.fromString("DAC5A304-6B9A-489A-96E4-481BBC080D40")
    private val provisioningCharacteristicUuid = UUID.fromString("8B7C0652-C6DD-449D-9E76-ED8A12903018")

    // This flow will hold the list of discovered devices
    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Add discovered device to the list
            val newDevices = _foundDevices.value + result.device
            _foundDevices.value = newDevices.distinctBy { it.address }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Device connected, now discover services
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Services discovered, now you can send/receive data
                // TODO: Implement logic to send Wi-Fi credentials
            }
        }
    }

    fun startScan() {
        _foundDevices.value = emptyList()
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(provisioningServiceUuid))
                .build()
        )
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner.startScan(filters, scanSettings, scanCallback)
    }

    fun stopScan() {
        bleScanner.stopScan(scanCallback)
    }

    fun connectDevice(device: BluetoothDevice) {
        stopScan()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun sendWifiCredentials(credentials: WifiCredentials) {
        val provisioningService = bluetoothGatt?.getService(provisioningServiceUuid)
        val characteristic = provisioningService?.getCharacteristic(provisioningCharacteristicUuid)

        if (characteristic != null) {
            val credentialsBytes = "${credentials.ssid},${credentials.password}".toByteArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(characteristic, credentialsBytes, credentialsBytes.size)
            }
            else {
                // The deprecated way for older SDKs
                characteristic.value = credentialsBytes
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.writeCharacteristic(characteristic)
            }

        } else {
            Log.e("BLE", "Provisioning characteristic not found!")
        }
    }
}
