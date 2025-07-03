package com.baltajmn.flowtime.features.screens.percentage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.features.screens.common.PercentageState
import com.baltajmn.flowtime.features.screens.common.composable.screen.AnimatedTimerContent
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerBaseScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PercentageScreen(
    viewModel: PercentageViewModel = koinViewModel(),
    onTimerRunning: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val isAnyTimerRunning = remember(state.isTimerRunning, state.isBreakRunning) {
        state.isTimerRunning || state.isBreakRunning
    }

    LaunchedEffect(Unit) {
        viewModel.getPercentageConfig()
        viewModel.getCurrentMinutes()
    }

    LaunchedEffect(isAnyTimerRunning) {
        onTimerRunning(isAnyTimerRunning)
    }

    AnimatedTimerContent(state) { timerState ->
        TimerBaseScreen(
            state = timerState,
            titleProvider = { s: PercentageState, ctx ->
                when {
                    s.isTimerRunning && !s.isBreakRunning -> ctx.getString(
                        R.string.time_title_working
                    )

                    !s.isTimerRunning && s.isBreakRunning -> ctx.getString(
                        R.string.time_title_resting
                    )
                    else -> ctx.getString(R.string.percentage_title)
                }
            },
            onStartClick = viewModel::startTimer,
            onBreakClick = viewModel::continueWithBreak,
            onFinishClick = viewModel::stopTimer,
            onSwitchChanged = viewModel::changeSwitch
        )
    }
}