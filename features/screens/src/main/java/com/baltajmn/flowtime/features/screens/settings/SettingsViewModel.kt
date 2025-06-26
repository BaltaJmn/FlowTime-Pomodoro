package com.baltajmn.flowtime.features.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ALERT
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataProvider: DataProvider,
    private val getAllStudyTimeUseCase: GetAllStudyTimeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        getUserLevel()
    }

    private fun getUserLevel() {
        viewModelScope.launch {
            val userLevel = getAllStudyTimeUseCase()
            val xpBase = 100
            val xpPerHour = 50
            val xpTotal = (userLevel * xpPerHour).toInt()

            var level = 0
            var xpRequiredForCurrentLevel = 0
            var xpRequiredForNextLevel = xpBase

            while (xpTotal >= xpRequiredForNextLevel) {
                level++
                xpRequiredForCurrentLevel = xpRequiredForNextLevel
                xpRequiredForNextLevel = xpBase * (level + 1) * (level + 1)
            }

            val xpInCurrentLevel = xpTotal - xpRequiredForCurrentLevel
            val xpNeededInThisLevel = xpRequiredForNextLevel - xpRequiredForCurrentLevel
            val progressPercentage = (xpInCurrentLevel.toDouble() / xpNeededInThisLevel) * 100

            _uiState.update {
                it.copy(
                    userLevel = level.toLong(),
                    progressPercentage = progressPercentage.toLong(),
                    showAlert = dataProvider.getBoolean(SHOW_ALERT, true)
                )
            }
        }
    }

    fun saveColor(color: AppTheme) {
        dataProvider.setString(THEME_COLOR, color.name)
    }

    fun saveSound(showSound: Boolean) {
        dataProvider.setBoolean(SHOW_SOUND, showSound)
    }

    fun saveAlert(showAlert: Boolean) {
        dataProvider.setBoolean(SHOW_ALERT, showAlert)
        _uiState.update {
            it.copy(
                showAlert = showAlert
            )
        }
    }
}