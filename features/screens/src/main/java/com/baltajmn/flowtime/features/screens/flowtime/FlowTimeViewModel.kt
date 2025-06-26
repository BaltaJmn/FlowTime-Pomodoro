package com.baltajmn.flowtime.features.screens.flowtime

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.CONTINUE_AFTER_BREAK_FLOW_TIME
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.features.screens.common.FlowTimeState
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

class FlowTimeViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider,
    private val soundService: SoundService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlowTimeState())
    val uiState: StateFlow<FlowTimeState> = _uiState.asStateFlow()

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

    fun getFlowTimeConfig() {
        _uiState.update {
            it.copy(
                rangesList = dataProvider.getRangeModelList(FLOW_TIME_RANGE) ?: listOf(
                    RangeModel(totalRange = 15, endRange = 15, rest = 5),
                    RangeModel(totalRange = 30, endRange = 15, rest = 10),
                    RangeModel(totalRange = 15, endRange = 15, rest = 15)
                ),
                continueAfterBreak = dataProvider.getCheckValue(CONTINUE_AFTER_BREAK_FLOW_TIME)
            )
        }
    }

    private fun getBreakTime() {
        var secondsBreak = 0
        val rangesList = _uiState.value.rangesList
        val seconds = _uiState.value.seconds

        for (i in rangesList.indices) {
            if (i > 0) {
                if (seconds > rangesList[i - 1].totalRange * 60 && seconds < rangesList[i].totalRange * 60) {
                    secondsBreak = rangesList[i].rest * 60
                }
            } else {
                if (seconds < rangesList[i].totalRange * 60) {
                    secondsBreak = rangesList[i].rest * 60
                }
            }
        }

        if (secondsBreak == 0) secondsBreak = 900

        _uiState.update {
            it.copy(
                seconds = 0,
                secondsBreak = secondsBreak.toLong()
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
        dataProvider.setCheckValue(key = CONTINUE_AFTER_BREAK_FLOW_TIME, value = value)
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