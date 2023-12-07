package com.baltajmn.flowtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.features.screens.flowtime.FlowTimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.ThemeChange -> {
                _uiState.update { it.copy(theme = event.theme) }
            }
        }
    }

    sealed class MainEvent {
        data class ThemeChange(val theme: AppTheme) : MainEvent()
    }
}