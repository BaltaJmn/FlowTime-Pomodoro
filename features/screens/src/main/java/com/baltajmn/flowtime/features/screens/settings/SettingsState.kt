package com.baltajmn.flowtime.features.screens.settings

import com.baltajmn.flowtime.core.persistence.model.RangeModel

data class SettingsState(
    val isLoading: Boolean = false,
    val userLevel: Long = 0,
    val progressPercentage: Long = 0,
    val flowTimeRanges: MutableList<RangeModel> = mutableListOf(
        RangeModel(totalRange = 15, endRange = 15, rest = 5),
        RangeModel(totalRange = 30, endRange = 15, rest = 10),
        RangeModel(totalRange = 45, endRange = 15, rest = 15)
    ),
    val pomodoroRange: RangeModel = RangeModel(totalRange = 25, endRange = 25, rest = 5),
    val percentage: Long = 0
)