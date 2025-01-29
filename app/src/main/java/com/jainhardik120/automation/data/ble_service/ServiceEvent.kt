package com.jainhardik120.automation.data.ble_service

import android.bluetooth.BluetoothGattCharacteristic

sealed class ServiceEvent{
    data object ScanLeDevice : ServiceEvent()
    data object DisconnectDevice : ServiceEvent()
    data class ConnectToDevice(val address : String) : ServiceEvent()
    data class SendData(val gattCharacteristic: BluetoothGattCharacteristic, val data: ByteArray) :
        ServiceEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SendData

            if (gattCharacteristic != other.gattCharacteristic) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = gattCharacteristic.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}