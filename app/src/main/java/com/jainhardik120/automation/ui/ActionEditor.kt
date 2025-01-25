package com.jainhardik120.automation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.jainhardik120.automation.data.KeyAction
import com.jainhardik120.automation.data.MouseActions

fun getUpdatedActionState(action: KeyAction, event: ActionEditorEvent): KeyAction {
    return when (event) {
        is ActionEditorEvent.UpdateWriteString -> KeyAction.WriteString(event.stringNum)
        is ActionEditorEvent.UpdateMacroAction -> KeyAction.MacroAction(
            event.noOfSteps,
            event.actions
        )
        is ActionEditorEvent.UpdateKeyCombo -> KeyAction.KeyCombo(event.noOfKeys, event.keys)
        is ActionEditorEvent.UpdateSimSingleKey -> KeyAction.SimSingleKey(event.key)
        is ActionEditorEvent.UpdateSingleKeyPress -> KeyAction.SingleKeyPress(event.key)
        is ActionEditorEvent.UpdateDelay -> KeyAction.Delay(event.multiplier, event.duration)
        is ActionEditorEvent.UpdateMouseMove -> KeyAction.MouseMove(event.x, event.y)
        is ActionEditorEvent.UpdateMouseAction -> KeyAction.MouseAction(event.action)
        is ActionEditorEvent.SendPacketToController -> KeyAction.SendPacketToController
        is ActionEditorEvent.UpdateSendImmediateString -> KeyAction.SendImmediateString(event.string)
        is ActionEditorEvent.Null -> KeyAction.Null
        is ActionEditorEvent.ChangeKeyActionType -> event.newAction
    }
}

sealed class ActionEditorEvent {
    data class UpdateWriteString(val stringNum: Int) : ActionEditorEvent()
    data class UpdateMacroAction(val noOfSteps: Int, val actions: List<KeyAction>) :
        ActionEditorEvent()

    data class UpdateKeyCombo(val noOfKeys: Int, val keys: List<Int>) : ActionEditorEvent()
    data class UpdateSimSingleKey(val key: Int) : ActionEditorEvent()
    data class UpdateSingleKeyPress(val key: Int) : ActionEditorEvent()
    data class UpdateDelay(val multiplier: Int, val duration: Int) : ActionEditorEvent()
    data class UpdateMouseMove(val x: Int, val y: Int) : ActionEditorEvent()
    data class UpdateMouseAction(val action: MouseActions) : ActionEditorEvent()
    object SendPacketToController : ActionEditorEvent()
    data class UpdateSendImmediateString(val string: String) : ActionEditorEvent()
    object Null : ActionEditorEvent()
    data class ChangeKeyActionType(val newAction: KeyAction) : ActionEditorEvent()
}

@Composable
fun ActionEditor(
    selectedAction: KeyAction,
    onEvent: (ActionEditorEvent) -> Unit
) {
    Column {
        MyDropDown(
            selectedAction = selectedAction,
            onActionChange = { newAction ->
                onEvent(ActionEditorEvent.ChangeKeyActionType(newAction))
            }
        )

        // Conditionally render UI based on selected action
        when (selectedAction) {
            is KeyAction.WriteString -> {
                val stringNum = (selectedAction as KeyAction.WriteString).stringNum
                TextField(
                    value = stringNum.toString(),
                    onValueChange = {
                        onEvent(
                            ActionEditorEvent.UpdateWriteString(
                                it.toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("String Number") }
                )
            }

            is KeyAction.MacroAction -> {
                val macroAction = selectedAction as KeyAction.MacroAction
                TextField(
                    value = macroAction.noOfSteps.toString(),
                    onValueChange = {
                        onEvent(
                            ActionEditorEvent.UpdateMacroAction(
                                it.toIntOrNull() ?: 0, macroAction.actions
                            )
                        )
                    },
                    label = { Text("Number of Steps") }
                )
                // Render nested key actions recursively
                macroAction.actions.forEachIndexed { index, nestedAction ->
                    ActionEditor(nestedAction) { event ->
                        val updatedActions = macroAction.actions.toMutableList()
                            .apply { this[index] = getUpdatedActionState(nestedAction, event) }
                        onEvent(
                            ActionEditorEvent.UpdateMacroAction(
                                macroAction.noOfSteps,
                                updatedActions
                            )
                        )
                    }
                }
            }

            is KeyAction.KeyCombo -> {
                val keyCombo = selectedAction as KeyAction.KeyCombo
                TextField(
                    value = keyCombo.noOfKeys.toString(),
                    onValueChange = {
                        onEvent(
                            ActionEditorEvent.UpdateKeyCombo(
                                it.toIntOrNull() ?: 0, keyCombo.keys
                            )
                        )
                    },
                    label = { Text("Number of Keys") }
                )
                // Add logic to display/edit key combo
            }

            is KeyAction.SimSingleKey -> {
                val key = (selectedAction as KeyAction.SimSingleKey).key
                TextField(
                    value = key.toString(),
                    onValueChange = {
                        onEvent(
                            ActionEditorEvent.UpdateSimSingleKey(
                                it.toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Simulated Key") }
                )
            }

            is KeyAction.SingleKeyPress -> {
                val key = (selectedAction as KeyAction.SingleKeyPress).key
                TextField(
                    value = key.toString(),
                    onValueChange = {
                        onEvent(
                            ActionEditorEvent.UpdateSingleKeyPress(
                                it.toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Key") }
                )
            }

            is KeyAction.Delay -> {
                val delay = selectedAction as KeyAction.Delay
                Row {
                    TextField(
                        value = delay.multiplier.toString(),
                        onValueChange = {
                            onEvent(
                                ActionEditorEvent.UpdateDelay(
                                    it.toIntOrNull() ?: 0, delay.duration
                                )
                            )
                        },
                        label = { Text("Multiplier") }
                    )
                    TextField(
                        value = delay.duration.toString(),
                        onValueChange = {
                            onEvent(
                                ActionEditorEvent.UpdateDelay(
                                    delay.multiplier,
                                    it.toIntOrNull() ?: 0
                                )
                            )
                        },
                        label = { Text("Duration") }
                    )
                }
            }

            is KeyAction.MouseMove -> {
                val mouseMove = selectedAction as KeyAction.MouseMove
                Row {
                    TextField(
                        value = mouseMove.x.toString(),
                        onValueChange = {
                            onEvent(
                                ActionEditorEvent.UpdateMouseMove(
                                    it.toIntOrNull() ?: 0, mouseMove.y
                                )
                            )
                        },
                        label = { Text("X Coordinate") }
                    )
                    TextField(
                        value = mouseMove.y.toString(),
                        onValueChange = {
                            onEvent(
                                ActionEditorEvent.UpdateMouseMove(
                                    mouseMove.x,
                                    it.toIntOrNull() ?: 0
                                )
                            )
                        },
                        label = { Text("Y Coordinate") }
                    )
                }
            }

            is KeyAction.MouseAction -> {
                val mouseAction = selectedAction as KeyAction.MouseAction
                // Add UI for selecting the mouse action (Click, Right Click, etc.)
            }

            is KeyAction.SendPacketToController -> {
                Text("No parameters required for SendPacketToController.")
            }

            is KeyAction.SendImmediateString -> {
                val string = (selectedAction as KeyAction.SendImmediateString).string
                TextField(
                    value = string,
                    onValueChange = { onEvent(ActionEditorEvent.UpdateSendImmediateString(it)) },
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
