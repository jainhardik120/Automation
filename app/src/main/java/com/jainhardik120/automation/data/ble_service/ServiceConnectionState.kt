package com.jainhardik120.automation.data.ble_service

data class ServiceConnectionState(
    val isRunning: Boolean = false,
    val isBound: Boolean = false,
    val currentServiceState: ServiceState = ServiceState(),
    val error: String? = null
)