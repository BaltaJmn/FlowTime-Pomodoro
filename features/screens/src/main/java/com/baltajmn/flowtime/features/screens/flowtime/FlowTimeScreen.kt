package com.baltajmn.flowtime.features.screens.flowtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.features.screens.common.FlowTimeState
import com.baltajmn.flowtime.features.screens.common.composable.screen.AnimatedTimerContent
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerBaseScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun FlowTimeScreen(
    viewModel: FlowTimeViewModel = koinViewModel(),
    onTimerRunning: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val isAnyTimerRunning = remember(state.isTimerRunning, state.isBreakRunning) {
        state.isTimerRunning || state.isBreakRunning
    }

    LaunchedEffect(Unit) {
        viewModel.getFlowTimeConfig()
        viewModel.getCurrentMinutes()
    }

    LaunchedEffect(isAnyTimerRunning) {
        onTimerRunning(isAnyTimerRunning)
    }

    AnimatedTimerContent(state) { timerState ->
        TimerBaseScreen(
            state = timerState,
            titleProvider = { s: FlowTimeState, ctx ->
                when {
                    s.isTimerRunning && !s.isBreakRunning -> ctx.getString(R.string.time_title_working)
                    !s.isTimerRunning && s.isBreakRunning -> ctx.getString(R.string.time_title_resting)
                    else -> ctx.getString(R.string.flow_time_title)
                }
            },
            onStartClick = viewModel::startTimer,
            onBreakClick = viewModel::continueWithBreak,
            onFinishClick = viewModel::stopTimer,
            onSwitchChanged = viewModel::changeSwitch
        )
    }
}