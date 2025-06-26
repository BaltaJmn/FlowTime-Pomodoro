package com.baltajmn.flowtime.features.screens.pomodoro

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.CONTINUE_AFTER_BREAK_POMODORO
import com.baltajmn.flowtime.features.screens.common.PomodoroState
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

class PomodoroViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider,
    private val soundService: SoundService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroState())
    val uiState: StateFlow<PomodoroState> = _uiState.asStateFlow()

    private val timerScope = CoroutineScope(dispatcherProvider.io + SupervisorJob())
    private var timerJob: Job? = null
    private var breakJob: Job? = null

    fun startTimer() {
        cancelAllJobs()
        timerJob = timerScope.launch {
            _uiState.update { it.copy(isTimerRunning = true, isBreakRunning = false) }

            while (currentCoroutineContext().isActive && _uiState.value.seconds > 0) {
                delay(1000)
                _uiState.update { currentState ->
                    val newSeconds = currentState.seconds - 1
                    currentState.copy(
                        seconds = newSeconds,
                        secondsFormatted = newSeconds.formatSecondsToTime()
                    )
                }
            }

            if (currentCoroutineContext().isActive) {
                startBreak()
            }
        }
    }

    private fun startBreak() {
        updateMinutesStudying()
        soundService.playConfirmationSound()
        getTime()

        cancelAllJobs()
        breakJob = timerScope.launch {
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
        updateMinutesStudying(fromConfig = false)
        cancelAllJobs()
        isRunning()
        getTime()
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

    fun getPomodoroConfig() {
        _uiState.update {
            it.copy(
                range = dataProvider.getRangeModel(SharedPreferencesItem.POMODORO_RANGE)
                    ?: RangeModel(
                        totalRange = 45,
                        endRange = 45,
                        rest = 15
                    ),
                continueAfterBreak = dataProvider.getCheckValue(CONTINUE_AFTER_BREAK_POMODORO)
            )
        }
    }

    fun getTime() {
        val range = _uiState.value.range
        _uiState.update {
            it.copy(
                seconds = (range.endRange * 60).toLong(),
                secondsBreak = (range.rest * 60).toLong(),
                secondsFormatted = (range.endRange * 60).toLong().formatSecondsToTime()
            )
        }
    }

    fun getCurrentMinutes() = _uiState.update {
        it.copy(
            minutesStudying = dataProvider.updateMinutes(0).formatMinutesStudying()
        )
    }

    fun changeSwitch(value: Boolean) {
        dataProvider.setCheckValue(
            key = CONTINUE_AFTER_BREAK_POMODORO,
            value = value
        )
        _uiState.update {
            it.copy(
                continueAfterBreak = value
            )
        }
    }

    private fun updateMinutesStudying(fromConfig: Boolean = true) {
        val endRange = _uiState.value.range.endRange
        val seconds = _uiState.value.seconds

        val time = if (fromConfig) {
            endRange
        } else if (seconds > (endRange * 60) - 60) {
            0
        } else {
            (endRange * 60 - seconds) / 60
        }

        _uiState.update {
            it.copy(
                minutesStudying = dataProvider.updateMinutes(time.toLong()).formatMinutesStudying()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
    }
}