package com.jainhardik120.automation.data.ble_service

import android.bluetooth.BluetoothGattCharacteristic

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