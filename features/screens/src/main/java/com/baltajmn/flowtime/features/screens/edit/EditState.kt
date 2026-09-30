package com.baltajmn.flowtime.features.screens.edit

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.data.timer.TimerMode

data class EditState(
    val mode: TimerMode = TimerMode.POMODORO,
    val flowTimeRanges: MutableList<RangeModel> = TimerDefaults.flowTimeRanges().toMutableList(),
    val pomodoroRange: RangeModel = TimerDefaults.pomodoro(),
    val percentage: Long = TimerDefaults.PERCENTAGE,
    val continueAfterBreak: Boolean = true
)
