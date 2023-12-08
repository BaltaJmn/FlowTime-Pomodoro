package com.baltajmn.flowtime.features.screens.flowtime

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
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
            isRunning()
            while (true) {
                delay(1000)
                val seconds = _uiState.value.seconds + 1
                _uiState.update {
                    it.copy(
                        seconds = seconds,
                        secondsFormatted = formatSecondsToTime(seconds)
                    )
                }
            }
        }
    }

    fun continueWithBreak() {
        soundService.playConfirmationSound()
        getFlowTimeConfig()

        timerJob?.cancel()
        breakJob?.cancel()
        breakJob = timerScope.launch {
            isRunning()
            resetTimer()
            do {
                delay(1000)
                val seconds = _uiState.value.secondsBreak - 1
                _uiState.update {
                    it.copy(
                        secondsBreak = seconds,
                        secondsFormatted = formatSecondsToTime(seconds)
                    )
                }
            } while (_uiState.value.secondsBreak > 0)

            soundService.playStartSound()
            startTimer()
        }
    }

    fun stopTimer() {
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
        var secondsBreak = 0
        val rangesList = dataProvider.getRangeModelList(FLOW_TIME_RANGE) ?: listOf(
            RangeModel(totalRange = 15, endRange = 15, rest = 5),
            RangeModel(totalRange = 30, endRange = 15, rest = 10),
            RangeModel(totalRange = 15, endRange = 15, rest = 15)
        )
        for (i in rangesList.indices) {
            if (i > 0) {
                if (_uiState.value.seconds > rangesList[i - 1].totalRange * 60 && _uiState.value.seconds < rangesList[i].totalRange * 60) {
                    secondsBreak = rangesList[i].rest * 60
                }
            } else {
                if (_uiState.value.seconds < rangesList[i].totalRange * 60) {
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

    private fun formatSecondsToTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        val formattedTime = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            String.format("%02d:%02d", minutes, remainingSeconds)
        }

        return formattedTime
    }

}