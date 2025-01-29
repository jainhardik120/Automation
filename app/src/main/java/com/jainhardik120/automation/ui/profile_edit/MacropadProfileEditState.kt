package com.jainhardik120.automation.ui.profile_edit

import com.jainhardik120.automation.utils.KeyAction

data class MacropadProfileEditState(
    val selectedProfileId: Int = -1,
    val actions: List<KeyAction> = listOf(),
    val strings: List<String> = listOf(),
    val name: String = ""
)