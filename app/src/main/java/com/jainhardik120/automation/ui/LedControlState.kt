package com.jainhardik120.automation.ui

data class LedControlState(
    val numLed: Int = 4,
    val ledStates: List<Boolean> = listOf()
)