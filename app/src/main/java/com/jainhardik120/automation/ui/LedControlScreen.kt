package com.jainhardik120.automation.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LedControlScreen(viewModel: LedControlViewModel) {
    val state = viewModel.state
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Automation") }, actions = {
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
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column {
                        Button(onClick = {
                            viewModel.scanDevices()
                        }) {
                            Text("Scan Devices")
                        }
                    }
                }
                itemsIndexed(viewModel.serviceState.currentServiceState.deviceList) { _, item ->
                    OutlinedCard(
                        onClick = {
                            viewModel.connectToDevice(item.address)
                        }
                    ) {
                        Text(item.name ?: "N/A")
                        Text(item.address)
                    }
                }
                items(state.ledStates.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LED ${index + 1}",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.ledStates[index],
                            onCheckedChange = { viewModel.toggleLedState(index) },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                item {
                    Column {
                        ActionEditor(state.action, viewModel::onActionEditorEvent)
                        Button(viewModel::sendCommand) {
                            Text("Send Command")
                        }
                    }
                }
            }

        }
    }

}