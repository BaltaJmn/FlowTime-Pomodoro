package com.baltajmn.flowtime.features.screens.settings

data class SettingsState(
    val isLoading: Boolean = false,
    val userLevel: Long = 0,
    val progressPercentage: Long = 0,
    val showAlert: Boolean = true
)