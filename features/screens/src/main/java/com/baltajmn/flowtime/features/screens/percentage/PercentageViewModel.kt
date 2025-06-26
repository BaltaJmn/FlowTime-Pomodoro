package com.baltajmn.flowtime.features.screens.percentage

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.features.screens.common.PercentageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PercentageViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider,
    private val soundService: SoundService
) : ViewModel() {
    private val _uiState = MutableStateFlow(PercentageState())
    val uiState: StateFlow<PercentageState> = _uiState.asStateFlow()

    private val timerScope = CoroutineScope(dispatcherProvider.io + SupervisorJob())
    private var timerJob: Job? = null
    private var breakJob: Job? = null

    fun startTimer() {
        cancelAllJobs()
        timerJob = timerScope.launch {
            _uiState.update { it.copy(isTimerRunning = true, isBreakRunning = false) }

            while (currentCoroutineContext().isActive) {
                delay(1000)
                _uiState.update { currentState ->
                    val newSeconds = currentState.seconds + 1
                    currentState.copy(
                        seconds = newSeconds,
                        secondsFormatted = newSeconds.formatSecondsToTime()
                    )
                }
            }
        }
    }

    fun continueWithBreak() {
        updateMinutesStudying()
        soundService.playConfirmationSound()
        getBreakTime()

        cancelAllJobs()
        breakJob = timerScope.launch {
            resetTimer()
            _uiState.update { it.copy(isTimerRunning = false, isBreakRunning = true) }

            while (currentCoroutineContext().isActive && _uiState.value.secondsBreak > 0) {
                delay(1000)
                _uiState.update { currentState ->
                    val newSeconds = currentState.secondsBreak - 1
                    currentState.copy(
                        secondsBreak = newSeconds,
                        secondsFormatted = newSeconds.formatSecondsToTime()
                    )
                }
            }

            if (currentCoroutineContext().isActive) {
                soundService.playStartSound()
                if (_uiState.value.continueAfterBreak) {
                    startTimer()
                } else {
                    stopTimer()
                }
            }
        }
    }

    fun stopTimer() {
        updateMinutesStudying()
        cancelAllJobs()
        isRunning()

        _uiState.update {
            it.copy(
                seconds = 0,
                secondsBreak = 0,
                secondsFormatted = "00:00",
                isTimerRunning = false,
                isBreakRunning = false
            )
        }
    }

    private fun cancelAllJobs() {
        timerJob?.cancel()
        breakJob?.cancel()
        timerJob = null
        breakJob = null
    }

    private fun isRunning() {
        timerJob?.isActive.let { isActive ->
            _uiState.update {
                it.copy(
                    isTimerRunning = isActive == true
                )
            }
        }

        breakJob?.isActive.let { isActive ->
            _uiState.update {
                it.copy(
                    isBreakRunning = isActive == true
                )
            }
        }
    }

    private fun resetTimer() {
        _uiState.update { it.copy(seconds = 0) }
    }

    fun getPercentageConfig() {
        _uiState.update {
            it.copy(
                percentage = dataProvider.getLong(SharedPreferencesItem.PERCENTAGE_RANGE),
                continueAfterBreak = dataProvider.getCheckValue(
                    SharedPreferencesItem.CONTINUE_AFTER_BREAK_PERCENTAGE
                )
            )
        }
    }

    private fun getBreakTime() {
        val secondsBreak: Long
        val percentage = _uiState.value.percentage
        val seconds = _uiState.value.seconds

        secondsBreak = (percentage * seconds) / 100

        _uiState.update {
            it.copy(
                seconds = 0,
                secondsBreak = secondsBreak
            )
        }
    }

    fun getCurrentMinutes() {
        _uiState.update {
            it.copy(
                minutesStudying = dataProvider.updateMinutes(0).formatMinutesStudying()
            )
        }
    }

    fun changeSwitch(value: Boolean) {
        dataProvider.setCheckValue(
            key = SharedPreferencesItem.CONTINUE_AFTER_BREAK_PERCENTAGE,
            value = value
        )
        _uiState.update {
            it.copy(
                continueAfterBreak = value
            )
        }
    }

    private fun updateMinutesStudying() {
        val minutesToAdd = _uiState.value.seconds / 60
        _uiState.update {
            it.copy(
                minutesStudying = dataProvider.updateMinutes(minutesToAdd).formatMinutesStudying()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
    }
}