package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.annotation.StringRes
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.timer.TimerMode

@get:StringRes
val TimerMode.label
    get() = when (this) {
        TimerMode.POMODORO -> R.string.mode_pomodoro
        TimerMode.FLOW_TIME -> R.string.mode_flow_time
        TimerMode.PERCENTAGE -> R.string.mode_percentage
    }

@get:StringRes
val TimerMode.advantages
    get() = when (this) {
        TimerMode.POMODORO -> R.string.pomodoro_advantages
        TimerMode.FLOW_TIME -> R.string.flow_time_advantages
        TimerMode.PERCENTAGE -> R.string.percentage_advantages
    }
