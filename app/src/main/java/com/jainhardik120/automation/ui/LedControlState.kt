package com.jainhardik120.automation.ui

import com.jainhardik120.automation.data.KeyAction

data class LedControlState(
    val numLed: Int = 4,
    val ledStates: List<Boolean> = listOf(),
    val action : KeyAction = KeyAction.Null
)