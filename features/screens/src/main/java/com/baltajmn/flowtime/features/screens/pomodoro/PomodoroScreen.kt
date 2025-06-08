package com.baltajmn.flowtime.features.screens.pomodoro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.ComposableLifecycle
import com.baltajmn.flowtime.features.screens.common.PomodoroState
import com.baltajmn.flowtime.features.screens.common.composable.screen.AnimatedTimerContent
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerBaseScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = koinViewModel(),
    onTimerRunning: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            viewModel.getPomodoroConfig()
            viewModel.getTime()
            viewModel.getCurrentMinutes()
        }
    }

    onTimerRunning(state.isTimerRunning || state.isBreakRunning)

    AnimatedTimerContent(state) { timerState ->
        TimerBaseScreen(
            state = timerState,
            titleProvider = { s: PomodoroState, ctx ->
                when {
                    !s.isTimerRunning && !s.isBreakRunning -> ctx.getString(R.string.pomodoro_title)
                    s.isBreakRunning -> ctx.getString(R.string.time_title_resting)
                    else -> ctx.getString(R.string.time_title_working)
                }
            },
            onStartClick = { viewModel.startTimer() },
            onFinishClick = { viewModel.stopTimer() },
            onSwitchChanged = {
                viewModel.changeSwitch(
                    value = it
                )
            }
        )
    }
}