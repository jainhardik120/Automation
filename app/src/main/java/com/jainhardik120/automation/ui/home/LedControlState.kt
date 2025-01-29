package com.jainhardik120.automation.ui.home

import com.jainhardik120.automation.data.database.entities.MacropadProfile
import com.jainhardik120.automation.utils.KeyAction
import com.jainhardik120.automation.utils.MacroAction

data class LedControlState(
    val numLed: Int = 4,
    val ledStates: List<Boolean> = listOf(),
    val action: KeyAction = KeyAction.Null,
    val macroActions: List<MacroAction> = listOf(),
    val allProfiles: List<MacropadProfile> = listOf()
)