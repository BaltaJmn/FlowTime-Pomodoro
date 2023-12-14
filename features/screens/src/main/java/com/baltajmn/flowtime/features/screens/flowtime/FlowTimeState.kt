package com.baltajmn.flowtime.features.screens.flowtime

import com.baltajmn.flowtime.core.persistence.model.RangeModel

data class FlowTimeState(
    val isLoading: Boolean = false,

    var isTimerRunning: Boolean = false,
    var isBreakRunning: Boolean = false,

    val rangesList: List<RangeModel> = listOf(),

    var seconds: Long = 0,
    var secondsBreak: Long = 0,

    var secondsFormatted: String = "00:00",
    val minutesStudying: String = "0 m"
)