package com.jainhardik120.automation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LedControlScreen(viewModel: LedControlViewModel) {
    val state = viewModel.state
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "LED Control")
        LazyColumn {
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
        }
    }
}