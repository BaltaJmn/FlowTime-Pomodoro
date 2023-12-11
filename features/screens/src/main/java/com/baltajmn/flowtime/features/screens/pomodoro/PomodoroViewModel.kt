package com.baltajmn.flowtime.features.screens.pomodoro

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider,
    private val soundService: SoundService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroState())
    val uiState: StateFlow<PomodoroState> = _uiState

    private val timerScope = CoroutineScope(dispatcherProvider.io)
    private var timerJob: Job? = null
    private var breakJob: Job? = null

    init {
        getPomodoroConfig()
    }

    fun startTimer() {
        getPomodoroConfig()
        breakJob?.cancel()
        timerJob?.cancel()
        timerJob = timerScope.launch {
            isRunning()
            do {
                delay(1000)
                val seconds = _uiState.value.seconds - 1
                _uiState.update {
                    it.copy(
                        seconds = seconds,
                        secondsFormatted = seconds.formatSecondsToTime()
                    )
                }
            } while (_uiState.value.seconds > 0)
            startBreak()
        }
    }

    private fun startBreak() {
        soundService.playConfirmationSound()
        updateMinutesStudying()

        timerJob?.cancel()
        breakJob?.cancel()
        breakJob = timerScope.launch {
            isRunning()
            do {
                delay(1000)
                val seconds = _uiState.value.secondsBreak - 1
                _uiState.update {
                    it.copy(
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
        breakJob?.cancel()
        timerJob?.cancel()

        isRunning()
        getPomodoroConfig()
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
        val range = dataProvider.getRangeModel(SharedPreferencesItem.POMODORO_RANGE)
            ?: RangeModel(
                totalRange = 45,
                endRange = 45,
                rest = 15
            )

        _uiState.update {
            it.copy(
                seconds = (range.endRange * 60).toLong(),
                secondsBreak = (range.rest * 60).toLong(),
                secondsFormatted = (range.endRange * 60).toLong().formatSecondsToTime()
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
        val range = dataProvider.getRangeModel(SharedPreferencesItem.POMODORO_RANGE)
            ?: RangeModel(
                totalRange = 45,
                endRange = 45,
                rest = 15
            )

        _uiState.update {
            it.copy(
                minutesStudying =
                dataProvider.updateMinutes(range.endRange.toLong()).formatMinutesStudying()
            )
        }
    }
}