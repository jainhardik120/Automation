package com.jainhardik120.automation.ui.home

import com.jainhardik120.automation.utils.MacroAction

sealed class MacroPadEvent {
    data class ActionClicked(val action: MacroAction, val released: Boolean) : MacroPadEvent()
}