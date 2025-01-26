package com.jainhardik120.automation.data

import android.bluetooth.BluetoothGattCharacteristic

sealed class ServiceEvent{
    data object ScanLeDevice : ServiceEvent()
    data class ConnectToDevice(val address : String) : ServiceEvent()
    data class EnableNotifications(
        val gattCharacteristic: BluetoothGattCharacteristic,
        val enabled: Boolean
    ) : ServiceEvent()

    data class SendData(val gattCharacteristic: BluetoothGattCharacteristic, val data: ByteArray) :
        ServiceEvent()
}