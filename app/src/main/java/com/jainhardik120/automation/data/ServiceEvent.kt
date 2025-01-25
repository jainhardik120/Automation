package com.jainhardik120.automation.data

sealed class ServiceEvent{
    data class UpdateLedStates(val newStates : List<Boolean>) : ServiceEvent()
    data object ScanLeDevice : ServiceEvent()
    data class ConnectToDevice(val address : String) : ServiceEvent()
    data class SendKeypadData(val data : ByteArray) : ServiceEvent()
}