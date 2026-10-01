package com.baltajmn.flowtime.features.screens.common.composable.components

import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.core.design.resources.*

val TimerMode.label
    get() = when (this) {
        TimerMode.POMODORO -> Res.string.mode_pomodoro
        TimerMode.FLOW_TIME -> Res.string.mode_flow_time
        TimerMode.PERCENTAGE -> Res.string.mode_percentage
    }

val TimerMode.advantages
    get() = when (this) {
        TimerMode.POMODORO -> Res.string.pomodoro_advantages
        TimerMode.FLOW_TIME -> Res.string.flow_time_advantages
        TimerMode.PERCENTAGE -> Res.string.percentage_advantages
    }
