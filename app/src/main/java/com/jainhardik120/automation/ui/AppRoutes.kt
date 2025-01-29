package com.jainhardik120.automation.ui

import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoutes {
    @Serializable
    data object LedControl : AppRoutes()

    @Serializable
    data object MacropadProfilesScreen : AppRoutes()

    @Serializable
    data class MacroPadProfileEditScreen(val id: Int = -1) : AppRoutes()

}