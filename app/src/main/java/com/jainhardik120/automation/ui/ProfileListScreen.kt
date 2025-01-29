package com.jainhardik120.automation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ProfileListScreen(
    viewModel: ApplicationViewModel,
    onProfileClick: (Int) -> Unit = {}
) {
    Column {
        Button({ onProfileClick(-1) }) {
            Text("New Profile")
        }
        LazyColumn {

        }
    }
}