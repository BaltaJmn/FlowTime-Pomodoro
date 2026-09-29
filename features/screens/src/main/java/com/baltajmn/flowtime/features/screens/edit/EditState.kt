package com.baltajmn.flowtime.features.screens.edit

import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults

data class EditState(
    val isLoading: Boolean = false,
    val screenType: ScreenType = ScreenType.Pomodoro,
    val flowTimeRanges: MutableList<RangeModel> = TimerDefaults.flowTimeRanges().toMutableList(),
    val pomodoroRange: RangeModel = TimerDefaults.pomodoro(),
    val percentage: Long = TimerDefaults.PERCENTAGE
)