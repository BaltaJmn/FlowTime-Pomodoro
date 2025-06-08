package com.baltajmn.flowtime.features.screens.common

import com.baltajmn.flowtime.core.persistence.model.RangeModel

interface TimerState<T : TimerState<T>> {
    val isLoading: Boolean
    var isTimerRunning: Boolean
    var isBreakRunning: Boolean
    var continueAfterBreak: Boolean
    var secondsFormatted: String
    val minutesStudying: String
    var seconds: Long
    var secondsBreak: Long

    fun copyTimer(
        isLoading: Boolean = this.isLoading,
        isTimerRunning: Boolean = this.isTimerRunning,
        isBreakRunning: Boolean = this.isBreakRunning,
        continueAfterBreak: Boolean = this.continueAfterBreak,
        secondsFormatted: String = this.secondsFormatted,
        minutesStudying: String = this.minutesStudying,
        seconds: Long = this.seconds,
        secondsBreak: Long = this.secondsBreak
    ): T
}

data class FlowTimeState(
    override val isLoading: Boolean = false,
    override var isTimerRunning: Boolean = false,
    override var isBreakRunning: Boolean = false,
    override var continueAfterBreak: Boolean = true,
    val rangesList: List<RangeModel> = emptyList(),
    override var seconds: Long = 0,
    override var secondsBreak: Long = 0,
    override var secondsFormatted: String = "00:00",
    override val minutesStudying: String = "0 m"
) : TimerState<FlowTimeState> {
    override fun copyTimer(
        isLoading: Boolean,
        isTimerRunning: Boolean,
        isBreakRunning: Boolean,
        continueAfterBreak: Boolean,
        secondsFormatted: String,
        minutesStudying: String,
        seconds: Long,
        secondsBreak: Long
    ): FlowTimeState = copy(
        isLoading = isLoading,
        isTimerRunning = isTimerRunning,
        isBreakRunning = isBreakRunning,
        continueAfterBreak = continueAfterBreak,
        secondsFormatted = secondsFormatted,
        seconds = seconds,
        secondsBreak = secondsBreak
    )
}

data class PomodoroState(
    override val isLoading: Boolean = false,
    override var isTimerRunning: Boolean = false,
    override var isBreakRunning: Boolean = false,
    override var continueAfterBreak: Boolean = true,
    val range: RangeModel = RangeModel(totalRange = 45, endRange = 45, rest = 15),
    override var seconds: Long = 0,
    override var secondsBreak: Long = 0,
    override var secondsFormatted: String = "00:00",
    override val minutesStudying: String = "0 m"
) : TimerState<PomodoroState> {
    override fun copyTimer(
        isLoading: Boolean,
        isTimerRunning: Boolean,
        isBreakRunning: Boolean,
        continueAfterBreak: Boolean,
        secondsFormatted: String,
        minutesStudying: String,
        seconds: Long,
        secondsBreak: Long
    ): PomodoroState = copy(
        isLoading = isLoading,
        isTimerRunning = isTimerRunning,
        isBreakRunning = isBreakRunning,
        continueAfterBreak = continueAfterBreak,
        secondsFormatted = secondsFormatted,
        minutesStudying = minutesStudying,
        seconds = seconds,
        secondsBreak = secondsBreak
    )
}

data class PercentageState(
    override val isLoading: Boolean = false,
    override var isTimerRunning: Boolean = false,
    override var isBreakRunning: Boolean = false,
    override var continueAfterBreak: Boolean = true,
    var percentage: Long = 0,
    override var seconds: Long = 0,
    override var secondsBreak: Long = 0,
    override var secondsFormatted: String = "00:00",
    override val minutesStudying: String = "0 m"
) : TimerState<PercentageState> {
    override fun copyTimer(
        isLoading: Boolean,
        isTimerRunning: Boolean,
        isBreakRunning: Boolean,
        continueAfterBreak: Boolean,
        secondsFormatted: String,
        minutesStudying: String,
        seconds: Long,
        secondsBreak: Long
    ): PercentageState = copy(
        isLoading = isLoading,
        isTimerRunning = isTimerRunning,
        isBreakRunning = isBreakRunning,
        continueAfterBreak = continueAfterBreak,
        secondsFormatted = secondsFormatted,
        minutesStudying = minutesStudying,
        seconds = seconds,
        secondsBreak = secondsBreak
    )
}