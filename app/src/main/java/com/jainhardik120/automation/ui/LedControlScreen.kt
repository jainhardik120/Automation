package com.jainhardik120.automation.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@SuppressLint("MissingPermission")
@Composable
fun DeviceDialog(
    isScanning: Boolean,
    isConnected: Boolean,
    deviceList: List<BluetoothDevice>,
    onScanClick: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onConnectedDeviceClick: () -> Unit
) {
    Column {
        if (isConnected) {
            Button({
                onConnectedDeviceClick()
            }) {
                Text("Disconnect")
            }
        } else {
            LazyColumn {
                item {
                    Button({
                        onScanClick()
                    }) {
                        Text(if (isScanning) "Scanning..." else "Scan Devices")
                    }
                }
                itemsIndexed(deviceList) { _, item ->
                    OutlinedCard(
                        onClick = {
                            onDeviceClick(item)
                        }
                    ) {
                        Text(item.name ?: "N/A")
                        Text(item.address)
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LedControlScreen(viewModel: LedControlViewModel) {
    val state = viewModel.state
    val serviceState = viewModel.serviceState.currentServiceState
    val sheetState = rememberModalBottomSheetState()
    var bottomSheetDisplayed by remember { mutableStateOf(false) }
    if (bottomSheetDisplayed) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                bottomSheetDisplayed = false
            }
        ) {
            DeviceDialog(
                isScanning = serviceState.isScanning,
                isConnected = serviceState.isConnected,
                deviceList = serviceState.deviceList,
                onScanClick = { viewModel.scanDevices() },
                onDeviceClick = { device -> viewModel.connectToDevice(device.address) },
                onConnectedDeviceClick = { viewModel.disconnect() }
            )
        }
    }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Automation") }, actions = {
            IconButton({
                bottomSheetDisplayed = true
            }) {
                Icon(Icons.Default.Settings, "Device")
            }
            IconButton(onClick = {
                if (viewModel.serviceState.isBound) {
                    viewModel.stopService()
                } else {
                    viewModel.startService()
                }
            }) {
                if (viewModel.serviceState.isBound) {
                    Icon(Icons.Default.Close, "Stop Service")
                } else {
                    Icon(Icons.Default.Done, "Start Service")
                }
            }
        })
    }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

        }
    }
}