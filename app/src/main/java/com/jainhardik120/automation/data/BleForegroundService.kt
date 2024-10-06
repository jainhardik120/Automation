package com.jainhardik120.automation.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.jainhardik120.automation.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ServiceState(
    val deviceList: List<BluetoothDevice> = emptyList()
)

@AndroidEntryPoint
class BleForegroundService : Service() {

    companion object {
        private const val TAG = "BluetoothService"
        const val CHANNEL_ID = "ble_channel"
        const val CHANNEL_NAME = "Macro pad controller"
        private const val GATT_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
        private const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        private const val LED_CHARACTERISTIC_UUID = "beb5483f-36e1-4688-b7f5-ea07361b26a8"
        private const val LED_SERVICE_UUID = "4fafc202-1fb5-459e-8fcc-c5c9c331914b"
    }

    private val binder = BLEBinder()
    private val _state = MutableStateFlow(ServiceState())
    private val bluetoothManager by lazy {
        this.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val bluetoothLeScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    private val notificationManager by lazy {
        this.getSystemService(NotificationManager::class.java)
    }
    private var bluetoothGatt: BluetoothGatt? = null
    private var keypadCharacteristic: BluetoothGattCharacteristic? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null

    private var scanning = false
    private var handler: Handler? = null

    private var serviceRunning = false


    private fun hasPermission(permission: String): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED)
    }

    inner class BLEBinder : Binder() {
        fun getService() = this@BleForegroundService

        val state: StateFlow<ServiceState>
            get() = _state
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!serviceRunning) {
            serviceRunning = true
            start()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BLE Service Running")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun start() {
        val notification = createNotification("Waiting for device to connect...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let {
                if (!_state.value.deviceList.contains(it)) {
                    Log.d(TAG, "onScanResult: Value in service")
                    val updatedDeviceList = _state.value.deviceList + it
                    _state.value = _state.value.copy(
                        deviceList = updatedDeviceList
                    )
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed: Scan failed $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "scanLeDevice: Bluetooth Scanner not found")
            return
        }
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "scanLeDevice: Permission not granted")
            return
        }
        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        if (!scanning) {
            handler = Looper.myLooper()?.let { Handler(it) }
            handler?.postDelayed({
                scanning = false
                bluetoothLeScanner!!.stopScan(leScanCallback)
            }, 10000)
            scanning = true
            bluetoothLeScanner!!.startScan(null, scanSettings, leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner!!.stopScan(leScanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendLedState(state: ByteArray) {
        ledCharacteristic?.let { characteristic ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    state,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            } else {
                characteristic.value = state
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotificationForCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor =
            characteristic.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            var updatedNotification: Notification? = null
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                updatedNotification = createNotification("Device connected")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updatedNotification = createNotification("Device disconnected")
            }
            updatedNotification?.let {
                notificationManager.notify(1, updatedNotification)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (gatt != null && status == BluetoothGatt.GATT_SUCCESS) {
                val keypadService = gatt.getService(java.util.UUID.fromString(SERVICE_UUID))
                val ledService = gatt.getService(java.util.UUID.fromString(LED_SERVICE_UUID))
                keypadCharacteristic = keypadService?.getCharacteristic(
                    java.util.UUID.fromString(GATT_CHARACTERISTIC_UUID)
                )
                ledCharacteristic = ledService?.getCharacteristic(
                    java.util.UUID.fromString(
                        LED_CHARACTERISTIC_UUID
                    )
                )
                if (keypadCharacteristic != null) {
                    enableNotificationForCharacteristic(gatt, keypadCharacteristic!!)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == java.util.UUID.fromString(GATT_CHARACTERISTIC_UUID)) {
                val stringValue = value.toString(Charsets.UTF_8)
//                sendLedState(counter)
//                counter++
//                counter %= 16
                sendKeyNotification(stringValue)
            }
        }
    }


    private fun sendKeyNotification(key: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Key Pressed")
            .setContentText("Key $key was pressed")
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(address: String){
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service Destroyed")
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        }
        super.onDestroy()
    }

    fun onEvent(event: ServiceEvent) {
        when (event) {
            is ServiceEvent.UpdateLedStates -> {
                var byteValue = 0
                for (i in event.newStates.indices) {
                    if (event.newStates[i]) {
                        byteValue = byteValue or (1 shl i)
                    }
                }
                val byteArray = byteArrayOf(byteValue.toByte())
                sendLedState(byteArray)
            }

            is ServiceEvent.ConnectToDevice -> {
                connectToDevice(event.address)
            }
            ServiceEvent.ScanLeDevice -> {
                scanLeDevice()
            }
        }
    }
}

