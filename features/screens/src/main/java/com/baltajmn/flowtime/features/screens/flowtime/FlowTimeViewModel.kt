package com.baltajmn.flowtime.features.screens.flowtime

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlowTimeViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider,
    private val soundService: SoundService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlowTimeState())
    val uiState: StateFlow<FlowTimeState> = _uiState

    private val timerScope = CoroutineScope(dispatcherProvider.io)
    private var timerJob: Job? = null
    private var breakJob: Job? = null

    fun startTimer() {
        breakJob?.cancel()
        timerJob?.cancel()
        timerJob = timerScope.launch {
            while (true) {
                delay(1000)
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
            startTimer()
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

    fun getFlowTimeConfig() {
        _uiState.update {
            it.copy(
                rangesList = dataProvider.getRangeModelList(FLOW_TIME_RANGE) ?: listOf(
                    RangeModel(totalRange = 15, endRange = 15, rest = 5),
                    RangeModel(totalRange = 30, endRange = 15, rest = 10),
                    RangeModel(totalRange = 15, endRange = 15, rest = 15)
                )
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

    private fun updateMinutesStudying() {
        _uiState.update {
            it.copy(
                minutesStudying =
                dataProvider.updateMinutes((_uiState.value.seconds / 60)).formatMinutesStudying()
            )
        }
    }

}