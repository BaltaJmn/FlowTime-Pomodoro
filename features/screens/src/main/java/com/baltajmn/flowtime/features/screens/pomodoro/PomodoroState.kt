package com.baltajmn.flowtime.features.screens.pomodoro

import com.baltajmn.flowtime.core.persistence.model.RangeModel

data class PomodoroState(
    val isLoading: Boolean = false,

    var isTimerRunning: Boolean = false,
    var isBreakRunning: Boolean = false,

    val range: RangeModel = RangeModel(totalRange = 45, endRange = 45, rest = 15),

    var seconds: Long = 0,
    var secondsBreak: Long = 0,

    var secondsFormatted: String = "00:00",
    val minutesStudying: String = "0 m"
)