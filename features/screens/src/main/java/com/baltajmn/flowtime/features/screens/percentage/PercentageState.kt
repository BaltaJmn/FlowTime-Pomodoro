package com.baltajmn.flowtime.features.screens.percentage

data class PercentageState(
    val isLoading: Boolean = false,
    var isTimerRunning: Boolean = false,
    var isBreakRunning: Boolean = false,
    var continueAfterBreak: Boolean = true,
    var percentage: Long = 0,
    var seconds: Long = 0,
    var secondsBreak: Long = 0,
    var secondsFormatted: String = "00:00",
    val minutesStudying: String = "0 m"
)