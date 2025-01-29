package com.jainhardik120.automation.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ActionEditor(
    selectedAction: KeyAction,
    updateAction: (KeyAction) -> Unit
) {
    Column {
        MyDropDown(
            selectedAction = selectedAction,
            onActionChange = updateAction
        )

        // Conditionally render UI based on selected action
        when (selectedAction) {
            is KeyAction.WriteString -> {
                TextField(
                    value = selectedAction.stringNum.toString(),
                    onValueChange = {
                        updateAction(selectedAction.copy(stringNum = it.toIntOrNull() ?: 0))
                    },
                    label = { Text("String Number") }
                )
            }

            is KeyAction.MacroAction -> {
                selectedAction.actions.forEachIndexed { index, nestedAction ->
                    ActionEditor(nestedAction) { updatedNestedAction ->
                        val updatedActions = selectedAction.actions.toMutableList()
                            .apply { this[index] = updatedNestedAction }
                        updateAction(selectedAction.copy(actions = updatedActions))
                    }
                }
                Button({
                    updateAction(
                        selectedAction.copy(
                            noOfSteps = selectedAction.noOfSteps + 1,
                            actions = selectedAction.actions.toMutableList().apply {
                                this.add(KeyAction.Null)
                            })
                    )
                }) {
                    Text("Add Nested Action")
                }
            }

            is KeyAction.KeyCombo -> {
                TextField(
                    value = selectedAction.noOfKeys.toString(),
                    onValueChange = {
                        updateAction(
                            selectedAction.copy(
                                noOfKeys = it.toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Number of Keys") }
                )
                // Add logic to display/edit key combo
            }

            is KeyAction.SimSingleKey -> {
                TextField(
                    value = selectedAction.key.toString(),
                    onValueChange = {
                        updateAction(
                            selectedAction.copy(
                                key = it.toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Simulated Key") }
                )
            }

            is KeyAction.SingleKeyPress -> {
                TextField(
                    value = selectedAction.key.toString(),
                    onValueChange = {
                        updateAction(
                            selectedAction.copy(
                                key = it.toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Key") }
                )
            }

            is KeyAction.Delay -> {
                Row {
                    TextField(
                        value = selectedAction.multiplier.toString(),
                        onValueChange = {
                            updateAction(
                                selectedAction.copy(
                                    multiplier = it.toIntOrNull() ?: 0
                                )
                            )
                        },
                        label = { Text("Multiplier") }
                    )
                    TextField(
                        value = selectedAction.duration.toString(),
                        onValueChange = {
                            updateAction(
                                selectedAction.copy(
                                    duration = it.toIntOrNull() ?: 0
                                )
                            )
                        },
                        label = { Text("Duration") }
                    )
                }
            }

            is KeyAction.MouseMove -> {
                Row {
                    TextField(
                        value = selectedAction.x.toString(),
                        onValueChange = {
                            updateAction(
                                selectedAction.copy(
                                    x = it.toIntOrNull() ?: 0
                                )
                            )
                        },
                        label = { Text("X Coordinate") }
                    )
                    TextField(
                        value = selectedAction.y.toString(),
                        onValueChange = {
                            updateAction(
                                selectedAction.copy(
                                    y = it.toIntOrNull() ?: 0
                                )
                            )
                        },
                        label = { Text("Y Coordinate") }
                    )
                }
            }

            is KeyAction.MouseAction -> {
                // Add UI for selecting the mouse action (Click, Right Click, etc.)
            }

            is KeyAction.SendPacketToController -> {
                Text("No parameters required for SendPacketToController.")
            }

            is KeyAction.SendImmediateString -> {
                TextField(
                    value = selectedAction.string,
                    onValueChange = {
                        updateAction(selectedAction.copy(string = it))
                    },
                    label = { Text("Immediate String") }
                )
            }

            is KeyAction.Null -> {
                Text("Null action selected.")
            }
        }
    }
}

@Composable
fun MyDropDown(
    selectedAction: KeyAction,
    onActionChange: (KeyAction) -> Unit
) {
    val actionTypes = listOf(
        KeyAction.WriteString(0),
        KeyAction.MacroAction(0, emptyList()),
        KeyAction.KeyCombo(0, emptyList()),
        KeyAction.SimSingleKey(0),
        KeyAction.SingleKeyPress(0),
        KeyAction.Delay(1, 1000),
        KeyAction.MouseMove(0, 0),
        KeyAction.MouseAction(MouseActions.MOUSE_BUTTON_RIGHT),
        KeyAction.SendPacketToController,
        KeyAction.SendImmediateString(""),
        KeyAction.Null
    )

    var expanded by remember { mutableStateOf(false) }

    Box {
        Text(
            selectedAction::class.simpleName ?: "Select Action",
            modifier = Modifier.clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actionTypes.forEach { action ->
                DropdownMenuItem(onClick = {
                    onActionChange(action)
                    expanded = false
                }, text = {
                    Text(action::class.simpleName ?: "")
                })
            }
        }
    }
}
