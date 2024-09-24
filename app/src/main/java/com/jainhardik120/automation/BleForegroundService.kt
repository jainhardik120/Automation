package com.jainhardik120.automation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
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

        val scannedDevices : StateFlow<List<BluetoothDevice>>
            get() = _scannedDevices.asStateFlow()
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private fun start() {
        val notification = createNotification(0)
        startForeground(1, notification)
    }

    private fun createNotification(currentValue: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BLE Service Running")
            .setContentText("Current Value: $currentValue")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }


    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let {
                if(!_scannedDevices.value.contains(it)){
                    _scannedDevices.value+=it
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


//    private fun updateNotification(newValue: Int) {
//        val notification = createNotification(newValue)
//        val notificationManager =
//            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(1, notification) // Update the existing notification
//    }
//
//
//
////    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
////    private fun connectToDevice(macAddress: String) {
////        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
////        if (device == null) {
////            Log.e(TAG, "Device not found. Unable to connect.")
////            return
////        }
////        Log.d(TAG, "Connecting to $macAddress")
////        bluetoothGatt = device.connectGatt(this, false, gattCallback)
////    }
////
////    private val gattCallback = object : BluetoothGattCallback() {
////
////        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
////        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
////            super.onConnectionStateChange(gatt, status, newState)
////            if (newState == BluetoothProfile.STATE_CONNECTED) {
////                Log.d(TAG, "Connected to GATT server. Starting service discovery.")
////                gatt?.discoverServices()
////            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
////                Log.d(TAG, "Disconnected from GATT server.")
////            }
////        }
////
////        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
////        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
////            super.onServicesDiscovered(gatt, status)
////            if (status == BluetoothGatt.GATT_SUCCESS) {
////                Log.d(TAG, "Service discovery completed.")
////                val service = gatt?.getService(java.util.UUID.fromString(SERVICE_UUID))
////                gattCharacteristic =
////                    service?.getCharacteristic(java.util.UUID.fromString(GATT_CHARACTERISTIC_UUID))
////                if (gattCharacteristic != null) {
////                    if (gatt != null) {
////                        enableNotificationForCharacteristic(gatt, gattCharacteristic!!)
////                    }
////                }
////            } else {
////                Log.e(TAG, "Service discovery failed with status: $status")
////            }
////        }
////
////        override fun onCharacteristicChanged(
////            gatt: BluetoothGatt?,
////            characteristic: BluetoothGattCharacteristic?
////        ) {
////            super.onCharacteristicChanged(gatt, characteristic)
////            if (characteristic?.uuid == java.util.UUID.fromString(GATT_CHARACTERISTIC_UUID)) {
////                val value = characteristic?.getStringValue(0)
////                Log.d(TAG, "Characteristic notification received: $value")
////                if (value != null) {
////                    sendKeyNotification(value)
////                }
////            }
////        }
////    }
////
////    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
////    private fun enableNotificationForCharacteristic(
////        gatt: BluetoothGatt,
////        characteristic: BluetoothGattCharacteristic
////    ) {
////        gatt.setCharacteristicNotification(characteristic, true)
////        val descriptor =
////            characteristic.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
////        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
////        gatt.writeDescriptor(descriptor)
////    }
////
////    private fun sendKeyNotification(key: String) {
////        val notification = NotificationCompat.Builder(this, "running_channel")
////            .setSmallIcon(R.drawable.ic_launcher_foreground)
////            .setContentTitle("Key Pressed")
////            .setContentText("Key $key was pressed")
////            .build()
////
////        val notificationManager =
////            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
////        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
////    }
////
////    override fun onDestroy() {
////        if (ActivityCompat.checkSelfPermission(
////                this,
////                Manifest.permission.BLUETOOTH_CONNECT
////            ) == PackageManager.PERMISSION_GRANTED
////        ) {
////            bluetoothGatt?.disconnect()
////            bluetoothGatt?.close()
////        }
////        super.onDestroy()
////    }
//
//
////
////    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
////        return START_STICKY
////    }
////

}
