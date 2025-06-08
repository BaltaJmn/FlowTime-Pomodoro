package com.baltajmn.flowtime.features.screens.common

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class BaseTimerViewModel<T : TimerState<T>>(
    protected val dispatcherProvider: DispatcherProvider,
    protected val dataProvider: DataProvider,
    protected val soundService: SoundService
) : ViewModel() {

    protected val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<T> get() = _uiState

    protected val timerScope = CoroutineScope(dispatcherProvider.io)
    protected var timerJob: Job? = null
    protected var breakJob: Job? = null

    abstract fun initialState(): T

    // Función abstracta para iniciar el timer, según la lógica de cada pantalla
    abstract fun startTimer()

    // Función abstracta para manejar el break
    abstract fun handleBreak()

    fun stopTimer() {
        updateMinutesStudying()
        breakJob?.cancel()
        timerJob?.cancel()
        updateRunningStatus()
        _uiState.update { state ->
            state.copyTimer(
                seconds = 0,
                secondsBreak = 0,
                secondsFormatted = "00:00"
            )
        }
    }

    protected fun updateRunningStatus() {
        timerJob?.isActive.let { isActive ->
            _uiState.update { state -> state.copyTimer(isTimerRunning = isActive ?: false) }
        }
        breakJob?.isActive.let { isActive ->
            _uiState.update { state -> state.copyTimer(isBreakRunning = isActive ?: false) }
        }
    }

    fun getCurrentMinutes() {
        _uiState.update { state ->
            state.copyTimer(
                minutesStudying = dataProvider.updateMinutes(0).formatMinutesStudying()
            )
        }
    }

    protected open fun updateMinutesStudying() {
        _uiState.update { state ->
            state.copyTimer(
                minutesStudying = dataProvider.updateMinutes(state.seconds / 60)
                    .formatMinutesStudying()
            )
        }
    }
}
