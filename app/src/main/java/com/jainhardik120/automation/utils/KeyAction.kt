package com.jainhardik120.automation.utils

sealed class KeyAction {
    data class WriteString(val stringNum: Int) : KeyAction()
    data class MacroAction(val noOfSteps: Int, val actions: List<KeyAction>) : KeyAction()
    data class KeyCombo(val noOfKeys: Int, val keys: List<Int>) : KeyAction()
    data class SimSingleKey(val key: Int) : KeyAction()
    data class SingleKeyPress(val key: Int) : KeyAction()
    data class Delay(val multiplier: Int, val duration: Int) : KeyAction()
    data class MouseMove(val x: Int, val y: Int) : KeyAction()
    data class MouseAction(val action: MouseActions) : KeyAction()
    data object SendPacketToController : KeyAction()
    data class SendImmediateString(val string: String) : KeyAction()
    data object Null : KeyAction()
}