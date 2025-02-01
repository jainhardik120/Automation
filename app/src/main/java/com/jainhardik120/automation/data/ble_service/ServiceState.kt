package com.jainhardik120.automation.data.ble_service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt

data class ServiceState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val bluetoothGatt: BluetoothGatt? = null,
    val deviceList: List<BluetoothDevice> = emptyList()
)