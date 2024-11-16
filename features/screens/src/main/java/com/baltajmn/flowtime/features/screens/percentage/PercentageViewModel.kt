package com.baltajmn.flowtime.features.screens.percentage

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PercentageViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider,
    private val soundService: SoundService
) : ViewModel() {
    private val _uiState = MutableStateFlow(PercentageState())
    val uiState: StateFlow<PercentageState> = _uiState

    private val timerScope = CoroutineScope(dispatcherProvider.io)
    private var timerJob: Job? = null
    private var breakJob: Job? = null

    fun startTimer() {
        breakJob?.cancel()
        timerJob?.cancel()
        timerJob = timerScope.launch {
            while (true) {
                delay(1)
                val seconds = _uiState.value.seconds + 1
                _uiState.update {
                    it.copy(
                        isTimerRunning = true,
                        isBreakRunning = false,
                        seconds = seconds,
                        secondsFormatted = seconds.formatSecondsToTime()
                    )
                }
            }
        }
    }

    fun continueWithBreak() {
        updateMinutesStudying()
        soundService.playConfirmationSound()
        getBreakTime()

        timerJob?.cancel()
        breakJob?.cancel()
        breakJob = timerScope.launch {
            resetTimer()
            do {
                delay(1000)
                val seconds = _uiState.value.secondsBreak - 1
                _uiState.update {
                    it.copy(
                        isTimerRunning = false,
                        isBreakRunning = true,
                        secondsBreak = seconds,
                        secondsFormatted = seconds.formatSecondsToTime()
                    )
                }
            } while (_uiState.value.secondsBreak > 0)

            soundService.playStartSound()

            if (_uiState.value.continueAfterBreak) {
                startTimer()
            } else {
                stopTimer()
            }
        }
    }

    fun stopTimer() {
        updateMinutesStudying()

        breakJob?.cancel()
        timerJob?.cancel()

        isRunning()

        _uiState.update {
            it.copy(
                seconds = 0,
                secondsBreak = 0,
                secondsFormatted = "00:00"
            )
        }
    }

    private fun isRunning() {
        timerJob?.isActive.let { isActive ->
            _uiState.update {
                it.copy(
                    isTimerRunning = isActive ?: false
                )
            }
        }

        breakJob?.isActive.let { isActive ->
            _uiState.update {
                it.copy(
                    isBreakRunning = isActive ?: false
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
            key = SharedPreferencesItem.CONTINUE_AFTER_BREAK_FLOW_TIME,
            value = value
        )
        _uiState.update {
            it.copy(
                continueAfterBreak = value
            )
        }
    }

    private fun updateMinutesStudying() {
        _uiState.update {
            it.copy(
                minutesStudying = dataProvider.updateMinutes((_uiState.value.seconds / 60))
                    .formatMinutesStudying()
            )
        }
    }
}