package com.jainhardik120.automation.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jainhardik120.automation.data.ServiceConnectionState
import com.jainhardik120.automation.data.ServiceConnector
import com.jainhardik120.automation.data.ServiceEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LedControlViewModel @Inject constructor(
    private val serviceConnector: ServiceConnector
) : ViewModel() {

    var state by mutableStateOf(LedControlState())

    var serviceState by mutableStateOf(ServiceConnectionState())
        private set

    companion object {
        private const val TAG = "LedControlViewModel"
    }

    init {
        viewModelScope.launch {
            launch {
                serviceConnector.connectionState.collect { newServiceState ->
                    serviceState = newServiceState
                }
            }
            launch {
                serviceConnector.notificationFlow.onEach {
                    Log.d(TAG, "NotificationFlow: ${it.characteristic.uuid}")
                }.collect()
            }
        }
        resetLedStates()
        startService()
    }

    fun startListening() {
        val gatt = serviceState.currentServiceState.bluetoothGatt ?: return
        serviceConnector.sendEvent(
            ServiceEvent.EnableNotifications(
                gatt.getService(
                    UUID.fromString(
                        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
                    )
                ).getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")), true
            )
        )
    }

    private fun resetLedStates() {
        val newLedStates = mutableListOf<Boolean>()
        for (i in 1..state.numLed) {
            newLedStates.add(false)
        }
        state = state.copy(
            ledStates = newLedStates
        )
    }

    fun startService() {
        serviceConnector.startServiceAndBind()
    }

    fun stopService() {
        serviceConnector.stopServiceAndUnbind()
    }

    fun scanDevices() {
        serviceConnector.sendEvent(ServiceEvent.ScanLeDevice)
    }

    fun connectToDevice(address: String) {
        serviceConnector.sendEvent(ServiceEvent.ConnectToDevice(address))
    }

    private fun sendData() {
        var byteValue = 0
        for (i in state.ledStates.indices) {
            if (state.ledStates[i]) {
                byteValue = byteValue or (1 shl i)
            }
        }
        val byteArray = byteArrayOf(byteValue.toByte())
        serviceState.currentServiceState.bluetoothGatt?.getService(
            UUID.fromString("4fafc202-1fb5-459e-8fcc-c5c9c331914b")
        )?.getCharacteristic(UUID.fromString("beb5483f-36e1-4688-b7f5-ea07361b26a8"))?.let {
            serviceConnector.sendEvent(
                ServiceEvent.SendData(
                    it, byteArray
                )
            )
        }
    }

    fun disconnect() {
        serviceConnector.sendEvent(ServiceEvent.DisconnectDevice)
    }

    fun toggleLedState(index: Int) {
        if (index >= state.numLed) {
            return
        }
        val newLedStates = state.ledStates.toMutableList().apply {
            this[index] = !this[index]
        }
        state = state.copy(
            ledStates = newLedStates
        )
        sendData()
    }

    fun onActionEditorEvent(event: ActionEditorEvent) {
        state = state.copy(action = getUpdatedActionState(state.action, event))
    }

}