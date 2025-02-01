package com.jainhardik120.automation.ui.profile_edit

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jainhardik120.automation.utils.ActionEditor

@Composable
fun ProfileEditScreen(
    viewModel: MacropadProfileEditViewModel
) {
    LazyColumn {
        itemsIndexed(viewModel.state.actions) { index, action ->
            ActionEditor(selectedAction = action, updateAction = {
                viewModel.updateActionAtIndex(it, index)
            })
        }
        item {
            Button({
                viewModel.insertActionAtEnd()
            }) {
                Text("Add New Action")
            }
        }
    }
}