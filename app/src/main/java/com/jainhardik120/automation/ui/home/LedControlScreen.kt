package com.jainhardik120.automation.ui.home

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jainhardik120.automation.ui.ApplicationViewModel
import com.jainhardik120.automation.utils.MacroAction
import kotlinx.coroutines.CancellationException

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

@Composable
fun MacroPad(
    actions: List<MacroAction>,
    onEvent: (MacroPadEvent) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(actions.size + 1) { index: Int ->
            if (index == actions.size) {
                ActionButton(
                    text = "New",
                    onClick = {

                    },
                )
            } else {
                ActionButton(
                    text = actions[index].displayName,
                    onClick = { released ->
                        onEvent(MacroPadEvent.ActionClicked(actions[index], released))
                    }
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    text: String = "",
    onClick: (Boolean) -> Unit,
    shape: Shape = MaterialTheme.shapes.large,
    border: BorderStroke? = null
) {
    val haptic = LocalHapticFeedback.current
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        modifier = modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onClick(false)
                        try {
                            awaitRelease()
                            onClick(true)
                        } catch (e: CancellationException) {
                            onClick(true)
                        }
                    }
                )
            },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                Column(
                    Modifier
                        .size(96.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = {
                        Text(text = text, textAlign = TextAlign.Center)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LedControlScreen(viewModel: ApplicationViewModel) {
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
        Row(
            Modifier
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .clipToBounds()
        ) {
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
        }
    }, bottomBar = {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            contentColor = contentColorFor(BottomAppBarDefaults.containerColor),
            tonalElevation = BottomAppBarDefaults.ContainerElevation
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                    .padding(BottomAppBarDefaults.ContentPadding)
            ) {
                Text("Bottom Bar")
            }
        }

    }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MacroPad(viewModel.state.macroActions, viewModel::onEvent)
        }
    }
}