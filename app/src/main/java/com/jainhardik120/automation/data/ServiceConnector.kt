package com.jainhardik120.automation.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity.BIND_AUTO_CREATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceConnector(
    private val context: Context
) {
    private var service: BleForegroundService? = null

    private val _internalServiceState = MutableStateFlow(ServiceState())
    val serviceState: StateFlow<ServiceState> = _internalServiceState

    fun sendEvent(event: ServiceEvent) = service?.onEvent(event)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            service = (binder as BleForegroundService.BLEBinder).getService()
            collectServiceState(binder.state)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            _internalServiceState.value = ServiceState()
            service = null
        }
    }

    private fun collectServiceState(serviceStateFlow: StateFlow<ServiceState>) {
        CoroutineScope(Dispatchers.IO).launch {
            serviceStateFlow.collect { newServiceState ->
                _internalServiceState.value = newServiceState
            }
        }
    }

    fun startServiceAndBind() {
        val intent = Intent(context, BleForegroundService::class.java)
        context.startService(intent)
        context.bindService(intent, connection, BIND_AUTO_CREATE)
    }

    fun stopServiceAndUnbind() {
        context.unbindService(connection)
        context.stopService(Intent(context, BleForegroundService::class.java))
    }

}