package com.baltajmn.flowtime.features.screens.pomodoro

data class PomodoroState(
    val isLoading: Boolean = false,
    var isTimerRunning: Boolean = false,
    var isBreakRunning: Boolean = false,

    var seconds: Long = 0,
    var secondsBreak: Long = 0,

    var secondsFormatted: String = "00:00",
)