package com.jainhardik120.automation.data.ble_service

import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServiceConnector(
    private val context: Context
) {
    private var service: BleForegroundService? = null

    private val _connectionState = MutableStateFlow(ServiceConnectionState())
    val connectionState: StateFlow<ServiceConnectionState> = _connectionState.asStateFlow()

    private val _notificationStateFlow = MutableSharedFlow<BluetoothCallbackData>(
        replay = 1,  // Buffer last value
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val notificationFlow: SharedFlow<BluetoothCallbackData> = _notificationStateFlow.asSharedFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            binder?.let {
                val bleBinder = binder as BleForegroundService.BLEBinder
                service = bleBinder.service
                _connectionState.update {
                    it.copy(
                        isRunning = true,
                        isBound = true
                    )
                }
                collectServiceState(bleBinder)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            resetServiceState()
        }
    }

    init {
        checkInitialServiceStatus()
        bindServiceIfRunning()
    }

    private fun checkInitialServiceStatus() {
        if (isServiceRunningInSystem()) {
            _connectionState.update {
                it.copy(isRunning = true)
            }
        }
    }

    fun sendEvent(event: ServiceEvent) {
        service?.onEvent(event)
    }

    private fun collectServiceState(bleBinder: BleForegroundService.BLEBinder) {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        scope.launch {
            launch {
                bleBinder.state.collect { newServiceState ->
                    _connectionState.update {
                        it.copy(currentServiceState = newServiceState)
                    }
                }
            }
            launch {
                bleBinder.notificationFlow.collect {
                    _notificationStateFlow.emit(it)
                }
            }
        }
    }

    fun stopServiceAndUnbind() {
        val currentState = _connectionState.value
        if (currentState.isRunning) {
            try {
                if (currentState.isBound) {
                    context.unbindService(connection)
                }
                context.stopService(Intent(context, BleForegroundService::class.java))
                resetServiceState()
            } catch (e: Exception) {
                _connectionState.update {
                    it.copy(error = "Error stopping service: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun resetServiceState() {
        _connectionState.update {
            ServiceConnectionState(
                isRunning = false,
                isBound = false,
                currentServiceState = ServiceState()
            )
        }
        service = null
    }

    private fun isServiceRunningInSystem(): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        return jobScheduler.allPendingJobs.any {
            it.service.className == BleForegroundService::class.java.name
        }
    }

    private fun bindServiceIfRunning() {
        val currentState = _connectionState.value
        val intent = Intent(context, BleForegroundService::class.java)
        if (!currentState.isRunning) {
            return
        }
        try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            _connectionState.update {
                it.copy(error = "Error binding service: ${e.localizedMessage}")
            }
        }
    }

    fun startServiceAndBind() {
        val currentState = _connectionState.value
        val intent = Intent(context, BleForegroundService::class.java)
        if (!currentState.isRunning) {
            context.startService(intent)
        }
        try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            _connectionState.update {
                it.copy(error = "Error binding service: ${e.localizedMessage}")
            }
        }
    }
}