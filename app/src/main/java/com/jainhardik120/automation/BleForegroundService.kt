package com.jainhardik120.automation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@AndroidEntryPoint
class BleForegroundService : Service() {

    enum class Actions {
        START, STOP
    }

    companion object {
        private const val TAG = "BluetoothService"
        const val CHANNEL_ID = "ble_channel"
        const val CHANNEL_NAME = "Macro pad controller"
        private const val GATT_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
        private const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        private const val LED_CHARACTERISTIC_UUID = "beb5483f-36e1-4688-b7f5-ea07361b26a8"
        private const val LED_SERVICE_UUID = "4fafc202-1fb5-459e-8fcc-c5c9c331914b"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private val binder = BLEBinder()
    private val _scannedDevices = MutableStateFlow(emptyList<BluetoothDevice>())

    inner class BLEBinder : Binder() {
        fun getService() = this@BleForegroundService

        val scannedDevices: StateFlow<List<BluetoothDevice>>
            get() = _scannedDevices.asStateFlow()
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private fun start() {
        val notification = createNotification("Waiting for device to connect...")
        startForeground(
            1, notification, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
        )
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


    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let {
                if (!_scannedDevices.value.contains(it)) {
                    _scannedDevices.value += it
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed: Scan failed $errorCode")
        }
    }

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


    private fun hasPermission(permission: String): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED)
    }


    private var scanning = false
    private val handler = Handler()


    @SuppressLint("MissingPermission")
    fun scanLeDevice() {
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
            handler.postDelayed({
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

    private fun sendLedState(state: Int) {
        ledCharacteristic?.let { characteristic ->
            val byteArray = byteArrayOf(state.toByte())
            characteristic.value = byteArray
            bluetoothGatt?.writeCharacteristic(characteristic)
            Log.d(TAG, "Sent LED state: $state")
        } ?: Log.e(TAG, "Characteristic is not available")
    }


    private fun enableNotificationForCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor =
            characteristic.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }


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
                keypadCharacteristic = keypadService?.getCharacteristic(java.util.UUID.fromString(GATT_CHARACTERISTIC_UUID))
                ledCharacteristic = ledService?.getCharacteristic(java.util.UUID.fromString(
                    LED_CHARACTERISTIC_UUID))
                if (keypadCharacteristic != null) {
                    enableNotificationForCharacteristic(gatt, keypadCharacteristic!!)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic?.uuid == java.util.UUID.fromString(GATT_CHARACTERISTIC_UUID)) {
                val value = characteristic?.getStringValue(0)
                Log.d(TAG, "Characteristic notification received: $value")
                if (value != null) {
                    sendKeyNotification(value)
                }
            }
        }
    }

    private var counter = 0

    private fun sendKeyNotification(key: String) {

        sendLedState(counter)
        counter++
        counter%=16

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Key Pressed")
            .setContentText("Key $key was pressed")
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun connectToDevice(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }

    override fun onDestroy() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        super.onDestroy()
    }
}
