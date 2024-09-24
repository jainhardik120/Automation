package com.jainhardik120.automation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.jainhardik120.automation.ui.theme.AutomationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val currentValue = MutableStateFlow(0)
    val isScanning = MutableStateFlow(false)
    private val bluetoothDeviceList = MutableStateFlow(emptyList<BluetoothDevice>())

    private var service: BleForegroundService? = null
    private var isBound = false
    private var isServiceRunning = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            service = (binder as BleForegroundService.BLEBinder).getService()
            isBound = true

            lifecycleScope.launch {
                binder.scannedDevices.collectLatest {
                    bluetoothDeviceList.value = it
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
            isBound = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
                0
            )
        }
        enableEdgeToEdge()
        setContent {
            val isS by isScanning.collectAsState()
            val dl by bluetoothDeviceList.collectAsState()
            AutomationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Scanning : $isS")
                        Button(onClick = { startServiceAndBind() }) {
                            Text("Start Service")
                        }
                        Button(onClick = { stopServiceAndUnbind() }) {
                            Text("Stop Service")
                        }
                        Button(onClick = {
                            service?.scanLeDevice()
                        }) {
                            Text("Scan Devices")
                        }
                        LazyColumn {
                            itemsIndexed(dl) { index, item ->
                                OutlinedCard(
                                    onClick = {
                                        service?.connectToDevice(item.address)
                                    }
                                ) {
                                    Text(item.name?:"N/A")
                                    Text(item.address)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Method to start the service and bind to it
    private fun startServiceAndBind() {
        if (!isServiceRunning) {
            val intent = Intent(applicationContext, BleForegroundService::class.java).apply {
                action = BleForegroundService.Actions.START.toString()
            }
            startService(intent)
            isServiceRunning = true
        }
        if (!isBound) {
            val intent = Intent(applicationContext, BleForegroundService::class.java)
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    // Method to stop the service and unbind from it
    private fun stopServiceAndUnbind() {
        if (isServiceRunning) {
            val intent = Intent(applicationContext, BleForegroundService::class.java).apply {
                action = BleForegroundService.Actions.STOP.toString()
            }
            startService(intent)
            isServiceRunning = false
        }
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind the service if it's still bound when the activity is destroyed
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
