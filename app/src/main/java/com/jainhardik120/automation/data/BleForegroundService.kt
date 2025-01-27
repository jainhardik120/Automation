package com.jainhardik120.automation.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.jainhardik120.automation.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BluetoothCallbackData(
    val characteristic: BluetoothGattCharacteristic, val value: ByteArray
) {
    override fun equals(other: Any?) = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> {
            other as BluetoothCallbackData
            characteristic == other.characteristic && value.contentEquals(other.value)
        }
    }

    override fun hashCode() = 31 * characteristic.hashCode() + value.contentHashCode()
}

data class ServiceState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val bluetoothGatt: BluetoothGatt? = null,
    val deviceList: List<BluetoothDevice> = emptyList()
)

@AndroidEntryPoint
class BleForegroundService : Service() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val CHANNEL_ID = "ble_channel"
        const val CHANNEL_NAME = "Macro Pad Controller"
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val SCAN_TIMEOUT_MS = 10_000L

        private const val LAST_DEVICE_KEY = "last_device"
    }

    private val binder = BLEBinder()
    private val _state = MutableStateFlow(ServiceState())
    private val _notificationFlow = MutableSharedFlow<BluetoothCallbackData>(
        replay = 1,  // Buffer last value
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val bluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    private val bluetoothLeScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private var serviceRunning = false

    inner class BLEBinder : Binder() {
        val service: BleForegroundService get() = this@BleForegroundService

        val state: StateFlow<ServiceState>
            get() = _state
        val notificationFlow: SharedFlow<BluetoothCallbackData>
            get() = _notificationFlow

    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!serviceRunning) {
            serviceRunning = true
            startForegroundService()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences.getString(LAST_DEVICE_KEY, "")?.let {
            if (it.isEmpty()) return
            connectToDevice(it)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification("Waiting for device connection...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun createNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Automation Service Running").setContentText(text).setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false).build()
            .apply { flags = flags or Notification.FLAG_FOREGROUND_SERVICE }

    private fun updateNotification(text: String) {
        notificationManager.notify(1, createNotification(text))
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        bluetoothLeScanner ?: return

        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return

        val scanSettings =
            ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()

        if (!_state.value.isScanning) {
            updateScanState(true)
            _state.update { it.copy(deviceList = emptyList()) }
            bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
            MainScope().launch {
                delay(SCAN_TIMEOUT_MS)
                stopScan()
            }
        } else {
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        bluetoothLeScanner?.stopScan(leScanCallback)
        updateScanState(false)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                updateDeviceList(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            updateScanState(false)
        }
    }

    private fun updateScanState(isScanning: Boolean) {
        _state.update { it.copy(isScanning = isScanning) }
    }

    private fun updateDeviceList(device: BluetoothDevice) {
        _state.update { currentState ->
            currentState.copy(
                deviceList = if (device !in currentState.deviceList) currentState.deviceList + device
                else currentState.deviceList
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(address: String) {
        bluetoothAdapter?.getRemoteDevice(address)?.connectGatt(this, true, bluetoothGattCallback)
        sharedPreferences.edit().putString(LAST_DEVICE_KEY, address).apply()
    }

    @SuppressLint("MissingPermission")
    private fun disconnectDevice() {
        _state.value.bluetoothGatt?.disconnect()
        sharedPreferences.edit().remove(LAST_DEVICE_KEY).apply()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun writeData(
        characteristic: BluetoothGattCharacteristic, data: ByteArray
    ) {
        val gatt = _state.value.bluetoothGatt ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
        } else {
            characteristic.value = data
            gatt.writeCharacteristic(characteristic)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun enableNotification(
        characteristic: BluetoothGattCharacteristic, enabled: Boolean
    ) {
        val gatt = _state.value.bluetoothGatt ?: return
        gatt.setCharacteristicNotification(characteristic, enabled)
        characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)?.let { descriptor ->
            val value = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                descriptor.value = value
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            _state.update {
                it.copy(
                    isConnected = (newState == BluetoothProfile.STATE_CONNECTED),
                    bluetoothGatt = if (newState == BluetoothProfile.STATE_CONNECTED) gatt else null
                )
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
                updateNotification("Connected to ${gatt.device.name}")
            } else {
                updateNotification("Waiting for device connection...")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.update { it.copy(bluetoothGatt = gatt) }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray
        ) {
            _notificationFlow.tryEmit(BluetoothCallbackData(characteristic, value))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            _state.value.bluetoothGatt?.let { gatt ->
                gatt.disconnect()
                gatt.close()
            }
        }
        super.onDestroy()
    }

    private fun hasPermission(permission: String) =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    fun onEvent(event: ServiceEvent) {
        when (event) {
            is ServiceEvent.ConnectToDevice -> connectToDevice(event.address)
            is ServiceEvent.DisconnectDevice -> disconnectDevice()
            is ServiceEvent.ScanLeDevice -> scanLeDevice()
            is ServiceEvent.SendData -> writeData(event.gattCharacteristic, event.data)
            is ServiceEvent.EnableNotifications -> enableNotification(
                event.gattCharacteristic, event.enabled
            )
        }
    }
}