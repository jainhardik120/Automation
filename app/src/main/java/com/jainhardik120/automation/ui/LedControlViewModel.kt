package com.jainhardik120.automation.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jainhardik120.automation.data.ServiceConnector
import com.jainhardik120.automation.data.ServiceEvent
import com.jainhardik120.automation.data.ServiceState
import com.jainhardik120.automation.data.toCreateInstructionPacket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedControlViewModel @Inject constructor(
    private val serviceConnector: ServiceConnector
) : ViewModel() {

    var state by mutableStateOf(LedControlState())

    var serviceState by mutableStateOf(ServiceState())
        private set


    init {
        resetLedStates()
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
        viewModelScope.launch {
            serviceConnector.serviceState.collect { newServiceState ->
                serviceState = newServiceState
            }
        }
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
        serviceConnector.sendEvent(ServiceEvent.UpdateLedStates(state.ledStates))
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

    fun sendCommand(){
        val data = state.action.toCreateInstructionPacket(false)
        serviceConnector.sendEvent(ServiceEvent.SendKeypadData(data))
    }

    fun onActionEditorEvent(event : ActionEditorEvent){
        state = state.copy(action = getUpdatedActionState(state.action, event))
    }

}