package com.baltajmn.flowtime.features.screens.settings

import com.baltajmn.flowtime.core.design.theme.Appearance

data class SettingsState(
    val isLoading: Boolean = false,
    val userLevel: Long = 0,
    val progressPercentage: Long = 0,
    val showAlert: Boolean = true,
    val keepScreenOn: Boolean = true,
    val appearance: Appearance = Appearance()
)